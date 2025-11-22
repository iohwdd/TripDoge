package com.tripdog.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Token工具类
 * 用于从请求中提取和处理用户token
 *
 * @author tripdog
 */
@Slf4j
public class TokenUtils {

    /**
     * 请求头中token的key
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Session中token的key（兼容性）
     */
    public static final String SESSION_TOKEN_KEY = "SESSION_TOKEN";

    /**
     * 从请求中提取token
     * 优先从Authorization请求头获取，其次从Session中获取（向后兼容）
     *
     * @param request HTTP请求
     * @return token字符串，如果不存在返回null
     */
    public static String extractToken(HttpServletRequest request) {
        // 1. 优先从Authorization请求头获取
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(TOKEN_PREFIX)) {
            String token = authHeader.substring(TOKEN_PREFIX.length());
            if (StringUtils.hasText(token)) {
                log.debug("从Authorization请求头提取token成功");
                return token;
            }
        }

        // 2. 从Session中获取（向后兼容）
        try {
            if (request.getSession(false) != null) {
                String sessionToken = (String) request.getSession(false).getAttribute(SESSION_TOKEN_KEY);
                if (StringUtils.hasText(sessionToken)) {
                    log.debug("从Session中提取token成功");
                    return sessionToken;
                }
            }
        } catch (Exception e) {
            log.warn("从Session中提取token失败", e);
        }

        log.debug("未能从请求中提取到token");
        return null;
    }

    /**
     * 验证token格式是否有效
     *
     * @param token token字符串
     * @return 是否有效
     */
    public static boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        // token应该是32位UUID去掉横线 + 时间戳，总共 >=45 位
        if (token.length() < 45) {
            return false;
        }

        return token.matches("^[a-fA-F0-9]{32}[0-9]{13,}$");
    }

    /**
     * 构建Authorization请求头值
     *
     * @param token 用户token
     * @return Authorization请求头值
     */
    public static String buildAuthorizationHeader(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return TOKEN_PREFIX + token;
    }

    /**
     * 从Authorization请求头中解析token
     *
     * @param authorizationHeader Authorization请求头值
     * @return token，如果解析失败返回null
     */
    public static String parseAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(TOKEN_PREFIX.length());
        return StringUtils.hasText(token) ? token : null;
    }
}
