package com.nonfou.github.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理端 Token 使用记录查询参数。
 */
@Data
public class AdminTokenUsageQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer pageSize = 20;

    private Integer days = 7;

    private Long apiKeyId;

    private String model;

    private String endpoint;

    private Boolean success;
}
