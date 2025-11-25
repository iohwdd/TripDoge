package com.tripdog.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Mock LLM 配置类
 * 当没有配置LLM Provider或配置为mock时，使用Mock实现
 * 使用OpenAi兼容的模型，但配置为本地Mock端点（实际不会调用）
 * 确保系统可以在没有API Key的情况下启动
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLLMConfig {

    @Bean
    @Primary
    public StreamingChatModel mockStreamingChatModel() {
        log.info("使用Mock StreamingChatModel - 系统可以在没有API Key的情况下启动");
        // 使用OpenAi兼容的模型，配置为本地Mock端点
        // 注意：实际调用时会失败，但不会影响启动
        return OpenAiStreamingChatModel.builder()
                .apiKey("mock-api-key")
                .baseUrl("http://localhost:9999/mock") // 不存在的端点
                .modelName("mock-model")
                .timeout(Duration.ofSeconds(1)) // 短超时，快速失败
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @Primary
    public ChatModel mockChatModel() {
        log.info("使用Mock ChatModel - 系统可以在没有API Key的情况下启动");
        // 使用OpenAi兼容的模型，配置为本地Mock端点
        return OpenAiChatModel.builder()
                .apiKey("mock-api-key")
                .baseUrl("http://localhost:9999/mock") // 不存在的端点
                .modelName("mock-model")
                .timeout(Duration.ofSeconds(1)) // 短超时，快速失败
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> mockEmbeddingStore() {
        log.info("使用Mock EmbeddingStore - 内存存储，重启后数据会丢失");
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        log.info("使用Mock EmbeddingModel - 系统可以在没有API Key的情况下启动");
        // 使用OpenAi兼容的EmbeddingModel，配置为本地Mock端点
        return OpenAiEmbeddingModel.builder()
                .apiKey("mock-api-key")
                .baseUrl("http://localhost:9999/mock") // 不存在的端点
                .modelName("mock-embedding-model")
                .build();
    }

    @Bean
    @Primary
    public EmbeddingStoreIngestor embeddingStoreIngestor(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        log.info("使用Mock EmbeddingStoreIngestor - 内存存储，重启后数据会丢失");
        return EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
    }
}

