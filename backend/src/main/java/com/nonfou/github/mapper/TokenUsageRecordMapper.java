package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.TokenUsageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * Token 使用记录 Mapper。
 */
@Mapper
public interface TokenUsageRecordMapper extends BaseMapper<TokenUsageRecord> {
}
