package com.nonfou.github.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 回复工单请求
 */
@Data
public class ReplyTicketRequest {

    /**
     * 回复内容
     */
    @NotBlank(message = "回复内容不能为空")
    private String message;
}
