package com.tripdog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import com.tripdog.common.validator.EmailFormat;

/**
 * 邮箱验证码发送DTO
 */
@Data
public class EmailCodeDTO {

    /**
     * 邮箱
     */
    @EmailFormat
    @NotBlank(message = "邮箱不能为空")
    private String email;

}
