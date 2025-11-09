package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.BalanceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额变动日志 Mapper
 */
@Mapper
public interface BalanceLogMapper extends BaseMapper<BalanceLog> {
}
