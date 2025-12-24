package com.tripdog.cron;

import com.tripdog.common.utils.RoleConfigParser;
import com.tripdog.mapper.RoleMapper;
import com.tripdog.mapper.UserSkillLimitMapper;
import com.tripdog.model.entity.RoleDO;
import com.tripdog.model.entity.UserSkillLimitDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 月初刷新技能额度
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillLimitTask {
    private final UserSkillLimitMapper userSkillLimitMapper;
    private final RoleMapper roleMapper;

    @Scheduled(cron = "0 0 12 1 * ?")
    public void runTask() {
        try {
            List<RoleDO> roleList = roleMapper.selectActiveRoles();
            Map<Long, Integer> roleSkillLimitMap = new HashMap<>();
            for (RoleDO roleDO : roleList) {
                int limitMonth = RoleConfigParser.extractSkillExecLimitMonth(roleDO.getRoleSetting());
                roleSkillLimitMap.put(roleDO.getId(), limitMonth);
            }

            int batchSize = 100;
            long lastId = 0L;
            int totalUpdated = 0;

            while (true) {
                List<UserSkillLimitDO> list = userSkillLimitMapper.pageById(lastId, batchSize);
                if (list.isEmpty()) {
                    break;
                }

                for (UserSkillLimitDO item : list) {
                    Integer limit = roleSkillLimitMap.get(item.getRoleId());
                    if (limit != null) {
                        int updated = userSkillLimitMapper.updateLimitByUserAndRole(
                            item.getUserId(), item.getRoleId(), limit
                        );
                        totalUpdated += updated;
                    }
                }

                // 使用最后一条记录的 ID 作为下一次查询的起点
                lastId = list.getLast().getId();

                if (list.size() < batchSize) {
                    break;
                }
            }

            log.info("技能额度重置任务完成，共更新 {} 条记录", totalUpdated);
        } catch (Exception e) {
            log.error("技能额度重置任务失败", e);
        }
    }
}
