package com.tripdog.service.impl;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;

import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.common.openapi.OpenApiClient;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.common.utils.FileUploadUtils;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.common.utils.RoleConfigParser;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.model.dto.FileUploadDTO;
import com.tripdog.model.dto.OpenApiChatDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.service.ChatService;
import com.tripdog.service.IntimacyService;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.ai.tts.QwenRealtimeTtsService;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
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
    private final ConversationServiceImpl conversationServiceImpl;
    private final RoleMapper roleMapper;
    private final AssistantService assistantService;
    private final QwenRealtimeTtsService qwenRealtimeTtsService;
    private final IntimacyService intimacyService;
    private final OpenApiClient openApiClient;

    @Override
    public SseEmitter chat(Long roleId, Long userId, ChatReqDTO chatReqDTO) {
        ThreadLocalUtils.set(ROLE_ID, roleId);
        SseEmitter emitter = new SseEmitter(-1L);
        final QwenRealtimeTtsService.RealtimeTtsSession[] ttsHolder = new QwenRealtimeTtsService.RealtimeTtsSession[1];
        AtomicBoolean emitterClosed = new AtomicBoolean(false);
        final String[] ttsKeyHolder = new String[1];

        try {
            // 1. 获取或创建会话
            ConversationDO conversation = conversationServiceImpl.getOrCreateConversation(userId, roleId);
            ttsKeyHolder[0] = "chat:" + conversation.getConversationId();
            // 新一轮对话时，若上次 TTS 还在播，先终止再开始
            qwenRealtimeTtsService.stopSession(ttsKeyHolder[0]);
            try {
                var intimacyChange = intimacyService.handleUserMessage(userId, roleId);
                if (intimacyChange != null && intimacyChange.getDelta() != null && intimacyChange.getDelta() > 0) {
                    // 推送亲密度提升事件
                    emitter.send(SseEmitter.event()
                        .name("intimacy")
                        .data(Map.of(
                            "delta", intimacyChange.getDelta(),
                            "intimacy", intimacyChange.getIntimacy() == null ? null : intimacyChange.getIntimacy().getIntimacy()
                        )));
                }
            } catch (Exception e) {
                log.warn("亲密度更新失败，roleId={}, userId={}", roleId, userId, e);
            }

            // 2. 获取角色信息
            RoleDO role = roleMapper.selectById(roleId);
            if (role == null) {
                emitter.completeWithError(new RuntimeException("角色不存在"));
                return emitter;
            }

            // 3. 从角色配置中提取系统提示词
            String systemPrompt = RoleConfigParser.extractSystemPrompt(role.getAiSetting());
            log.info("角色[{}]使用系统提示词: {}", role.getName(), systemPrompt);

            StringBuilder responseBuilder = new StringBuilder();

            // 文本模型 or 视觉模型
            ChatAssistant assistant;
            if(chatReqDTO.getFile() != null && FileUtil.isImage(chatReqDTO.getFile().getOriginalFilename())) {
                OpenApiChatDTO dto = new OpenApiChatDTO();
                dto.setContent(chatReqDTO.getMessage());
                dto.setFile(chatReqDTO.getFile());
                dto.setConversationId(conversation.getConversationId());
                dto.setBrand(OpenApiClient.QWEN);
                return openApiClient.chat(dto);
            } else {
                assistant = assistantService.getAssistant(systemPrompt);
            }

            ttsHolder[0] = Boolean.TRUE.equals(chatReqDTO.getStreamAudio())
                ? qwenRealtimeTtsService.startOrReplaceSession(ttsKeyHolder[0], delta -> sendAudioDelta(emitter, emitterClosed, delta), chatReqDTO.getVoice()).orElse(null)
                : null;

            TokenStream stream = assistant.chat(conversation.getConversationId(), chatReqDTO.getMessage());

            stream.onPartialResponse((data) -> {
                if (emitterClosed.get()) {
                    return;
                }
                try {
                    if (data != null) {
                        responseBuilder.append(data);
                        if (ttsHolder[0] != null) {
                            ttsHolder[0].appendText(data);
                        }
                        emitter.send(SseEmitter.event()
                                .data(data)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("message")
                        );
                    }
                } catch (Exception e) {
                    handleEmitterException(emitter, emitterClosed, e);
                }
            }).onCompleteResponse((data) -> {
                try {

                    // 8. 更新会话统计
                    conversationServiceImpl.updateConversationStats(conversation.getConversationId(), null, null);
                    if (ttsHolder[0] != null) {
                        ttsHolder[0].finish();
                        ttsHolder[0].awaitCompletion(10, TimeUnit.SECONDS);
                        qwenRealtimeTtsService.stopSession(ttsKeyHolder[0]);
                    }

                    if (!emitterClosed.get()) {
                    emitter.send(SseEmitter.event()
                        .data("[DONE]")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("done"));
                    emitter.complete();
                        emitterClosed.set(true);
                    }
                } catch (Exception e) {
                    handleEmitterException(emitter, emitterClosed, e);
                } finally {
                    if (ttsHolder[0] != null) {
                        ttsHolder[0].close();
                    }
                }
            }).onError((ex) -> {
                log.error("AI聊天流处理异常", ex);
                if (ttsHolder[0] != null) {
                    ttsHolder[0].finish();
                    ttsHolder[0].close();
                    qwenRealtimeTtsService.stopSession(ttsKeyHolder[0]);
                }
                if (emitterClosed.compareAndSet(false, true)) {
                emitter.completeWithError(ex);
                }
            }).start();

        } catch (Exception e) {
            log.error("聊天服务处理异常", e);
            if (ttsHolder[0] != null) {
                ttsHolder[0].close();
            }
            if (ttsKeyHolder[0] != null) {
                qwenRealtimeTtsService.stopSession(ttsKeyHolder[0]);
            }
            if (emitterClosed.compareAndSet(false, true)) {
            emitter.completeWithError(e);
            }
        } finally {
            ThreadLocalUtils.remove(ROLE_ID);
        }

        return emitter;
    }

    private void sendAudioDelta(SseEmitter emitter, AtomicBoolean emitterClosed, String base64Pcm) {
        if (!StringUtils.hasText(base64Pcm)) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .data(Map.of("audio_delta", base64Pcm))
                .id(String.valueOf(System.currentTimeMillis()))
                .name("audio"));
        } catch (Exception e) {
            handleEmitterException(emitter, emitterClosed, e);
        }
    }

    private void handleEmitterException(SseEmitter emitter, AtomicBoolean emitterClosed, Exception e) {
        if (e instanceof IllegalStateException || e.getCause() instanceof IOException) {
            if (emitterClosed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
            log.warn("SSE连接已关闭，后续数据丢弃: {}", e.getMessage());
            return;
        }
        log.error("SSE发送失败", e);
        if (emitterClosed.compareAndSet(false, true)) {
            emitter.completeWithError(e);
        }
    }
}
