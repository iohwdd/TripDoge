package com.tripdog.ai.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

/**
 * PgVector配置属性类
 * 仅在dashscope provider时启用，避免在mock/deepseek模式下尝试绑定pgvector配置
 * 如果缺少配置，提供降级逻辑，避免启动失败
 * 
 * @author: iohw
 * @date: 2025/9/26 13:24
 */
@Slf4j
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
    
    /**
     * 检查配置是否完整
     * 如果缺少必要配置，记录警告但不阻止启动
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(host) || port <= 0 || !StringUtils.hasText(database)) {
            log.warn("PgVector配置不完整，RAG功能可能不可用。host={}, port={}, database={}", 
                    host, port, database);
            log.warn("如需使用RAG功能，请配置pgvector相关环境变量：PGVECTOR_HOST, PGVECTOR_PORT, PGVECTOR_DATABASE等");
        } else {
            log.info("PgVector配置已加载：host={}, port={}, database={}", host, port, database);
        }
    }
    
    /**
     * 检查配置是否可用
     */
    public boolean isAvailable() {
        return StringUtils.hasText(host) && port > 0 && StringUtils.hasText(database) 
                && StringUtils.hasText(user) && StringUtils.hasText(password);
    }
}