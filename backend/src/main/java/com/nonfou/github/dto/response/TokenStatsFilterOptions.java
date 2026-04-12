package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 使用记录筛选项集合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatsFilterOptions {

    private Integer days;

    private List<TokenFilterOption> apiKeys;

    private List<TokenFilterOption> endpoints;
}
