package com.nonfou.github.dto.request.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工单请求体.
 */
@Data
public class TicketCreateRequest {

    @NotBlank(message = "主题不能为空")
    @Size(max = 200, message = "主题长度不能超过200个字符")
    private String subject;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 20, message = "优先级长度不合法")
    private String priority = "normal";
}
