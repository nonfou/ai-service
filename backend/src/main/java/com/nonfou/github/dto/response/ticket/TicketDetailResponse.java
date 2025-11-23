package com.nonfou.github.dto.response.ticket;

import com.nonfou.github.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工单详情响应.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {

    private Ticket ticket;
    private List<TicketMessageResponse> messages;
}
