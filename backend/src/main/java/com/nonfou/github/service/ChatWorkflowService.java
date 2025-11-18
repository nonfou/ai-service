package com.nonfou.github.service;

import com.nonfou.github.config.BackendRoutingProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ChatResponse.Choice;
import com.nonfou.github.dto.response.ChatResponse.Message;
import com.nonfou.github.dto.response.ChatResponse.Usage;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.entity.User;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.service.proxy.ModelProxy;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.util.SessionUtil;
import com.nonfou.github.util.TokenEstimator;
import org.springframework.lang.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天请求整体编排，负责认证、路由、计费与日志。
 */
@Slf4j
@Service
public class ChatWorkflowService {

    private final ApiKeyService apiKeyService;
    private final BalanceService balanceService;
    private final BackendRoutingProperties routingProperties;
    private final TokenEstimator tokenEstimator;
    private final UsageMetricsService usageMetricsService;
    private final Map<String, ModelProxy> proxyLookup;

    public ChatWorkflowService(ApiKeyService apiKeyService,
                               BalanceService balanceService,
                               BackendRoutingProperties routingProperties,
                               TokenEstimator tokenEstimator,
                               List<ModelProxy> modelProxies,
                               @Nullable UsageMetricsService usageMetricsService) {
        this.apiKeyService = apiKeyService;
        this.balanceService = balanceService;
        this.routingProperties = routingProperties;
        this.tokenEstimator = tokenEstimator;
        this.usageMetricsService = usageMetricsService;
        this.proxyLookup = modelProxies.stream()
                .collect(Collectors.toMap(
                        proxy -> proxy.getProvider().toLowerCase(),
                        proxy -> proxy));
    }

    public ChatResponse handleChat(String authorization, ChatRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(false);

        ModelProxy proxy = selectProxy(request.getModel());
        try {
            ChatResponse response = proxy.chat(request, context.apiKey());
            recordSuccess(context, request, response, requestTime, proxy.getProvider());
            return response;
        } catch (ChatAuthorizationException ex) {
            throw ex;
        } catch (Exception ex) {
            recordFailure(context, request, requestTime, ex, proxy.getProvider());
            throw new ChatProcessingException("聊天服务暂时不可用，请稍后重试", ex);
        }
    }

    public SseEmitter handleStream(String authorization, ChatRequest request) {
        ChatContext context = authenticate(authorization);
        request.setStream(true);
        ModelProxy proxy = selectProxy(request.getModel());
        return proxy.chatStream(request, context.apiKey());
    }

    private void recordSuccess(ChatContext context,
                               ChatRequest request,
                               ChatResponse response,
                               LocalDateTime startTime,
                               String provider) {
        LocalDateTime endTime = LocalDateTime.now();
        Usage usage = resolveUsage(request, response);
        int inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
        int outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

        BigDecimal cost = balanceService.calculateCost(request.getModel(), inputTokens, outputTokens);
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildApiCall(context, request, startTime, endTime, duration, provider);
        apiCall.setInputTokens(inputTokens);
        apiCall.setOutputTokens(outputTokens);
        apiCall.setCost(cost);
        apiCall.setRawCost(cost);
        apiCall.setMarkupRate(BigDecimal.ONE);
        apiCall.setMarkupCost(BigDecimal.ZERO);
        apiCall.setStatus(1);

        Long apiCallId = balanceService.logApiCall(apiCall);
        apiCall.setId(apiCallId);
        balanceService.deductBalance(context.user().getId(), cost, apiCallId);
        apiKeyService.updateLastUsedTime(context.apiKeyValue());

        recordUsageMetrics(context.apiKey().getId(), apiCall);

        log.info("API调用成功: userId={}, model={}, tokens={}, cost={}",
                context.user().getId(),
                request.getModel(),
                inputTokens + outputTokens,
                cost);
    }

    private void recordFailure(ChatContext context,
                               ChatRequest request,
                               LocalDateTime startTime,
                               Exception exception,
                               String provider) {
        LocalDateTime endTime = LocalDateTime.now();
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildApiCall(context, request, startTime, endTime, duration, provider);
        apiCall.setInputTokens(0);
        apiCall.setOutputTokens(0);
        apiCall.setCost(BigDecimal.ZERO);
        apiCall.setRawCost(BigDecimal.ZERO);
        apiCall.setMarkupRate(BigDecimal.ONE);
        apiCall.setMarkupCost(BigDecimal.ZERO);
        apiCall.setStatus(0);
        apiCall.setErrorMsg(exception.getMessage());

        Long apiCallId = balanceService.logApiCall(apiCall);
        apiCall.setId(apiCallId);
        recordUsageMetrics(context.apiKey().getId(), apiCall);

        log.error("API调用失败: userId={}, error={}", context.user().getId(), exception.getMessage());
    }

