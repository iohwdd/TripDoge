package com.tripdog.ai.mcp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import static com.tripdog.ai.mcp.McpConstants.WEB_SEARCH;

/**
 * @author: iohw
 * @date: 2025/9/27 12:09
 * @description:
 */
@Configuration
public class McpClientFactory {
    @Value("${mcp.search-link:}")
    private String searchMcpLink;
    private static final Map<String, McpClient> map = new HashMap<>();

    public McpClient getMcpClient(String k) {
        if(map.containsKey(k)) {
            return map.get(k);
        }
        McpClient client;
        switch (k) {
            case WEB_SEARCH:
                client = getWebSearchMcpClient();
                if (client != null) {
                    map.put(k, client);
                }
                return client;
        }
        return null;
    }

    private McpClient getWebSearchMcpClient() {
        // 如果search-link为空，返回null，不创建MCP客户端
        if (searchMcpLink == null || searchMcpLink.isEmpty()) {
            return null;
        }
        
        try {
            StreamableHttpMcpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url(searchMcpLink)
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
