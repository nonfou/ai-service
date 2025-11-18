package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * API Key（明文存储便于快速追踪）
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 提供方（copilot/openrouter 等）
     */
    private String provider;

    /**
     * 实际使用的后端账户ID
     */
    private Long backendAccountId;

    /**
     * 输入token数量
     */
    private Integer inputTokens;

    /**
     * 输出token数量
     */
    private Integer outputTokens;

    /**
     * 缓存读取token数量
     */
    private Integer cacheReadTokens;

    /**
     * 缓存写入token数量
     */
    private Integer cacheWriteTokens;

    /**
     * 费用（元）
     */
    private BigDecimal cost;

    /**
     * 原始成本（未加成）
     */
    private BigDecimal rawCost;

    /**
     * 加成倍率
     */
    private BigDecimal markupRate;

    /**
     * 加成金额
     */
    private BigDecimal markupCost;

    /**
     * 会话哈希（用于粘性会话分析）
     */
    private String sessionHash;

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
