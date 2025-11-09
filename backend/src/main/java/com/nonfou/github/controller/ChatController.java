package com.nonfou.github.controller;

import com.nonfou.github.common.Result;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.User;
import com.nonfou.github.service.ApiKeyService;
import com.nonfou.github.service.BalanceService;
import com.nonfou.github.service.CopilotProxyService;
import com.nonfou.github.service.OpenRouterProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 聊天 API Controller - 多账户增强版
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private CopilotProxyService copilotProxyService;

    @Autowired
    private OpenRouterProxyService openRouterProxyService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Value("${backend.routing.default-provider:copilot}")
    private String defaultProvider;

    /**
     * 聊天接口（非流式）
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ChatRequest request) {

        LocalDateTime requestTime = LocalDateTime.now();

        // 提取 API Key
        String apiKeyString = extractApiKey(authorization);
        if (apiKeyString == null) {
            return Result.error(401, "无效的 Authorization Header");
        }

        // 获取 ApiKey 实体（用于多账户调度）
        ApiKey apiKey = apiKeyService.getApiKeyEntity(apiKeyString);
        if (apiKey == null) {
            // 降级到users表验证（兼容旧密钥）
            User user = balanceService.getUserByApiKey(apiKeyString);
            if (user == null) {
                return Result.error(401, "无效的 API Key");
            }
            // 为兼容性创建临时 ApiKey 对象
            apiKey = new ApiKey();
            apiKey.setUserId(user.getId());
            apiKey.setApiKey(apiKeyString);
        }

        // 获取用户信息
        User user = balanceService.getUserById(apiKey.getUserId());
        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            return Result.error(403, "账户已被禁用");
        }

        try {
            // 确保非流式
            request.setStream(false);

            // 根据配置选择代理服务（Copilot 或 OpenRouter）
            ChatResponse response = routeRequest(request, apiKey);
            LocalDateTime responseTime = LocalDateTime.now();

            // 计算 tokens 和费用
            int inputTokens = response.getUsage().getPromptTokens();
            int outputTokens = response.getUsage().getCompletionTokens();
            BigDecimal cost = balanceService.calculateCost(request.getModel(), inputTokens, outputTokens);

            // 计算耗时
            int duration = (int) ChronoUnit.MILLIS.between(requestTime, responseTime);

            // 记录 API 调用
            Long apiCallId = balanceService.logApiCall(
                    user.getId(), apiKeyString, request.getModel(),
                    inputTokens, outputTokens, cost,
                    requestTime, responseTime, duration,
                    1, null
            );

            // 扣除余额（注意：多账户系统使用 QuotaService，这里保持兼容）
            balanceService.deductBalance(user.getId(), cost, apiCallId);

            // 更新密钥最后使用时间
            apiKeyService.updateLastUsedTime(apiKeyString);

            log.info("API调用成功: userId={}, model={}, tokens={}, cost={}",
                    user.getId(), request.getModel(), inputTokens + outputTokens, cost);

            return Result.success(response);

        } catch (Exception e) {
            LocalDateTime responseTime = LocalDateTime.now();
            int duration = (int) ChronoUnit.MILLIS.between(requestTime, responseTime);

            // 记录失败的调用
            balanceService.logApiCall(
                    user.getId(), apiKeyString, request.getModel(),
                    0, 0, BigDecimal.ZERO,
                    requestTime, responseTime, duration,
                    0, e.getMessage()
            );

            log.error("API调用失败: userId={}, error={}", user.getId(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 兼容 OpenAI 格式的接口（非流式）
     */
    @PostMapping("/v1/chat/completions")
    public Object chatCompletions(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Validated ChatRequest request) {

        // 如果是流式请求，返回 SseEmitter
        if (Boolean.TRUE.equals(request.getStream())) {
            return chatCompletionsStream(authorization, request);
        }

        // 非流式请求
        return chat(authorization, request);
    }

    /**
     * 流式聊天接口
     */
    public SseEmitter chatCompletionsStream(
            String authorization,
            ChatRequest request) {

        // 提取 API Key
        String apiKeyString = extractApiKey(authorization);
        if (apiKeyString == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("无效的 Authorization Header"));
            return emitter;
        }

        // 获取 ApiKey 实体
        ApiKey apiKey = apiKeyService.getApiKeyEntity(apiKeyString);
        if (apiKey == null) {
            // 降级到users表验证
            User user = balanceService.getUserByApiKey(apiKeyString);
            if (user == null) {
                SseEmitter emitter = new SseEmitter();
                emitter.completeWithError(new RuntimeException("无效的 API Key"));
                return emitter;
            }
            // 为兼容性创建临时 ApiKey 对象
            apiKey = new ApiKey();
            apiKey.setUserId(user.getId());
            apiKey.setApiKey(apiKeyString);
        }

        // 获取用户信息
        User user = balanceService.getUserById(apiKey.getUserId());
        if (user == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("用户不存在"));
            return emitter;
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("账户已被禁用"));
            return emitter;
        }

        try {
            // 确保流式
            request.setStream(true);

            // 根据配置选择代理服务
            return routeStreamRequest(request, apiKey);

        } catch (Exception e) {
            log.error("流式API调用失败: userId={}, error={}", user.getId(), e.getMessage());
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * 路由请求到合适的代理服务（非流式）
     */
    private ChatResponse routeRequest(ChatRequest request, ApiKey apiKey) {
        // 判断使用哪个提供商
        String provider = determineProvider(request.getModel());

        if ("openrouter".equalsIgnoreCase(provider)) {
            return openRouterProxyService.chat(request, apiKey);
        } else {
            // 默认使用 Copilot
            return copilotProxyService.chat(request, apiKey);
        }
    }

    /**
     * 路由流式请求到合适的代理服务
     */
    private SseEmitter routeStreamRequest(ChatRequest request, ApiKey apiKey) {
        String provider = determineProvider(request.getModel());

        if ("openrouter".equalsIgnoreCase(provider)) {
            return openRouterProxyService.chatStream(request, apiKey);
        } else {
            return copilotProxyService.chatStream(request, apiKey);
        }
    }

    /**
     * 根据模型名称判断使用哪个提供商
     */
    private String determineProvider(String model) {
        if (model == null) {
            return defaultProvider;
        }

        // OpenRouter 通常使用 provider/model 格式，如 openai/gpt-4, anthropic/claude-3
        if (model.contains("/")) {
            return "openrouter";
        }

        // Copilot 支持的模型（可配置）
        if (model.startsWith("gpt-") || model.startsWith("o1-") || model.startsWith("claude-")) {
            // 检查是否配置了 OpenRouter 作为默认
            if ("openrouter".equalsIgnoreCase(defaultProvider)) {
                return "openrouter";
            }
            return "copilot";
        }

        // 默认
        return defaultProvider;
    }

    /**
     * 提取 API Key
     */
    private String extractApiKey(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        // 支持两种格式：
        // 1. Bearer sk-xxx
        // 2. sk-xxx
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        } else if (authorization.startsWith("sk-")) {
            return authorization;
        }

        return null;
    }
}
