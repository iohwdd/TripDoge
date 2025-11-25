package com.tripdog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * DeepSeek ChatModel 配置类（用于 CompressAssistant）
 * 手动创建 ChatModel Bean（当使用 deepseek provider 时）
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek", matchIfMissing = false)
public class DeepSeekChatModelConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKeyFromConfig;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:deepseek-chat}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;

    @Value("${langchain4j.open-ai.chat-model.timeout:60}")
    private Integer timeoutSeconds;

    @Bean
    @Primary
    public ChatModel chatModel() {
        // 优先使用配置文件中的api-key，如果没有则使用环境变量
        String apiKey = (apiKeyFromConfig != null && !apiKeyFromConfig.isEmpty()) 
                ? apiKeyFromConfig 
                : deepseekApiKey;
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY is required when using deepseek provider. " +
                    "Please set DEEPSEEK_API_KEY environment variable or configure langchain4j.open-ai.chat-model.api-key");
        }
        
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}

