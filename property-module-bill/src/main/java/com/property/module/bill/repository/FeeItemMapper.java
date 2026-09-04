package com.property.module.bill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.bill.entity.FeeItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用项 Mapper
 */
@Mapper
public interface FeeItemMapper extends BaseMapper<FeeItemEntity> {
}
