package com.tripdog.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * MinIO配置类
 * 可选配置，如果不需要文件上传功能可以不配置
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    private String endpoint;
    private String port;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    @Bean
    @ConditionalOnProperty(name = "minio.endpoint")
    public MinioClient minioClient() {
        // 检查配置是否完整
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            log.warn("MinIO配置不完整，文件上传功能可能不可用");
            return null;
        }
        
        try {
            log.info("初始化MinIO客户端: endpoint={}, port={}", endpoint, port);
            return MinioClient.builder()
                    .endpoint("http://" + endpoint + ":" + port)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败，文件上传功能将不可用", e);
            return null;
        }
    }
}
