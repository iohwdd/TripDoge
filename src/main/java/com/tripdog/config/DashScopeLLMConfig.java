package com.tripdog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;

/**
 * DashScope LLM 配置类
 * 使用通义千问模型（Qwen）
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope", matchIfMissing = false)
public class DashScopeLLMConfig {

    @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key:}")
    private String streamingApiKeyFromConfig;

    @Value("${DASHSCOPE_API_KEY:}")
    private String dashscopeApiKey;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name:qwen3-max}")
    private String streamingModelName;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.temperature:0.7}")
    private Double streamingTemperature;

    @Value("${langchain4j.community.dashscope.chat-model.api-key:}")
    private String chatApiKeyFromConfig;

    @Value("${langchain4j.community.dashscope.chat-model.model-name:qwen3-max}")
    private String chatModelName;

    @Value("${langchain4j.community.dashscope.chat-model.temperature:0.7}")
    private Double chatTemperature;

    @Bean
    @Primary
    public StreamingChatModel streamingChatModel() {
        // 优先使用配置文件中的api-key，如果没有则使用环境变量
        String apiKey = (streamingApiKeyFromConfig != null && !streamingApiKeyFromConfig.isEmpty()) 
                ? streamingApiKeyFromConfig 
                : dashscopeApiKey;
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY is required when using dashscope provider. " +
                    "Please set DASHSCOPE_API_KEY environment variable or configure langchain4j.community.dashscope.streaming-chat-model.api-key");
        }
        
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(streamingModelName)
                .temperature(streamingTemperature != null ? streamingTemperature.floatValue() : 0.7f)
                .build();
    }

    @Bean
    @Primary
    public ChatModel chatModel() {
        // 优先使用配置文件中的api-key，如果没有则使用环境变量
        String apiKey = (chatApiKeyFromConfig != null && !chatApiKeyFromConfig.isEmpty()) 
                ? chatApiKeyFromConfig 
                : dashscopeApiKey;
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY is required when using dashscope provider. " +
                    "Please set DASHSCOPE_API_KEY environment variable or configure langchain4j.community.dashscope.chat-model.api-key");
        }
        
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(chatTemperature != null ? chatTemperature.floatValue() : 0.7f)
                .build();
    }
}

