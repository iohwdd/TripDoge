package com.tripdog.mapper;

import com.tripdog.model.entity.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息表 Mapper 接口
 */
@Mapper
public interface RoleMapper {

    /**
     * 插入角色
     */
    int insert(RoleDO role);

    /**
     * 根据ID删除角色
     */
    int deleteById(Long id);

    /**
     * 更新角色
     */
    int updateById(RoleDO role);

    /**
     * 根据ID查询角色
     */
    RoleDO selectById(Long id);

    /**
     * 动态条件查询角色列表
     */
    List<RoleDO> selectRoleList(RoleDO role);

    /**
     * 根据code查询角色
     */
    RoleDO selectByCode(@Param("code") String code);

    /**
     * 查询启用状态的角色列表
     */
    List<RoleDO> selectActiveRoles();

    List<RoleDO> selectActiveRolesPaged(@Param("offset") int offset, @Param("limit") int limit);
}
