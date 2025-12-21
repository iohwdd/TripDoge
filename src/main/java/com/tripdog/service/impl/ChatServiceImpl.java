package com.tripdog.service.impl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.tripdog.ai.model.internal.ChatApiClient;
import com.tripdog.model.dto.ChatDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;

import com.tripdog.ai.model.openapi.OpenApiClient;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.model.dto.OpenApiChatDTO;
import com.tripdog.model.entity.ConversationDO;
import com.tripdog.service.ChatService;
import com.tripdog.service.IntimacyService;
import com.tripdog.ai.tts.QwenRealtimeTtsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.tripdog.common.Constants.CONVERSATION_ID;
import static com.tripdog.common.Constants.ROLE_ID;

/**
 * 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final ConversationServiceImpl conversationServiceImpl;
    private final IntimacyService intimacyService;
    private final QwenRealtimeTtsService qwenRealtimeTtsService;
    private final OpenApiClient openApiClient;
    private final ChatApiClient chatClient;

    @Override
    public SseEmitter chat(Long roleId, Long userId, ChatReqDTO chatReqDTO) {
        long start = System.currentTimeMillis();
        ThreadLocalUtils.set(ROLE_ID, roleId);
        SseEmitter emitter = new SseEmitter(-1L);
        AtomicBoolean emitterClosed = new AtomicBoolean(false);
        final AtomicReference<QwenRealtimeTtsService.RealtimeTtsSession> ttsHolder = new AtomicReference<>();
        final AtomicReference<String> ttsKeyHolder = new AtomicReference<>();
        try {
            // 获取或创建会话
            ConversationDO conversation = conversationServiceImpl.getOrCreateConversation(userId, roleId);
            ThreadLocalUtils.set(CONVERSATION_ID, conversation.getConversationId());

            // 实时语音合成
            if(Boolean.TRUE.equals(chatReqDTO.getStreamAudio())) {
                ttsKeyHolder.set("chat:" + conversation.getConversationId());
                // 新一轮对话时，若上次 TTS 还在播，则终止
                qwenRealtimeTtsService.stopSession(ttsKeyHolder.get());
                ttsHolder.set(qwenRealtimeTtsService.startOrReplaceSession(ttsKeyHolder.get(),
                    delta -> sendAudioDelta(emitter, emitterClosed, delta), chatReqDTO.getVoice()).orElse(null));
            }

            // 亲密度更新
            handleIntimacyChange(emitter, userId, roleId);

            // 文本模型 or 视觉模型路由
            String originalFilename = chatReqDTO.getFile() != null ? chatReqDTO.getFile().getOriginalFilename() : null;
            if(originalFilename != null && FileUtil.isImage(originalFilename)) {
                // 视觉模型
                OpenApiChatDTO dto = new OpenApiChatDTO();
                dto.setEmitter(emitter);
                dto.setEmitterClosed(emitterClosed);
                dto.setContent(chatReqDTO.getMessage());
                dto.setFile(chatReqDTO.getFile());
                dto.setConversationId(conversation.getConversationId());
                dto.setBrand(OpenApiClient.QWEN);
                dto.setTtsKey(ttsKeyHolder.get());
                dto.setTtsHolder(ttsHolder.get());
                return openApiClient.chat(dto);
            } else {
                // 文本模型
                ChatDTO dto = new ChatDTO();
                dto.setEmitter(emitter);
                dto.setEmitterClosed(emitterClosed);
                dto.setContent(chatReqDTO.getMessage());
                dto.setConversationId(conversation.getConversationId());
                dto.setTtsKey(ttsKeyHolder.get());
                dto.setTtsHolder(ttsHolder.get());
                return chatClient.chat(dto);
            }

        } catch (Exception e) {
            log.error("聊天服务处理异常", e);
            cleanupTtsResources(ttsHolder, ttsKeyHolder);
            if (emitterClosed.compareAndSet(false, true)) {
                emitter.completeWithError(e);
            }
        } finally {
            ThreadLocalUtils.remove(ROLE_ID);
        }
        log.info("userid: {}, ai response time consuming: {}s", userId, (System.currentTimeMillis() - start) / 1000);
        return emitter;
    }

    private void handleIntimacyChange(SseEmitter emitter, Long userId, Long roleId) {
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

    /**
     * 清理 TTS 相关资源
     */
    private void cleanupTtsResources(AtomicReference<QwenRealtimeTtsService.RealtimeTtsSession> ttsHolder,
                                     AtomicReference<String> ttsKeyHolder) {
        if (ttsHolder.get() != null) {
            try {
                ttsHolder.get().finish();
                ttsHolder.get().awaitCompletion(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 异常不中断流程
            } finally {
                try {
                    ttsHolder.get().close();
                } catch (Exception e) {
                    // 日志记录
                }
            }
        }
        if (ttsKeyHolder.get() != null) {
            try {
                qwenRealtimeTtsService.stopSession(ttsKeyHolder.get());
            } catch (Exception e) {
                // 日志记录
            }
        }
    }

}
