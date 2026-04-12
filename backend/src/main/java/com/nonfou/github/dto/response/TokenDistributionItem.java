package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分布统计项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDistributionItem {

    private String name;

    private Long requestCount;

    private Long totalTokens;
}
