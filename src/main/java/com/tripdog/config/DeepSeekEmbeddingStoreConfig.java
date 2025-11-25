package com.tripdog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.util.StringUtils;

import com.tripdog.common.utils.ThreadLocalUtils;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import static com.tripdog.common.Constants.FILE_ID;
import static com.tripdog.common.Constants.FILE_NAME;
import static com.tripdog.common.Constants.ROLE_ID;
import static com.tripdog.common.Constants.UPLOAD_TIME;

/**
 * DeepSeek EmbeddingStore 配置类
 * 当使用 DeepSeek 且 PgVector 不可用时，使用内存 EmbeddingStore
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek", matchIfMissing = false)
public class DeepSeekEmbeddingStoreConfig {

    @Value("${DEEPSEEK_API_KEY:}")
    private String apiKey;

    /**
     * 创建内存 EmbeddingStore（临时方案，用于测试）
     * 注意：内存存储不支持持久化，重启后数据会丢失
     */
    @Bean
    @Primary
    public EmbeddingStore<TextSegment> inMemoryEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 创建 EmbeddingModel（使用 DeepSeek API，兼容 OpenAI 格式）
     * 注意：DeepSeek 可能不支持 embedding，这里使用兼容的配置
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // 使用 OpenAI 兼容的 EmbeddingModel
        // 注意：DeepSeek 可能不支持 embedding，如果失败，可能需要使用 DashScope
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com/v1")
                .modelName("text-embedding-3-small") // DeepSeek 可能不支持，但先尝试
                .build();
    }

    /**
     * 创建 EmbeddingStoreIngestor（用于文档向量化）
     * 与 PgVectorEmbeddingStoreInit 中的实现保持一致
     */
    @Bean
    @Primary
    public EmbeddingStoreIngestor textEmbeddingStoreIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 20);
        return EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(documentSplitter)
                .documentTransformer(dc -> {
                    Long roleId = (Long) ThreadLocalUtils.get(ROLE_ID);
                    Long userId = (Long) ThreadLocalUtils.get("userId");
                    String fileId = (String) ThreadLocalUtils.get(FILE_ID);
                    String fileName = (String) ThreadLocalUtils.get(FILE_NAME);
                    String uploadTime = (String) ThreadLocalUtils.get(UPLOAD_TIME);

                    if (roleId != null) dc.metadata().put(ROLE_ID, roleId);
                    if (userId != null) dc.metadata().put("userId", userId);
                    if (StringUtils.hasText(fileId)) dc.metadata().put(FILE_ID, fileId);
                    if (StringUtils.hasText(fileName)) dc.metadata().put(FILE_NAME, fileName);
                    if (StringUtils.hasText(uploadTime)) dc.metadata().put(UPLOAD_TIME, uploadTime);

                    return dc;
                })
                .build();
    }
}

