package com.property.module.bill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.bill.dto.response.FeeItemStatVO;
import com.property.module.bill.entity.BillItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 账单明细 Mapper
 */
@Mapper
public interface BillItemMapper extends BaseMapper<BillItemEntity> {

    /** 查询包含指定费用项的账单ID列表（去重） */
    @Select("SELECT DISTINCT bill_id FROM t_bill_item WHERE fee_item_id = #{feeItemId} AND del_flag = 0")
    List<Long> selectBillIdsByFeeItemId(@Param("feeItemId") Long feeItemId);

    /** 根据ID列表批量查询账单明细 */
    @Select({"<script>",
            "SELECT * FROM t_bill_item WHERE id IN ",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
            " AND del_flag = 0",
            "</script>"})
    List<BillItemEntity> selectBatchByIds(@Param("ids") List<Long> ids);

    /** 更新指定明细的已交金额 */
    @Update("UPDATE t_bill_item SET paid_amount = paid_amount + #{amount} WHERE id = #{id} AND del_flag = 0")
    int addPaidAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 查询指定账单的全部明细（按 ID 升序，用于金额分摊） */
    @Select("SELECT * FROM t_bill_item WHERE bill_id = #{billId} AND del_flag = 0 ORDER BY id ASC")
    List<BillItemEntity> selectByBillId(@Param("billId") Long billId);

    /** 按费用项统计指定账期的应收/实收/户数 */
    @Select("SELECT " +
            "  i.fee_item_id, " +
            "  MAX(f.item_name) AS fee_item_name, " +
            "  COALESCE(SUM(i.amount), 0) AS receivable, " +
            "  COALESCE(SUM(i.paid_amount), 0) AS received, " +
            "  COUNT(DISTINCT i.bill_id) AS bill_count " +
            "FROM t_bill_item i " +
            "JOIN t_bill b ON b.id = i.bill_id AND b.del_flag = 0 " +
            "LEFT JOIN t_fee_item f ON f.id = i.fee_item_id " +
            "WHERE b.bill_period = #{period} AND i.del_flag = 0 AND b.status != 3 " +
            "GROUP BY i.fee_item_id " +
            "ORDER BY MAX(f.sort_order)")
    List<Map<String, Object>> selectFeeItemStatsByPeriod(@Param("period") String period);

    /** 按费用项统计指定账期的已缴户数（已缴清或部分缴费的账单） */
    @Select("SELECT " +
            "  i.fee_item_id, " +
            "  COUNT(DISTINCT i.bill_id) AS paid_count " +
            "FROM t_bill_item i " +
            "JOIN t_bill b ON b.id = i.bill_id AND b.del_flag = 0 " +
            "WHERE b.bill_period = #{period} AND i.del_flag = 0 " +
            "  AND b.status IN (1, 2) AND i.paid_amount > 0 " +
            "GROUP BY i.fee_item_id")
    List<Map<String, Object>> selectPaidBillCountByPeriod(@Param("period") String period);
}
