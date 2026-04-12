package com.nonfou.github.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nonfou.github.dto.request.AdminApiKeyCreateRequest;
import com.nonfou.github.dto.request.AdminApiKeyUpdateRequest;
import com.nonfou.github.dto.response.AdminApiKeyResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.User;
import com.nonfou.github.exception.BusinessException;
import com.nonfou.github.mapper.ApiKeyMapper;
import com.nonfou.github.mapper.UserMapper;
import com.nonfou.github.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 平台 API Key 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String SYSTEM_USER_EMAIL = "system@local";

    private final ApiKeyMapper apiKeyMapper;
    private final UserMapper userMapper;
    private final EncryptionUtil encryptionUtil;
    private final SystemConfigService systemConfigService;

    /**
     * 管理端获取平台 API Key 列表
     */
    public List<AdminApiKeyResponse> listAdminApiKeys() {
        Long systemUserId = ensureSystemUserId();
        return apiKeyMapper.selectList(systemUserKeyWrapper(systemUserId)).stream()
                .map(item -> toAdminResponse(item, false))
                .collect(Collectors.toList());
    }

    /**
     * 管理端创建平台 API Key
     */
    @Transactional
    public AdminApiKeyResponse createAdminApiKey(AdminApiKeyCreateRequest request) {
        Long systemUserId = ensureSystemUserId();
        String keyName = normalizeRequiredText(request.getKeyName(), "密钥名称不能为空");
        validateDuplicateKeyName(systemUserId, keyName, null);

        ApiKey entity = new ApiKey();
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setUserId(systemUserId);
        entity.setKeyName(keyName);
        entity.setApiKey(generateApiKey());
        entity.setRelayBaseUrl(normalizeRelayBaseUrl(request.getRelayBaseUrl()));
        entity.setUpstreamApiKey(encryptOptional(request.getUpstreamApiKey()));
        entity.setDescription(normalizeOptionalText(request.getDescription()));
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.insertDirect(entity);

        log.info("创建平台 API Key 成功: keyId={}, keyName={}", entity.getId(), entity.getKeyName());
        return toAdminResponse(entity, true);
    }

    /**
     * 管理端更新平台 API Key
     */
    @Transactional
    public void updateAdminApiKey(Long keyId, AdminApiKeyUpdateRequest request) {
        ApiKey apiKey = getAdminApiKey(keyId);

        if (StringUtils.hasText(request.getKeyName())) {
            String keyName = request.getKeyName().trim();
            validateDuplicateKeyName(apiKey.getUserId(), keyName, keyId);
            apiKey.setKeyName(keyName);
        }
        if (StringUtils.hasText(request.getRelayBaseUrl())) {
            apiKey.setRelayBaseUrl(normalizeRelayBaseUrl(request.getRelayBaseUrl()));
        }
        if (request.getUpstreamApiKey() != null) {
            apiKey.setUpstreamApiKey(encryptOptional(request.getUpstreamApiKey()));
        }
        if (request.getDescription() != null) {
            apiKey.setDescription(normalizeOptionalText(request.getDescription()));
        }

        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);
        log.info("更新平台 API Key 成功: keyId={}", keyId);
    }

    /**
     * 管理端更新平台 API Key 状态
     */
    @Transactional
    public void updateAdminApiKeyStatus(Long keyId, Integer status) {
        ApiKey apiKey = getAdminApiKey(keyId);
        apiKey.setStatus(status);
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);
        log.info("更新平台 API Key 状态: keyId={}, status={}", keyId, status);
    }

    /**
     * 管理端删除平台 API Key
     */
    @Transactional
    public void deleteAdminApiKey(Long keyId) {
        ApiKey apiKey = getAdminApiKey(keyId);
        apiKeyMapper.deleteById(apiKey.getId());
        log.info("删除平台 API Key: keyId={}", keyId);
    }

    /**
     * 管理端重新生成平台 API Key
     */
    @Transactional
    public AdminApiKeyResponse regenerateAdminApiKey(Long keyId) {
        ApiKey apiKey = getAdminApiKey(keyId);
        apiKey.setApiKey(generateApiKey());
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);

        log.info("重置平台 API Key: keyId={}", keyId);
        return toAdminResponse(apiKey, true);
    }

    /**
     * 根据客户端 API Key 获取路由配置
     */
    public ApiKey getRoutingApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, apiKey.trim())
                .eq(ApiKey::getStatus, 1)
                .orderByDesc(ApiKey::getId)
                .last("LIMIT 1");

        ApiKey entity = apiKeyMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }

        if (StringUtils.hasText(entity.getUpstreamApiKey())) {
            entity.setUpstreamApiKey(decryptOptional(entity.getUpstreamApiKey()));
        }
        if (!StringUtils.hasText(entity.getRelayBaseUrl())) {
            entity.setRelayBaseUrl(requireConfiguredRelayBaseUrl());
        }
        return entity;
    }

    /**
     * 根据管理端 ID 获取路由配置
     */
    public ApiKey getAdminRoutingApiKey(Long keyId) {
        ApiKey apiKey = getAdminApiKey(keyId);
        if (StringUtils.hasText(apiKey.getUpstreamApiKey())) {
            apiKey.setUpstreamApiKey(decryptOptional(apiKey.getUpstreamApiKey()));
        }
        if (!StringUtils.hasText(apiKey.getRelayBaseUrl())) {
            apiKey.setRelayBaseUrl(requireConfiguredRelayBaseUrl());
        }
        return apiKey;
    }

    /**
     * 更新最后使用时间
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

    private LambdaQueryWrapper<ApiKey> systemUserKeyWrapper(Long userId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt);
        return wrapper;
    }

    private ApiKey getAdminApiKey(Long keyId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, ensureSystemUserId())
                .eq(ApiKey::getId, keyId)
                .last("LIMIT 1");

        ApiKey apiKey = apiKeyMapper.selectOne(wrapper);
        if (apiKey == null) {
            throw new RuntimeException("API密钥不存在");
        }
        return apiKey;
    }

    private void validateDuplicateKeyName(Long userId, String keyName, Long excludeId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId)
                .eq(ApiKey::getKeyName, keyName);
        if (excludeId != null) {
            wrapper.ne(ApiKey::getId, excludeId);
        }
        if (apiKeyMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("密钥名称已存在");
        }
    }

    private AdminApiKeyResponse toAdminResponse(ApiKey apiKey, boolean showFullKey) {
        return AdminApiKeyResponse.builder()
                .id(String.valueOf(apiKey.getId()))
                .keyName(apiKey.getKeyName())
                .apiKey(showFullKey ? apiKey.getApiKey() : maskApiKey(apiKey.getApiKey()))
                .relayBaseUrl(StringUtils.hasText(apiKey.getRelayBaseUrl()) ? apiKey.getRelayBaseUrl() : resolveConfiguredRelayBaseUrl())
                .upstreamApiKey(maskEncryptedOptional(apiKey.getUpstreamApiKey()))
                .description(apiKey.getDescription())
                .status(apiKey.getStatus())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    private Long ensureSystemUserId() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, SYSTEM_USER_EMAIL)
                .last("LIMIT 1");

        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            return user.getId();
        }

        User entity = new User();
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setEmail(SYSTEM_USER_EMAIL);
        entity.setPassword("");
        entity.setBalance(BigDecimal.ZERO);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        userMapper.insertDirect(entity);
        return entity.getId();
    }

    private String requireConfiguredRelayBaseUrl() {
        String configured = resolveConfiguredRelayBaseUrl();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException("未配置 Copilot Relay 地址，请先在管理端为 API Key 填写转发地址");
        }
        return configured;
    }

    private String resolveConfiguredRelayBaseUrl() {
        String configured = systemConfigService.get("copilot_api_url");
        if (!StringUtils.hasText(configured)) {
            return null;
        }
        return normalizeRelayBaseUrl(configured);
    }

    private String normalizeRelayBaseUrl(String relayBaseUrl) {
        String value = normalizeRequiredText(relayBaseUrl, "转发地址不能为空");
        String normalized = value;
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.toLowerCase(Locale.ROOT).contains("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String encryptOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return encryptionUtil.encrypt(value.trim());
    }

    private String decryptOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return encryptionUtil.decrypt(value);
    }

    private String maskEncryptedOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return maskApiKey(decryptOptional(value));
    }

    private String generateApiKey() {
        return "sk-" + IdUtil.fastSimpleUUID();
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey) || apiKey.length() < 12) {
            return apiKey;
        }
        return apiKey.substring(0, 8) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
