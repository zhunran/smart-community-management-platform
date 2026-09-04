package com.property.module.owner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.owner.entity.OwnerVehicleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业主车辆 Mapper
 */
@Mapper
public interface OwnerVehicleMapper extends BaseMapper<OwnerVehicleEntity> {
}
