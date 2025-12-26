package com.tripdog.service.impl;

import com.tripdog.mapper.SystemSettingMapper;
import com.tripdog.model.entity.SystemSettingDO;
import com.tripdog.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置服务实现类
 * 启动时将所有系统设置加载到内存，提供快速访问
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingMapper systemSettingMapper;

    /**
     * 内存缓存
     */
    private final ConcurrentHashMap<String, String> settingCache = new ConcurrentHashMap<>();

    /**
     * 项目启动时初始化，加载所有系统设置到内存
     */
    @PostConstruct
    public void init() {
        reload();
        log.info("系统设置已加载到内存，共 {} 条设置", settingCache.size());
    }

    /**
     * 获取系统设置值（字符串）
     */
    @Override
    public String get(String key) {
        return settingCache.get(key);
    }

    /**
     * 获取系统设置值（字符串），带默认值
     */
    @Override
    public String get(String key, String defaultValue) {
        return settingCache.getOrDefault(key, defaultValue);
    }

    /**
     * 获取系统设置值（整数）
     */
    @Override
    public Integer getInt(String key) {
        String value = settingCache.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("系统设置 {} 无法转换为Integer，值为: {}", key, value);
            return null;
        }
    }

    /**
     * 获取系统设置值（整数），带默认值
     */
    @Override
    public Integer getInt(String key, Integer defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取系统设置值（长整数）
     */
    @Override
    public Long getLong(String key) {
        String value = settingCache.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("系统设置 {} 无法转换为Long，值为: {}", key, value);
            return null;
        }
    }

    /**
     * 获取系统设置值（长整数），带默认值
     */
    @Override
    public Long getLong(String key, Long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取系统设置值（布尔值）
     */
    @Override
    public Boolean getBoolean(String key) {
        String value = settingCache.get(key);
        if (value == null) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * 获取系统设置值（布尔值），带默认值
     */
    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置系统设置值（如果key存在则更新，不存在则插入）
     * 同时更新内存缓存
     */
    @Override
    public void set(String key, String value) {
        SystemSettingDO setting = new SystemSettingDO();
        setting.setKey(key);
        setting.setValue(value);
        systemSettingMapper.insertOrUpdateByKey(setting);
        // 更新内存缓存
        settingCache.put(key, value);
        log.info("系统设置已更新: key={}, value={}", key, value);
    }

    /**
     * 删除系统设置
     * 同时移除内存缓存
     */
    @Override
    public void delete(String key) {
        systemSettingMapper.deleteByKey(key);
        settingCache.remove(key);
        log.info("系统设置已删除: key={}", key);
    }

    /**
     * 重新加载所有系统设置到内存
     * 适用于数据库设置被外部修改的场景
     */
    @Override
    public void reload() {
        settingCache.clear();
        List<SystemSettingDO> allSettings = systemSettingMapper.selectAll();
        for (SystemSettingDO setting : allSettings) {
            settingCache.put(setting.getKey(), setting.getValue());
        }
        log.info("系统设置已重新加载，共 {} 条设置", settingCache.size());
    }

    /**
     * 检查设置是否存在
     */
    @Override
    public boolean exists(String key) {
        return settingCache.containsKey(key);
    }
}