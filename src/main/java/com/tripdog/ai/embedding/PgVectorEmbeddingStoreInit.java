package com.tripdog.ai.embedding;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.tripdog.common.utils.ThreadLocalUtils;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author: iohw
 * @date: 2025/9/26 13:24
 * @description: PgVector EmbeddingStore 配置（仅在dashscope provider时启用）
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope", matchIfMissing = false)
public class PgVectorEmbeddingStoreInit {
    final String ROLE_ID = "roleId";
    final String USER_ID = "userId";
    final String FILE_ID = "fileId";
    final String FILE_NAME = "fileName";
    final String UPLOAD_TIME = "uploadTime";
    final PgVectorProperties pgVectorProperties;

    @Bean
    EmbeddingStore<TextSegment> textEmbeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(pgVectorProperties.getHost())
                .port(pgVectorProperties.getPort())
                .user(pgVectorProperties.getUser())
                .password(pgVectorProperties.getPassword())
                .database(pgVectorProperties.getDatabase())
                .table(pgVectorProperties.getTable())
                .dimension(1024)
                .dropTableFirst(false)
                .createTable(true)
                .build();

    }

    @Bean
    public EmbeddingStoreIngestor textEmbeddingStoreIngestor(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300,20);
        return EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .documentSplitter(documentSplitter)
            .documentTransformer(dc -> {
                Long roleId = (Long) ThreadLocalUtils.get(ROLE_ID);
                Long userId = (Long) ThreadLocalUtils.get(USER_ID);
                String fileId = (String) ThreadLocalUtils.get(FILE_ID);
                String fileName = (String) ThreadLocalUtils.get(FILE_NAME);
                String uploadTime = (String) ThreadLocalUtils.get(UPLOAD_TIME);

                if(roleId != null) dc.metadata().put(ROLE_ID, roleId);
                if(userId != null) dc.metadata().put(USER_ID, userId);
                if(StringUtils.hasText(fileId)) dc.metadata().put(FILE_ID, fileId);
                if(StringUtils.hasText(fileName)) dc.metadata().put(FILE_NAME, fileName);
                if(StringUtils.hasText(uploadTime)) dc.metadata().put(UPLOAD_TIME, uploadTime);

                return dc;
            })
            .build();
    }
}
