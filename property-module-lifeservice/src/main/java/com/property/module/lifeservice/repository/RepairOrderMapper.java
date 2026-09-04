package com.property.module.lifeservice.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.lifeservice.entity.RepairOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报修工单 Mapper
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrderEntity> {

    /**
     * 行级锁查询（用于状态流转时防止并发冲突）
     */
    @Select("SELECT * FROM t_repair_order WHERE id = #{id} FOR UPDATE")
    RepairOrderEntity selectByIdForUpdate(@Param("id") Long id);

    /**
     * 各状态数量统计（已完成/已评价工单参与平均时长计算）
     */
    @Select("SELECT status, COUNT(*) AS count FROM t_repair_order WHERE del_flag = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 平均处理时长（小时）：已完成(4)/已评价(5) 工单的 update_time - create_time
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, create_time, update_time)) FROM t_repair_order"
            + " WHERE del_flag = 0 AND status IN (4, 5)")
    BigDecimal avgHandleHours();

    /**
     * 超时扫描：待审核(0)/待派单(1) 超过指定时长未处理
     */
    @Select("SELECT * FROM t_repair_order WHERE del_flag = 0 AND timeout_flag = 0"
            + " AND status IN (0, 1) AND create_time < #{deadline}")
    List<RepairOrderEntity> selectTimeoutPendingAudit(@Param("deadline") LocalDateTime deadline);

    /**
     * 超时扫描：已派单(2) 超过指定时长未被接单
     */
    @Select("SELECT * FROM t_repair_order WHERE del_flag = 0 AND timeout_flag = 0"
            + " AND status = 2 AND update_time < #{deadline}")
    List<RepairOrderEntity> selectTimeoutUnaccepted(@Param("deadline") LocalDateTime deadline);
}