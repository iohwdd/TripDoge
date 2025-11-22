package com.tripdog.controller;

import com.tripdog.common.ErrorCode;
import com.tripdog.common.Result;
import com.tripdog.model.dto.EmailCodeDTO;
import com.tripdog.model.dto.UserLoginDTO;
import com.tripdog.model.dto.UserRegisterDTO;
import com.tripdog.model.vo.EmailCodeVO;
import com.tripdog.model.vo.UserInfoVO;
import com.tripdog.service.EmailService;
import com.tripdog.service.UserService;
import com.tripdog.service.impl.UserSessionService;
import com.tripdog.common.utils.TokenUtils;
import org.slf4j.MDC;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户注册、登录、信息管理相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserSessionService userSessionService;


    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "新用户注册账号，需要邮箱验证码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "10101", description = "邮箱已存在"),
            @ApiResponse(responseCode = "10102", description = "注册失败"),
            @ApiResponse(responseCode = "10153", description = "验证码错误")
    })
    @PostMapping("/register")
    public Result<UserInfoVO> register(@RequestBody @Validated UserRegisterDTO registerDTO) {
        try {
            UserInfoVO userInfoVO = userService.register(registerDTO);
            return Result.success(userInfoVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录系统，登录成功后返回访问token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "10100", description = "用户不存在"),
            @ApiResponse(responseCode = "10103", description = "登录失败"),
            @ApiResponse(responseCode = "10104", description = "密码错误")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated UserLoginDTO loginDTO, HttpServletRequest request) {
        try {
            UserInfoVO userInfoVO = userService.login(loginDTO);

            // 创建用户Session并生成token
            String token = userSessionService.createSession(userInfoVO);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("userInfo", userInfoVO);
            data.put("token", token);
            data.put("tokenType", "Bearer");

            return Result.success(data);
        } catch (RuntimeException e) {
            logLoginFailure(loginDTO, request, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送邮箱验证码
     */
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送验证码，用于注册验证")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证码发送成功"),
            @ApiResponse(responseCode = "10150", description = "验证码发送失败")
    })
    @PostMapping("/sendEmail")
    public Result<EmailCodeVO> sendEmailCode(@RequestBody @Validated EmailCodeDTO emailCodeDTO) {
        try {
            // 生成并发送验证码
            String code = emailService.generateAndSendCode(emailCodeDTO.getEmail());
            if (code == null) {
                return Result.error(ErrorCode.EMAIL_CODE_SEND_FAILED);
            }

            EmailCodeVO emailCodeVO = new EmailCodeVO(code);
            return Result.success(emailCodeVO);
        } catch (Exception e) {
            return Result.error(ErrorCode.EMAIL_CODE_SEND_FAILED);
        }
    }

    private void logLoginFailure(UserLoginDTO loginDTO, HttpServletRequest request, RuntimeException e) {
        String clientIp = request != null ? request.getRemoteAddr() : "unknown";
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        log.warn("LOGIN_FAIL email={}, ip={}, uri=/user/login, ua={}, reason={}, traceId={}",
            loginDTO.getEmail(), clientIp, userAgent, e.getMessage(), getTraceId());
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "N/A";
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "用户退出登录，清除登录状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求中提取token
        String token = TokenUtils.extractToken(request);
        if (token != null) {
            // 删除用户Session
            userSessionService.removeSession(token);
        }
        return Result.success("退出登录成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息，需要登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取用户信息成功"),
            @ApiResponse(responseCode = "10105", description = "用户未登录"),
            @ApiResponse(responseCode = "10106", description = "会话已过期")
    })
    @PostMapping("/info")
    public Result<UserInfoVO> getCurrentUser() {
        // 从用户会话服务获取当前登录用户信息
        UserInfoVO loginUser = userSessionService.getCurrentUser();
        if (loginUser == null) {
            return Result.error(ErrorCode.USER_NOT_LOGIN);
        }

        return Result.success(loginUser);
    }

}
