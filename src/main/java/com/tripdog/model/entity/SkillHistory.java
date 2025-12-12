package com.tripdog.model.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SkillHistory {
    private Long id;
    private Long userId;
    private Long roleId;
    private String skill;           // e.g. travel
    private String destination;
    private Integer days;
    private String people;
    private String budget;
    private List<String> preferences;
    private String mdPath;          // minio object path or url (兼容老逻辑)
    private String mdUrl;           // 预签名/可访问 URL
    private LocalDateTime createdAt;
}

