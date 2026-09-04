package com.property.module.bill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.bill.entity.BillEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账单 Mapper
 */
@Mapper
public interface BillMapper extends BaseMapper<BillEntity> {

    /** 当月应收总额（含已缴清和未缴清的，不含已作废） */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_bill " +
            "WHERE bill_period = #{period} AND del_flag = 0 AND status != 3")
    BigDecimal sumReceivableByPeriod(@Param("period") String period);

    /** 当月实收总额（已缴清+部分缴费的已交金额） */
    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM t_bill " +
            "WHERE bill_period = #{period} AND del_flag = 0 AND status IN (1, 2)")
    BigDecimal sumReceivedByPeriod(@Param("period") String period);

    /** 累计欠费总额（未缴清的总金额 - 已交金额，不含已作废） */
    @Select("SELECT COALESCE(SUM(total_amount - paid_amount), 0) FROM t_bill " +
            "WHERE status IN (0, 1, 5) AND del_flag = 0")
    BigDecimal sumArrears();

    /** 查询逾期账单（已过缴费截止日且未缴清，含已标记逾期的） */
    @Select("SELECT * FROM t_bill WHERE due_date < #{today} AND status IN (0, 1, 5) AND del_flag = 0 " +
            "ORDER BY due_date ASC")
    List<BillEntity> selectOverdueBills(@Param("today") LocalDate today);

    /** 更新账单逾期罚息 */
    @Update("UPDATE t_bill SET late_fee = #{lateFee}, update_time = NOW() WHERE id = #{id} AND del_flag = 0")
    int updateLateFee(@Param("id") Long id, @Param("lateFee") BigDecimal lateFee);

    /** 更新账单状态为逾期 */
    @Update("UPDATE t_bill SET status = 5, late_fee = #{lateFee}, update_time = NOW() " +
            "WHERE id = #{id} AND status IN (0, 1) AND del_flag = 0")
    int markAsOverdue(@Param("id") Long id, @Param("lateFee") BigDecimal lateFee);

    /** 行级锁查询（FOR UPDATE），用于并发缴费场景防止超收 */
    @Select("SELECT * FROM t_bill WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    BillEntity selectByIdForUpdate(@Param("id") Long id);
}
