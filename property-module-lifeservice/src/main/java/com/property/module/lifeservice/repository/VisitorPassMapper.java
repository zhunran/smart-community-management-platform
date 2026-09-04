package com.property.module.lifeservice.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.lifeservice.entity.VisitorPassEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访客通行码 Mapper
 */
@Mapper
public interface VisitorPassMapper extends BaseMapper<VisitorPassEntity> {
}
