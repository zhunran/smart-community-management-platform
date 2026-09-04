package com.property.adminapi.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.adminapi.dto.response.PaymentOrderImportVO;
import com.property.module.bill.dto.request.ManualPaymentRequest;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.service.BillService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * EasyExcel 缴费记录导入监听器
 *
 * 逐行读取 → 校验 → 批量缴费
 * 支持 Excel 列：账单编号、支付方式、付款人、支付时间、备注
 */
@Slf4j
@RequiredArgsConstructor
public class PaymentOrderImportListener implements ReadListener<PaymentOrderImportVO> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BATCH_SIZE = 100;

    private final BillMapper billMapper;
    private final BillService billService;
    private final TransactionTemplate txTemplate;

    private final List<PaymentOrderImportVO> cachedList = new ArrayList<>();

    @Getter
    private int successCount = 0;

    @Getter
    private int failCount = 0;

    @Getter
    private final List<PaymentOrderImportVO> failList = new ArrayList<>();

    @Override
    public void invoke(PaymentOrderImportVO row, AnalysisContext context) {
        // 校验单行
        validateRow(row);

        if (row.isValid()) {
            cachedList.add(row);
        } else {
            failCount++;
            failList.add(row);
        }

        // 达到批次阈值，批量处理
        if (cachedList.size() >= BATCH_SIZE) {
            batchProcess();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!cachedList.isEmpty()) {
            batchProcess();
        }
        log.info("缴费记录导入完成：成功{}条，失败{}条", successCount, failCount);
    }

    /**
     * 批量处理缴费（每行一个独立事务，成功行提交、失败行回滚）
     */
    private void batchProcess() {
        for (PaymentOrderImportVO row : cachedList) {
            try {
                txTemplate.executeWithoutResult(status -> {
                    // 根据账单编号查询账单
                    BillEntity bill = billMapper.selectOne(
                            new LambdaQueryWrapper<BillEntity>()
                                    .eq(BillEntity::getBillNo, row.getBillNo())
                                    .ne(BillEntity::getStatus, 3)  // 非作废
                    );
                    if (bill == null) {
                        throw new IllegalArgumentException("账单不存在或已作废：" + row.getBillNo());
                    }

                    // 构建手动缴费请求
                    ManualPaymentRequest request = new ManualPaymentRequest();
                    request.setBillId(bill.getId());
                    request.setPaymentMethod(row.getPaymentMethod());
                    request.setPayerName(row.getPayerName());
                    request.setRemark(row.getRemark());

                    billService.manualPayment(request);
                });
                successCount++;

            } catch (Exception e) {
                log.warn("缴费导入失败 [billNo={}]: {}", row.getBillNo(), e.getMessage());
                row.setValid(false);
                row.setErrorMsg(e.getMessage());
                failCount++;
                failList.add(row);
            }
        }
        cachedList.clear();
    }

    /**
     * 校验单行数据
     */
    private void validateRow(PaymentOrderImportVO row) {
        StringBuilder errors = new StringBuilder();

        if (row.getBillNo() == null || row.getBillNo().isBlank()) {
            errors.append("账单编号不能为空；");
        }

        // 解析支付方式
        row.setPaymentMethod(parsePaymentMethod(row.getPaymentMethodLabel()));
        if (row.getPaymentMethod() == null) {
            errors.append("支付方式无效（现金/转账）；");
        }

        if (!errors.isEmpty()) {
            row.setValid(false);
            row.setErrorMsg(errors.toString());
        }
    }

    /**
     * 解析支付方式中文标签
     */
    private Integer parsePaymentMethod(String label) {
        if (label == null || label.isBlank()) return null;
        return switch (label.trim()) {
            case "现金" -> 4;
            case "转账" -> 5;
            default -> null;
        };
    }
}
