package com.nonfou.github.service;

import com.nonfou.github.config.BackendRoutingProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ChatResponse.Choice;
import com.nonfou.github.dto.response.ChatResponse.Message;
import com.nonfou.github.dto.response.ChatResponse.Usage;
import com.nonfou.github.dto.response.EmbeddingsResponse;
import com.nonfou.github.dto.response.ResponsesResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.ApiCall;
import com.nonfou.github.entity.User;
import com.nonfou.github.exception.BusinessException;
import com.nonfou.github.exception.ChatAuthorizationException;
import com.nonfou.github.exception.ChatProcessingException;
import com.nonfou.github.exception.ChatUpstreamException;
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
        } catch (ChatUpstreamException | BusinessException ex) {
            recordFailure(context, request, requestTime, ex, proxy.getProvider());
            throw ex;
        } catch (Exception ex) {
            recordFailure(context, request, requestTime, ex, proxy.getProvider());
            throw new ChatProcessingException("聊天服务暂时不可用，请稍后重试", ex);
        }
    }

    public SseEmitter handleStream(String authorization, ChatRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(true);
        ModelProxy proxy = selectProxy(request.getModel());

        // 创建计费回调
        StreamCompletionCallback callback = createStreamCallback(context, request, requestTime, proxy.getProvider());
        return proxy.chatStream(request, context.apiKey(), callback);
    }

    /**
     * Claude 格式流式请求处理
     * 返回 Anthropic SSE 格式的响应
     */
    public SseEmitter handleClaudeStream(String authorization, ChatRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(true);
        ModelProxy proxy = selectProxy(request.getModel());

        // 创建计费回调
        StreamCompletionCallback callback = createStreamCallback(context, request, requestTime, proxy.getProvider());
        return proxy.claudeStream(request, context.apiKey(), callback);
    }

    /**
     * 创建流式请求完成回调，用于计费
     */
    private StreamCompletionCallback createStreamCallback(ChatContext context, ChatRequest request,
                                                           LocalDateTime requestTime, String provider) {
        return (tokenUsage, success, errorMessage) -> {
            LocalDateTime endTime = LocalDateTime.now();
            int duration = (int) ChronoUnit.MILLIS.between(requestTime, endTime);

            ApiCall apiCall = buildApiCall(context, request, requestTime, endTime, duration, provider);
            apiCall.setInputTokens(tokenUsage.getInputTokens());
            apiCall.setOutputTokens(tokenUsage.getOutputTokens());

            if (success) {
                BigDecimal cost = balanceService.calculateCost(request.getModel(), tokenUsage);
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

                int totalTokens = tokenUsage.getInputTokens() + tokenUsage.getOutputTokens()
                        + tokenUsage.getCacheReadTokens() + tokenUsage.getCacheWriteTokens();
                log.info("流式API调用成功: userId={}, model={}, tokens={}, cacheRead={}, cacheWrite={}, cost={}",
                        context.user().getId(),
                        request.getModel(),
                        totalTokens,
                        tokenUsage.getCacheReadTokens(),
                        tokenUsage.getCacheWriteTokens(),
                        cost);
            } else {
                apiCall.setCost(BigDecimal.ZERO);
                apiCall.setRawCost(BigDecimal.ZERO);
                apiCall.setMarkupRate(BigDecimal.ONE);
                apiCall.setMarkupCost(BigDecimal.ZERO);
                apiCall.setStatus(0);
                apiCall.setErrorMsg(errorMessage);

                Long apiCallId = balanceService.logApiCall(apiCall);
                apiCall.setId(apiCallId);
                recordUsageMetrics(context.apiKey().getId(), apiCall);

                log.error("流式API调用失败: userId={}, error={}", context.user().getId(), errorMessage);
            }
        };
    }

    // ============================================================
    // Responses API 支持（用于 Codex 系列模型）
    // ============================================================

    /**
     * 非流式 Responses 请求处理
     */
    public ResponsesResponse handleResponses(String authorization, ResponsesRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(false);

        CopilotProxyService copilotProxy = getCopilotProxy();
        try {
            ResponsesResponse response = copilotProxy.responses(request, context.apiKey());
            recordResponsesSuccess(context, request, response, requestTime);
            return response;
        } catch (ChatAuthorizationException ex) {
            throw ex;
        } catch (ChatUpstreamException | BusinessException ex) {
            recordResponsesFailure(context, request, requestTime, ex);
            throw ex;
        } catch (Exception ex) {
            recordResponsesFailure(context, request, requestTime, ex);
            throw new ChatProcessingException("Responses 服务暂时不可用，请稍后重试", ex);
        }
    }

    /**
     * 流式 Responses 请求处理
     */
    public SseEmitter handleResponsesStream(String authorization, ResponsesRequest request) {
        ChatContext context = authenticate(authorization);
        request.setStream(true);
        CopilotProxyService copilotProxy = getCopilotProxy();
        return copilotProxy.responsesStream(request, context.apiKey());
    }

    // ============================================================
    // Embeddings API 支持
    // ============================================================

    /**
     * Embeddings 请求处理
     */
    public EmbeddingsResponse handleEmbeddings(String authorization, EmbeddingsRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();

        CopilotProxyService copilotProxy = getCopilotProxy();
        try {
            EmbeddingsResponse response = copilotProxy.embeddings(request, context.apiKey());
            recordEmbeddingsSuccess(context, request, response, requestTime);
            return response;
        } catch (ChatAuthorizationException ex) {
            throw ex;
        } catch (ChatUpstreamException | BusinessException ex) {
            recordEmbeddingsFailure(context, request, requestTime, ex);
            throw ex;
        } catch (Exception ex) {
            recordEmbeddingsFailure(context, request, requestTime, ex);
            throw new ChatProcessingException("Embeddings 服务暂时不可用，请稍后重试", ex);
        }
    }

    private void recordEmbeddingsSuccess(ChatContext context,
                                          EmbeddingsRequest request,
                                          EmbeddingsResponse response,
                                          LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        int promptTokens = 0;
        int totalTokens = 0;

        if (response.getUsage() != null) {
            promptTokens = response.getUsage().getPromptTokens() != null ? response.getUsage().getPromptTokens() : 0;
            totalTokens = response.getUsage().getTotalTokens() != null ? response.getUsage().getTotalTokens() : 0;
        }

        BigDecimal cost = balanceService.calculateCost(request.getModel(), promptTokens, 0);
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildEmbeddingsApiCall(context, request, startTime, endTime, duration);
        apiCall.setInputTokens(promptTokens);
        apiCall.setOutputTokens(0);
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

        log.info("Embeddings API调用成功: userId={}, model={}, tokens={}, cost={}",
                context.user().getId(),
                request.getModel(),
                totalTokens,
                cost);
    }

    private void recordEmbeddingsFailure(ChatContext context,
                                          EmbeddingsRequest request,
                                          LocalDateTime startTime,
                                          Exception ex) {
        LocalDateTime endTime = LocalDateTime.now();
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildEmbeddingsApiCall(context, request, startTime, endTime, duration);
        apiCall.setInputTokens(0);
        apiCall.setOutputTokens(0);
        apiCall.setCost(BigDecimal.ZERO);
        apiCall.setRawCost(BigDecimal.ZERO);
        apiCall.setMarkupRate(BigDecimal.ONE);
        apiCall.setMarkupCost(BigDecimal.ZERO);
        apiCall.setStatus(0);
        apiCall.setErrorMsg(ex.getMessage());

        balanceService.logApiCall(apiCall);
        log.warn("Embeddings API调用失败: userId={}, model={}, error={}",
                context.user().getId(),
                request.getModel(),
                ex.getMessage());
    }

    private ApiCall buildEmbeddingsApiCall(ChatContext context,
                                            EmbeddingsRequest request,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime,
                                            int duration) {
        ApiCall apiCall = new ApiCall();
        apiCall.setUserId(context.user().getId());
        apiCall.setApiKey(context.apiKeyValue());
        apiCall.setModel(request.getModel());
        apiCall.setProvider("copilot");
        apiCall.setCacheReadTokens(0);
        apiCall.setCacheWriteTokens(0);
        apiCall.setRequestTime(startTime);
        apiCall.setResponseTime(endTime);
        apiCall.setDuration(duration);
        apiCall.setSessionHash(SessionUtil.generateSessionHash(
                context.apiKeyValue(),
                request.getModel(),
                null
        ));
        return apiCall;
    }

    private CopilotProxyService getCopilotProxy() {
        ModelProxy proxy = proxyLookup.get("copilot");
        if (proxy == null || !(proxy instanceof CopilotProxyService)) {
            throw new IllegalStateException("未配置 Copilot 代理服务");
        }
        return (CopilotProxyService) proxy;
    }

    private void recordResponsesSuccess(ChatContext context,
                                         ResponsesRequest request,
                                         ResponsesResponse response,
                                         LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        int inputTokens = 0;
        int outputTokens = 0;

        if (response.getUsage() != null) {
            inputTokens = response.getUsage().getInputTokens() != null ? response.getUsage().getInputTokens() : 0;
            outputTokens = response.getUsage().getOutputTokens() != null ? response.getUsage().getOutputTokens() : 0;
        }

        BigDecimal cost = balanceService.calculateCost(request.getModel(), inputTokens, outputTokens);
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildResponsesApiCall(context, request, startTime, endTime, duration);
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

        log.info("Responses API调用成功: userId={}, model={}, tokens={}, cost={}",
                context.user().getId(),
                request.getModel(),
                inputTokens + outputTokens,
                cost);
    }

    private void recordResponsesFailure(ChatContext context,
                                         ResponsesRequest request,
                                         LocalDateTime startTime,
                                         Exception exception) {
        LocalDateTime endTime = LocalDateTime.now();
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildResponsesApiCall(context, request, startTime, endTime, duration);
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

        log.error("Responses API调用失败: userId={}, error={}", context.user().getId(), exception.getMessage());
    }

    private ApiCall buildResponsesApiCall(ChatContext context,
                                           ResponsesRequest request,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           int duration) {
        ApiCall apiCall = new ApiCall();
        apiCall.setUserId(context.user().getId());
        apiCall.setApiKey(context.apiKeyValue());
        apiCall.setModel(request.getModel());
        apiCall.setProvider("copilot");
        apiCall.setCacheReadTokens(0);
        apiCall.setCacheWriteTokens(0);
        apiCall.setRequestTime(startTime);
        apiCall.setResponseTime(endTime);
        apiCall.setDuration(duration);
        apiCall.setSessionHash(SessionUtil.generateSessionHash(
                context.apiKeyValue(),
                request.getModel(),
                null
        ));
        return apiCall;
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

        // 提取缓存 token 信息
        int cacheReadTokens = 0;
        int cacheWriteTokens = 0;

        // OpenAI 格式: prompt_tokens_details.cached_tokens
        if (usage.getPromptTokensDetails() != null && usage.getPromptTokensDetails().getCachedTokens() != null) {
            cacheReadTokens = usage.getPromptTokensDetails().getCachedTokens();
        }
        // Anthropic 格式: cache_read_input_tokens
        if (usage.getCacheReadInputTokens() != null) {
            cacheReadTokens = usage.getCacheReadInputTokens();
        }
        // Anthropic 格式: cache_creation_input_tokens
        if (usage.getCacheCreationInputTokens() != null) {
            cacheWriteTokens = usage.getCacheCreationInputTokens();
        }

        // 使用支持缓存 token 的计费方法
        TokenUsage tokenUsage = TokenUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheReadTokens(cacheReadTokens)
                .cacheWriteTokens(cacheWriteTokens)
                .build();
        BigDecimal cost = balanceService.calculateCost(request.getModel(), tokenUsage);

        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildApiCall(context, request, startTime, endTime, duration, provider);
        apiCall.setInputTokens(inputTokens);
        apiCall.setOutputTokens(outputTokens);
        apiCall.setCacheReadTokens(cacheReadTokens);
        apiCall.setCacheWriteTokens(cacheWriteTokens);
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

        int totalTokens = inputTokens + outputTokens + cacheReadTokens + cacheWriteTokens;
        log.info("API调用成功: userId={}, model={}, tokens={}, cacheRead={}, cacheWrite={}, cost={}",
                context.user().getId(),
                request.getModel(),
                totalTokens,
                cacheReadTokens,
                cacheWriteTokens,
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
        Choice choice = response.getChoices().getFirst();
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
