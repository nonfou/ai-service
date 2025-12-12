package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Anthropic Claude Messages API 响应格式
 * 用于 /v1/messages 接口返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaudeResponse {

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 对象类型，固定为 "message"
     */
    private String type;

    /**
     * 角色，固定为 "assistant"
     */
    private String role;

    /**
     * 内容块列表
     */
    private List<ContentBlock> content;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 停止原因
     */
    @JsonProperty("stop_reason")
    private String stopReason;

    /**
     * 停止序列（如果有）
     */
    @JsonProperty("stop_sequence")
    private String stopSequence;

    /**
     * Token 使用统计
     */
    private Usage usage;

    /**
     * 内容块
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentBlock {
        /**
         * 类型: text, tool_use 等
         */
        private String type;

        /**
         * 文本内容（type=text 时）
         */
        private String text;
    }

    /**
     * Token 使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        @JsonProperty("cache_creation_input_tokens")
        private Integer cacheCreationInputTokens;

        @JsonProperty("cache_read_input_tokens")
        private Integer cacheReadInputTokens;
    }

    /**
     * 从 OpenAI 格式的 ChatResponse 转换为 Claude 格式
     */
    public static ClaudeResponse fromChatResponse(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        }

        String text = "";
        String stopReason = "end_turn";

        if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
            ChatResponse.Choice choice = chatResponse.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                text = choice.getMessage().getContent();
            }
            if (choice.getFinishReason() != null) {
                stopReason = mapFinishReason(choice.getFinishReason());
            }
        }

        Usage usage = null;
        if (chatResponse.getUsage() != null) {
            usage = Usage.builder()
                    .inputTokens(chatResponse.getUsage().getPromptTokens())
                    .outputTokens(chatResponse.getUsage().getCompletionTokens())
                    .build();
        }

        return ClaudeResponse.builder()
                .id(chatResponse.getId() != null ? chatResponse.getId() : "msg_" + System.currentTimeMillis())
                .type("message")
                .role("assistant")
                .content(List.of(ContentBlock.builder()
                        .type("text")
                        .text(text)
                        .build()))
                .model(chatResponse.getModel())
                .stopReason(stopReason)
                .usage(usage)
                .build();
    }

    /**
     * 将 OpenAI 的 finish_reason 映射为 Claude 的 stop_reason
     */
    private static String mapFinishReason(String finishReason) {
        if (finishReason == null) {
            return "end_turn";
        }
        return switch (finishReason.toLowerCase()) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls", "function_call" -> "tool_use";
            case "content_filter" -> "end_turn";
            default -> "end_turn";
        };
    }
}
