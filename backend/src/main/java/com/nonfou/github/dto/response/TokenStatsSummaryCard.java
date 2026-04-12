package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 仪表盘摘要卡片。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatsSummaryCard {

    private String key;

    private String label;

    private Long value;

    private String unit;

    private String detail;
}
