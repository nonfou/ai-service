package com.nonfou.github.dto.request.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员回复工单请求体.
 */
@Data
public class AdminTicketReplyRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
