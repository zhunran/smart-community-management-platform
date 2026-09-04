package com.property.module.bill.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.property.module.bill.entity.PaymentOrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付只读 Mapper（供账单模块写入 t_payment 表）
 */
@Mapper
public interface BillPaymentMapper {

    /** 查询当日最大支付单号 */
    @Select("SELECT payment_no FROM t_payment WHERE payment_no LIKE #{prefix} AND del_flag = 0 ORDER BY payment_no DESC LIMIT 1")
    String selectLastPaymentNo(@Param("prefix") String prefix);

    /** 插入支付记录 */
    @Insert("INSERT INTO t_payment (id, payment_no, bill_id, room_id, owner_id, payment_method, " +
            "payment_amount, payment_time, payment_status, payer_name, remark, create_by) " +
            "VALUES (#{id}, #{paymentNo}, #{billId}, #{roomId}, #{ownerId}, #{paymentMethod}, " +
            "#{paymentAmount}, #{paymentTime}, #{paymentStatus}, #{payerName}, #{remark}, #{createBy})")
    int insertPayment(@Param("id") Long id,
                      @Param("paymentNo") String paymentNo,
                      @Param("billId") Long billId,
                      @Param("roomId") Long roomId,
                      @Param("ownerId") Long ownerId,
                      @Param("paymentMethod") Integer paymentMethod,
                      @Param("paymentAmount") BigDecimal paymentAmount,
                      @Param("paymentTime") LocalDateTime paymentTime,
                      @Param("paymentStatus") Integer paymentStatus,
                      @Param("payerName") String payerName,
                      @Param("remark") String remark,
                      @Param("createBy") String createBy);

    /** 根据支付单号查询支付记录 */
    @Select("SELECT * FROM t_payment WHERE payment_no = #{paymentNo} AND del_flag = 0 LIMIT 1")
    PaymentOrderEntity selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /** 查询指定账单下待支付的在线支付订单数量（防重复下单） */
    @Select("SELECT COUNT(*) FROM t_payment WHERE bill_id = #{billId} AND payment_status = 0 AND del_flag = 0")
    int countPendingByBillId(@Param("billId") Long billId);

    /** 查询指定账单下最新一条待支付的在线支付订单（复用重新发起支付） */
    @Select("SELECT * FROM t_payment WHERE bill_id = #{billId} AND payment_status = 0 AND del_flag = 0 ORDER BY create_time DESC LIMIT 1")
    PaymentOrderEntity selectPendingByBillId(@Param("billId") Long billId);

    /** 根据支付单号查询支付记录（含 FOR UPDATE 锁） */
    @Select("SELECT * FROM t_payment WHERE payment_no = #{paymentNo} AND del_flag = 0 FOR UPDATE")
    PaymentOrderEntity selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /** 查询待对账的支付记录（WAITING / PROCESSING，创建超过 N 分钟） */
    @Select("SELECT * FROM t_payment WHERE payment_status IN (0, 1) AND del_flag = 0 " +
            "AND create_time <= #{createTimeBefore} ORDER BY create_time ASC LIMIT #{limit}")
    List<PaymentOrderEntity> selectPendingReconciliation(@Param("createTimeBefore") LocalDateTime createTimeBefore,
                                                          @Param("limit") int limit);

    /** 更新支付记录状态（带原状态乐观锁） */
    @Update("UPDATE t_payment SET payment_status = #{targetStatus}, transaction_id = #{transactionId}, " +
            "payment_time = #{paymentTime}, update_time = #{paymentTime} " +
            "WHERE payment_no = #{paymentNo} AND payment_status = #{expectedStatus}")
    int updatePaymentStatus(@Param("paymentNo") String paymentNo,
                             @Param("expectedStatus") Integer expectedStatus,
                             @Param("targetStatus") Integer targetStatus,
                             @Param("transactionId") String transactionId,
                             @Param("paymentTime") LocalDateTime paymentTime);
}
