package com.tripdog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import com.tripdog.common.validator.EmailFormat;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

    /**
     * 邮箱
     */
    @EmailFormat
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

}
