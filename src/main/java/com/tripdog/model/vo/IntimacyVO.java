package com.tripdog.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class IntimacyVO {
    private Long roleId;
    private Integer intimacy;
    private LocalDateTime lastMsgTime;
    private LocalDate lastDailyBonusDate;
}

