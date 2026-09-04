package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.ActivitySignupEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动报名记录 Mapper
 */
@Mapper
public interface ActivitySignupMapper extends BaseMapper<ActivitySignupEntity> {
}