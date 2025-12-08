package com.tripdog.model.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/**
 * @author: iohw
 * @date: 2025/9/23 23:45
 * @description:
 */
@Data
public class ChatReqDTO {
    private String message;
    private MultipartFile file;
    private Boolean streamAudio;
    private String voice;
}
