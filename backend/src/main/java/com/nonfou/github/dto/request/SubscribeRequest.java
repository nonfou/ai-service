package com.nonfou.github.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 订阅套餐请求
 */
@Data
public class SubscribeRequest {

    /**
     * 套餐ID
     */
    @NotNull(message = "套餐ID不能为空")
    private Long planId;
}
