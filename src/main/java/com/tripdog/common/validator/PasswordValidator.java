package com.tripdog.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码强度验证器
 * 验证密码是否包含至少一个字母和一个数字
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    @Override
    public void initialize(Password constraintAnnotation) {
        // 初始化方法，可以在这里获取注解参数
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // 空值由 @NotBlank 处理
        }

        // 检查是否包含至少一个字母
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        // 检查是否包含至少一个数字
        boolean hasDigit = password.matches(".*[0-9].*");

        return hasLetter && hasDigit;
    }
}


