package com.tripdog.ai.compress;

import com.tripdog.config.ai.AiModelHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tripdog.ai.assistant.CompressAssistant;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;

/**
 * CompressAssistant单独配置类
 * 将CompressAssistant从AssistantService中分离出来，避免循环依赖
 */
@Configuration
@RequiredArgsConstructor
public class CompressAssistantConfig {
    private final AiModelHolder aiModelHolder;

    @Bean
    CompressAssistant compressAssistant() {
        return AiServices.builder(CompressAssistant.class)
            .chatModel(aiModelHolder.getDefaultChat())
            .build();
    }
}
