package com.property.module.lifeservice.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.lifeservice.entity.VenueBookingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场地预约 Mapper
 */
@Mapper
public interface VenueBookingMapper extends BaseMapper<VenueBookingEntity> {
}
