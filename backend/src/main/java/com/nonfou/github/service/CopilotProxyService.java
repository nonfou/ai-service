package com.nonfou.github.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonfou.github.dto.request.ChatRequest;
import com.nonfou.github.dto.response.ChatResponse;
import com.nonfou.github.entity.ApiKey;
import com.nonfou.github.entity.BackendAccount;
import com.nonfou.github.entity.Model;
import com.nonfou.github.mapper.ModelMapper;
import com.nonfou.github.service.CostCalculatorService.CostResult;
import com.nonfou.github.service.CostCalculatorService.TokenUsage;
import com.nonfou.github.service.proxy.ModelProxy;
import com.nonfou.github.util.SessionUtil;
import com.nonfou.github.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Copilot 浠ｇ悊鏈嶅姟 - 澶氳处鎴峰寮虹増
 */
@Slf4j
@Service
public class CopilotProxyService implements ModelProxy {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AccountSchedulerService accountScheduler;

    @Autowired
    private SessionStickinessService sessionStickinessService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private CostCalculatorService costCalculatorService;

    @Autowired
    private BackendAccountService backendAccountService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenEstimator tokenEstimator;

    @Autowired
    @Qualifier("streamTaskExecutor")
    private TaskExecutor streamTaskExecutor;

    @Value("${backend.copilot.base-url:https://api.githubcopilot.com}")
    private String copilotBaseUrl;

    @Value("${backend.streaming.timeout-ms:300000}")
    private long streamTimeoutMs;

    @Override
    public String getProvider() {
        return "copilot";
    }

