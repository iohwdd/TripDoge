package com.tripdog.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 亲密度主表实体，对应 t_intimacy。
 */
@Data
public class IntimacyDO {
    private Long id;
    private Long uid;
    private Long roleId;
    private Integer intimacy;
    /**
     * 最近一次用户侧发言时间，用于活跃/扣减判断。
     */
    private LocalDateTime lastMsgTime;
    /**
     * 最近一次日首条加分所在自然日。
     */
    private LocalDate lastDailyBonusDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

