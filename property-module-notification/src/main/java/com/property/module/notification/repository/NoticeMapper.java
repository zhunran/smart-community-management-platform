package com.property.module.notification.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.notification.entity.NoticeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper
 */
@Mapper
public interface NoticeMapper extends BaseMapper<NoticeEntity> {
}