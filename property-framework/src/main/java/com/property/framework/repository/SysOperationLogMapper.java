package com.property.framework.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.framework.entity.SysOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLogEntity> {
}
