package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.ForumLikeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞记录 Mapper
 */
@Mapper
public interface ForumLikeMapper extends BaseMapper<ForumLikeEntity> {
}