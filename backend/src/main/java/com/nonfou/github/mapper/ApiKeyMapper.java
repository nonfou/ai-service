package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.ApiKey;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * API密钥 Mapper
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    @Insert("""
            INSERT INTO api_keys (
                id, user_id, key_name, api_key, relay_base_url, upstream_api_key,
                description, status, last_used_at, created_at, updated_at
            ) VALUES (
                #{id}, #{userId}, #{keyName}, #{apiKey}, #{relayBaseUrl}, #{upstreamApiKey},
                #{description}, #{status}, #{lastUsedAt}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertDirect(ApiKey apiKey);
}
