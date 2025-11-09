package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API调用日志实体
 */
@Data
@TableName("api_calls")
public class ApiCall {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 输入token数
     */
    private Integer inputTokens;

    /**
     * 输出token数
     */
    private Integer outputTokens;

    /**
     * 费用（元）
     */
    private BigDecimal cost;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 耗时（毫秒）
     */
    private Integer duration;

    /**
     * 状态：1-成功，0-失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
