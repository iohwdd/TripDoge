package com.tripdog.ai.provider.impl;

import com.tripdog.ai.provider.LLMClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;

/**
 * LLM Client适配器实现
 * 将LangChain4j的StreamingChatModel和ChatModel适配为统一的LLMClient接口
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@RequiredArgsConstructor
public class LLMClientAdapter implements LLMClient {
    
    private final StreamingChatModel streamingChatModel;
    private final ChatModel chatModel;
    private final String providerName;
    
    @Override
    public StreamingChatModel getStreamingChatModel() {
        return streamingChatModel;
    }
    
    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }
    
    @Override
    public String getProviderName() {
        return providerName;
    }
    
    @Override
    public boolean isAvailable() {
        return streamingChatModel != null && chatModel != null;
    }
}

