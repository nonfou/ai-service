package com.nonfou.github.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单消息实体
 */
@Data
@TableName("ticket_messages")
public class TicketMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 用户ID（用户消息时非空）
     */
    private Long userId;

    /**
     * 管理员ID（管理员回复时非空）
     */
    private Long adminId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 是否管理员消息：1-是，0-否
     */
    private Integer isStaff;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
