package com.tripdog.common.utils;

import com.tripdog.service.SystemSettingService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 系统设置工具类
 * 在任意地方都可以直接通过 SystemSettingUtils.get() 方式获取系统设置
 *
 * 使用示例：
 *  String value = SystemSettingUtils.get("api_key");
 *  int timeout = SystemSettingUtils.getInt("request_timeout", 30);
 *  boolean enabled = SystemSettingUtils.getBoolean("feature_enabled", false);
 */
@Component
public class SystemSettingUtils implements ApplicationContextAware {

    private static SystemSettingService systemSettingService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        systemSettingService = applicationContext.getBean(SystemSettingService.class);
    }

    /**
     * 获取系统设置值（字符串）
     */
    public static String get(String key) {
        return systemSettingService.get(key);
    }

    /**
     * 获取系统设置值（字符串），带默认值
     */
    public static String get(String key, String defaultValue) {
        return systemSettingService.get(key, defaultValue);
    }

    /**
     * 获取系统设置值（整数）
     */
    public static Integer getInt(String key) {
        return systemSettingService.getInt(key);
    }

    /**
     * 获取系统设置值（整数），带默认值
     */
    public static Integer getInt(String key, Integer defaultValue) {
        return systemSettingService.getInt(key, defaultValue);
    }

    /**
     * 获取系统设置值（长整数）
     */
    public static Long getLong(String key) {
        return systemSettingService.getLong(key);
    }

    /**
     * 获取系统设置值（长整数），带默认值
     */
    public static Long getLong(String key, Long defaultValue) {
        return systemSettingService.getLong(key, defaultValue);
    }

    /**
     * 获取系统设置值（布尔值）
     */
    public static Boolean getBoolean(String key) {
        return systemSettingService.getBoolean(key);
    }

    /**
     * 获取系统设置值（布尔值），带默认值
     */
    public static Boolean getBoolean(String key, Boolean defaultValue) {
        return systemSettingService.getBoolean(key, defaultValue);
    }

    /**
     * 设置系统设置值
     */
    public static void set(String key, String value) {
        systemSettingService.set(key, value);
    }

    /**
     * 删除系统设置
     */
    public static void delete(String key) {
        systemSettingService.delete(key);
    }

    /**
     * 重新加载所有系统设置
     */
    public static void reload() {
        systemSettingService.reload();
    }

    /**
     * 检查设置是否存在
     */
    public static boolean exists(String key) {
        return systemSettingService.exists(key);
    }
}