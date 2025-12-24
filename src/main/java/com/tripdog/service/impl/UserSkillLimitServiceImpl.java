package com.tripdog.service.impl;

import com.tripdog.common.utils.RoleConfigParser;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.mapper.UserSkillLimitMapper;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.entity.UserSkillLimitDO;
import com.tripdog.service.UserSkillLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSkillLimitServiceImpl implements UserSkillLimitService {
    private final UserSkillLimitMapper userSkillLimitMapper;
    private final RoleMapper roleMapper;

    @Override
    public int getRoleSkillLimit(Long userId, Long roleId) {
        // todo 本地缓存优化
        RoleDO role = roleMapper.selectById(roleId);
        return RoleConfigParser.extractSkillExecLimitMonth(role.getRoleSetting());
    }

    @Override
    public int getUserCurrentSkillLimit(Long userId, Long roleId) {
        // 获取用户当前月度剩余额度
        UserSkillLimitDO userSkillLimitDO = userSkillLimitMapper.selectByUserAndRole(userId, roleId);
        if (userSkillLimitDO == null) {
            // 如果无记录，自动初始化并返回角色配置的限制
            int roleSkillLimitMonth = getRoleSkillLimit(userId, roleId);
            userSkillLimitMapper.insert(userId, roleId, roleSkillLimitMonth);
            return roleSkillLimitMonth;
        }
        return userSkillLimitDO.getSkillExecLimitMonth();
    }

    @Override
    public void updateSkillLimit(Long userId, Long roleId, Integer limitDelta) {
        UserSkillLimitDO userSkillLimitDO = userSkillLimitMapper.selectByUserAndRole(userId, roleId);
        if (userSkillLimitDO == null) {
            // 向前兼容：如果用户无额度记录，自动初始化（获取角色配置中的月度限制）
            int roleSkillLimitMonth = getRoleSkillLimit(userId, roleId);
            userSkillLimitMapper.insert(userId, roleId, roleSkillLimitMonth);
            // 重新查询
            userSkillLimitDO = userSkillLimitMapper.selectByUserAndRole(userId, roleId);
        }
        userSkillLimitMapper.updateLimitByUserAndRole(userId, roleId, userSkillLimitDO.getSkillExecLimitMonth() + limitDelta);
    }


}
