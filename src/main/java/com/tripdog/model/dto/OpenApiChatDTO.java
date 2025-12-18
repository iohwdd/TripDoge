package com.tripdog.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;



@Data
public class OpenApiChatDTO {
    private String brand;
    private String conversationId;
    private String content;
    private MultipartFile file; // todo 多个图片支持
    private String fileType;
    private String fileId;
}
