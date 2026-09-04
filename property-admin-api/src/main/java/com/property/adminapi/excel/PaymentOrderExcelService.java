package com.property.adminapi.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.adminapi.dto.response.PaymentOrderExcelVO;
import com.property.adminapi.dto.response.PaymentOrderImportVO;
import com.property.module.bill.dto.request.PaymentOrderPageQuery;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.repository.PaymentOrderMapper;
import com.property.module.bill.service.BillService;
import com.property.module.owner.service.OwnerService;
import com.property.module.housing.service.RoomDataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 缴费记录 Excel 导出服务
 */
@Service
@RequiredArgsConstructor
public class PaymentOrderExcelService {

    private static final String[] PAYMENT_METHOD_NAMES = {"", "支付宝", "微信", "银行卡", "现金", "转账", "其他"};
    private static final String[] PAYMENT_STATUS_NAMES = {"待支付", "支付中", "支付成功", "支付失败", "已退款", "部分退款"};
    private static final int PAGE_SIZE = 2000;

    private final PaymentOrderMapper paymentOrderMapper;
    private final RoomDataService roomDataService;
    private final OwnerService ownerService;
    private final BillMapper billMapper;
    private final BillService billService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 导出缴费记录 Excel（流式分批写入）
     */
    public void exportExcel(PaymentOrderPageQuery query, HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode(
                "缴费记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        // 构建查询条件
        LambdaQueryWrapper<PaymentOrderEntity> wrapper = new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(query.getBillId() != null, PaymentOrderEntity::getBillId, query.getBillId())
                .eq(query.getRoomId() != null, PaymentOrderEntity::getRoomId, query.getRoomId())
                .eq(query.getOwnerId() != null, PaymentOrderEntity::getOwnerId, query.getOwnerId())
                .eq(query.getPaymentMethod() != null, PaymentOrderEntity::getPaymentMethod, query.getPaymentMethod())
                .eq(query.getPaymentStatus() != null, PaymentOrderEntity::getPaymentStatus, query.getPaymentStatus())
                .like(query.getPaymentNo() != null && !query.getPaymentNo().isEmpty(), PaymentOrderEntity::getPaymentNo, query.getPaymentNo())
                .like(query.getPayerName() != null && !query.getPayerName().isEmpty(), PaymentOrderEntity::getPayerName, query.getPayerName())
                .ge(query.getPaymentTimeStart() != null, PaymentOrderEntity::getPaymentTime, query.getPaymentTimeStart())
                .le(query.getPaymentTimeEnd() != null, PaymentOrderEntity::getPaymentTime, query.getPaymentTimeEnd())
                .orderByDesc(PaymentOrderEntity::getPaymentTime);

        long total = paymentOrderMapper.selectCount(wrapper);

        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), PaymentOrderExcelVO.class).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("缴费记录").build();

            for (int i = 0; i * PAGE_SIZE < total; i++) {
                List<PaymentOrderEntity> entities = paymentOrderMapper.selectList(
                        wrapper.last("LIMIT " + (i * PAGE_SIZE) + "," + PAGE_SIZE)
                );
                List<PaymentOrderExcelVO> voList = entities.stream().map(this::toExcelVO).collect(Collectors.toList());
                excelWriter.write(voList, writeSheet);
            }
        }
    }

    /**
     * 导入缴费记录 Excel（每行独立事务）
     */
    public PaymentOrderImportListener importExcel(InputStream inputStream) throws IOException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        PaymentOrderImportListener listener = new PaymentOrderImportListener(billMapper, billService, txTemplate);
        EasyExcel.read(inputStream, PaymentOrderImportVO.class, listener)
                .sheet()
                .doRead();
        return listener;
    }

    private PaymentOrderExcelVO toExcelVO(PaymentOrderEntity entity) {
        PaymentOrderExcelVO vo = new PaymentOrderExcelVO();
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setPaymentMethodName(getPaymentMethodName(entity.getPaymentMethod()));
        vo.setPaymentAmount(entity.getPaymentAmount() != null ? entity.getPaymentAmount().toString() : "");
        vo.setPaymentStatusName(getPaymentStatusName(entity.getPaymentStatus()));
        vo.setPaymentTime(entity.getPaymentTime() != null
                ? entity.getPaymentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        vo.setTransactionId(entity.getTransactionId());
        vo.setPayerName(entity.getPayerName());
        vo.setRemark(entity.getRemark());

        // 房屋信息
        if (entity.getRoomId() != null) {
            vo.setBuildingName(roomDataService.getBuildingNameByRoomId(entity.getRoomId()));
            vo.setRoomCode(roomDataService.getRoomCodeByRoomId(entity.getRoomId()));
        }

        // 业主信息
        if (entity.getOwnerId() != null) {
            vo.setOwnerName(ownerService.getOwnerNameById(entity.getOwnerId()));
            vo.setOwnerPhone(ownerService.getOwnerPhoneById(entity.getOwnerId()));
        }

        // 账单信息
        if (entity.getBillId() != null) {
            BillEntity bill = billMapper.selectById(entity.getBillId());
            if (bill != null) {
                vo.setBillNo(bill.getBillNo());
                vo.setBillPeriod(bill.getBillPeriod());
            }
        }

        return vo;
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null || method < 0 || method >= PAYMENT_METHOD_NAMES.length) return "";
        return PAYMENT_METHOD_NAMES[method];
    }

    private String getPaymentStatusName(Integer status) {
        if (status == null || status < 0 || status >= PAYMENT_STATUS_NAMES.length) return "";
        return PAYMENT_STATUS_NAMES[status];
    }
}
