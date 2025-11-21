package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.CreateApiKeyRequest;
import com.nonfou.github.dto.response.ApiKeyResponse;
import com.nonfou.github.service.ApiKeyService;
import com.nonfou.github.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * API密钥管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user/api-keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    /**
     * 获取用户所有API密钥
     */
    @GetMapping
    public Result<List<ApiKeyResponse>> getUserApiKeys() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        List<ApiKeyResponse> keys = apiKeyService.getUserApiKeys(userId);
        return Result.success(keys);
    }

    /**
     * 创建API密钥
     */
    @PostMapping
    public Result<ApiKeyResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            ApiKeyResponse apiKey = apiKeyService.createApiKey(userId, request);
            return Result.success(apiKey);
        } catch (Exception e) {
            log.error("创建API密钥失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新API密钥状态
     */
    @PutMapping("/{keyId}")
    public Result<Void> updateApiKeyStatus(
            @PathVariable Long keyId,
            @RequestBody Map<String, Integer> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            Integer status = body.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态值无效");
            }

            apiKeyService.updateApiKeyStatus(userId, keyId, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新API密钥状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除API密钥
     */
    @DeleteMapping("/{keyId}")
    public Result<Void> deleteApiKey(@PathVariable Long keyId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            apiKeyService.deleteApiKey(userId, keyId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除API密钥失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重新生成API密钥
     */
    @PostMapping("/{keyId}/regenerate")
    public Result<ApiKeyResponse> regenerateApiKey(@PathVariable Long keyId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未授权");
        }

        try {
            ApiKeyResponse apiKey = apiKeyService.regenerateApiKey(userId, keyId);
            return Result.success(apiKey);
        } catch (Exception e) {
            log.error("重新生成API密钥失败", e);
            return Result.error(e.getMessage());
        }
    }
}
