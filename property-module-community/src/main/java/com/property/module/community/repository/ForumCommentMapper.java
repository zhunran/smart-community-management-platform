package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.ForumCommentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛评论 Mapper
 */
@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumCommentEntity> {
}