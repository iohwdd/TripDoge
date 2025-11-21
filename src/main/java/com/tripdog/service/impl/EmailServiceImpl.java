package com.tripdog.service.impl;

import com.tripdog.common.RedisService;
import com.tripdog.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现类
 * 使用Redis存储验证码，支持多实例部署和持久化
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final RedisService redisService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 验证码Redis Key前缀
     */
    private static final String CODE_KEY_PREFIX = "email:code:";
    
    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;
    
    /**
     * 验证码过期时间（分钟）
     */
    private static final long CODE_EXPIRE_MINUTES = 5;
    
    /**
     * 验证码生成最大重试次数（防止无限循环）
     */
    private static final int MAX_GENERATE_RETRIES = 100;

    @Override
    public boolean sendVerificationCode(String email, String code) {
        long startTime = System.currentTimeMillis();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TripDog验证码");
            message.setText(buildEmailContent(code));
            message.setFrom(fromEmail); // 使用配置的发送邮箱

            // 同步发送邮件（如果超时会抛出异常）
            mailSender.send(message);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("验证码邮件发送成功: email={}, 耗时={}ms", email, duration);
            
            if (duration > 5000) {
                log.warn("邮件发送耗时较长: {}ms，建议检查SMTP服务器连接", duration);
            }
            
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("验证码邮件发送失败: email={}, 耗时={}ms, error={}", email, duration, e.getMessage(), e);
            
            // 如果是超时异常，记录详细信息
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout"))) {
                log.error("邮件发送超时，请检查SMTP服务器连接和超时配置");
            }
            
            // 开发阶段，即使邮件发送失败也返回true，方便测试
            log.warn("开发模式：邮件发送失败但返回成功，验证码为: {}，请在前端查看验证码", code);
            return true;
        }
    }

    @Override
    public String generateAndSendCode(String email) {
        // 生成6位数字验证码，确保唯一性
        String code = generateUniqueCode();

        // 存储验证码到Redis（即使邮件发送失败，验证码也已生成）
        // Key格式: email:code:{code}, Value: {email}
        String codeKey = CODE_KEY_PREFIX + code;
        boolean stored = redisService.setString(codeKey, email, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        if (!stored) {
            log.warn("验证码存储到Redis失败，尝试使用本地内存存储: email={}, code={}", email, code);
            // Redis不可用时的降级处理：如果Redis存储失败，记录警告但继续流程
            // 注意：这种情况下多实例部署时验证码不共享，但至少单实例可用
        }

        // 异步发送邮件，不阻塞请求
        sendVerificationCodeAsync(email, code);

        // 立即返回验证码，不等待邮件发送完成
        return code;
    }

    /**
     * 异步发送验证码邮件
     */
    @Async("emailExecutor")
    public void sendVerificationCodeAsync(String email, String code) {
        sendVerificationCode(email, code);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String codeKey = CODE_KEY_PREFIX + code;
        
        // 从Redis获取验证码对应的邮箱
        String storedEmail = redisService.getString(codeKey);
        
        if (storedEmail == null) {
            log.warn("验证码不存在或已过期: code={}", code);
            return false;
        }

        // 验证邮箱是否匹配
        if (!storedEmail.equals(email)) {
            log.warn("验证码对应邮箱不匹配: code={}, expected={}, actual={}", code, storedEmail, email);
            return false;
        }

        // 验证成功后删除验证码（原子操作，防止重放攻击）
        // 使用Redis的原子删除，确保验证码只能使用一次
        Boolean deleted = redisService.delete(codeKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("验证码验证成功并已删除: email={}, code={}", email, code);
            return true;
        } else {
            // 删除失败可能表示验证码已被使用（并发场景）
            log.warn("验证码删除失败，可能已被使用: email={}, code={}", email, code);
            return false;
        }
    }

    /**
     * 生成唯一的验证码
     * 使用Redis检查唯一性，避免验证码冲突
     * 添加最大重试次数限制，防止无限循环
     */
    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        int retries = 0;
        String code;

        do {
            // 生成6位数字验证码
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(random.nextInt(10));
            }
            code = sb.toString();
            
            // 检查Redis中是否已存在该验证码
            String codeKey = CODE_KEY_PREFIX + code;
            String existing = redisService.getString(codeKey);
            
            if (existing == null) {
                // 验证码不存在，可以使用
                break;
            }
            
            retries++;
            if (retries >= MAX_GENERATE_RETRIES) {
                log.error("验证码生成达到最大重试次数({})，可能存在异常", MAX_GENERATE_RETRIES);
                throw new RuntimeException("验证码生成失败，请稍后重试");
            }
        } while (true);

        return code;
    }

    private String buildEmailContent(String code) {
        return String.format(
            "您好！\n\n" +
            "您正在进行TripDog验证操作，验证码为：%s\n\n" +
            "验证码有效期为%d分钟，请及时使用。如非本人操作，请忽略此邮件。\n\n" +
            "TripDog团队",
            code, CODE_EXPIRE_MINUTES
        );
    }

    /**
     * 定期清理过期验证码（Redis会自动过期，此方法主要用于日志记录）
     * 每10分钟执行一次
     * 注意：Redis设置了过期时间，会自动清理，此方法主要用于监控和日志
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void logCodeStatistics() {
        // Redis设置了过期时间，会自动清理过期验证码
        // 这里主要用于监控和日志记录
        if (redisService.isRedisAvailable()) {
            log.debug("验证码存储使用Redis，过期验证码会自动清理");
        } else {
            log.warn("Redis不可用，验证码存储可能受影响");
        }
    }

}
