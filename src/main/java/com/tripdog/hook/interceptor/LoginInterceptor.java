package com.tripdog.hook.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.common.utils.ThreadLocalUtils;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.impl.UserSessionService;
import com.tripdog.common.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static com.tripdog.common.Constants.USER_ID;

/**
 * 登录拦截器
 * 基于Redis token验证用户登录状态
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求URI和方法
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.debug("登录拦截器处理请求: {} {}", method, requestURI);

        // 对于OPTIONS请求（预检请求），直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 从请求中提取token
        String token = TokenUtils.extractToken(request);
        String traceId = getTraceId();

        if (token == null) {
            log.warn("TOKEN_MISSING ip={}, uri={}, ua={}, traceId={}",
                request.getRemoteAddr(), requestURI, request.getHeader("User-Agent"), traceId);
            writeErrorResponse(response, ErrorCode.USER_NOT_LOGIN);
            return false;
        }

        // 验证token格式
        if (!TokenUtils.isValidTokenFormat(token)) {
            log.warn("TOKEN_INVALID_FORMAT token={}, ip={}, uri={}, ua={}, traceId={}",
                token, request.getRemoteAddr(), requestURI, request.getHeader("User-Agent"), traceId);
            writeErrorResponse(response, ErrorCode.USER_NOT_LOGIN);
            return false;
        }

        // 获取用户Session信息
        UserInfoVO loginUser = userSessionService.getSession(token);
        if (loginUser == null) {
            log.warn("TOKEN_SESSION_MISSING token={}, ip={}, uri={}, ua={}, traceId={}",
                token, request.getRemoteAddr(), requestURI, request.getHeader("User-Agent"), traceId);
            writeErrorResponse(response, ErrorCode.USER_NOT_LOGIN);
            return false;
        }
        ThreadLocalUtils.set(USER_ID, loginUser.getId());

        // 将用户信息放入请求属性中，便于Controller使用
        request.setAttribute("loginUser", loginUser);
        request.setAttribute("userToken", token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalUtils.remove(USER_ID);
        ThreadLocalUtils.clear();
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Result<Void> result = Result.error(errorCode);
            String jsonResult = objectMapper.writeValueAsString(result);
            response.getWriter().write(jsonResult);
        } catch (Exception e) {
            log.error("写入错误响应失败", e);
        }
    }

    private String getTraceId() {
        Object traceId = ThreadLocalUtils.get("traceId");
        return traceId != null ? traceId.toString() : "N/A";
    }

}
