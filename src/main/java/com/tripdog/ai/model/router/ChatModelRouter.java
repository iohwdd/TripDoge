package com.tripdog.ai.model.router;

import com.tripdog.ai.model.internal.ChatApiClient;
import com.tripdog.ai.model.openapi.OpenApiClient;
import com.tripdog.ai.tts.QwenRealtimeTtsService;
import com.tripdog.common.utils.FileUtil;
import com.tripdog.model.dto.ChatDTO;
import com.tripdog.model.dto.ChatReqDTO;
import com.tripdog.model.dto.ChatRouteContext;
import com.tripdog.model.dto.OpenApiChatDTO;
import com.tripdog.model.entity.ConversationDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatModelRouter {
    private final OpenApiClient openApiClient;
    private final ChatApiClient chatClient;

    public SseEmitter route(ChatRouteContext context) {
        ChatReqDTO chatReqDTO = context.getChatReqDTO();
        ConversationDO conversation = context.getConversation();
        SseEmitter emitter = context.getEmitter();
        AtomicBoolean emitterClosed = context.getEmitterClosed();
        QwenRealtimeTtsService.RealtimeTtsSession ttsHolder = context.getTtsHolder();
        String ttsKey = context.getTtsKey();
        String originalFilename = chatReqDTO.getFile() != null ? chatReqDTO.getFile().getOriginalFilename() : null;
        if (originalFilename != null && FileUtil.isImage(originalFilename)) {
            OpenApiChatDTO dto = new OpenApiChatDTO();
            dto.setEmitter(emitter);
            dto.setEmitterClosed(emitterClosed);
            dto.setContent(chatReqDTO.getMessage());
            dto.setFile(chatReqDTO.getFile());
            dto.setConversationId(conversation.getConversationId());
            dto.setBrand(OpenApiClient.QWEN);
            dto.setTtsKey(ttsKey);
            dto.setTtsHolder(ttsHolder);
            return openApiClient.chat(dto);
        }

        ChatDTO dto = new ChatDTO();
        dto.setEmitter(emitter);
        dto.setEmitterClosed(emitterClosed);
        dto.setContent(chatReqDTO.getMessage());
        dto.setConversationId(conversation.getConversationId());
        dto.setTtsKey(ttsKey);
        dto.setTtsHolder(ttsHolder);
        return chatClient.chat(dto);
    }
}
