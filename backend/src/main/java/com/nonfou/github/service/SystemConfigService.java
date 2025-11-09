package com.nonfou.github.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.entity.SystemConfig;
import com.nonfou.github.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务
 */
@Slf4j
@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    private final Map<String, String> configCache = new HashMap<>();

    /**
     * 获取配置值
     */
    public String get(String key) {
        // 先从缓存获取
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }

        // 从数据库获取
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            configCache.put(key, config.getConfigValue());
            return config.getConfigValue();
        }

        return null;
    }

    /**
     * 获取配置值（带默认值）
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 BigDecimal 配置
     */
    public BigDecimal getBigDecimal(String key) {
        String value = get(key);
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    /**
     * 获取 Integer 配置
     */
    public Integer getInteger(String key) {
        String value = get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 更新配置
     */
    public void update(String key, String value) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setConfigValue(value);
            systemConfigMapper.updateById(config);
            // 更新缓存
            configCache.put(key, value);
            log.info("配置已更新: {} = {}", key, value);
        }
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        configCache.clear();
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        for (SystemConfig config : configs) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
        log.info("配置缓存已刷新，共 {} 项", configs.size());
    }
}
