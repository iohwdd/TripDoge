package com.tripdog.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceImpl测试类
 * 验证P0-1修复：注册流程事务保护
 */
class UserServiceImplTest {

    @Test
    void testRegisterMethodHasTransactionalAnnotation() throws NoSuchMethodException {
        // 验证register方法是否有@Transactional注解
        Method registerMethod = UserServiceImpl.class.getMethod("register", 
            com.tripdog.model.dto.UserRegisterDTO.class);
        
        assertTrue(registerMethod.isAnnotationPresent(Transactional.class), 
            "register方法应该有@Transactional注解");
        
        Transactional transactional = registerMethod.getAnnotation(Transactional.class);
        assertNotNull(transactional, "Transactional注解应该存在");
        
        // 验证rollbackFor配置
        Class<?>[] rollbackFor = transactional.rollbackFor();
        assertEquals(1, rollbackFor.length, "rollbackFor应该包含Exception");
        assertEquals(Exception.class, rollbackFor[0], "rollbackFor应该包含Exception类");
    }
}




