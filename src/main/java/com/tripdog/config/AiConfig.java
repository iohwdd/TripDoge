package com.tripdog.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

/**
 * @author: iohw
 * @date: 2025/9/22 22:58
 * @description: AI相关配置类 - 配置MCP (Model Context Protocol) 传输
 */
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope", matchIfMissing = false)
public class AiConfig {

    @Value("${DASHSCOPE_API_KEY:}")
    private String dashscopeApiKey;

    /**
     * MCP传输配置（仅在dashscope provider时启用）
     * @return
     */
    @Bean
    public McpTransport webSearchMcpTransport() {
        if (dashscopeApiKey == null || dashscopeApiKey.isEmpty()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY is required when using dashscope provider");
        }
        
        Map<String, String> headers = Map.of(
            "Authorization", "Bearer " + dashscopeApiKey,
            "Content-Type", "application/json"
        );

        return new StreamableHttpMcpTransport.Builder()
            .url("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse")
            .customHeaders(headers)
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
