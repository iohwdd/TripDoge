package com.tripdog.model.vo;

import lombok.Data;

/**
 * 角色详情VO - 包含完整的角色配置信息
 *
 * @author: iohw
 * @date: 2025/9/25
 */
@Data
public class RoleDetailVO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色代码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色描述
     */
    private String description;


    /**
     * 性格特征
     */
    private String[] personality;

    /**
     * 专长领域
     */
    private String[] specialties;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 月度技能执行额度
     */
    private Integer skillLimit;
}
