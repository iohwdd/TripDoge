package com.tripdog.service.impl;

import java.io.IOException;
import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.FileValidationUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.FileUploadDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.service.ChatService;
import com.tripdog.mapper.ChatHistoryMapper;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.common.utils.RoleConfigParser;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.tripdog.common.Constants.ROLE_ID;

/**
 * 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final StreamingChatModel chatLanguageModel;
    private final ConversationServiceImpl conversationServiceImpl;
    private final ChatHistoryMapper chatHistoryMapper;
    private final RoleMapper roleMapper;
    private final AssistantService assistantService;
    private final FileUploadUtils fileUploadUtils;

    @Override
    public SseEmitter chat(Long roleId, Long userId, ChatReqDTO chatReqDTO) {
        ThreadLocalUtils.set(ROLE_ID, roleId);
        // 设置SSE连接超时时间（30分钟）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            // 1. 获取或创建会话
            ConversationDO conversation = conversationServiceImpl.getOrCreateConversation(userId, roleId);

            // 2. 获取角色信息
            RoleDO role = roleMapper.selectById(roleId);
            if (role == null) {
                safeCompleteWithError(emitter, new RuntimeException("角色不存在"), "ROLE_NOT_FOUND", roleId, userId);
                return emitter;
            }

            // 3. 从角色配置中提取系统提示词
            String systemPrompt = RoleConfigParser.extractSystemPrompt(role.getAiSetting());
            log.info("角色[{}]使用系统提示词: {}", role.getName(), systemPrompt);

            StringBuilder responseBuilder = new StringBuilder();
            // 使用角色专用的聊天助手，传入角色的系统提示词
            ChatAssistant assistant = assistantService.getAssistant();

            MultipartFile file = chatReqDTO.getFile();
            TokenStream stream;
            if(file != null) {
                // 验证图片文件（多模态支持）
                try {
                    FileValidationUtils.validateImageFile(file);
                } catch (IllegalArgumentException e) {
                    log.warn("图片文件验证失败: {}", e.getMessage());
                    safeCompleteWithError(emitter, new RuntimeException("图片文件验证失败: " + e.getMessage()),
                        "IMAGE_INVALID", roleId, userId);
                    return emitter;
                }
                
                FileUploadDTO fileUploadDTO = fileUploadUtils.upload2Minio(chatReqDTO.getFile(), userId, "/tmp");
                String imageUrl = fileUploadUtils.getUrlFromMinio(fileUploadDTO.getFileUrl());
                UserMessage message = UserMessage.from(TextContent.from(chatReqDTO.getMessage()), ImageContent.from(URI.create(imageUrl)));
                stream = assistant.chat(conversation.getConversationId(), message);
            }else {
                stream = assistant.chat(conversation.getConversationId(), chatReqDTO.getMessage());
            }

            stream.onPartialResponse((data) -> {
                try {
                    responseBuilder.append(data);
                    emitter.send(SseEmitter.event()
                        .data(data)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("message")
                    );
                } catch (IOException e) {
                    log.error("发送SSE部分响应失败，客户端可能已断开连接", e);
                    // 客户端断开连接时，completeWithError会抛出异常，需要捕获
                    safeCompleteWithError(emitter, e, "PARTIAL_SEND_ERROR", roleId, userId);
                }
            }).onCompleteResponse((data) -> {
                try {
                    // 更新会话统计
                    conversationServiceImpl.updateConversationStats(conversation.getConversationId(), null, null);

                    emitter.send(SseEmitter.event()
                        .data("[DONE]")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("done"));
                    safeComplete(emitter, "STREAM_DONE", roleId, userId);
                    log.debug("SSE流式响应完成: conversationId={}", conversation.getConversationId());
                } catch (IOException e) {
                    log.error("发送SSE完成响应失败，客户端可能已断开连接", e);
                    safeCompleteWithError(emitter, e, "COMPLETE_SEND_ERROR", roleId, userId);
                }
            }).onError((ex) -> {
                log.error("AI聊天流处理异常", ex);
                safeCompleteWithError(emitter, ex, "STREAM_ERROR", roleId, userId);
            }).start();
            
            // 添加超时处理回调
            emitter.onTimeout(() -> {
                log.warn("SSE连接超时: conversationId={}, roleId={}, userId={}", 
                    conversation.getConversationId(), roleId, userId);
                safeComplete(emitter, "TIMEOUT", roleId, userId);
            });
            
            // 添加完成回调（正常完成或异常完成）
            emitter.onCompletion(() -> {
                log.debug("SSE连接已完成: conversationId={}, roleId={}, userId={}", 
                    conversation.getConversationId(), roleId, userId);
                // 清理ThreadLocal（虽然finally中也会清理，但这里确保及时清理）
                ThreadLocalUtils.clear();
            });
            
            // 添加错误回调
            emitter.onError((ex) -> {
                log.error("SSE连接发生错误: conversationId={}, roleId={}, userId={}, error={}", 
                    conversation.getConversationId(), roleId, userId, ex.getMessage(), ex);
                ThreadLocalUtils.clear();
            });

        } catch (Exception e) {
            log.error("聊天服务处理异常", e);
            safeCompleteWithError(emitter, e, "UNEXPECTED_ERROR", roleId, userId);
        } finally {
            ThreadLocalUtils.clear();
        }

        return emitter;
    }

    private void safeComplete(SseEmitter emitter, String reason, Long roleId, Long userId) {
        try {
            emitter.complete();
        } catch (Exception ex) {
            log.debug("SSE连接已关闭，无法完成: reason={}, roleId={}, userId={}", reason, roleId, userId, ex);
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable throwable, String reason, Long roleId, Long userId) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception ex) {
            log.debug("SSE连接已关闭，无法发送错误: reason={}, roleId={}, userId={}", reason, roleId, userId, ex);
        }
    }
}