    /**
     * 闈炴祦寮忚亰澶?
     */
    @Override
    public ChatResponse chat(ChatRequest request, ApiKey apiKey) {
        // 1. 鐢熸垚浼氳瘽鍝堝笇锛堢敤浜庝細璇濈矘鎬э級
        String sessionHash = SessionUtil.generateSessionHash(
            apiKey.getApiKey(),
            request.getModel(),
            request.getMessages()
        );

        // 2. 閫夋嫨鍚庣璐︽埛
        BackendAccount account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
        if (account == null) {
            throw new RuntimeException("No available Copilot account");
        }

        // 3. 妫€鏌ラ厤棰?
        quotaService.checkAndEnforceQuota(apiKey.getUserId());

        // 4. 鏋勫缓璇锋眰
        String endpoint = copilotBaseUrl + "/v1/chat/completions";
        HttpHeaders headers = buildHeaders(account);

        // 纭繚闈炴祦寮?
        ChatRequest modifiedRequest = new ChatRequest();
        modifiedRequest.setModel(request.getModel());
        modifiedRequest.setMessages(request.getMessages());
        modifiedRequest.setMaxTokens(request.getMaxTokens());
        modifiedRequest.setTemperature(request.getTemperature());
        modifiedRequest.setStream(false);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(modifiedRequest, headers);

        try {
            // 5. 鍙戦€佽姹?
            log.info("璋冪敤 Copilot API: account={}, model={}, messages={}",
                    account.getAccountName(), request.getModel(), request.getMessages().size());

            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(endpoint, entity, ChatResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ChatResponse chatResponse = response.getBody();

                // 6. 璁＄畻鎴愭湰
                if (chatResponse.getUsage() != null) {
                    TokenUsage usage = TokenUsage.builder()
                            .inputTokens(chatResponse.getUsage().getPromptTokens())
                            .outputTokens(chatResponse.getUsage().getCompletionTokens())
                            .cacheReadTokens(0)
                            .cacheWriteTokens(0)
                            .build();

                    CostResult costResult = costCalculatorService.calculate(
                            usage, request.getModel(), account.getId(), apiKey.getUserId());

                    // 7. 鎵ｉ櫎閰嶉
                    quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());

                    log.info("Copilot API 鍝嶅簲鎴愬姛: account={}, tokens={}, cost=${}",
                            account.getAccountName(),
                            chatResponse.getUsage().getTotalTokens(),
                            costResult.getTotalCost());
                }

                // 8. 鏇存柊璐︽埛浣跨敤缁熻
                backendAccountService.recordSuccess(account.getId());

                // 9. 淇濆瓨浼氳瘽绮樻€?
                sessionStickinessService.saveMapping(sessionHash, account.getId());

                return chatResponse;
            } else {
                // 10. 璁板綍澶辫触
                backendAccountService.recordError(account.getId(), "HTTP " + response.getStatusCode());
                throw new RuntimeException("Copilot API 鍝嶅簲寮傚父: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // 11. 璁板綍澶辫触
            backendAccountService.recordError(account.getId(), e.getMessage());
            log.error("璋冪敤 Copilot API 澶辫触: account={}", account.getAccountName(), e);
            throw new RuntimeException("AI 鏈嶅姟鏆傛椂涓嶅彲鐢? " + e.getMessage());
        }
    }

    /**
     * 娴佸紡鑱婂ぉ
     */
    @Override
    public SseEmitter chatStream(ChatRequest request, ApiKey apiKey) {
        SseEmitter emitter = new SseEmitter(streamTimeoutMs);

        String sessionHash = SessionUtil.generateSessionHash(
                apiKey.getApiKey(),
                request.getModel(),
                request.getMessages()
        );

        BackendAccount account = accountScheduler.selectAccount(apiKey, request.getModel(), sessionHash);
        if (account == null) {
            emitter.completeWithError(new RuntimeException("No available Copilot account"));
            return emitter;
        }

        try {
            quotaService.checkAndEnforceQuota(apiKey.getUserId());
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        StreamingResources resources = new StreamingResources();
        registerEmitterCallbacks(emitter, resources);

        streamTaskExecutor.execute(() -> {
            AtomicReference<TokenUsage> usageRef = new AtomicReference<>();
            StringBuilder fullContent = new StringBuilder();

            try {
                HttpURLConnection connection = buildStreamingConnection(account);
                resources.connection = connection;

                ChatRequest modifiedRequest = new ChatRequest();
                modifiedRequest.setModel(request.getModel());
                modifiedRequest.setMessages(request.getMessages());
                modifiedRequest.setMaxTokens(request.getMaxTokens());
                modifiedRequest.setTemperature(request.getTemperature());
                modifiedRequest.setStream(true);

                String requestBody = objectMapper.writeValueAsString(modifiedRequest);
                connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

                log.info("娴佸紡璋冪敤 Copilot API: account={}, model={}",
                        account.getAccountName(), request.getModel());

                resources.reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;

                while ((line = resources.reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                        continue;
                    }

                    String data = line.substring(6);
                    if ("[DONE]".equals(data.trim())) {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        break;
                    }

                    emitter.send(SseEmitter.event().data(data));
                    handleStreamChunk(data, fullContent, usageRef);
                }

                finalizeStreamingCost(request, apiKey, account, sessionHash, fullContent.toString(), usageRef.get());
                emitter.complete();
            } catch (Exception e) {
                log.error("娴佸紡璋冪敤 Copilot API 澶辫触: account={}", account.getAccountName(), e);
                backendAccountService.recordError(account.getId(), e.getMessage());
                emitter.completeWithError(e);
            } finally {
                closeResources(resources);
            }
        });

        return emitter;
    }

    /**
     * 娴嬭瘯杩炴帴锛堜娇鐢ㄨ皟搴﹀櫒閫夋嫨鐨勮处鎴凤級
     */
    public boolean testConnection(ApiKey apiKey) {
        try {
            BackendAccount account = accountScheduler.selectAccount(apiKey, "gpt-4", null);
            if (account == null) {
                return false;
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                backendAccountService.recordSuccess(account.getId());
            } else {
                backendAccountService.recordError(account.getId(), "Connection test failed");
            }

            return success;
        } catch (Exception e) {
            log.error("Copilot API 杩炴帴娴嬭瘯澶辫触", e);
            return false;
        }
    }

    /**
     * 鑾峰彇鍙敤妯″瀷鍒楄〃锛堜娇鐢ㄧ涓€涓彲鐢ㄨ处鎴凤級
     */
    public String getModels(ApiKey apiKey) {
        try {
            BackendAccount account = accountScheduler.selectAccount(apiKey, "gpt-4", null);
            if (account == null) {
                throw new RuntimeException("No available Copilot account");
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("鑾峰彇 Copilot 妯″瀷鍒楄〃澶辫触", e);
            throw new RuntimeException("Failed to get models: " + e.getMessage());
        }
    }

    /**
     * 鍋ュ悍妫€鏌ユ寚瀹氳处鎴?
     */
    public boolean healthCheck(Long accountId) {
        try {
            BackendAccount account = backendAccountService.getById(accountId);
            if (account == null || !"copilot".equals(account.getProvider())) {
                return false;
            }

            String endpoint = copilotBaseUrl + "/v1/models";
            HttpHeaders headers = buildHeaders(account);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, String.class);

            boolean healthy = response.getStatusCode() == HttpStatus.OK;

            if (healthy) {
                backendAccountService.updateHealth(accountId, "active", "鍋ュ悍妫€鏌ラ€氳繃");
            } else {
                backendAccountService.updateHealth(accountId, "error", "鍋ュ悍妫€鏌ュけ璐? HTTP " + response.getStatusCode());
            }

            return healthy;
        } catch (Exception e) {
            log.error("Copilot 璐︽埛鍋ュ悍妫€鏌ュけ璐? accountId={}", accountId, e);
            backendAccountService.updateHealth(accountId, "error", "鍋ュ悍妫€鏌ュ紓甯? " + e.getMessage());
            return false;
        }
    }

    /**
     * 鏋勫缓璇锋眰澶?
     */
    private HttpHeaders buildHeaders(BackendAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String decryptedToken = backendAccountService.decryptToken(account.getAccessToken());
        headers.set("Authorization", "Bearer " + decryptedToken);

        return headers;
    }

    private HttpURLConnection buildStreamingConnection(BackendAccount account) throws Exception {
        String endpoint = copilotBaseUrl + "/v1/chat/completions";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + backendAccountService.decryptToken(account.getAccessToken()));
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout((int) streamTimeoutMs);
        return connection;
    }

    private void handleStreamChunk(String data,
                                   StringBuilder fullContent,
                                   AtomicReference<TokenUsage> usageRef) {
        try {
            JsonNode node = objectMapper.readTree(data);

            if (node.has("usage")) {
                Map<String, Object> usageMap = objectMapper.convertValue(node.get("usage"), MAP_TYPE);
                usageRef.set(TokenUsage.from(usageMap));
            }

            JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray()) {
                for (JsonNode choice : choices) {
                    JsonNode delta = choice.get("delta");
                    if (delta != null) {
                        JsonNode contentNode = delta.get("content");
                        if (contentNode != null && !contentNode.isNull()) {
                            fullContent.append(contentNode.asText(""));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("蹇界暐鏃犳硶瑙ｆ瀽鐨?Copilot 娴佸紡鐗囨: {}", data);
        }
    }

    private void finalizeStreamingCost(ChatRequest request,
                                       ApiKey apiKey,
                                       BackendAccount account,
                                       String sessionHash,
                                       String fullContent,
                                       TokenUsage usage) {
        try {
            TokenUsage finalUsage = usage != null ? usage : tokenEstimator.estimateUsage(request, fullContent);
            if (finalUsage == null) {
                log.warn("Copilot 娴佸紡鍝嶅簲缂哄皯 usage 淇℃伅锛岃烦杩囪璐? account={}, model={}",
                        account.getAccountName(), request.getModel());
            } else {
                CostResult costResult = costCalculatorService.calculate(
                        finalUsage, request.getModel(), account.getId(), apiKey.getUserId());
                quotaService.deductQuota(apiKey.getUserId(), costResult.getTotalCost());
                log.info("Copilot 娴佸紡鍝嶅簲瀹屾垚: account={}, tokens(in/out)={}/{}, cost=${}",
                        account.getAccountName(),
                        finalUsage.getInputTokens(),
                        finalUsage.getOutputTokens(),
                        costResult.getTotalCost());
            }

            backendAccountService.recordSuccess(account.getId());
            sessionStickinessService.saveMapping(sessionHash, account.getId());
        } catch (Exception e) {
            log.error("Copilot 娴佸紡璁¤垂澶辫触: account={}", account.getAccountName(), e);
            throw new RuntimeException("Copilot 璁¤垂澶辫触: " + e.getMessage(), e);
        }
    }

    private void registerEmitterCallbacks(SseEmitter emitter, StreamingResources resources) {
        emitter.onCompletion(() -> closeResources(resources));
        emitter.onTimeout(() -> closeResources(resources));
        emitter.onError(error -> closeResources(resources));
    }

    private void closeResources(StreamingResources resources) {
        if (resources == null) {
            return;
        }
        try {
            if (resources.reader != null) {
                resources.reader.close();
            }
        } catch (Exception ignored) {
        }

        if (resources.connection != null) {
            resources.connection.disconnect();
        }
    }

    private static class StreamingResources {
        private volatile BufferedReader reader;
        private volatile HttpURLConnection connection;
    }
}
