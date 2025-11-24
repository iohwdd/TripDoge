package com.tripdog.config;

import com.tripdog.ai.provider.LLMClient;
import com.tripdog.ai.provider.impl.LLMClientAdapter;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM Client工厂类
 * 根据配置创建统一的LLMClient实例，封装不同Provider的实现
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Slf4j
@Configuration
public class LLMClientFactory {
    
    @Value("${llm.provider:mock}")
    private String providerName;
    
    @Bean
    @Primary
    public LLMClient llmClient(
            StreamingChatModel streamingChatModel,
            ChatModel chatModel) {
        
        log.info("创建LLMClient，使用Provider: {}", providerName);
        
        return new LLMClientAdapter(streamingChatModel, chatModel, providerName);
    }
}

