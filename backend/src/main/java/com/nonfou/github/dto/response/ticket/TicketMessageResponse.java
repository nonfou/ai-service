package com.nonfou.github.dto.response.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nonfou.github.entity.TicketMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单消息响应体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageResponse {

    private Long id;
    private Long ticketId;
    private Long userId;
    private Long adminId;
    private String message;

    @JsonProperty("isAdmin")
    private boolean adminMessage;

    /**
     * 与旧版字段兼容,依旧返回 isStaff.
     */
    @JsonProperty("isStaff")
    private Integer staffFlag;

    private LocalDateTime createdAt;

    public static TicketMessageResponse fromEntity(TicketMessage entity) {
        boolean isAdmin = entity.getIsStaff() != null && entity.getIsStaff() == 1;
        return TicketMessageResponse.builder()
                .id(entity.getId())
                .ticketId(entity.getTicketId())
                .userId(entity.getUserId())
                .adminId(entity.getAdminId())
                .message(entity.getMessage())
                .adminMessage(isAdmin)
                .staffFlag(isAdmin ? 1 : 0)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
