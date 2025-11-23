package com.tripdog.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传结果VO
 * 包含上传状态和向量化状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocUploadResultVO {
    /**
     * 文档信息
     */
    private DocVO doc;
    
    /**
     * 向量化是否成功
     */
    private boolean vectorizationSuccess;
    
    /**
     * 向量化错误信息（如果失败）
     */
    private String vectorizationError;
    
    /**
     * 是否为配置错误（true表示配置错误，false表示临时错误）
     */
    private Boolean isConfigError;
}


