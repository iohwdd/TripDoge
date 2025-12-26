package com.tripdog.mapper;

import com.tripdog.model.entity.SystemSettingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统设置 Mapper
 */
@Mapper
public interface SystemSettingMapper {

    /**
     * 插入系统设置
     */
    int insert(SystemSettingDO systemSetting);

    /**
     * 根据ID删除系统设置
     */
    int deleteById(Long id);

    /**
     * 根据ID更新系统设置
     */
    int updateById(SystemSettingDO systemSetting);

    /**
     * 根据ID查询系统设置
     */
    SystemSettingDO selectById(Long id);

    /**
     * 根据key查询系统设置
     */
    SystemSettingDO selectByKey(@Param("key") String key);

    /**
     * 查询所有系统设置
     */
    List<SystemSettingDO> selectAll();

    /**
     * 根据条件查询系统设置列表
     */
    List<SystemSettingDO> selectSystemSettingList(SystemSettingDO systemSetting);

    /**
     * 根据key删除系统设置
     */
    int deleteByKey(@Param("key") String key);

    /**
     * 根据key更新系统设置（key唯一索引）
     */
    int updateByKey(SystemSettingDO systemSetting);

    /**
     * 根据key存在则更新，不存在则插入（upsert）
     */
    int insertOrUpdateByKey(SystemSettingDO systemSetting);

    /**
     * 判断key是否存在
     */
    int existsByKey(@Param("key") String key);
}