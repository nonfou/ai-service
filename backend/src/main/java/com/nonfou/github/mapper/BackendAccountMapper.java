package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.BackendAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后端账户 Mapper
 */
@Mapper
public interface BackendAccountMapper extends BaseMapper<BackendAccount> {

    /**
     * 根据提供商和状态查询账户列表
     */
    @Select("SELECT * FROM backend_accounts WHERE provider = #{provider} AND status = #{status} ORDER BY priority ASC")
    List<BackendAccount> selectByProviderAndStatus(@Param("provider") String provider, @Param("status") String status);

    /**
     * 更新账户最后使用时间
     */
    @Update("UPDATE backend_accounts SET last_used_at = #{lastUsedAt}, error_count = 0 WHERE id = #{id}")
    int updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    /**
     * 增加错误计数
     */
    @Update("UPDATE backend_accounts SET error_count = error_count + 1, last_error_at = #{lastErrorAt}, last_error_message = #{lastErrorMessage} WHERE id = #{id}")
    int incrementErrorCount(@Param("id") Long id, @Param("lastErrorAt") LocalDateTime lastErrorAt, @Param("lastErrorMessage") String lastErrorMessage);

    /**
     * 重置错误计数
     */
    @Update("UPDATE backend_accounts SET error_count = 0, status = 'active' WHERE id = #{id}")
    int resetErrorCount(@Param("id") Long id);

    /**
     * 根据提供商查询所有激活的账户
     */
    @Select("SELECT * FROM backend_accounts WHERE provider = #{provider} AND status = 'active' ORDER BY priority ASC, last_used_at ASC")
    List<BackendAccount> selectActiveByProvider(@Param("provider") String provider);
}
