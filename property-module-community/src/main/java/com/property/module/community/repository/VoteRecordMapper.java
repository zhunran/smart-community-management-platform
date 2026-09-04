package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.VoteRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投票记录 Mapper
 */
@Mapper
public interface VoteRecordMapper extends BaseMapper<VoteRecordEntity> {
}
