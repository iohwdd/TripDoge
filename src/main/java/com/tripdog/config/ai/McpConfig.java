package com.tripdog.config.ai;

import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class McpConfig {
    @Value("${DASHSCOPE_API_KEY}")
    private String dashscopeApiKey;

    @Bean
    public McpTransport webSearchMcpTransport() {
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
