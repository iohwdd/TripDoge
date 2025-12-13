package com.tripdog.model.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 亲密度变更流水，对应 t_intimacy_record。
 */
@Data
public class IntimacyRecordDO {
    private Long id;
    private Long uid;
    private Long roleId;
    /**
     * 变更值，可正可负（如 +10 / -10）。
     */
    private Integer delta;
    /**
     * 变更后的当前亲密度。
     */
    private Integer intimacy;
    /**
     * 变更原因：如 daily_first / every_10 / inactivity_penalty。
     */
    private String reason;
    private LocalDateTime createdAt;
}

