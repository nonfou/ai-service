package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.SubscriptionPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅套餐 Mapper
 */
@Mapper
public interface SubscriptionPlanMapper extends BaseMapper<SubscriptionPlan> {
}
