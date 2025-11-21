package com.tripdog.service.impl;

import com.tripdog.common.RedisService;
import com.tripdog.common.utils.TokenUtils;
import com.tripdog.model.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户Session管理服务，Redis优先，支持本地缓存降级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final RedisService redisService;
    private final Map<String, SessionEntry> localSessions = new ConcurrentHashMap<>();
    private final Map<Long, String> localUserTokens = new ConcurrentHashMap<>();

    @Value("${session.fallback.enabled:true}")
    private boolean fallbackEnabled;

    @Value("${session.fallback.max-size:1000}")
    private int fallbackMaxSize;

    private static final long SESSION_TIMEOUT = 30;
    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String USER_TOKEN_PREFIX = "user:token:";

    public String createSession(UserInfoVO userInfo) {
        if (userInfo == null || userInfo.getId() == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        String token = generateToken();
        String sessionKey = SESSION_KEY_PREFIX + token;
        String userTokenKey = USER_TOKEN_PREFIX + userInfo.getId();

        try {
            if (redisService.isRedisAvailable()) {
                String existingToken = redisService.getString(userTokenKey);
                if (existingToken != null) {
                    redisService.delete(SESSION_KEY_PREFIX + existingToken);
                }
                redisService.setObject(sessionKey, userInfo, SESSION_TIMEOUT, TimeUnit.MINUTES);
                redisService.setString(userTokenKey, token, SESSION_TIMEOUT, TimeUnit.MINUTES);
                removeLocalSessionByUserId(userInfo.getId());
                log.debug("为用户 {} 创建session成功", userInfo.getId());
                return token;
            }
        } catch (Exception e) {
            log.error("创建用户session失败，用户ID: {}", userInfo.getId(), e);
        }

        if (!canUseLocalFallback()) {
            throw new RuntimeException("创建用户session失败，Redis不可用且未启用本地缓存");
        }

        storeLocalSession(token, userInfo);
        return token;
    }

    public UserInfoVO getSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String sessionKey = SESSION_KEY_PREFIX + token;

        try {
            if (redisService.isRedisAvailable()) {
                UserInfoVO userInfo = redisService.getObject(sessionKey, UserInfoVO.class);
                if (userInfo != null) {
                    renewSession(token);
                    return userInfo;
                }
                return null;
            }
        } catch (Exception e) {
            log.error("获取用户session失败，token: {}", token, e);
        }

        return getLocalSession(token);
    }

    public void renewSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String sessionKey = SESSION_KEY_PREFIX + token;

        try {
            if (redisService.isRedisAvailable() && Boolean.TRUE.equals(redisService.hasKey(sessionKey))) {
                UserInfoVO userInfo = redisService.getObject(sessionKey, UserInfoVO.class);
                if (userInfo != null) {
                    redisService.expire(sessionKey, SESSION_TIMEOUT, TimeUnit.MINUTES);
                    redisService.expire(USER_TOKEN_PREFIX + userInfo.getId(), SESSION_TIMEOUT, TimeUnit.MINUTES);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("续期用户session失败，token: {}", token, e);
        }

        renewLocalSession(token);
    }

    public void removeSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String sessionKey = SESSION_KEY_PREFIX + token;

        try {
            if (redisService.isRedisAvailable()) {
                UserInfoVO userInfo = redisService.getObject(sessionKey, UserInfoVO.class);
                redisService.delete(sessionKey);
                if (userInfo != null) {
                    redisService.delete(USER_TOKEN_PREFIX + userInfo.getId());
                    log.info("删除用户session成功，用户ID: {}", userInfo.getId());
                }
            }
        } catch (Exception e) {
            log.error("删除用户session失败，token: {}", token, e);
        }

        removeLocalSession(token);
    }

    public void removeUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String userTokenKey = USER_TOKEN_PREFIX + userId;

        try {
            if (redisService.isRedisAvailable()) {
                String token = redisService.getString(userTokenKey);
                if (token != null) {
                    redisService.delete(SESSION_KEY_PREFIX + token);
                }
                redisService.delete(userTokenKey);
                log.info("删除用户所有session成功，用户ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("删除用户所有session失败，用户ID: {}", userId, e);
        }

        removeLocalSessionByUserId(userId);
    }

    public boolean isSessionValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String sessionKey = SESSION_KEY_PREFIX + token;
        if (redisService.isRedisAvailable()) {
            return Boolean.TRUE.equals(redisService.hasKey(sessionKey));
        }
        return hasValidLocalSession(token);
    }

    public long getSessionRemainingTime(String token) {
        if (token == null || token.trim().isEmpty()) {
            return -1;
        }

        String sessionKey = SESSION_KEY_PREFIX + token;
        if (redisService.isRedisAvailable()) {
            return redisService.getExpire(sessionKey, TimeUnit.SECONDS);
        }
        return getLocalSessionRemainingSeconds(token);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public UserInfoVO getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object loginUser = request.getAttribute("loginUser");
            if (loginUser instanceof UserInfoVO) {
                return (UserInfoVO) loginUser;
            }
        }
        return getCurrentUserSafely();
    }

    private UserInfoVO getCurrentUserSafely() {
        try {
            String token = getCurrentToken();
            if (token != null) {
                return getSession(token);
            }
        } catch (Exception e) {
            log.debug("从Session获取当前用户失败: {}", e.getMessage());
        }
        return null;
    }

    public String getCurrentToken() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String tokenAttr = (String) request.getAttribute("userToken");
            if (tokenAttr != null && !tokenAttr.trim().isEmpty()) {
                return tokenAttr;
            }
            return TokenUtils.extractToken(request);
        }
        return null;
    }

    public Long getCurrentUserId() {
        UserInfoVO user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public String getCurrentUserNickname() {
        UserInfoVO user = getCurrentUser();
        return user != null ? user.getNickname() : null;
    }

    @Scheduled(fixedDelayString = "${session.fallback.cleanup-interval-ms:60000}")
    public void cleanupLocalSessions() {
        if (!canUseLocalFallback()) {
            localSessions.clear();
            localUserTokens.clear();
            return;
        }

        if (localSessions.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        localSessions.forEach((token, entry) -> {
            if (entry == null || entry.isExpired(now)) {
                removeLocalSession(token, entry);
            }
        });

        enforceLocalCacheLimit();
    }

    private void storeLocalSession(String token, UserInfoVO userInfo) {
        enforceLocalCacheLimit();
        SessionEntry entry = new SessionEntry(userInfo, calculateExpireAt());
        localSessions.put(token, entry);
        String oldToken = localUserTokens.put(userInfo.getId(), token);
        if (oldToken != null && !oldToken.equals(token)) {
            localSessions.remove(oldToken);
        }
        log.warn("Redis不可用，使用本地Session缓存，用户ID: {}", userInfo.getId());
    }

    private UserInfoVO getLocalSession(String token) {
        SessionEntry entry = localSessions.get(token);
        if (entry == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (entry.isExpired(now)) {
            removeLocalSession(token, entry);
            return null;
        }

        entry.renew(calculateExpireAt());
        return entry.getUserInfo();
    }

    private void renewLocalSession(String token) {
        SessionEntry entry = localSessions.get(token);
        if (entry == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (entry.isExpired(now)) {
            removeLocalSession(token, entry);
            return;
        }
        entry.renew(calculateExpireAt());
    }

    private void removeLocalSession(String token) {
        SessionEntry entry = localSessions.remove(token);
        if (entry != null && entry.getUserInfo() != null) {
            localUserTokens.remove(entry.getUserInfo().getId(), token);
        }
    }

    private void removeLocalSession(String token, SessionEntry entry) {
        if (entry == null) {
            removeLocalSession(token);
            return;
        }
        if (localSessions.remove(token, entry) && entry.getUserInfo() != null) {
            localUserTokens.remove(entry.getUserInfo().getId(), token);
        }
    }

    private void removeLocalSessionByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        String token = localUserTokens.remove(userId);
        if (token != null) {
            localSessions.remove(token);
        }
    }

    private boolean hasValidLocalSession(String token) {
        if (!canUseLocalFallback()) {
            return false;
        }
        SessionEntry entry = localSessions.get(token);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired(System.currentTimeMillis())) {
            removeLocalSession(token, entry);
            return false;
        }
        return true;
    }

    private long getLocalSessionRemainingSeconds(String token) {
        if (!canUseLocalFallback()) {
            return -1L;
        }
        SessionEntry entry = localSessions.get(token);
        if (entry == null) {
            return -1L;
        }
        long remainingMillis = entry.getExpireAt() - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            removeLocalSession(token, entry);
            return -1L;
        }
        return TimeUnit.MILLISECONDS.toSeconds(remainingMillis);
    }

    private void enforceLocalCacheLimit() {
        if (!canUseLocalFallback()) {
            localSessions.clear();
            localUserTokens.clear();
            return;
        }
        while (localSessions.size() >= fallbackMaxSize && fallbackMaxSize > 0) {
            String tokenToRemove = findOldestToken();
            if (tokenToRemove == null) {
                break;
            }
            log.warn("本地session缓存达到上限，移除token={}", tokenToRemove);
            removeLocalSession(tokenToRemove);
        }
    }

    private String findOldestToken() {
        long oldestExpire = Long.MAX_VALUE;
        String oldestToken = null;
        for (Map.Entry<String, SessionEntry> entry : localSessions.entrySet()) {
            SessionEntry value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value.getExpireAt() < oldestExpire) {
                oldestExpire = value.getExpireAt();
                oldestToken = entry.getKey();
            }
        }
        return oldestToken;
    }

    private long calculateExpireAt() {
        return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(SESSION_TIMEOUT);
    }

    private boolean canUseLocalFallback() {
        return fallbackEnabled && fallbackMaxSize > 0;
    }

    private static class SessionEntry {
        private final UserInfoVO userInfo;
        private volatile long expireAt;

        SessionEntry(UserInfoVO userInfo, long expireAt) {
            this.userInfo = userInfo;
            this.expireAt = expireAt;
        }

        UserInfoVO getUserInfo() {
            return userInfo;
        }

        long getExpireAt() {
            return expireAt;
        }

        void renew(long newExpireAt) {
            this.expireAt = newExpireAt;
        }

        boolean isExpired(long now) {
            return now >= expireAt;
        }
    }
}
