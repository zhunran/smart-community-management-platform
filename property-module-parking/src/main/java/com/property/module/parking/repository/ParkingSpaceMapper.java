package com.property.module.parking.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.parking.entity.ParkingSpaceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车位 Mapper
 */
@Mapper
public interface ParkingSpaceMapper extends BaseMapper<ParkingSpaceEntity> {
}
