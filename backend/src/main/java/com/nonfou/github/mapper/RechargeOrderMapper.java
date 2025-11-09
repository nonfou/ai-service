package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值订单 Mapper
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
}
