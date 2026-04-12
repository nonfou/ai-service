package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 筛选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenFilterOption {

    private String label;

    private String value;
}
