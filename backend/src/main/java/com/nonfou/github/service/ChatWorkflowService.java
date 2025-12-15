package com.nonfou.github.service;

import com.nonfou.github.config.BackendRoutingProperties;
import com.nonfou.github.config.CopilotProxyProperties;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.request.ClaudeRequest;
import com.nonfou.github.dto.request.EmbeddingsRequest;
import com.nonfou.github.dto.request.ResponsesRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.dto.response.ChatResponse.Choice;
import com.nonfou.github.dto.response.ChatResponse.Message;
import com.nonfou.github.dto.response.ChatResponse.Usage;
import com.nonfou.github.dto.response.ClaudeResponse;
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
    private final CopilotProxyProperties copilotProxyProperties;
    private final TokenEstimator tokenEstimator;
    private final UsageMetricsService usageMetricsService;
    private final ModelService modelService;
    private final Map<String, ModelProxy> proxyLookup;

    public ChatWorkflowService(ApiKeyService apiKeyService,
                               BalanceService balanceService,
                               BackendRoutingProperties routingProperties,
                               CopilotProxyProperties copilotProxyProperties,
                               TokenEstimator tokenEstimator,
                               List<ModelProxy> modelProxies,
                               @Nullable UsageMetricsService usageMetricsService,
                               ModelService modelService) {
        this.apiKeyService = apiKeyService;
        this.balanceService = balanceService;
        this.routingProperties = routingProperties;
        this.copilotProxyProperties = copilotProxyProperties;
        this.tokenEstimator = tokenEstimator;
        this.usageMetricsService = usageMetricsService;
        this.modelService = modelService;
        this.proxyLookup = modelProxies.stream()
                .collect(Collectors.toMap(
                        proxy -> proxy.getProvider().toLowerCase(),
                        proxy -> proxy));
    }

    public ChatResponse handleChat(String authorization, ChatRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(false);

        // Token 预检测：在发送请求前检查是否超过模型限制
        validateInputTokens(request);

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

        // Token 预检测：在发送请求前检查是否超过模型限制
        validateInputTokens(request);

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

        // Token 预检测：在发送请求前检查是否超过模型限制
        validateInputTokens(request);

        ModelProxy proxy = selectProxy(request.getModel());

        // 创建计费回调
        StreamCompletionCallback callback = createStreamCallback(context, request, requestTime, proxy.getProvider());
        return proxy.claudeStream(request, context.apiKey(), callback);
    }

    // ============================================================
    // Claude Messages API 专用方法
    // ============================================================

    /**
     * Claude Messages API 非流式请求处理
     * 返回 Anthropic Messages API 格式的响应
     */
    public ClaudeResponse handleClaudeChat(String authorization, ClaudeRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(false);

        // 热身请求优化：检测 Claude Code 热身请求并使用小模型
        applyWarmupModelOptimization(request);

        // Token 预检测：在发送请求前检查是否超过模型限制
        validateClaudeInputTokens(request);

        CopilotProxyService copilotProxy = getCopilotProxy();
        try {
            ClaudeResponse response = copilotProxy.claudeMessages(request, context.apiKey());
            recordClaudeSuccess(context, request, response, requestTime);
            return response;
        } catch (ChatAuthorizationException ex) {
            throw ex;
        } catch (ChatUpstreamException | BusinessException ex) {
            recordClaudeFailure(context, request, requestTime, ex);
            throw ex;
        } catch (Exception ex) {
            recordClaudeFailure(context, request, requestTime, ex);
            throw new ChatProcessingException("Claude 服务暂时不可用，请稍后重试", ex);
        }
    }

    /**
     * Claude Messages API 流式请求处理
     * 返回 Anthropic SSE 格式的响应
     */
    public SseEmitter handleClaudeStreamRequest(String authorization, ClaudeRequest request) {
        ChatContext context = authenticate(authorization);
        LocalDateTime requestTime = LocalDateTime.now();
        request.setStream(true);

        // 热身请求优化：检测 Claude Code 热身请求并使用小模型
        applyWarmupModelOptimization(request);

        // Token 预检测：在发送请求前检查是否超过模型限制
        validateClaudeInputTokens(request);

        CopilotProxyService copilotProxy = getCopilotProxy();
        // 创建计费回调
        StreamCompletionCallback callback = createClaudeStreamCallback(context, request, requestTime);
        return copilotProxy.claudeMessagesStream(request, context.apiKey(), callback);
    }

    /**
     * 检测 Claude Code 热身请求并使用小模型替代
     * 当请求有 anthropic-beta 头且无 tools 时，强制使用配置的小模型
     * 用于减少 Claude Code 2.0.28+ 热身请求的高级模型配额消耗
     */
    private void applyWarmupModelOptimization(ClaudeRequest request) {
        // 检测条件：有 anthropic-beta 头且无 tools
        boolean hasAnthropicBeta = request.getAnthropicBeta() != null && !request.getAnthropicBeta().isEmpty();
        boolean noTools = request.getTools() == null || request.getTools().isEmpty();

        if (hasAnthropicBeta && noTools) {
            String warmupModel = copilotProxyProperties.getWarmupModel();
            if (warmupModel != null && !warmupModel.isEmpty()) {
                String originalModel = request.getModel();
                request.setModel(warmupModel);
                log.debug("Claude Code 热身请求优化: {} -> {}", originalModel, warmupModel);
            }
        }
    }

    /**
     * 创建 Claude 流式请求完成回调，用于计费
     */
    private StreamCompletionCallback createClaudeStreamCallback(ChatContext context, ClaudeRequest request,
                                                                  LocalDateTime requestTime) {
        return (tokenUsage, success, errorMessage) -> {
            LocalDateTime endTime = LocalDateTime.now();
            int duration = (int) ChronoUnit.MILLIS.between(requestTime, endTime);

            ApiCall apiCall = buildClaudeApiCall(context, request, requestTime, endTime, duration);
            apiCall.setInputTokens(tokenUsage.getInputTokens());
            apiCall.setOutputTokens(tokenUsage.getOutputTokens());
            apiCall.setCacheReadTokens(tokenUsage.getCacheReadTokens());
            apiCall.setCacheWriteTokens(tokenUsage.getCacheWriteTokens());

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
                log.info("Claude 流式API调用成功: userId={}, model={}, tokens={}, cacheRead={}, cacheWrite={}, cost={}",
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

                log.error("Claude 流式API调用失败: userId={}, error={}", context.user().getId(), errorMessage);
            }
        };
    }

    private void recordClaudeSuccess(ChatContext context,
                                      ClaudeRequest request,
                                      ClaudeResponse response,
                                      LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        int inputTokens = 0;
        int outputTokens = 0;
        int cacheReadTokens = 0;
        int cacheWriteTokens = 0;

        if (response.getUsage() != null) {
            inputTokens = response.getUsage().getInputTokens() != null ? response.getUsage().getInputTokens() : 0;
            outputTokens = response.getUsage().getOutputTokens() != null ? response.getUsage().getOutputTokens() : 0;
            cacheReadTokens = response.getUsage().getCacheReadInputTokens() != null ? response.getUsage().getCacheReadInputTokens() : 0;
            cacheWriteTokens = response.getUsage().getCacheCreationInputTokens() != null ? response.getUsage().getCacheCreationInputTokens() : 0;
        }

        TokenUsage tokenUsage = TokenUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheReadTokens(cacheReadTokens)
                .cacheWriteTokens(cacheWriteTokens)
                .build();
        BigDecimal cost = balanceService.calculateCost(request.getModel(), tokenUsage);
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildClaudeApiCall(context, request, startTime, endTime, duration);
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
        log.info("Claude API调用成功: userId={}, model={}, tokens={}, cacheRead={}, cacheWrite={}, cost={}",
                context.user().getId(),
                request.getModel(),
                totalTokens,
                cacheReadTokens,
                cacheWriteTokens,
                cost);
    }

    private void recordClaudeFailure(ChatContext context,
                                      ClaudeRequest request,
                                      LocalDateTime startTime,
                                      Exception exception) {
        LocalDateTime endTime = LocalDateTime.now();
        int duration = (int) ChronoUnit.MILLIS.between(startTime, endTime);

        ApiCall apiCall = buildClaudeApiCall(context, request, startTime, endTime, duration);
        apiCall.setInputTokens(0);
        apiCall.setOutputTokens(0);
        apiCall.setCacheReadTokens(0);
        apiCall.setCacheWriteTokens(0);
        apiCall.setCost(BigDecimal.ZERO);
        apiCall.setRawCost(BigDecimal.ZERO);
        apiCall.setMarkupRate(BigDecimal.ONE);
        apiCall.setMarkupCost(BigDecimal.ZERO);
        apiCall.setStatus(0);
        apiCall.setErrorMsg(exception.getMessage());

        Long apiCallId = balanceService.logApiCall(apiCall);
        apiCall.setId(apiCallId);
        recordUsageMetrics(context.apiKey().getId(), apiCall);

        log.error("Claude API调用失败: userId={}, error={}", context.user().getId(), exception.getMessage());
    }

    private ApiCall buildClaudeApiCall(ChatContext context,
                                        ClaudeRequest request,
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

    /**
     * 公开的 API Key 验证方法
     * 用于不需要执行实际请求但需要验证认证的场景（如 count_tokens）
     *
     * @param authorization Authorization 头的值
     * @throws ChatAuthorizationException 如果认证失败
     */
    public void validateApiKey(String authorization) {
        authenticate(authorization);
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

    // ============================================================
    // Token 预检测方法
    // ============================================================

    /**
     * 检测 ChatRequest 的输入 token 是否超过模型限制
     * 如果超过限制，抛出 ChatUpstreamException
     */
    private void validateInputTokens(ChatRequest request) {
        if (request == null || request.getMessages() == null) {
            return;
        }

        String model = request.getModel();
        int maxContextTokens = modelService.getMaxContextTokens(model);
        int estimatedInputTokens = estimateRequestInputTokens(request);

        // 预留 10% 的缓冲空间用于输出
        int effectiveLimit = (int) (maxContextTokens * 0.9);

        if (estimatedInputTokens > effectiveLimit) {
            String errorMessage = String.format(
                    "请求内容过长：估算输入 %d tokens，超过模型 %s 的上下文限制（%d tokens）。请缩短对话内容或清理历史消息后重试。",
                    estimatedInputTokens, model, maxContextTokens);
            log.warn("Token 预检测失败: model={}, estimated={}, limit={}",
                    model, estimatedInputTokens, maxContextTokens);
            throw new ChatUpstreamException(400, errorMessage);
        }
    }

    /**
     * 检测 ClaudeRequest 的输入 token 是否超过模型限制
     * 如果超过限制，抛出 ChatUpstreamException
     */
    private void validateClaudeInputTokens(ClaudeRequest request) {
        if (request == null || request.getMessages() == null) {
            return;
        }

        String model = request.getModel();
        int maxContextTokens = modelService.getMaxContextTokens(model);
        int estimatedInputTokens = estimateClaudeRequestInputTokens(request);

        // 预留 10% 的缓冲空间用于输出
        int effectiveLimit = (int) (maxContextTokens * 0.9);

        if (estimatedInputTokens > effectiveLimit) {
            String errorMessage = String.format(
                    "请求内容过长：估算输入 %d tokens，超过模型 %s 的上下文限制（%d tokens）。请缩短对话内容或清理历史消息后重试。",
                    estimatedInputTokens, model, maxContextTokens);
            log.warn("Token 预检测失败: model={}, estimated={}, limit={}",
                    model, estimatedInputTokens, maxContextTokens);
            throw new ChatUpstreamException(400, errorMessage);
        }
    }

    /**
     * 估算 ChatRequest 的输入 token 数
     */
    private int estimateRequestInputTokens(ChatRequest request) {
        if (request == null || request.getMessages() == null) {
            return 0;
        }
        return request.getMessages().stream()
                .map(ChatRequest.Message::getContent)
                .mapToInt(content -> tokenEstimator.estimateTextTokens(content != null ? content.toString() : ""))
                .sum();
    }

    /**
     * 估算 ClaudeRequest 的输入 token 数
     */
    private int estimateClaudeRequestInputTokens(ClaudeRequest request) {
        if (request == null || request.getMessages() == null) {
            return 0;
        }
        int tokens = request.getMessages().stream()
                .map(ClaudeRequest.Message::getContent)
                .mapToInt(content -> tokenEstimator.estimateTextTokens(content != null ? content.toString() : ""))
                .sum();

        // 加上 system prompt 的 token
        if (request.getSystem() != null) {
            tokens += tokenEstimator.estimateTextTokens(request.getSystem().toString());
        }

        // 加上 tools 的 token（工具定义也会占用上下文）
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            for (ClaudeRequest.Tool tool : request.getTools()) {
                if (tool.getDescription() != null) {
                    tokens += tokenEstimator.estimateTextTokens(tool.getDescription());
                }
                if (tool.getInputSchema() != null) {
                    tokens += tokenEstimator.estimateTextTokens(tool.getInputSchema().toString());
                }
            }
        }

        return tokens;
    }

}
