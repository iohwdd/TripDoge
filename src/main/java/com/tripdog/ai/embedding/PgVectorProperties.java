package com.tripdog.ai.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * PgVector配置属性类
 * 仅在dashscope provider时启用，避免在mock/deepseek模式下尝试绑定pgvector配置
 * 
 * @author: iohw
 * @date: 2025/9/26 13:24
 */
@Configuration
@ConfigurationProperties(prefix = "pgvector")
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope", matchIfMissing = false)
@Data
public class PgVectorProperties {
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private String table;
}