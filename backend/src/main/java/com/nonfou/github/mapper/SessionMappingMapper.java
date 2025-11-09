package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.SessionMapping;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 会话粘性映射 Mapper
 */
@Mapper
public interface SessionMappingMapper extends BaseMapper<SessionMapping> {

    /**
     * 根据会话哈希查询映射
     */
    @Select("SELECT * FROM session_mappings WHERE session_hash = #{sessionHash} AND expires_at > NOW() LIMIT 1")
    SessionMapping selectBySessionHash(@Param("sessionHash") String sessionHash);

    /**
     * 删除过期的会话映射
     */
    @Delete("DELETE FROM session_mappings WHERE expires_at < #{expireTime}")
    int deleteExpired(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 根据用户ID查询活跃会话数
     */
    @Select("SELECT COUNT(*) FROM session_mappings WHERE user_id = #{userId} AND expires_at > NOW()")
    int countActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * 根据后端账户ID查询活跃会话数
     */
    @Select("SELECT COUNT(*) FROM session_mappings WHERE backend_account_id = #{accountId} AND expires_at > NOW()")
    int countActiveSessionsByAccountId(@Param("accountId") Long accountId);
}
