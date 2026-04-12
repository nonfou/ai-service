package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Insert("""
            INSERT INTO users (
                id, email, password, api_key, balance, status, created_at, updated_at
            ) VALUES (
                #{id}, #{email}, #{password}, #{apiKey}, #{balance}, #{status}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertDirect(User user);

    /**
     * 原子扣减余额（带余额检查）
     * 只有当余额 >= amount 时才执行扣减，避免竞态条件
     *
     * @return 受影响行数，0 表示余额不足或用户不存在
     */
    @Update("UPDATE users SET balance = balance - #{amount}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalanceAtomic(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子增加余额
     *
     * @return 受影响行数
     */
    @Update("UPDATE users SET balance = balance + #{amount}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{userId}")
    int addBalanceAtomic(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
