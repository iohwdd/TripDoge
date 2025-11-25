package com.tripdog.integration;

import com.tripdog.model.dto.UserRegisterDTO;
import com.tripdog.service.EmailService;
import com.tripdog.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService事务集成测试
 * 验证P0-1修复：注册流程事务保护
 */
import org.junit.jupiter.api.Disabled;

@Disabled("Integration test requires full environment; disabled for CI")
//@SpringBootTest
//@ActiveProfiles("test")
class UserServiceTransactionIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // 准备测试数据
    }

    @Test
    @Transactional
    void testRegister_WithValidData_ShouldSucceed() {
        // 测试正常注册流程
        // 注意：这需要完整的测试环境（数据库、Redis等）
        // 实际测试中应该使用Testcontainers
        
        // 生成验证码
        String email = "test@example.com";
        String code = emailService.generateAndSendCode(email);
        
        // 创建注册DTO
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setEmail(email);
        registerDTO.setCode(code);
        registerDTO.setPassword("Test123456");
        registerDTO.setNickname("TestUser");
        
        // 验证register方法有@Transactional注解
        try {
            var method = UserService.class.getMethod("register", UserRegisterDTO.class);
            assertTrue(method.isAnnotationPresent(Transactional.class) ||
                       userService.getClass().getMethod("register", UserRegisterDTO.class)
                           .isAnnotationPresent(Transactional.class),
                "register方法应该有@Transactional注解");
        } catch (NoSuchMethodException e) {
            fail("register方法不存在");
        }
    }

    @Test
    void testRegisterMethod_HasTransactionalAnnotation() throws NoSuchMethodException {
        // 验证register方法有@Transactional注解
        var method = userService.getClass().getMethod("register", UserRegisterDTO.class);
        assertTrue(method.isAnnotationPresent(Transactional.class),
            "register方法应该有@Transactional注解");
        
        var transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "Transactional注解应该存在");
        
        // 验证rollbackFor配置
        Class<?>[] rollbackFor = transactional.rollbackFor();
        assertEquals(1, rollbackFor.length, "rollbackFor应该包含Exception");
        assertEquals(Exception.class, rollbackFor[0], "rollbackFor应该是Exception类");
    }
}


