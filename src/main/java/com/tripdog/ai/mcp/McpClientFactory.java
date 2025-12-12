package com.tripdog.ai.mcp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

import static com.tripdog.ai.mcp.McpConstants.MAP_MCP;
import static com.tripdog.ai.mcp.McpConstants.WEB_SEARCH;

/**
 * @author: iohw
 * @date: 2025/9/27 12:09
 * @description:
 */
@Configuration
public class McpClientFactory {
    @Value("${mcp.search-link}")
    private String searchMcpLink;
    @Value("${mcp.map-link}")
    private String mapMcpLink;

    private static final Map<String, McpClient> map = new HashMap<>();

    public McpClient getMcpClient(String k) {
        if(map.containsKey(k)) {
            return map.get(k);
        }
        McpClient client;
        switch (k) {
            case WEB_SEARCH:
                client = getClientMcp(searchMcpLink);
                if (client != null) {
                    map.put(k, client);
                }
                return client;
            case MAP_MCP:
                client = getClientMcp(mapMcpLink);
                if (client != null) {
                    map.put(k, client);
                }
                return client;
        }
        return null;
    }

    private McpClient getClientMcp(String mcpUrl) {
        try {
            StreamableHttpMcpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url(mcpUrl)
                .timeout(Duration.ofSeconds(5)) // 更短的超时时间
                .logRequests(true)
                .logResponses(true)
                .build();
            return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        } catch (Exception e) {
            System.err.println("Failed to create WebSearch MCP client: " + e.getMessage());
            // 不抛异常，返回null让应用继续运行
            return null;
        }
    }
}
