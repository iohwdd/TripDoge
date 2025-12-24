package com.tripdog.mapper;

import com.tripdog.model.entity.UserSkillLimitDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSkillLimitMapper {

    /**
     * 插入新记录（id 自增，时间自动设置）
     */
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId, @Param("skillExecLimitMonth") Integer skillExecLimitMonth);

    /**
     * 插入或更新（根据 user_id + role_id 唯一键）
     */
    int upsert(UserSkillLimitDO record);

    UserSkillLimitDO selectByUserAndRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<UserSkillLimitDO> listByUser(@Param("userId") Long userId);

    List<UserSkillLimitDO> listByRole(@Param("roleId") Long roleId);

    /**
     * 根据 userId 和 roleId 更新技能额度
     */
    int updateLimitByUserAndRole(@Param("userId") Long userId, @Param("roleId") Long roleId, @Param("limit") Integer limit);

    /**
     * 基于主键ID的分页查询（更优的分页方式）
     * @param lastId 上一页最后的ID，首次查询传0
     * @param limit 每页数量
     */
    List<UserSkillLimitDO> pageById(@Param("lastId") Long lastId, @Param("limit") Integer limit);

    /**
     * 查询技能额度总数
     */
    long countAll();
}


