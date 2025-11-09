package com.nonfou.github.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.dto.request.CreateApiKeyRequest;
import com.nonfou.github.dto.response.ApiKeyResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.mapper.ApiKeyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API密钥服务
 */
@Slf4j
@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    /**
     * 创建API密钥
     */
    @Transactional
    public ApiKeyResponse createApiKey(Long userId, CreateApiKeyRequest request) {
        // 检查密钥名称是否重复
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
                .eq(ApiKey::getKeyName, request.getKeyName());
        if (apiKeyMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("密钥名称已存在");
        }

        // 生成API密钥
        String apiKey = generateApiKey();

        // 创建密钥记录
        ApiKey entity = new ApiKey();
        entity.setUserId(userId);
        entity.setKeyName(request.getKeyName());
        entity.setApiKey(apiKey);
        entity.setStatus(1); // 默认启用
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        apiKeyMapper.insert(entity);
        log.info("API密钥创建成功: userId={}, keyName={}", userId, request.getKeyName());

        // 返回完整密钥（仅在创建时返回）
        return ApiKeyResponse.builder()
                .id(entity.getId())
                .keyName(entity.getKeyName())
                .apiKey(apiKey) // 完整密钥
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 获取用户所有API密钥列表
     */
    public List<ApiKeyResponse> getUserApiKeys(Long userId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt);

        List<ApiKey> keys = apiKeyMapper.selectList(wrapper);

        return keys.stream().map(key -> ApiKeyResponse.builder()
                .id(key.getId())
                .keyName(key.getKeyName())
                .apiKey(maskApiKey(key.getApiKey())) // 脱敏
                .status(key.getStatus())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * 更新密钥状态（启用/禁用）
     */
    @Transactional
    public void updateApiKeyStatus(Long userId, Long keyId, Integer status) {
        // 验证密钥归属
        ApiKey apiKey = getApiKeyByUserAndId(userId, keyId);
        if (apiKey == null) {
            throw new RuntimeException("API密钥不存在");
        }

        apiKey.setStatus(status);
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);

        log.info("API密钥状态更新: userId={}, keyId={}, status={}", userId, keyId, status);
    }

    /**
     * 删除API密钥
     */
    @Transactional
    public void deleteApiKey(Long userId, Long keyId) {
        // 验证密钥归属
        ApiKey apiKey = getApiKeyByUserAndId(userId, keyId);
        if (apiKey == null) {
            throw new RuntimeException("API密钥不存在");
        }

        apiKeyMapper.deleteById(keyId);
        log.info("API密钥删除成功: userId={}, keyId={}", userId, keyId);
    }

    /**
     * 重新生成API密钥
     */
    @Transactional
    public ApiKeyResponse regenerateApiKey(Long userId, Long keyId) {
        // 验证密钥归属
        ApiKey apiKey = getApiKeyByUserAndId(userId, keyId);
        if (apiKey == null) {
            throw new RuntimeException("API密钥不存在");
        }

        // 生成新密钥
        String newApiKey = generateApiKey();
        apiKey.setApiKey(newApiKey);
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);

        log.info("API密钥重新生成: userId={}, keyId={}", userId, keyId);

        // 返回完整新密钥
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .keyName(apiKey.getKeyName())
                .apiKey(newApiKey) // 完整新密钥
                .status(apiKey.getStatus())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    /**
     * 根据API密钥查询用户ID（用于认证）
     */
    public Long getUserIdByApiKey(String apiKey) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey)
                .eq(ApiKey::getStatus, 1); // 仅查询启用的密钥

        ApiKey key = apiKeyMapper.selectOne(wrapper);
        return key != null ? key.getUserId() : null;
    }

    /**
     * 更新密钥最后使用时间
     */
    @Transactional
    public void updateLastUsedTime(String apiKey) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey);

        ApiKey key = apiKeyMapper.selectOne(wrapper);
        if (key != null) {
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyMapper.updateById(key);
        }
    }

    /**
     * 验证密钥是否有效
     */
    public boolean validateApiKey(String apiKey) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey)
                .eq(ApiKey::getStatus, 1);

        return apiKeyMapper.selectCount(wrapper) > 0;
    }

    /**
     * 根据API密钥字符串获取ApiKey实体（用于多账户调度）
     */
    public ApiKey getApiKeyEntity(String apiKey) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey)
                .eq(ApiKey::getStatus, 1); // 仅查询启用的密钥
        return apiKeyMapper.selectOne(wrapper);
    }

    /**
     * 获取指定用户的指定密钥
     */
    private ApiKey getApiKeyByUserAndId(Long userId, Long keyId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
                .eq(ApiKey::getId, keyId);
        return apiKeyMapper.selectOne(wrapper);
    }

    /**
     * 生成API密钥
     */
    private String generateApiKey() {
        return "sk-" + IdUtil.fastSimpleUUID();
    }

    /**
     * 脱敏API密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return apiKey;
        }
        // 保留前8位和后4位,中间用***代替
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
