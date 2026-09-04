package com.property.framework.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.framework.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}