package com.tripdog.ai.compress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tripdog.ai.assistant.CompressAssistant;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;

/**
 * CompressAssistant单独配置类
 * 将CompressAssistant从AssistantService中分离出来，避免循环依赖
 * 直接使用ChatModel，通过条件注解实现Provider解耦
 */
@Configuration
@RequiredArgsConstructor
public class CompressAssistantConfig {
    private final ChatModel chatModel;

    @Bean
    CompressAssistant compressAssistant() {
        return AiServices.builder(CompressAssistant.class)
            .chatModel(chatModel)
            .build();
    }
}
