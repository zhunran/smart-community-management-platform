package com.property.module.owner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.owner.entity.OwnerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业主 Mapper
 */
@Mapper
public interface OwnerMapper extends BaseMapper<OwnerEntity> {

    /**
     * 统计手机号数量（包括逻辑删除的记录）
     * 避免 @TableLogic 过滤导致唯一键冲突
     */
    @Select("SELECT COUNT(*) FROM t_owner WHERE phone = #{phone}")
    long countByPhoneIncludeDeleted(@Param("phone") String phone);

    /**
     * 统计证件号码数量（包括逻辑删除的记录）
     */
    @Select("SELECT COUNT(*) FROM t_owner WHERE id_card_no = #{idCardNo}")
    long countByIdCardNoIncludeDeleted(@Param("idCardNo") String idCardNo);

    /**
     * 根据房号模糊查询业主ID列表（通过 t_owner_room + t_room 关联）
     */
    @Select("SELECT DISTINCT o.id FROM t_owner o " +
            "JOIN t_owner_room rl ON rl.owner_id = o.id AND rl.del_flag = 0 " +
            "JOIN t_room r ON r.id = rl.room_id AND r.del_flag = 0 " +
            "WHERE r.room_code LIKE CONCAT('%', #{roomCode}, '%')")
    List<Long> selectOwnerIdsByRoomCode(@Param("roomCode") String roomCode);
}
