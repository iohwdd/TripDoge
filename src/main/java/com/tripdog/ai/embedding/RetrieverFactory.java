package com.tripdog.ai.embedding;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Configuration;

import com.tripdog.common.utils.ThreadLocalUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;

import static com.tripdog.common.Constants.*;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * @author: iohw
 * @date: 2025/9/26 14:21
 * @description:
 */
@Configuration
@RequiredArgsConstructor
public class RetrieverFactory {
    final EmbeddingStore<TextSegment> embeddingStore;
    final EmbeddingModel embeddingModel;
    final Map<String, EmbeddingStoreContentRetriever> cache = new HashMap<>();

    public EmbeddingStoreContentRetriever getRetriever() {
        Long userId = (Long) ThreadLocalUtils.get(USER_ID);
        Long roleId = (Long) ThreadLocalUtils.get(ROLE_ID);
        String conversationId = (String) ThreadLocalUtils.get(CONVERSATION_ID);

        if (userId == null || roleId == null) {
            throw new IllegalStateException("ThreadLocal USER_ID/ROLE_ID not set before building retriever");
        }

        String k = roleId + ":" + userId;
        if(cache.containsKey(k)) {
            return cache.get(k);
        }

        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(10) //todo 配置抽离
            .minScore(0.8)
            .filter(metadataKey(ROLE_ID).isEqualTo(roleId)
                .and(metadataKey(USER_ID).isEqualTo(userId))
                    .or(metadataKey(CONVERSATION_ID).isEqualTo(conversationId)
                            .and(metadataKey(SUMMARY_TAG).isEqualTo("true"))
                    )
            )
            .build();
        cache.put(k, retriever);
        return retriever;
    }
}
