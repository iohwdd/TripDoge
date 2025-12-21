package com.tripdog.ai.model.internal;

import com.tripdog.ai.AssistantService;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.ai.tts.QwenRealtimeTtsService;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.dto.ChatDTO;
import com.tripdog.service.ConversationService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tripdog.common.Constants.ROLE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatApiClient {
    private final AssistantService assistantService;
    private final ConversationService conversationService;
    private final QwenRealtimeTtsService qwenRealtimeTtsService;
    
    public SseEmitter chat(ChatDTO dto) {
        SseEmitter emitter = dto.getEmitter();
        AtomicBoolean emitterClosed = dto.getEmitterClosed();
        String conversationId = dto.getConversationId();
        String content = dto.getContent();
        QwenRealtimeTtsService.RealtimeTtsSession ttsHolder = dto.getTtsHolder();
        String ttsKey = dto.getTtsKey();
        ChatAssistant assistant = assistantService.getAssistant();
        TokenStream stream = assistant.chat(conversationId, content);
        stream.onPartialResponse((data) -> {
            try {
                // 如果连接已关闭，跳过处理
                if (emitterClosed != null && emitterClosed.get()) {
                    return;
                }
                if (data != null) {
                    if (ttsHolder != null) {
                        ttsHolder.appendText(data);
                    }
                    emitter.send(SseEmitter.event()
                            .data(data)
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name("message")
                    );
                }
            } catch (Exception e) {
                if (emitterClosed != null) {
                    emitterClosed.set(true);
                }
                handleEmitterException(emitter, e);
            }
        }).onCompleteResponse((data) -> {
            try {
                // 更新会话统计
                conversationService.updateConversationStats(conversationId, null, null);
                cleanupTtsResources(ttsHolder, ttsKey);

                // 如果连接未关闭，发送完成信号
                if (emitterClosed == null || !emitterClosed.get()) {
                    emitter.send(SseEmitter.event()
                            .data("[DONE]")
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name("done"));
                    emitter.complete();
                }
            } catch (Exception e) {
                if (emitterClosed != null) {
                    emitterClosed.set(true);
                }
                handleEmitterException(emitter, e);
            }
        }).onError((ex) -> {
            log.error("AI聊天流处理异常", ex);
            if (emitterClosed != null) {
                emitterClosed.set(true);
            }
            cleanupTtsResources(ttsHolder, ttsKey);
            emitter.completeWithError(ex);
        }).start();

        return emitter;
    }

    private void handleEmitterException(SseEmitter emitter, Exception e) {
        if (e instanceof IllegalStateException || e.getCause() instanceof IOException) {
            emitter.complete();
            log.warn("SSE连接已关闭，后续数据丢弃: {}", e.getMessage());
            return;
        }
        log.error("SSE发送失败", e);
        emitter.completeWithError(e);
    }

    /**
     * 清理 TTS 相关资源
     */
    private void cleanupTtsResources(QwenRealtimeTtsService.RealtimeTtsSession ttsHolder,
                                     String ttsKeyHolder) {
        if (ttsHolder != null) {
            try {
                ttsHolder.finish();
                ttsHolder.awaitCompletion(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 异常不中断流程
                log.warn("err happened in cleanupTtsResources: {}", e.getMessage());
            } finally {
                ttsHolder.close();
            }
        }
        if (ttsKeyHolder != null) {
            try {
                qwenRealtimeTtsService.stopSession(ttsKeyHolder);
            } catch (Exception e) {
                // 日志记录
            }
        }
    }
}
