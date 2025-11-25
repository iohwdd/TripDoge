package com.tripdog.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档删除结果DTO
 * 包含各个删除步骤的详细状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocDeleteResultDTO {
    /**
     * MinIO文件删除是否成功
     */
    private boolean minioDeleted;
    
    /**
     * 向量数据删除是否成功
     */
    private boolean vectorDeleted;
    
    /**
     * 数据库记录删除是否成功
     */
    private boolean dbDeleted;
    
    /**
     * 是否全部成功
     */
    private boolean allSuccess;
    
    /**
     * 详细消息
     */
    private String message;
}



