package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anthropic Claude count_tokens API 响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeCountTokensResponse {

    /**
     * 输入 token 数量
     */
    @JsonProperty("input_tokens")
    private Integer inputTokens;
}
