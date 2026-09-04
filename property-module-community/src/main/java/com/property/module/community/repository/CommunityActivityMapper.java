package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.CommunityActivityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 社区活动 Mapper
 */
@Mapper
public interface CommunityActivityMapper extends BaseMapper<CommunityActivityEntity> {

    /**
     * 报名人数 +1（乐观锁：仅当 signup_count < max_participants 时更新）
     */
    @Update("UPDATE t_community_activity SET signup_count = signup_count + 1, version = version + 1"
            + " WHERE id = #{activityId} AND signup_count < max_participants")
    int incrSignupCount(@Param("activityId") Long activityId);

    /**
     * 报名人数 -1（取消报名时调用）
     */
    @Update("UPDATE t_community_activity SET signup_count = signup_count - 1, version = version + 1"
            + " WHERE id = #{activityId} AND signup_count > 0")
    int decrSignupCount(@Param("activityId") Long activityId);

    /**
     * 更新活动状态
     */
    @Update("UPDATE t_community_activity SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}