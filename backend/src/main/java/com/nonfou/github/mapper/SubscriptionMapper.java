package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅记录 Mapper
 */
@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
