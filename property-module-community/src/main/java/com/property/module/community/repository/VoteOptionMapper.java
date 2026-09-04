package com.property.module.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.community.entity.VoteOptionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 投票选项 Mapper
 */
@Mapper
public interface VoteOptionMapper extends BaseMapper<VoteOptionEntity> {

    /**
     * 选项票数 +1
     */
    @Update("UPDATE t_vote_option SET vote_count = vote_count + 1 WHERE id = #{optionId}")
    int incrVoteCount(@Param("optionId") Long optionId);
}
