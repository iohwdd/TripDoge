package com.tripdog.ai.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * LLM Client 统一接口
 * 封装不同Provider的LLM功能，提供统一的访问接口
 * 
 * @author: AI Assistant
 * @date: 2025-11-24
 */
public interface LLMClient {
    
    /**
     * 获取StreamingChatModel实例
     * 用于流式对话场景
     * 
     * @return StreamingChatModel实例
     */
    StreamingChatModel getStreamingChatModel();
    
    /**
     * 获取ChatModel实例
     * 用于非流式对话场景（如压缩助手）
     * 
     * @return ChatModel实例
     */
    ChatModel getChatModel();
    
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

