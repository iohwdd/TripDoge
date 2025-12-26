package com.tripdog.service;

/**
 * 系统设置服务接口
 */
public interface SystemSettingService {

    /**
     * 获取系统设置值（字符串）
     */
    String get(String key);

    /**
     * 获取系统设置值（字符串），带默认值
     */
    String get(String key, String defaultValue);

    /**
     * 获取系统设置值（整数）
     */
    Integer getInt(String key);

    /**
     * 获取系统设置值（整数），带默认值
     */
    Integer getInt(String key, Integer defaultValue);

    /**
     * 获取系统设置值（长整数）
     */
    Long getLong(String key);

    /**
     * 获取系统设置值（长整数），带默认值
     */
    Long getLong(String key, Long defaultValue);

    /**
     * 获取系统设置值（布尔值）
     */
    Boolean getBoolean(String key);

    /**
     * 获取系统设置值（布尔值），带默认值
     */
    Boolean getBoolean(String key, Boolean defaultValue);

    /**
     * 设置系统设置值（如果key存在则更新，不存在则插入）
     */
    void set(String key, String value);

    /**
     * 删除系统设置
     */
    void delete(String key);

    /**
     * 重新加载所有系统设置到内存
     */
    void reload();

    /**
     * 检查设置是否存在
     */
    boolean exists(String key);
}