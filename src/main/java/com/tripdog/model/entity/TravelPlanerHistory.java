package com.tripdog.model.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 旅行规划历史（持久化到 t_travel_planer_history）
 * 极简字段：用户归属 + 基本行程信息 + Markdown 存储位置。
 */
@Data
public class TravelPlanerHistory {
    private Long id;
    private Long userId;
    private Long roleId;
    private String destination;
    private Integer days;
    private String people;
    /**
     * 标签（JSON 字符串存储，格式为数组，如 ["美食","亲子"]）
     */
    private String preferences;
    private String mdPath;
    private String mdUrl;
    private LocalDateTime createdAt;
}

