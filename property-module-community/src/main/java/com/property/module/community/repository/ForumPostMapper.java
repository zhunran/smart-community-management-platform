package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.ForumPostEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛帖子 Mapper
 */
@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPostEntity> {
}