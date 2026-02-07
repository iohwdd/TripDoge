package com.tripdog.model.dto;

import com.tripdog.ai.tts.QwenRealtimeTtsService;
import com.tripdog.model.entity.ConversationDO;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class ChatRouteContext {
    private ChatReqDTO chatReqDTO;
    private ConversationDO conversation;
    private SseEmitter emitter;
    private AtomicBoolean emitterClosed;
    private QwenRealtimeTtsService.RealtimeTtsSession ttsHolder;
    private String ttsKey;
}
