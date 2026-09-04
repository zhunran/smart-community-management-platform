package com.property.module.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.dto.request.BillGenerateRequest;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.request.ItemizedPaymentRequest;
import com.property.module.bill.dto.request.ManualPaymentRequest;
import com.property.module.bill.dto.response.BillDetailVO;
import com.property.module.bill.dto.response.BillItemVO;
import com.property.module.bill.dto.response.BillVO;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.BillItemEntity;
import com.property.module.bill.entity.BillStatusEnum;
import com.property.module.bill.entity.FeeItemEntity;
import com.property.module.bill.entity.FeeStandardEntity;
import com.property.module.bill.repository.*;
import com.property.module.bill.service.BillItemAllocator;
import com.property.module.bill.service.BillService;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.owner.service.OwnerRoomService;
import com.property.module.owner.service.OwnerService;
import com.property.module.housing.service.RoomDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillMapper billMapper;
    private final BillItemMapper billItemMapper;
    private final FeeItemMapper feeItemMapper;
    private final FeeStandardMapper feeStandardMapper;
    private final RoomDataService roomDataService;
    private final OwnerRoomService ownerRoomService;
    private final OwnerService ownerService;
    private final BillPaymentMapper billPaymentMapper;
    private final BillItemAllocator billItemAllocator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generate(BillGenerateRequest request) {
        String period = normalizePeriod(request.getBillPeriod());
        LocalDate billDate = LocalDate.now();
        LocalDate dueDate = parseDueDate(request.getDueDate(), period);

        // 1. 获取所有有效费用项
        List<FeeItemEntity> feeItems = feeItemMapper.selectList(
                new LambdaQueryWrapper<FeeItemEntity>()
                        .eq(FeeItemEntity::getStatus, 1)
                        .orderByAsc(FeeItemEntity::getSortOrder)
        );
        if (feeItems.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "没有启用的费用项，无法生成账单");
        }

        // 2. 获取房屋列表（ownerId 优先，其次 roomIds，最后全部有效房屋）
        List<Long> roomIds;
        if (request.getOwnerId() != null) {
            roomIds = ownerRoomService.getRoomIdsByOwnerId(request.getOwnerId());
            if (roomIds.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_FAILED, "该业主名下没有有效房屋，无法生成账单");
            }
        } else if (request.getRoomIds() != null && !request.getRoomIds().isEmpty()) {
            roomIds = request.getRoomIds();
        } else {
            roomIds = roomDataService.getAllActiveRoomIds();
        }

        if (roomIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "没有有效的房屋，无法生成账单");
        }

        // 3. 预加载费用标准覆盖（roomId + feeItemId → unitPrice）
        List<FeeStandardEntity> allStandards = feeStandardMapper.selectList(
                new LambdaQueryWrapper<FeeStandardEntity>()
                        .eq(FeeStandardEntity::getStatus, 1)
                        .le(FeeStandardEntity::getStartDate, billDate)
                        .apply("(end_date IS NULL OR end_date >= {0})", billDate)
        );
        Map<String, BigDecimal> standardMap = allStandards.stream()
                .collect(Collectors.toMap(
                        s -> s.getRoomId() + ":" + s.getFeeItemId(),
                        FeeStandardEntity::getUnitPrice,
                        (a, b) -> b   // 取最新的
                ));

        int successCount = 0;

        // 4. 遍历房屋生成账单
        for (Long roomId : roomIds) {
            try {
                successCount += generateBillForRoom(roomId, period, billDate, dueDate, feeItems, standardMap);
            } catch (Exception e) {
                log.warn("生成账单失败 [roomId={}, period={}]: {}", roomId, period, e.getMessage());
            }
        }

        log.info("账单生成完成：成功{}笔，账期{}", successCount, period);
        return successCount;
    }

    /**
     * 为单个房屋生成账单
     */
    private int generateBillForRoom(Long roomId, String period, LocalDate billDate, LocalDate dueDate,
                                    List<FeeItemEntity> feeItems, Map<String, BigDecimal> standardMap) {
        // 幂等检查：该房屋本账期是否已生成非终态账单（排除已作废/已减免/已缴清）
        Long exists = billMapper.selectCount(
                new LambdaQueryWrapper<BillEntity>()
                        .eq(BillEntity::getRoomId, roomId)
                        .eq(BillEntity::getBillPeriod, period)
                        .notIn(BillEntity::getStatus,
                                BillStatusEnum.VOIDED.getValue(),
                                BillStatusEnum.DISCOUNTED.getValue(),
                                BillStatusEnum.PAID.getValue())
        );
        if (exists > 0) {
            log.info("跳过已生成 [roomId={}, period={}]", roomId, period);
            return 0;
        }

        // 查询房屋面积
        BigDecimal area = roomDataService.getAreaByRoomId(roomId);
        if (area == null) {
            log.warn("房屋不存在 [roomId={}]", roomId);
            return 0;
        }

        // 查询主要业主
        Long ownerId = ownerRoomService.getPrimaryOwnerIdByRoomId(roomId);
        if (ownerId == null) {
            log.warn("房屋无主要业主 [roomId={}]", roomId);
            return 0;
        }

        // 生成账单编号
        String billNo = generateBillNo(period);

        // 创建账单主表
        BillEntity bill = new BillEntity();
        bill.setBillNo(billNo);
        bill.setRoomId(roomId);
        bill.setOwnerId(ownerId);
        bill.setBillPeriod(period);
        bill.setBillType(1);
        bill.setBillDate(billDate);
        bill.setDueDate(dueDate);
        bill.setTotalAmount(BigDecimal.ZERO);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setLateFee(BigDecimal.ZERO);
        bill.setStatus(BillStatusEnum.UNPAID.getValue());
        billMapper.insert(bill);

        // 生成明细
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FeeItemEntity feeItem : feeItems) {
            // 获取单价（优先使用房屋级别覆盖）
            BigDecimal unitPrice = getUnitPrice(roomId, feeItem, standardMap);

            // 计算金额
            BillItemCalcResult calcResult = calcBillItem(roomId, area, feeItem, unitPrice, period);
            if (calcResult == null || calcResult.amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BillItemEntity item = new BillItemEntity();
            item.setBillId(bill.getId());
            item.setFeeItemId(feeItem.getId());
            item.setFeeItemName(feeItem.getItemName());
            item.setCalcBase(calcResult.calcBase);
            item.setUnitPrice(unitPrice);
            item.setQuantity(calcResult.quantity);
            item.setAmount(calcResult.amount);
            item.setDiscountAmount(BigDecimal.ZERO);
            item.setPaidAmount(BigDecimal.ZERO);
            billItemMapper.insert(item);

            totalAmount = totalAmount.add(calcResult.amount);
        }

        // 更新账单总金额
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 没有有效费用项，删除账单
            billMapper.deleteById(bill.getId());
            return 0;
        }

        bill.setTotalAmount(totalAmount);
        billMapper.updateById(bill);

        return 1;
    }

    /**
     * 计算单条费用项
     */
    private BillItemCalcResult calcBillItem(Long roomId, BigDecimal area, FeeItemEntity feeItem,
                                            BigDecimal unitPrice, String period) {
        BigDecimal calcBase;
        BigDecimal quantity;
        BigDecimal amount;

        Integer calcType = feeItem.getCalcType() != null ? feeItem.getCalcType() : 1;
        Integer billingCycle = feeItem.getBillingCycle() != null ? feeItem.getBillingCycle() : 1;

        // 计算数量（月数）
        quantity = switch (billingCycle) {
            case 2 -> BigDecimal.valueOf(3);   // 季度
            case 3 -> BigDecimal.valueOf(6);   // 半年
            case 4 -> BigDecimal.valueOf(12);  // 年
            default -> BigDecimal.ONE;          // 月
        };

        switch (calcType) {
            case 1 -> {
                // 按面积：物业费 = 面积 × 单价 × 月数
                calcBase = area;
                amount = area.multiply(unitPrice).multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            }
            case 2 -> {
                // 按户
                calcBase = BigDecimal.ONE;
                amount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            }
            case 3 -> {
                // 按用量（水/电）— 无抄表数据时按预估用量 100
                calcBase = BigDecimal.valueOf(100);
                amount = calcBase.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            }
            case 4 -> {
                // 固定金额
                calcBase = BigDecimal.ONE;
                amount = unitPrice.setScale(2, RoundingMode.HALF_UP);
            }
            default -> {
                return null;
            }
        }
        return new BillItemCalcResult(calcBase, quantity, amount);
    }

    /**
     * 获取最终单价（房屋级别覆盖 > 费用项默认单价）
     */
    private BigDecimal getUnitPrice(Long roomId, FeeItemEntity feeItem, Map<String, BigDecimal> standardMap) {
        String key = roomId + ":" + feeItem.getId();
        BigDecimal override = standardMap.get(key);
        return override != null ? override : feeItem.getUnitPrice();
    }

    /**
     * 生成账单编号：BILL + yyyyMMdd + 4位流水
     */
    private String generateBillNo(String period) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当日最大流水号
        String prefix = "BILL" + datePart;
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<BillEntity>()
                .likeRight(BillEntity::getBillNo, prefix)
                .orderByDesc(BillEntity::getBillNo)
                .last("LIMIT 1");
        BillEntity last = billMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getBillNo() != null) {
            String lastSeq = last.getBillNo().substring(last.getBillNo().length() - 4);
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 解析缴费截止日期
     */
    private LocalDate parseDueDate(String dueDateStr, String period) {
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            return LocalDate.parse(dueDateStr);
        }
        // 默认当月最后一天
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
    }

    /**
     * 规范化账期格式为 yyyy-MM（兼容 yyyy-M 输入，避免与统计查询格式不一致）
     */
    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return period;
        }
        try {
            YearMonth ym = YearMonth.parse(period.trim(), DateTimeFormatter.ofPattern("yyyy-M"));
            return ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            log.warn("账期格式无法识别，拒绝写入 [period={}]", period);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账期格式错误：" + period + "，应为 yyyy-MM，如 2026-08");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long manualPayment(ManualPaymentRequest request) {
        // 校验支付方式
        if (request.getPaymentMethod() == null ||
                (request.getPaymentMethod() != 4 && request.getPaymentMethod() != 5)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式仅支持：4-现金(CASH)、5-转账(TRANSFER)");
        }

        // 查询账单
        BillEntity bill = billMapper.selectById(request.getBillId());
        if (bill == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "账单不存在");
        }

        BillStatusEnum currentStatus = BillStatusEnum.fromValue(bill.getStatus());
        if (currentStatus == null || !currentStatus.canPay()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "当前账单状态不允许缴费 [status=" + bill.getStatus() + "]");
        }

        // 计算待缴金额
        BigDecimal paidSoFar = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amount = bill.getTotalAmount().subtract(paidSoFar);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "账单无需缴费");
        }

        // 生成支付记录
        LocalDateTime now = LocalDateTime.now();
        String paymentNo = generatePaymentNo();

        // 获取付款人姓名
        String payerName = request.getPayerName();
        if (payerName == null || payerName.isBlank()) {
            payerName = "业主ID:" + bill.getOwnerId();
        }

        Long paymentId = IdWorker.getId();
        int inserted = billPaymentMapper.insertPayment(
                paymentId,                 // id
                paymentNo,                 // payment_no
                bill.getId(),              // bill_id
                bill.getRoomId(),          // room_id
                bill.getOwnerId(),         // owner_id
                request.getPaymentMethod(),// payment_method
                amount,                    // payment_amount
                now,                       // payment_time
                2,                         // payment_status（支付成功）
                payerName,                // payer_name
                request.getRemark(),       // remark
                SecurityUtil.getUsername() // create_by
        );
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "创建支付记录失败");
        }

        // 更新账单已缴金额和状态（自动计算 UNPAID → PAID / PARTIAL → PAID）
        BigDecimal newPaidAmount = paidSoFar.add(amount);
        bill.setPaidAmount(newPaidAmount);
        bill.setStatus(BillStatusEnum.computeAfterPayment(newPaidAmount, bill.getTotalAmount()).getValue());
        billMapper.updateById(bill);

        // 同步分摊到账单明细，保证费用项维度实收统计准确
        billItemAllocator.allocate(bill.getId(), amount);

        log.info("手动缴费成功 [billId={}, paymentNo={}, method={}, amount={}]",
                bill.getId(), paymentNo, request.getPaymentMethod(), amount);
        return paymentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long itemizedPayment(ItemizedPaymentRequest request) {
        // 校验支付方式
        if (request.getPaymentMethod() == null ||
                (request.getPaymentMethod() != 4 && request.getPaymentMethod() != 5)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式仅支持：4-现金(CASH)、5-转账(TRANSFER)");
        }

        // 查询账单
        BillEntity bill = billMapper.selectById(request.getBillId());
        if (bill == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "账单不存在");
        }

        BillStatusEnum currentStatus = BillStatusEnum.fromValue(bill.getStatus());
        if (currentStatus == null || !currentStatus.canPay()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR,
                    "当前账单状态不允许缴费 [status=" + bill.getStatus() + "]");
        }

        // 查询本次缴费涉及的账单明细
        List<Long> itemIds = request.getItems().stream()
                .map(ItemizedPaymentRequest.ItemPayment::getBillItemId)
                .collect(Collectors.toList());
        List<BillItemEntity> dbItems = billItemMapper.selectBatchByIds(itemIds);

        // 验证明细归属和金额
        Map<Long, BillItemEntity> itemMap = dbItems.stream()
                .collect(Collectors.toMap(BillItemEntity::getId, item -> item));
        BigDecimal totalPayAmount = BigDecimal.ZERO;

        for (ItemizedPaymentRequest.ItemPayment payItem : request.getItems()) {
            BillItemEntity dbItem = itemMap.get(payItem.getBillItemId());
            if (dbItem == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXISTS,
                        "账单明细不存在 [id=" + payItem.getBillItemId() + "]");
            }
            if (!dbItem.getBillId().equals(request.getBillId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "账单明细 [id=" + payItem.getBillItemId() + "] 不属于该账单");
            }

            // 计算该明细剩余待缴金额
            BigDecimal paid = dbItem.getPaidAmount() != null ? dbItem.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal remain = dbItem.getAmount().subtract(paid);
            if (payItem.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "缴费金额必须大于0 [item=" + dbItem.getFeeItemName() + "]");
            }
            if (payItem.getAmount().compareTo(remain) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "缴费金额超过待缴金额 [item=" + dbItem.getFeeItemName()
                                + ", 本次=" + payItem.getAmount() + ", 待缴=" + remain + "]");
            }

            totalPayAmount = totalPayAmount.add(payItem.getAmount());
        }

        // 更新各明细的已交金额
        for (ItemizedPaymentRequest.ItemPayment payItem : request.getItems()) {
            billItemMapper.addPaidAmount(payItem.getBillItemId(), payItem.getAmount());
        }

        // 更新账单已交金额和状态（自动计算 UNPAID → PARTIAL / PAID）
        BigDecimal newPaidAmount = (bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO)
                .add(totalPayAmount);
        bill.setPaidAmount(newPaidAmount);
        bill.setStatus(BillStatusEnum.computeAfterPayment(newPaidAmount, bill.getTotalAmount()).getValue());
        billMapper.updateById(bill);

        // 生成支付记录
        LocalDateTime now = LocalDateTime.now();
        String paymentNo = generatePaymentNo();
        String payerName = request.getPayerName();
        if (payerName == null || payerName.isBlank()) {
            payerName = "业主ID:" + bill.getOwnerId();
        }

        String remark = request.getRemark();
        if (remark == null || remark.isBlank()) {
            String itemNames = dbItems.stream()
                    .map(BillItemEntity::getFeeItemName)
                    .collect(Collectors.joining("、"));
            remark = "分项缴费：" + itemNames;
        }

        Long paymentId = IdWorker.getId();
        int inserted = billPaymentMapper.insertPayment(
                paymentId,
                paymentNo,
                bill.getId(),
                bill.getRoomId(),
                bill.getOwnerId(),
                request.getPaymentMethod(),
                totalPayAmount,
                now,
                2,                         // payment_status（支付成功）
                payerName,
                remark,
                SecurityUtil.getUsername() // create_by
        );
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "创建支付记录失败");
        }

        log.info("分项缴费成功 [billId={}, paymentNo={}, method={}, amount={}, items={}]",
                bill.getId(), paymentNo, request.getPaymentMethod(), totalPayAmount, itemIds);
        return paymentId;
    }

    /**
     * 生成支付单号：PAY + yyyyMMdd + 4位流水
     */
    private String generatePaymentNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PAY" + datePart;
        String lastNo = billPaymentMapper.selectLastPaymentNo(prefix + "%");
        int seq = 1;
        if (lastNo != null) {
            String lastSeq = lastNo.substring(lastNo.length() - 4);
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    @Override
    public IPage<BillVO> page(BillPageQuery query) {
        Page<BillEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<BillEntity> wrapper = new LambdaQueryWrapper<BillEntity>()
                .eq(query.getRoomId() != null, BillEntity::getRoomId, query.getRoomId())
                .eq(query.getOwnerId() != null, BillEntity::getOwnerId, query.getOwnerId())
                .eq(query.getBillType() != null, BillEntity::getBillType, query.getBillType())
                .eq(query.getStatus() != null, BillEntity::getStatus, query.getStatus())
                .like(query.getBillPeriod() != null, BillEntity::getBillPeriod, query.getBillPeriod())
                .orderByDesc(BillEntity::getCreateTime);

        // 楼栋过滤：根据 buildingId 查询所属房屋ID列表，再过滤账单
        if (query.getBuildingId() != null && query.getBuildingId() > 0) {
            List<Long> roomIds = roomDataService.getRoomIdsByBuildingId(query.getBuildingId());
            if (roomIds.isEmpty()) {
                // 该楼栋下没有房屋，返回空结果
                Page<BillVO> emptyPage = new Page<>(page.getCurrent(), page.getSize());
                emptyPage.setRecords(List.of());
                emptyPage.setTotal(0);
                return emptyPage;
            }
            wrapper.in(BillEntity::getRoomId, roomIds);
        }

        // 账期范围过滤
        if (query.getBillPeriodStart() != null && !query.getBillPeriodStart().isEmpty()) {
            wrapper.ge(BillEntity::getBillPeriod, query.getBillPeriodStart());
        }
        if (query.getBillPeriodEnd() != null && !query.getBillPeriodEnd().isEmpty()) {
            wrapper.le(BillEntity::getBillPeriod, query.getBillPeriodEnd());
        }

        // 停车费过滤（停车费 fee_item_id = 4）
        if (query.getHasParkingFee() != null) {
            List<Long> parkingBillIds = billItemMapper.selectBillIdsByFeeItemId(4L);
            if (Boolean.TRUE.equals(query.getHasParkingFee())) {
                if (parkingBillIds.isEmpty()) {
                    // 没有含停车费的账单，返回空结果
                    Page<BillVO> emptyPage = new Page<>(page.getCurrent(), page.getSize());
                    emptyPage.setRecords(List.of());
                    emptyPage.setTotal(0);
                    return emptyPage;
                }
                wrapper.in(BillEntity::getId, parkingBillIds);
            } else {
                // 不含停车费：排除含停车费的账单
                wrapper.notIn(!parkingBillIds.isEmpty(), BillEntity::getId, parkingBillIds);
            }
        }

        IPage<BillEntity> entityPage = billMapper.selectPage(page, wrapper);
        IPage<BillVO> voPage = entityPage.convert(this::toVO);

        // 批量填充楼栋信息（替代逐条 N+1 查询）
        fillBuildingInfo(voPage.getRecords());

        return voPage;
    }

    /**
     * 批量查询楼栋信息并填充到账单VO（替代逐条 N+1 查询）
     */
    private void fillBuildingInfo(List<BillVO> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> roomIds = records.stream()
                .map(BillVO::getRoomId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) return;

        List<Map<String, Object>> infos = roomDataService.getRoomBuildingInfoBatch(roomIds);
        Map<Long, Long> buildingIdMap = new HashMap<>();
        Map<Long, String> buildingNameMap = new HashMap<>();
        for (Map<String, Object> info : infos) {
            Long roomId = ((Number) info.get("roomId")).longValue();
            Object bid = info.get("buildingId");
            buildingIdMap.put(roomId, bid != null ? ((Number) bid).longValue() : null);
            buildingNameMap.put(roomId, (String) info.get("buildingName"));
        }
        for (BillVO vo : records) {
            vo.setBuildingId(buildingIdMap.get(vo.getRoomId()));
            vo.setBuildingName(buildingNameMap.get(vo.getRoomId()));
        }
    }

    @Override
    public BillDetailVO getDetail(Long id) {
        BillEntity entity = billMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "账单不存在");
        }

        BillDetailVO vo = toDetailVO(entity);

        // 查询明细
        List<BillItemEntity> items = billItemMapper.selectList(
                new LambdaQueryWrapper<BillItemEntity>()
                        .eq(BillItemEntity::getBillId, id)
                        .orderByAsc(BillItemEntity::getCreateTime)
        );
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public BillDetailVO getDetail(Long id, Long ownerId) {
        BillDetailVO detail = getDetail(id);
        if (detail.getOwnerId() == null || !detail.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该账单");
        }
        return detail;
    }

    private BillVO toVO(BillEntity entity) {
        BillVO vo = new BillVO();
        vo.setId(entity.getId());
        vo.setBillNo(entity.getBillNo());
        vo.setRoomId(entity.getRoomId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setBillPeriod(entity.getBillPeriod());
        vo.setBillType(entity.getBillType());
        vo.setBillDate(entity.getBillDate());
        vo.setDueDate(entity.getDueDate());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setPaidAmount(entity.getPaidAmount());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private BillDetailVO toDetailVO(BillEntity entity) {
        BillDetailVO vo = new BillDetailVO();
        vo.setId(entity.getId());
        vo.setBillNo(entity.getBillNo());
        vo.setRoomId(entity.getRoomId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setBillPeriod(entity.getBillPeriod());
        vo.setBillType(entity.getBillType());
        vo.setBillDate(entity.getBillDate());
        vo.setDueDate(entity.getDueDate());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setPaidAmount(entity.getPaidAmount());
        vo.setDiscountAmount(entity.getDiscountAmount());
        vo.setLateFee(entity.getLateFee());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private BillItemVO toItemVO(BillItemEntity entity) {
        BillItemVO vo = new BillItemVO();
        vo.setId(entity.getId());
        vo.setFeeItemId(entity.getFeeItemId());
        vo.setFeeItemName(entity.getFeeItemName());
        vo.setCalcBase(entity.getCalcBase());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setQuantity(entity.getQuantity());
        vo.setAmount(entity.getAmount());
        vo.setDiscountAmount(entity.getDiscountAmount());
        vo.setPaidAmount(entity.getPaidAmount());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    /** 费用计算中间结果 */
    private record BillItemCalcResult(BigDecimal calcBase, BigDecimal quantity, BigDecimal amount) {}
}
