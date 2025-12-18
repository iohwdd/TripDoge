package com.tripdog.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dev.langchain4j.data.message.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.dashscope.tokenizers.Tokenizer;
import com.alibaba.dashscope.tokenizers.TokenizerFactory;
import com.google.common.reflect.TypeToken;
import com.tripdog.ai.compress.CompressionService;
import com.tripdog.common.Constants;
import com.tripdog.common.utils.JsonUtil;
import com.tripdog.mapper.ChatHistoryMapper;
import com.tripdog.model.entity.ChatHistoryDO;
import com.tripdog.model.builder.ConversationBuilder;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.tripdog.common.Constants.INJECT_TEMPLATE;

/**
 * @author: iohw
 * @date: 2025/4/13 10:35
 * @description:
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistentChatMemoryStore implements ChatMemoryStore {
    final ChatHistoryMapper chatHistoryMapper;
    private final Tokenizer tokenizer = TokenizerFactory.qwen();
    private final String USER = "user";
    private final String ASSISTANT = "assistant";
    private final String SYSTEM = "system";
    private final String TOOL = "tool";
    private final CompressionService compressionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ChatMessage> getMessages(Object o) {
        String conversationId = (String) o;
        List<ChatHistoryDO> chatHistoryDOS = chatHistoryMapper.selectAllById(conversationId);
        List<ChatMessage> chatMessages = new ArrayList<>();


        for (ChatHistoryDO d : chatHistoryDOS) {
            // 上下文使用检索增强后的内容
            String content = StringUtils.hasText(d.getEnhancedContent()) ? d.getEnhancedContent() : d.getContent();

            switch (d.getRole()) {
                case USER:
                    // 重建用户消息，需要判断是否有附件
                    if (StringUtils.hasText(d.getAttachmentPath())) {
                        // 构建包含文本和附件的多部分消息（参照图片附件持久化方式）
                        List<Content> contents = new ArrayList<>();
                        if (StringUtils.hasText(content)) {
                            contents.add(TextContent.from(content));
                        }
                        // 统一使用 ImageContent 包装附件（文件、图片等都用这个）
                        contents.add(ImageContent.from(d.getAttachmentPath()));
                        chatMessages.add(UserMessage.from(contents));
                    } else {
                        chatMessages.add(UserMessage.from(content));
                    }
                    break;
                case ASSISTANT:
                    if(StringUtils.hasText(d.getToolExecResult())) {
                        chatMessages.add(ToolExecutionResultMessage.from("id","toolName",d.getToolExecResult()));
                    } else {
                        if(StringUtils.hasText(d.getToolCall())) {
                            TypeToken<List<ToolExecutionRequest>> toolCalls = new TypeToken<>() {};
                            String toolCallJson = d.getToolCall();
                            chatMessages.add(AiMessage.from(JsonUtil.fromJsonList(toolCallJson, toolCalls)));
                        }else {
                            chatMessages.add(AiMessage.from(content));
                        }
                    }

                    break;
                case SYSTEM:
                    chatMessages.add(SystemMessage.from(content));
                    break;
            }
        }


        // 压缩处理
        return compressionService.compress(chatMessages);
    }

    @Override
    public void updateMessages(Object o, List<ChatMessage> list) {
        String conversationId = o.toString();
        ChatMessage latestMessage = list.getLast();
        String role = getRoleFromMessage(latestMessage);

        String message = getContentMessage(latestMessage);

        boolean isToolCall = false;
        ChatHistoryDO chatHistoryDO;
        if (Constants.USER.equals(role)) {
            chatHistoryDO = ConversationBuilder.buildUserMessage(conversationId, (UserMessage) latestMessage);
        } else if (Constants.ASSISTANT.equals(role)) {
            AiMessage aiMessage = (AiMessage) latestMessage;
            if(aiMessage.hasToolExecutionRequests()) {
                isToolCall = true;
                chatHistoryDO = ConversationBuilder.buildToolCallMessage(conversationId, aiMessage);
            }else {
                chatHistoryDO = ConversationBuilder.buildAssistantMessage(conversationId, message);
            }
        } else if (Constants.TOOL.equals(role)) {
            isToolCall = true;
            chatHistoryDO = ConversationBuilder.buildToolExecResultMessage(conversationId, (ToolExecutionResultMessage) latestMessage);
        } else {
            chatHistoryDO = ConversationBuilder.buildSystemMessage(conversationId, message);
        }

        if(!isToolCall) {
            String content = chatHistoryDO.getContent();
            if(isEnhanced(content)) {
                // 保存增强后的完整内容到 enhanced_content 字段
                chatHistoryDO.setEnhancedContent(content);
                // 原始内容提取并保存到 content 字段
                chatHistoryDO.setContent(extractOrigin(content));
            }
        }
        chatHistoryMapper.insert(chatHistoryDO);
    }

    @Override
    public void deleteMessages(Object o) {
        // 模型记忆删除不清表库数据
        // String conversationId = o.toString();
        // chatHistoryMapper.deleteByConversationId(conversationId);
    }


    private String getRoleFromMessage(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return SYSTEM;
        } else if (message instanceof UserMessage) {
            return USER;
        } else if (message instanceof AiMessage) {
            return ASSISTANT;
        } else if (message instanceof ToolExecutionResultMessage) {
            return TOOL;
        } else if (message instanceof CustomMessage) {
            return "custom";
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass().getName());
    }

    private String getContentMessage(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        } else if (message instanceof UserMessage userMessage) {
            // 处理多部分内容：只提取文本部分，忽略图片等其他内容
            List<Content> contents = userMessage.contents();
            if (contents != null && !contents.isEmpty()) {
                // 查找第一个文本内容
                for (Content content : contents) {
                    if (content.type().equals(ContentType.TEXT)) {
                        // 正确提取 TextContent 的文本内容，而不是 toString()
                        return ((TextContent) content).text();
                    }
                }
                // 如果没有找到文本内容，返回空（多部分消息的附件不应作为文本内容）
                return null;
            }
            // 降级处理：尝试使用 singleText()，仅当只有单个文本内容时成功
            try {
                return userMessage.singleText();
            } catch (IllegalStateException e) {
                // 多部分内容会抛异常，此时返回 null
                return null;
            }
        } else if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) message;
            // 如果AI消息包含工具调用请求，需要序列化保存
            // if (aiMessage.hasToolExecutionRequests()) {
            //     return serializeAiMessageWithToolCalls(aiMessage);
            // } else {
            //     return aiMessage.text();
            // }
            if (aiMessage.text() != null) {
                // fixme 使用智谱模型时，回复内容前缀有一大串null字符串，疑似langchain4j的bug
                return aiMessage.text().replace("null", "");
            }
            // 工具调用无内容
            return null;
        } else if (message instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) message;
            return serializeToolExecutionResult(toolMsg);
        } else if (message instanceof CustomMessage) {
            // 自定义消息可能需要JSON序列化
            return ((CustomMessage) message).toString();
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass().getName());
    }

    private String extractOrigin(String content) {
        int i = content.indexOf(INJECT_TEMPLATE);
        return i == -1 ? content : content.substring(i + INJECT_TEMPLATE.length());
    }

    private boolean isEnhanced(String content) {
        return content.contains(INJECT_TEMPLATE);
    }

    /**
     * 序列化工具执行结果
     */
    private String serializeToolExecutionResult(ToolExecutionResultMessage toolMsg) {
        try {
            var resultData = new HashMap<String, Object>();
            resultData.put("id", toolMsg.id());
            resultData.put("toolName", toolMsg.toolName());
            resultData.put("text", toolMsg.text());
            return objectMapper.writeValueAsString(resultData);
        } catch (JsonProcessingException e) {
            log.error("序列化工具执行结果失败", e);
            return String.format("{\"id\":\"%s\",\"toolName\":\"%s\",\"text\":\"%s\"}",
                toolMsg.id(), toolMsg.toolName(), toolMsg.text());
        }
    }

}