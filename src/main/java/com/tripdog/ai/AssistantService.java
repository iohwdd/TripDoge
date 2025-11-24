package com.tripdog.ai;

import org.springframework.context.annotation.Configuration;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.ai.embedding.RetrieverFactory;
import com.tripdog.ai.mcp.McpClientFactory;
import com.tripdog.ai.provider.LLMClient;
import com.tripdog.ai.tool.MyTools;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import static com.tripdog.ai.mcp.McpConstants.WEB_SEARCH;
import static com.tripdog.common.Constants.INJECT_TEMPLATE;

/**
 * Assistant服务类
 * 通过LLMClient统一接口访问LLM功能，实现Provider解耦
 * 
 * @author: iohw
 * @date: 2025/9/24 22:21
 */
@Configuration
@RequiredArgsConstructor
public class AssistantService {
    final LLMClient llmClient;
    final RetrieverFactory retrieverFactory;
    final CustomerChatMemoryProvider chatMemoryProvider;
    final McpClientFactory mcpClientFactory;

    public ChatAssistant getAssistant() {
        EmbeddingStoreContentRetriever embeddingStoreContentRetriever = retrieverFactory.getRetriever();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(embeddingStoreContentRetriever)
            .contentAggregator(new DefaultContentAggregator())
            .contentInjector(DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from("{{userMessage}}" + INJECT_TEMPLATE + "{{contents}}"))
                .build())
            .build();

        McpClient mcpClient = mcpClientFactory.getMcpClient(WEB_SEARCH);
        McpToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(mcpClient)
            .build();

        return AiServices.builder(ChatAssistant.class)
            .streamingChatModel(llmClient.getStreamingChatModel())
            .retrievalAugmentor(retrievalAugmentor)
            .chatMemoryProvider(chatMemoryProvider)
            .tools(new MyTools())
            .toolProvider(toolProvider)
            .build();
    }

}
