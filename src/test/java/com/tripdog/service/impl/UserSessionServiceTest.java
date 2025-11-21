package com.tripdog.service.impl;

import com.tripdog.common.RedisService;
import com.tripdog.model.vo.UserInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSessionServiceTest {

    private RedisService redisService;
    private UserSessionService userSessionService;

    @BeforeEach
    void setUp() throws Exception {
        redisService = mock(RedisService.class);
        userSessionService = new UserSessionService(redisService);
        setField(userSessionService, "fallbackEnabled", true);
        setField(userSessionService, "fallbackMaxSize", 10);
    }

    @Test
    void createSessionUsesRedisWhenAvailable() {
        when(redisService.isRedisAvailable()).thenReturn(true);

        UserInfoVO user = buildUser(1L);
        String token = userSessionService.createSession(user);

        assertNotNull(token);
        verify(redisService).setObject(startsWith("user:session:"), eq(user), eq(30L), eq(TimeUnit.MINUTES));
        verify(redisService).setString(startsWith("user:token:"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
        assertTrue(getLocalSessions().isEmpty(), "Redis可用时不应该写入本地缓存");
    }

    @Test
    void fallbackStoresAndRetrievesSessionWhenRedisDown() {
        when(redisService.isRedisAvailable()).thenReturn(false);

        UserInfoVO user = buildUser(2L);
        String token = userSessionService.createSession(user);

        when(redisService.isRedisAvailable()).thenReturn(false);
        UserInfoVO result = userSessionService.getSession(token);

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertFalse(getLocalSessions().isEmpty(), "应该写入本地缓存");
    }

    @Test
    void cleanupLocalSessionsRemovesExpiredEntries() throws Exception {
        when(redisService.isRedisAvailable()).thenReturn(false);
        UserInfoVO user = buildUser(3L);
        String token = userSessionService.createSession(user);

        Map<String, ?> cache = getLocalSessions();
        Object entry = cache.get(token);
        assertNotNull(entry);
        setField(entry, "expireAt", System.currentTimeMillis() - 1000);

        userSessionService.cleanupLocalSessions();
        assertTrue(cache.isEmpty(), "过期session应被清理");
    }

    private UserInfoVO buildUser(Long id) {
        UserInfoVO user = new UserInfoVO();
        user.setId(id);
        user.setNickname("user-" + id);
        return user;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getLocalSessions() {
        try {
            Field field = UserSessionService.class.getDeclaredField("localSessions");
            field.setAccessible(true);
            return (Map<String, ?>) field.get(userSessionService);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("无法访问localSessions", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
