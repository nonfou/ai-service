package com.nonfou.github.dto.request.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户工单回复请求体.
 */
@Data
public class TicketReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    private String message;
}
