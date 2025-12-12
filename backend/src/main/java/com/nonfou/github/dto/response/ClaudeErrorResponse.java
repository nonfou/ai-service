package com.nonfou.github.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anthropic Claude API 错误响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaudeErrorResponse {

    /**
     * 对象类型，固定为 "error"
     */
    private String type;

    /**
     * 错误详情
     */
    private Error error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        /**
         * 错误类型
         */
        private String type;

        /**
         * 错误信息
         */
        private String message;
    }

    /**
     * 创建错误响应
     */
    public static ClaudeErrorResponse of(String errorType, String message) {
        return ClaudeErrorResponse.builder()
                .type("error")
                .error(Error.builder()
                        .type(errorType)
                        .message(message)
                        .build())
                .build();
    }

    /**
     * 根据 HTTP 状态码创建对应的错误响应
     */
    public static ClaudeErrorResponse fromStatusCode(int statusCode, String message) {
        String errorType = switch (statusCode) {
            case 400 -> "invalid_request_error";
            case 401 -> "authentication_error";
            case 403 -> "permission_error";
            case 404 -> "not_found_error";
            case 429 -> "rate_limit_error";
            case 500, 502, 503, 504 -> "api_error";
            default -> "api_error";
        };
        return of(errorType, message);
    }
}
