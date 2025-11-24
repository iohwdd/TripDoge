package com.tripdog.ai.provider.impl;

import com.tripdog.ai.provider.LLMClient;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LLM Client适配器实现
 * 提供真正的业务抽象，封装底层StreamingChatModel和ChatModel
 * 业务层通过此接口访问LLM，完全解耦底层实现
 * 
 * 注意：当前实现直接使用底层模型，真正的业务逻辑（RAG、Memory等）在AssistantService中处理
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
@Slf4j
@RequiredArgsConstructor
public class LLMClientAdapter implements LLMClient {
    
    private final StreamingChatModel streamingChatModel;
    private final ChatModel chatModel;
    private final String providerName;
    
    @Override
    public TokenStream streamChat(String conversationId, String message) {
        // 直接使用StreamingChatModel，业务逻辑在AssistantService中处理
        return streamingChatModel.generateStream(message);
    }
    
    @Override
    public TokenStream streamChat(String conversationId, UserMessage message) {
        // 直接使用StreamingChatModel，业务逻辑在AssistantService中处理
        return streamingChatModel.generateStream(message);
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
