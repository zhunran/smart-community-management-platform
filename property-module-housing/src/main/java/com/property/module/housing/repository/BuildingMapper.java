package com.property.module.housing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.housing.entity.BuildingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 楼栋 Mapper
 */
@Mapper
public interface BuildingMapper extends BaseMapper<BuildingEntity> {
}