package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * API密钥 Mapper
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}
