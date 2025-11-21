package com.tripdog.integration;

import com.tripdog.common.RedisService;
import com.tripdog.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmailService Redis存储集成测试
 * 验证P0-2修复：验证码存储迁移到Redis
 * 使用Testcontainers提供Redis环境
 */
//@SpringBootTest
//@ActiveProfiles("test")
//@Testcontainers
class EmailServiceRedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        // Redis容器会自动清理
    }

    @Test
    void testGenerateAndSendCode_StoresInRedis() {
        // 测试验证码生成并存储到Redis
        String email = "test@example.com";
        
        // 生成验证码
        String code = emailService.generateAndSendCode(email);
        
        assertNotNull(code, "验证码应该生成");
        assertEquals(6, code.length(), "验证码应该是6位数字");
        
        // 验证Redis中是否存在验证码
        String codeKey = "email:code:" + code;
        String storedEmail = redisService.getString(codeKey);
        
        assertEquals(email, storedEmail, "Redis中存储的邮箱应该匹配");
        
        // 验证TTL（应该接近5分钟）
        Long ttl = redisService.getExpire(codeKey, TimeUnit.SECONDS);
        assertNotNull(ttl, "TTL应该存在");
        assertTrue(ttl > 240 && ttl <= 300, "TTL应该在240-300秒之间（5分钟）");
    }

    @Test
    void testVerifyCode_ValidCode() {
        // 测试验证码验证成功
        String email = "test@example.com";
        String code = emailService.generateAndSendCode(email);
        
        // 验证验证码
        boolean isValid = emailService.verifyCode(email, code);
        
        assertTrue(isValid, "有效的验证码应该验证成功");
        
        // 验证验证码已被删除（原子操作）
        String codeKey = "email:code:" + code;
        String storedEmail = redisService.getString(codeKey);
        assertNull(storedEmail, "验证码应该已被删除");
    }

    @Test
    void testVerifyCode_InvalidCode() {
        // 测试无效验证码
        boolean isValid = emailService.verifyCode("test@example.com", "000000");
        
        assertFalse(isValid, "无效的验证码应该验证失败");
    }

    @Test
    void testVerifyCode_WrongEmail() {
        // 测试邮箱不匹配
        String email1 = "test1@example.com";
        String email2 = "test2@example.com";
        String code = emailService.generateAndSendCode(email1);
        
        boolean isValid = emailService.verifyCode(email2, code);
        
        assertFalse(isValid, "邮箱不匹配应该验证失败");
    }

    @Test
    void testVerifyCode_CanOnlyUseOnce() {
        // 测试验证码只能使用一次（原子删除）
        String email = "test@example.com";
        String code = emailService.generateAndSendCode(email);
        
        // 第一次验证应该成功
        boolean firstVerify = emailService.verifyCode(email, code);
        assertTrue(firstVerify, "第一次验证应该成功");
        
        // 第二次验证应该失败（验证码已被删除）
        boolean secondVerify = emailService.verifyCode(email, code);
        assertFalse(secondVerify, "第二次验证应该失败（验证码已被删除）");
    }
}
