package com.tripdog.service;

public interface UserSkillLimitService {
    /**
     * 获取角色配置中的月度技能执行限制（固定值，从角色配置读取）
     */
    int getRoleSkillLimit(Long userId, Long roleId);

    /**
     * 获取用户当前月度剩余额度
     */
    int getUserCurrentSkillLimit(Long userId, Long roleId);

    /**
     * 更新用户月度技能执行额度（支持扣减：delta=-1，或增加）
     * 如果用户无额度记录，会自动初始化
     */
    void updateSkillLimit(Long userId, Long roleId, Integer delta);
}
