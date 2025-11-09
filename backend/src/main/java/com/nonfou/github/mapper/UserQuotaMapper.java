package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.UserQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户配额 Mapper
 */
@Mapper
public interface UserQuotaMapper extends BaseMapper<UserQuota> {

    /**
     * 根据用户ID和配额类型查询
     */
    @Select("SELECT * FROM user_quotas WHERE user_id = #{userId} AND quota_type = #{quotaType}")
    UserQuota selectByUserIdAndType(@Param("userId") Long userId, @Param("quotaType") String quotaType);

    /**
     * 根据用户ID查询所有配额
     */
    @Select("SELECT * FROM user_quotas WHERE user_id = #{userId}")
    List<UserQuota> selectByUserId(@Param("userId") Long userId);

    /**
     * 增加已使用金额
     */
    @Update("UPDATE user_quotas SET used_amount = used_amount + #{amount}, updated_at = NOW() WHERE id = #{id}")
    int incrementUsedAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 重置配额
     */
    @Update("UPDATE user_quotas SET used_amount = 0, reset_at = #{nextResetAt}, updated_at = NOW() WHERE id = #{id}")
    int resetQuota(@Param("id") Long id, @Param("nextResetAt") LocalDateTime nextResetAt);

    /**
     * 查询需要重置的配额列表
     */
    @Select("SELECT * FROM user_quotas WHERE reset_at <= NOW() AND is_enabled = true")
    List<UserQuota> selectQuotasNeedReset();

    /**
     * 查询需要告警的配额列表
     */
    @Select("SELECT * FROM user_quotas WHERE is_enabled = true " +
            "AND (used_amount / quota_amount * 100) >= alert_threshold " +
            "AND (last_alert_at IS NULL OR last_alert_at < DATE_SUB(NOW(), INTERVAL 1 HOUR))")
    List<UserQuota> selectQuotasNeedAlert();

    /**
     * 更新最后告警时间
     */
    @Update("UPDATE user_quotas SET last_alert_at = NOW() WHERE id = #{id}")
    int updateLastAlertAt(@Param("id") Long id);
}
