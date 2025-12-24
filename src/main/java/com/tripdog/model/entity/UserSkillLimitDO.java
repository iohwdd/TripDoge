package com.tripdog.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户技能额度表，对应表：t_user_skill_limit
 */
@Data
public class UserSkillLimitDO {
    private Long id;
    private Long userId;
    private Long roleId;
    /**
     * 月度执行额度
     */
    private Integer skillExecLimitMonth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


