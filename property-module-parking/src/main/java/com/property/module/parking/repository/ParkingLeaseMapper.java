package com.property.module.parking.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.parking.entity.ParkingLeaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车位租赁合同 Mapper
 */
@Mapper
public interface ParkingLeaseMapper extends BaseMapper<ParkingLeaseEntity> {
}
