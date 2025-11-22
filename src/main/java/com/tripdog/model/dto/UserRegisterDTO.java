package com.tripdog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.tripdog.common.validator.Password;

/**
 * 用户注册DTO
 */
@Data
public class UserRegisterDTO {

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Password
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 邮箱验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

}
