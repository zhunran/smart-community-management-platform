package com.property.module.housing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.housing.entity.RoomEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房屋 Mapper
 */
@Mapper
public interface RoomMapper extends BaseMapper<RoomEntity> {
}