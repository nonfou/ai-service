package com.nonfou.github.dto.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Claude SSE 事件解析器
 * 将 JSON 字符串解析为类型化的事件对象
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeStreamEventParser {

    private final ObjectMapper objectMapper;

    /**
     * 解析 Claude SSE 事件
     *
     * @param json 事件 JSON 字符串
     * @return 解析后的事件对象，解析失败返回基础 ClaudeStreamEvent
     */
    public ClaudeStreamEvent parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            ClaudeStreamEvent event = objectMapper.readValue(json, ClaudeStreamEvent.class);
            event.setRawJson(json);
            return event;
        } catch (JsonProcessingException e) {
            log.debug("解析 Claude 事件失败, 返回基础事件: {}", e.getMessage());
            // 返回一个基础事件，保留原始 JSON
            ClaudeStreamEvent fallback = new ClaudeStreamEvent();
            fallback.setRawJson(json);
            return fallback;
        }
    }

    /**
     * 将事件序列化为 JSON
     *
     * @param event 事件对象
     * @return JSON 字符串
     */
    public String toJson(ClaudeStreamEvent event) {
        // 优先返回原始 JSON（透传模式）
        if (event.getRawJson() != null) {
            return event.getRawJson();
        }

        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("序列化 Claude 事件失败", e);
            return "{}";
        }
    }
}
