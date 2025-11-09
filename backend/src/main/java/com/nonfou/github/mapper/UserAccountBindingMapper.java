package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.UserAccountBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户账户绑定 Mapper
 */
@Mapper
public interface UserAccountBindingMapper extends BaseMapper<UserAccountBinding> {

    /**
     * 根据用户ID查询所有绑定
     */
    @Select("SELECT * FROM user_account_bindings WHERE user_id = #{userId}")
    List<UserAccountBinding> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和后端账户ID查询绑定
     */
    @Select("SELECT * FROM user_account_bindings WHERE user_id = #{userId} AND backend_account_id = #{backendAccountId}")
    UserAccountBinding selectByUserIdAndAccountId(@Param("userId") Long userId, @Param("backendAccountId") Long backendAccountId);

    /**
     * 根据API Key ID查询绑定
     */
    @Select("SELECT * FROM user_account_bindings WHERE api_key_id = #{apiKeyId}")
    List<UserAccountBinding> selectByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    /**
     * 查询用户的默认账户
     */
    @Select("SELECT * FROM user_account_bindings WHERE user_id = #{userId} AND is_default = true LIMIT 1")
    UserAccountBinding selectDefaultByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和提供商查询绑定的账户ID列表
     */
    @Select("SELECT uab.backend_account_id FROM user_account_bindings uab " +
            "INNER JOIN backend_accounts ba ON uab.backend_account_id = ba.id " +
            "WHERE uab.user_id = #{userId} AND ba.provider = #{provider}")
    List<Long> selectAccountIdsByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
}
