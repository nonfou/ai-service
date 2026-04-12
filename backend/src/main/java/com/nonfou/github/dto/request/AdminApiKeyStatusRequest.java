package com.nonfou.github.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端更新 API Key 状态请求
 */
@Data
public class AdminApiKeyStatusRequest {

    /**
     * 状态：1-启用，0-禁用
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;
}
