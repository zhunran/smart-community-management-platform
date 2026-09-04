package com.property.module.parking.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.parking.entity.ParkingUsageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车位使用记录 Mapper
 */
@Mapper
public interface ParkingUsageMapper extends BaseMapper<ParkingUsageEntity> {
}
