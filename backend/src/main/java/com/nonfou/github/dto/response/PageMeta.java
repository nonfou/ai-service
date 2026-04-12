package com.nonfou.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页元数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMeta {

    private Integer page;

    private Integer pageSize;

    private Long total;

    private Integer totalPages;
}
