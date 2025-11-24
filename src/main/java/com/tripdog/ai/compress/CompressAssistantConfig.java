package com.tripdog.ai.compress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tripdog.ai.assistant.CompressAssistant;
import com.tripdog.ai.provider.LLMClient;

import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;

/**
 * CompressAssistant单独配置类
 * 将CompressAssistant从AssistantService中分离出来，避免循环依赖
 * 通过LLMClient统一接口访问ChatModel，实现Provider解耦
 */
@Configuration
@RequiredArgsConstructor
public class CompressAssistantConfig {
    private final LLMClient llmClient;

    @Bean
    CompressAssistant compressAssistant() {
        return AiServices.builder(CompressAssistant.class)
            .chatModel(llmClient.getChatModel())
            .build();
    }
}
