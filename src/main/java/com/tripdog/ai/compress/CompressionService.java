package com.tripdog.ai.compress;

import dev.langchain4j.data.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import com.alibaba.dashscope.tokenizers.TokenizerFactory;
import com.tripdog.ai.assistant.CompressAssistant;

/**
 * Mock压缩实现：
 * 逻辑：
 * 1. 若未启用或消息条数不足或token未超预算 → 原样返回
 * 2. 否则：
 *    - 提取除去最后 recentRawCount 的“老消息”
 *    - 对老消息做一个规则摘要（这里只是简单拼接与截断，真正实现留待后续）
 *    - 构造一个新的 SystemMessage 作为“历史摘要”
 *    - 返回： [原始第一条System(若有), 摘要SystemMessage, 最近原始消息...]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompressionService {
    private final CompressionConfig config;
    private final CompressAssistant compressAssistant;
    private final Tokenizer tokenizer = TokenizerFactory.qwen();

    public List<ChatMessage> compress(List<ChatMessage> original) {
        if (!config.isEnabled()) return original;
        if (original == null || original.size() < config.getMinMessagesToCompress()) return original;
        // 粗略token估算
        int estimatedTokens = original.stream()
            .mapToInt((item) -> {
                String text = "";
                if (item instanceof SystemMessage systemMessage) {
                    text = systemMessage.text();
                } else if (item instanceof UserMessage userMessage) {
                    text = userMessage.toString();
                } else if (item instanceof AiMessage) {
                    // 工具调用跳过
                    if(StringUtils.hasText(((AiMessage) item).text())) {
                        text = ((AiMessage) item).text();
                    }
                }
                return tokenizer.encodeOrdinary(text)
                    .size();
            })
            .sum();
        if (estimatedTokens <= config.getMaxTotalTokens()) return original;

        // 拆分：保留最近N条，确保不以AiMessage开头
        int recentCount = Math.min(config.getRecentRawCount(), original.size());
        int startIdx = original.size() - recentCount;

        SystemMessage systemMessage = (SystemMessage) original.getFirst();
        List<ChatMessage> recent = new ArrayList<>(original.subList(startIdx, original.size()));
        List<ChatMessage> older = new ArrayList<>(original.subList(1, startIdx));;

        // 摘要生成 todo 分类抓取一些代表内容
        String summary = buildSummary(older);

        String systemContent = systemMessage.text();
        String newSystemContent = systemContent + "\n以下是用户最近对话的摘要：\n" + summary;
        systemMessage = SystemMessage.from(newSystemContent);

        List<ChatMessage> result = new ArrayList<>();
        result.add(systemMessage);
        result.addAll(recent);

        log.info("Compression applied: original={}, returned={}, summaryTokens={}", original.size(), result.size(), tokenizer.encodeOrdinary(summary).size());
        return result;
    }

    private String buildSummary(List<ChatMessage> older) {
        StringBuilder originContext = new StringBuilder();
        for (ChatMessage m : older) {
            if (m instanceof UserMessage userMessage) {
                // 处理多部分内容：只提取文本部分用于摘要
                String userText = extractTextFromUserMessage(userMessage);
                originContext.append("[USER]").append(cut(userText)).append('\n');
            } else if (m instanceof AiMessage) {
                originContext.append("[ASSISTANT]").append(cut(((AiMessage) m).text())).append('\n');
            }
        }
        return compressAssistant.summary(originContext.toString());
    }

    /**
     * 从 UserMessage 中提取文本内容
     * 处理多部分内容（文本 + 附件）的情况
     */
    private String extractTextFromUserMessage(UserMessage userMessage) {
        List<Content> contents = userMessage.contents();
        if (contents != null && !contents.isEmpty()) {
            // 查找第一个文本内容
            for (Content content : contents) {
                if (content.type().equals(ContentType.TEXT)) {
                    return ((TextContent) content).text();
                }
            }
        }
        // 降级处理：尝试使用 singleText()，仅当只有单个文本内容时成功
        try {
            return userMessage.singleText();
        } catch (IllegalStateException e) {
            // 多部分内容会抛异常，此时返回空
            return "";
        }
    }


    private String cut(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+"," ");
        return s.length() > 80 ? s.substring(0,80) + "..." : s;
    }

    /**
     * Mock摘要生成方法，避免循环依赖
     * TODO: 后续可以通过ApplicationEventPublisher或其他方式集成CompressAssistant
     */
    private String mockSummary(String originContext) {
        if (originContext == null || originContext.trim().isEmpty()) {
            return "用户与助手进行了简短的对话交流。";
        }

        // 简单的关键词提取和摘要生成
        String[] lines = originContext.split("\n");
        int userCount = 0, assistantCount = 0;
        StringBuilder topics = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("[USER]")) {
                userCount++;
                topics.append(line.substring(6).trim()).append(" ");
            } else if (line.startsWith("[ASSISTANT]")) {
                assistantCount++;
            }
        }

        String topicsStr = topics.toString().trim();
        if (topicsStr.length() > 100) {
            topicsStr = topicsStr.substring(0, 100) + "...";
        }

        return String.format("用户提出了%d个问题，助手提供了%d次回应。主要讨论内容：%s",
                           userCount, assistantCount, topicsStr);
    }

    private int calculateToken(String text) {
        return tokenizer.encodeOrdinary(text).size();
    }
}
