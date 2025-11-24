package com.tripdog.ai.provider;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;

import java.util.function.Consumer;

/**
 * LLM Client 统一接口
 * 提供真正的业务抽象，封装不同Provider的LLM功能
 * 业务层通过此接口访问LLM，完全解耦底层实现
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
public interface LLMClient {
    
    /**
     * 流式聊天（文本消息）
     * 
     * @param conversationId 会话ID
     * @param message 用户消息
     * @return TokenStream流式响应
     */
    TokenStream streamChat(String conversationId, String message);
    
    /**
     * 流式聊天（支持多模态，如图片）
     * 
     * @param conversationId 会话ID
     * @param message 用户消息（可包含图片）
     * @return TokenStream流式响应
     */
    TokenStream streamChat(String conversationId, UserMessage message);
    
    /**
     * 非流式聊天（文本消息）
     * 注意：当前实现中ChatAssistant只支持流式，此方法通过流式实现
     * 
     * @param conversationId 会话ID
     * @param message 用户消息
     * @return 完整响应文本
     */
    default String chat(String conversationId, String message) {
        StringBuilder response = new StringBuilder();
        TokenStream stream = streamChat(conversationId, message);
        stream.onNext(response::append);
        stream.onComplete(() -> {});
        stream.onError((error) -> {
            throw new RuntimeException("Chat failed", error);
        });
        stream.start();
        return response.toString();
    }
    
    /**
     * 非流式聊天（支持多模态）
     * 注意：当前实现中ChatAssistant只支持流式，此方法通过流式实现
     * 
     * @param conversationId 会话ID
     * @param message 用户消息（可包含图片）
     * @return 完整响应文本
     */
    default String chat(String conversationId, UserMessage message) {
        StringBuilder response = new StringBuilder();
        TokenStream stream = streamChat(conversationId, message);
        stream.onNext(response::append);
        stream.onComplete(() -> {});
        stream.onError((error) -> {
            throw new RuntimeException("Chat failed", error);
        });
        stream.start();
        return response.toString();
    }
    
    /**
     * 获取Provider名称
     * 
     * @return Provider名称（mock, dashscope, deepseek等）
     */
    String getProviderName();
    
    /**
     * 检查Provider是否可用
     * 
     * @return true如果Provider已正确配置并可用
     */
    boolean isAvailable();
}
