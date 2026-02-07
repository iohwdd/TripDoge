package com.tripdog.ai;

import com.tripdog.ai.assistant.TravelPlaningAssistant;
import com.tripdog.config.ai.AiModelHolder;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import com.tripdog.ai.assistant.ChatAssistant;
import com.tripdog.ai.embedding.RetrieverFactory;
import com.tripdog.ai.mcp.McpClientFactory;
import com.tripdog.ai.tool.MyTools;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import static com.tripdog.ai.mcp.McpConstants.MAP_MCP;
import static com.tripdog.ai.mcp.McpConstants.WEB_SEARCH;
import static com.tripdog.common.Constants.INJECT_TEMPLATE;

/**
 * @author: iohw
 * @date: 2025/9/24 22:21
 * @description:
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AssistantService {
    final AiModelHolder aiModelHolder;
    final RetrieverFactory retrieverFactory;
    final CustomerChatMemoryProvider chatMemoryProvider;
    final McpClientFactory mcpClientFactory;

    public ChatAssistant getAssistant() {
        StreamingChatModel chatLanguageModel = aiModelHolder.getStreamingChatModel(AiModelHolder.QwenStreamingChat);
        // 如果本地部署了模型，优先使用本地模型
        if (aiModelHolder.getStreamingChatModel(AiModelHolder.LocalStreamingChat) != null) {
            log.info("使用本地模型...");
            chatLanguageModel = aiModelHolder.getStreamingChatModel(AiModelHolder.LocalStreamingChat);
        }

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
            .streamingChatModel(chatLanguageModel)
            .retrievalAugmentor(retrievalAugmentor)
            .chatMemoryProvider(chatMemoryProvider)
            .tools(new MyTools())
            .toolProvider(toolProvider)
            .build();
    }

    public TravelPlaningAssistant getTravelPlaningAssistant() {
        ChatModel chatModel = aiModelHolder.getDefaultChat();
        EmbeddingStoreContentRetriever embeddingStoreContentRetriever = retrieverFactory.getRetriever();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(embeddingStoreContentRetriever)
                .contentAggregator(new DefaultContentAggregator())
                .contentInjector(DefaultContentInjector.builder()
                        .promptTemplate(PromptTemplate.from("{{userMessage}}" + INJECT_TEMPLATE + "{{contents}}"))
                        .build())
                .build();

        McpClient mcpClient = mcpClientFactory.getMcpClient(MAP_MCP);
        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        return AiServices.builder(TravelPlaningAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .tools(new MyTools())
                .toolProvider(toolProvider)
                .build();
    }
}
