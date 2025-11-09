package com.nonfou.github.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 会话工具类
 * 用于生成会话哈希
 */
@Component
public class SessionUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成会话哈希（简化版本，用于多账户调度）
     * 基于 API Key、模型名称和消息内容生成哈希
     *
     * @param apiKey API密钥
     * @param model 模型名称
     * @param messages 消息列表
     * @return 会话哈希（64位十六进制字符串）
     */
    public static String generateSessionHash(String apiKey, String model, Object messages) {
        try {
            StringBuilder content = new StringBuilder();

            // 添加 API Key（用于区分不同用户）
            if (apiKey != null) {
                content.append(apiKey);
            }

            // 添加模型名称
            if (model != null) {
                content.append(":").append(model);
            }

            // 添加消息内容（仅第一条用户消息）
            if (messages != null) {
                if (messages instanceof List) {
                    List<?> msgList = (List<?>) messages;
                    for (Object msgObj : msgList) {
                        if (msgObj instanceof Map) {
                            Map<?, ?> msg = (Map<?, ?>) msgObj;
                            if ("user".equals(msg.get("role"))) {
                                Object msgContent = msg.get("content");
                                if (msgContent != null) {
                                    content.append(":").append(msgContent.toString());
                                    break; // 只取第一条用户消息
                                }
                            }
                        }
                    }
                }
            }

            // 如果没有内容，返回基于 API Key 的哈希
            if (content.length() == 0) {
                content.append("default");
            }

            return sha256(content.toString());
        } catch (Exception e) {
            // 出错返回基于 API Key 的简单哈希
            return apiKey != null ? sha256(apiKey) : null;
        }
    }

    /**
     * 生成会话哈希（原版本，兼容保留）
     * 优先级1: 从 metadata.user_id 提取 session_id
     * 优先级2: 基于 system prompt + 第一条 user message 计算 SHA-256
     * 优先级3: 返回 null（由调用方决定是否生成随机会话）
     *
     * @param requestBody 请求体（Map格式）
     * @return 会话哈希（64位十六进制字符串）或 null
     */
    public String generateSessionHash(Map<String, Object> requestBody) {
        try {
            // 优先级1: 检查 metadata.user_id 中的 session_id
            if (requestBody.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) requestBody.get("metadata");
                if (metadata != null && metadata.containsKey("user_id")) {
                    String userId = metadata.get("user_id").toString();
                    // 匹配格式: session_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
                    if (userId.startsWith("session_")) {
                        return userId.substring(8); // 去掉 "session_" 前缀
                    }
                }
            }

            // 优先级2: 基于内容生成哈希
            StringBuilder contentBuilder = new StringBuilder();

            // 提取 system prompt
            if (requestBody.containsKey("system")) {
                contentBuilder.append(requestBody.get("system").toString());
            }

            // 提取第一条 user message
            if (requestBody.containsKey("messages")) {
                Object messagesObj = requestBody.get("messages");
                if (messagesObj instanceof Iterable) {
                    for (Object msgObj : (Iterable<?>) messagesObj) {
                        if (msgObj instanceof Map) {
                            Map<String, Object> msg = (Map<String, Object>) msgObj;
                            if ("user".equals(msg.get("role"))) {
                                Object content = msg.get("content");
                                if (content != null) {
                                    contentBuilder.append(content.toString());
                                    break; // 只取第一条
                                }
                            }
                        }
                    }
                }
            }

            // 如果没有可用内容，返回 null
            if (contentBuilder.length() == 0) {
                return null;
            }

            // 计算 SHA-256 哈希
            return sha256(contentBuilder.toString());

        } catch (Exception e) {
            // 出错返回 null，让调用方决定是否使用随机会话
            return null;
        }
    }

    /**
     * 计算 SHA-256 哈希
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }
}
