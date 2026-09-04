package com.property.module.bill.service;

import com.property.module.bill.entity.BillItemEntity;
import com.property.module.bill.repository.BillItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账单明细金额分摊组件
 *
 * <p>整单缴费（现金/转账/支付宝回调）只更新了 t_bill.paid_amount，
 * 本组件负责把已缴金额按明细剩余待缴金额顺序分摊到 t_bill_item.paid_amount，
 * 保证费用项维度统计（fee-item-stats）的准确性。</p>
 */
@Component
@RequiredArgsConstructor
public class BillItemAllocator {

    private final BillItemMapper billItemMapper;

    /**
     * 将一笔缴费金额分摊到账单的各明细上
     *
     * @param billId    账单ID
     * @param payAmount 本次缴费金额
     */
    public void allocate(Long billId, BigDecimal payAmount) {
        if (billId == null || payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<BillItemEntity> items = billItemMapper.selectByBillId(billId);
        BigDecimal remaining = payAmount;
        for (BillItemEntity item : items) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal paid = item.getPaidAmount() != null ? item.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal due = item.getAmount().subtract(paid);
            if (due.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal alloc = due.min(remaining);
            billItemMapper.addPaidAmount(item.getId(), alloc);
            remaining = remaining.subtract(alloc);
        }
    }
}
