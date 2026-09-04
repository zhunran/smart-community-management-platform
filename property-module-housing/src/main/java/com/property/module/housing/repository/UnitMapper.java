package com.property.module.housing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.housing.entity.UnitEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 单元 Mapper
 */
@Mapper
public interface UnitMapper extends BaseMapper<UnitEntity> {
}