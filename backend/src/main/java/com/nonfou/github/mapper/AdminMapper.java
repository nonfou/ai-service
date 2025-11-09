package com.nonfou.github.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nonfou.github.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 Mapper
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
