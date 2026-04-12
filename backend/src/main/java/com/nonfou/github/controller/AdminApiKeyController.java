package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.AdminApiKeyCreateRequest;
import com.nonfou.github.dto.request.AdminApiKeyStatusRequest;
import com.nonfou.github.dto.request.AdminApiKeyUpdateRequest;
import com.nonfou.github.dto.response.AdminApiKeyResponse;
import com.nonfou.github.service.ApiKeyService;
import com.nonfou.github.service.CopilotProxyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * 管理端 API Key 管理 Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api-keys")
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;
    private final CopilotProxyService copilotProxyService;

    @GetMapping
    public Result<List<AdminApiKeyResponse>> listApiKeys() {
        try {
            return Result.success(apiKeyService.listAdminApiKeys());
        } catch (Exception e) {
            log.error("管理端获取 API Key 列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<AdminApiKeyResponse> createApiKey(@Valid @RequestBody AdminApiKeyCreateRequest request) {
        try {
            return Result.success(apiKeyService.createAdminApiKey(request));
        } catch (Exception e) {
            log.error("管理端创建 API Key 失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{keyId}")
    public Result<Void> updateApiKey(
            @PathVariable Long keyId,
            @RequestBody AdminApiKeyUpdateRequest request
    ) {
        try {
            apiKeyService.updateAdminApiKey(keyId, request);
            return Result.success();
        } catch (Exception e) {
            log.error("管理端更新 API Key 失败: keyId={}", keyId, e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{keyId}/status")
    public Result<Void> updateApiKeyStatus(
            @PathVariable Long keyId,
            @Valid @RequestBody AdminApiKeyStatusRequest request
    ) {
        try {
            apiKeyService.updateAdminApiKeyStatus(keyId, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            log.error("管理端更新 API Key 状态失败: keyId={}", keyId, e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{keyId}")
    public Result<Void> deleteApiKey(@PathVariable Long keyId) {
        try {
            apiKeyService.deleteAdminApiKey(keyId);
            return Result.success();
        } catch (Exception e) {
            log.error("管理端删除 API Key 失败: keyId={}", keyId, e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{keyId}/regenerate")
    public Result<AdminApiKeyResponse> regenerateApiKey(@PathVariable Long keyId) {
        try {
            return Result.success(apiKeyService.regenerateAdminApiKey(keyId));
        } catch (Exception e) {
            log.error("管理端重置 API Key 失败: keyId={}", keyId, e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{keyId}/usage")
    public Result<JsonNode> getUsage(@PathVariable Long keyId) {
        try {
            return Result.success(copilotProxyService.getUsage(apiKeyService.getAdminRoutingApiKey(keyId)));
        } catch (Exception e) {
            log.error("管理端获取 API Key usage 失败: keyId={}", keyId, e);
            return Result.error(e.getMessage());
        }
    }
}
