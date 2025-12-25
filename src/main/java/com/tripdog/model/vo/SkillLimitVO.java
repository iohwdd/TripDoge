package com.tripdog.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillLimitVO {
    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 用户当前月度剩余额度
     */
    private Integer currentLimit;

    /**
     * 角色配置的月度执行限制（每月重置的基准值）
     */
    private Integer roleLimit;
}