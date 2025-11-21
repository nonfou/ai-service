package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CSRF Token响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsrfTokenResponse {

    /**
     * CSRF Token
     */
    private String csrfToken;
}
