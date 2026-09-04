package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.CommunityVoteEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区投票 Mapper
 */
@Mapper
public interface CommunityVoteMapper extends BaseMapper<CommunityVoteEntity> {
}
