package com.property.module.parking.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.parking.entity.ParkingWarningEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 车位预警 Mapper
 *
 * 包含双轨对账核心 SQL，通过 SQL 级比对发现车位管理的异常数据。
 * 对账逻辑涵盖：租赁到期、状态不匹配、欠费、占用异常四大类。
 */
@Mapper
public interface ParkingWarningMapper extends BaseMapper<ParkingWarningEntity> {

    // ═══════════════════════════════════════════════════════
    // 双轨对账 SQL
    // ═══════════════════════════════════════════════════════

    /**
     * 对账 1：租赁合同已到期但车位仍标记为已租
     */
    @Select("SELECT s.id AS space_id, s.space_code, s.space_name, l.lease_end, l.id AS lease_id " +
            "FROM t_parking_space s " +
            "JOIN t_parking_lease l ON l.space_id = s.id AND l.del_flag = 0 " +
            "WHERE s.status IN (1, 2) AND s.del_flag = 0 " +
            "AND l.lease_end < #{today} AND l.status IN (0, 1) " +
            "ORDER BY l.lease_end ASC")
    List<Map<String, Object>> selectExpiredLeases(@Param("today") LocalDateTime today);

    /**
     * 对账 2：已售/已租车位但近 N 天无任何使用记录（疑似空置）
     */
    @Select("SELECT s.id AS space_id, s.space_code, s.space_name, s.owner_id, s.status " +
            "FROM t_parking_space s " +
            "WHERE s.status IN (1, 2) AND s.del_flag = 0 " +
            "AND NOT EXISTS ( " +
            "  SELECT 1 FROM t_parking_usage u " +
            "  WHERE u.space_id = s.id AND u.del_flag = 0 " +
            "  AND u.start_time >= #{since} " +
            ") " +
            "ORDER BY s.space_code")
    List<Map<String, Object>> selectIdleSpaces(@Param("since") LocalDateTime since);

    /**
     * 对账 3：临时停车已结束但未支付
     */
    @Select("SELECT u.id AS usage_id, u.space_id, s.space_code, s.space_name, " +
            "u.plate_no, u.start_time, u.end_time, u.duration_hours, u.fee_amount " +
            "FROM t_parking_usage u " +
            "JOIN t_parking_space s ON s.id = u.space_id AND s.del_flag = 0 " +
            "WHERE u.usage_type = 2 AND u.status = 1 AND u.payment_status = 0 " +
            "AND u.del_flag = 0 AND u.end_time IS NOT NULL " +
            "ORDER BY u.end_time DESC")
    List<Map<String, Object>> selectUnpaidTemporaryUsage();

    /**
     * 对账 4：空闲车位存在最近的使用记录（占用异常）
     */
    @Select("SELECT s.id AS space_id, s.space_code, s.space_name, " +
            "u.plate_no, u.start_time, u.end_time, u.usage_type " +
            "FROM t_parking_space s " +
            "JOIN t_parking_usage u ON u.space_id = s.id AND u.del_flag = 0 " +
            "WHERE s.status = 0 AND s.del_flag = 0 " +
            "AND u.status IN (0, 1) AND u.end_time IS NULL " +
            "ORDER BY u.start_time DESC")
    List<Map<String, Object>> selectOccupancyAnomaly();

    /**
     * 对账 5：租赁合同即将到期（提前 N 天预警）
     */
    @Select("SELECT s.id AS space_id, s.space_code, s.space_name, " +
            "l.contract_no, l.lease_end, l.owner_id, l.id AS lease_id " +
            "FROM t_parking_lease l " +
            "JOIN t_parking_space s ON s.id = l.space_id AND s.del_flag = 0 " +
            "WHERE l.status = 1 AND l.del_flag = 0 " +
            "AND l.lease_end BETWEEN #{today} AND #{deadline} " +
            "ORDER BY l.lease_end ASC")
    List<Map<String, Object>> selectLeasesAboutToExpire(@Param("today") LocalDateTime today,
                                                        @Param("deadline") LocalDateTime deadline);

    /**
     * 查询当前批次所有未关闭的预警
     */
    @Select("SELECT * FROM t_parking_warning WHERE batch_no = #{batchNo} AND status IN (0, 1) AND del_flag = 0 " +
            "ORDER BY warning_level DESC, create_time ASC")
    List<ParkingWarningEntity> selectByBatchNo(@Param("batchNo") String batchNo);

    /**
     * 查询同一车位同一预警类型的活跃预警数（用于去重）
     */
    @Select("SELECT COUNT(*) FROM t_parking_warning WHERE space_id = #{spaceId} " +
            "AND warning_type = #{warningType} AND status IN (0, 1) AND del_flag = 0")
    int countActiveBySpaceAndType(@Param("spaceId") Long spaceId, @Param("warningType") String warningType);
}
