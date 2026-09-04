package com.property.module.owner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.owner.entity.OwnerRoomEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业主-房屋关联 Mapper
 */
@Mapper
public interface OwnerRoomMapper extends BaseMapper<OwnerRoomEntity> {
}
