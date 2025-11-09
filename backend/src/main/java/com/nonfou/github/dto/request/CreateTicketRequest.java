package com.nonfou.github.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建工单请求
 */
@Data
public class CreateTicketRequest {

    /**
     * 主题
     */
    @NotBlank(message = "工单主题不能为空")
    private String subject;

    /**
     * 内容
     */
    @NotBlank(message = "工单内容不能为空")
    private String content;

    /**
     * 优先级: low, normal, high, urgent
     */
    private String priority = "normal";
}
