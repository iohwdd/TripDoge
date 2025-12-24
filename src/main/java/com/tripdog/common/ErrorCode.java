package com.tripdog.common;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 通用错误码 (10000-10099)
    SYSTEM_ERROR(10000, "系统异常"),
    PARAM_ERROR(10001, "参数错误"),
    DATA_NOT_FOUND(10002, "数据不存在"),
    OPERATION_FAILED(10003, "操作失败"),

    // 用户相关错误码 (10100-10199)
    USER_NOT_FOUND(10100, "用户不存在"),
    USER_EMAIL_EXISTS(10101, "邮箱已存在"),
    USER_REGISTER_FAILED(10102, "用户注册失败"),
    USER_LOGIN_FAILED(10103, "用户登录失败"),
    USER_PASSWORD_ERROR(10104, "密码错误"),
    USER_NOT_LOGIN(10105, "用户未登录"),
    USER_SESSION_EXPIRED(10106, "用户会话已过期"),

    // 验证码相关错误码 (10150-10199)
    EMAIL_CODE_SEND_FAILED(10150, "验证码发送失败"),
    EMAIL_CODE_INVALID(10151, "验证码无效"),
    EMAIL_CODE_EXPIRED(10152, "验证码已过期"),
    EMAIL_CODE_ERROR(10153, "验证码错误"),

    // 角色相关错误码 (10200-10299)
    ROLE_NOT_FOUND(10200, "角色不存在"),
    ROLE_CREATE_FAILED(10201, "角色创建失败"),
    NOT_FOUND_ERROR(10202, "角色不存在"),

    // 对话相关错误码 (10300-10399)
    CONVERSATION_NOT_FOUND(10300, "对话不存在"),
    CONVERSATION_CREATE_FAILED(10301, "对话创建失败"),

    // 聊天记录相关错误码 (10400-10499)
    CHAT_HISTORY_NOT_FOUND(10400, "聊天记录不存在"),
    CHAT_HISTORY_SAVE_FAILED(10401, "聊天记录保存失败"),

    // 权限相关错误码 (10500-10599)
    NO_AUTH(10500, "无权限访问"),
    NOT_FOUND(10404, "资源不存在"),

    // Minio
    NO_FOUND_FILE(10600, "文件不存在"),

    // 技能调度额度
    NO_SKILL_LIMIT(10700, "技能调度额度不足");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
