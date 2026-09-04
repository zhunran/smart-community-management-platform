package com.property.module.owner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.owner.entity.OwnerFamilyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家庭成员 Mapper
 */
@Mapper
public interface OwnerFamilyMapper extends BaseMapper<OwnerFamilyEntity> {
}
