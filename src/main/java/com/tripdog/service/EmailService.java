package com.tripdog.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     * @param email 邮箱地址
     * @param code 验证码
     */
    boolean sendVerificationCode(String email, String code);

    /**
     * 生成验证码并发送
     * @param email 邮箱地址
     * @return 验证码
     */
    String generateAndSendCode(String email);

    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String email, String code);

    /**
     * 定期统计/清理验证码信息（供调度器调用）
     */
    void logCodeStatistics();

}
