package com.property.module.owner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.owner.entity.ComplaintEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投诉建议 Mapper
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<ComplaintEntity> {
}
