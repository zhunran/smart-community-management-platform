package com.property.module.parking.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.parking.entity.ParkingChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车位变更日志 Mapper
 */
@Mapper
public interface ParkingChangeLogMapper extends BaseMapper<ParkingChangeLogEntity> {
}