    private ChatContext authenticate(String authorization) {
        String apiKeyValue = extractApiKey(authorization);
        if (apiKeyValue == null) {
            throw new ChatAuthorizationException(401, "无效的 Authorization Header");
        }

        ApiKey apiKey = apiKeyService.getApiKeyEntity(apiKeyValue);
        User user;
        if (apiKey == null) {
            user = balanceService.getUserByApiKey(apiKeyValue);
            if (user == null) {
                throw new ChatAuthorizationException(401, "无效的 API Key");
            }
            apiKey = buildLegacyApiKey(user);
        } else {
            user = balanceService.getUserById(apiKey.getUserId());
        }

        if (user == null) {
            throw new ChatAuthorizationException(401, "用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new ChatAuthorizationException(403, "账户已被禁用");
        }

        return new ChatContext(apiKeyValue, apiKey, user);
    }

    private ApiKey buildLegacyApiKey(User user) {
        ApiKey legacyApiKey = new ApiKey();
        legacyApiKey.setUserId(user.getId());
        legacyApiKey.setApiKey(user.getApiKey());
        legacyApiKey.setStatus(user.getStatus());
        legacyApiKey.setKeyName("legacy-user-api-key");
        return legacyApiKey;
    }

    private ModelProxy selectProxy(String model) {
        String provider = determineProvider(model);
        ModelProxy proxy = proxyLookup.get(provider.toLowerCase());
        if (proxy == null) {
            throw new IllegalStateException("未配置提供方: " + provider);
        }
        return proxy;
    }

    private String determineProvider(String model) {
        if (model == null || model.isEmpty()) {
            return routingProperties.getDefaultProvider();
        }

        if (model.contains("/")) {
            return "openrouter";
        }

        if (model.startsWith("gpt-") || model.startsWith("o1-") || model.startsWith("claude-")) {
            if ("openrouter".equalsIgnoreCase(routingProperties.getDefaultProvider())) {
                return "openrouter";
            }
            return "copilot";
        }

        return routingProperties.getDefaultProvider();
    }

    private String extractApiKey(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        } else if (authorization.startsWith("sk-")) {
            return authorization;
        }

        return null;
    }

    private record ChatContext(String apiKeyValue, ApiKey apiKey, User user) {
    }

    private Usage resolveUsage(ChatRequest request, ChatResponse response) {
        Usage usage = response.getUsage();
        if (usage != null && usage.getPromptTokens() != null && usage.getCompletionTokens() != null) {
            return usage;
        }

        TokenUsage estimate = tokenEstimator.estimateUsage(request, extractResponseText(response));

        Usage computed = usage != null ? usage : new Usage();
        computed.setPromptTokens(estimate.getInputTokens());
        computed.setCompletionTokens(estimate.getOutputTokens());
        computed.setTotalTokens(estimate.getInputTokens() + estimate.getOutputTokens());
        return computed;
    }

    private String extractResponseText(ChatResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        Choice choice = response.getChoices().get(0);
        Message message = choice != null ? choice.getMessage() : null;
        return message != null && message.getContent() != null ? message.getContent() : "";
    }

    private ApiCall buildApiCall(ChatContext context,
                                 ChatRequest request,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime,
                                 int duration,
                                 String provider) {
        ApiCall apiCall = new ApiCall();
        apiCall.setUserId(context.user().getId());
        apiCall.setApiKey(context.apiKeyValue());
        apiCall.setModel(request.getModel());
        apiCall.setProvider(provider);
        apiCall.setCacheReadTokens(0);
        apiCall.setCacheWriteTokens(0);
        apiCall.setRequestTime(startTime);
        apiCall.setResponseTime(endTime);
        apiCall.setDuration(duration);
        apiCall.setSessionHash(SessionUtil.generateSessionHash(
                context.apiKeyValue(),
                request.getModel(),
                request.getMessages()
        ));
        return apiCall;
    }

    private void recordUsageMetrics(Long apiKeyId, ApiCall apiCall) {
        if (usageMetricsService != null) {
            usageMetricsService.recordUsage(apiKeyId, apiCall);
        }
    }
}
