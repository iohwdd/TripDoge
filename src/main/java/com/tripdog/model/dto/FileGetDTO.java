package com.tripdog.model.dto;

import lombok.Data;

@Data
public class FileGetDTO {
    private String objectKey;

    /**
     * 下载时显示的文件名（可选）
     */
    private String fileName;
}
