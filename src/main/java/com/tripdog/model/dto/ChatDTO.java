package com.tripdog.model.dto;

import com.tripdog.ai.tts.QwenRealtimeTtsService;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class ChatDTO {
    private SseEmitter emitter;
    private String conversationId;
    private String content;
    private String ttsKey;
    private QwenRealtimeTtsService.RealtimeTtsSession ttsHolder;
    private AtomicBoolean emitterClosed;
}
