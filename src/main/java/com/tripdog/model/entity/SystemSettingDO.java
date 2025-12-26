package com.tripdog.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统设置实体类
 */
@Data
public class SystemSettingDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设置键
     */
    private String key;

    /**
     * 设置值
     */
    private String value;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}