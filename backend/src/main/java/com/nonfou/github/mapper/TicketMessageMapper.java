package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.TicketMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单消息 Mapper
 */
@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessage> {
}
