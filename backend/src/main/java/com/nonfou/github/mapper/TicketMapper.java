package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
