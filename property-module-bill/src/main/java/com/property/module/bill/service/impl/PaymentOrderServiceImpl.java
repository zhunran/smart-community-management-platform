package com.property.module.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.dto.request.PaymentOrderPageQuery;
import com.property.module.bill.dto.response.PaymentOrderVO;
import com.property.module.bill.entity.BillEntity;
import com.property.module.bill.entity.PaymentOrderEntity;
import com.property.module.bill.repository.BillMapper;
import com.property.module.bill.repository.PaymentOrderMapper;
import com.property.module.bill.service.PaymentOrderService;
import com.property.module.owner.service.OwnerRoomService;
import com.property.module.owner.service.OwnerService;
import com.property.module.housing.service.RoomDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支付记录服务实现
 */
@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private static final String[] PAYMENT_METHOD_NAMES = {"", "支付宝", "微信", "银行卡", "现金", "转账", "其他"};
    private static final String[] PAYMENT_STATUS_NAMES = {"待支付", "支付中", "支付成功", "支付失败", "已退款", "部分退款"};

    private final PaymentOrderMapper paymentOrderMapper;
    private final RoomDataService roomDataService;
    private final OwnerRoomService ownerRoomService;
    private final OwnerService ownerService;
    private final BillMapper billMapper;

    @Override
    public IPage<PaymentOrderVO> page(PaymentOrderPageQuery query) {
        Page<PaymentOrderEntity> page = new Page<>(query.getCurrent(), query.getSize());
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

        IPage<PaymentOrderEntity> entityPage = paymentOrderMapper.selectPage(page, wrapper);

        // 批量预查询关联数据（避免 N+1 查询）
        Map<Long, String[]> roomInfoMap = Collections.emptyMap();
        Map<Long, String[]> ownerInfoMap = Collections.emptyMap();
        Map<Long, BillEntity> billMap = Collections.emptyMap();

        List<PaymentOrderEntity> records = entityPage.getRecords();
        if (records != null && !records.isEmpty()) {
            // 批量查询房屋信息
            List<Long> roomIds = records.stream().map(PaymentOrderEntity::getRoomId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!roomIds.isEmpty()) {
                List<Map<String, Object>> roomInfos = roomDataService.getRoomCodeNameBatch(roomIds);
                // 批量查询楼栋
                List<Map<String, Object>> buildingInfos = roomDataService.getRoomBuildingInfoBatch(roomIds);
                Map<Long, String[]> buildingMap = new HashMap<>();
                for (Map<String, Object> info : buildingInfos) {
                    Long rid = ((Number) info.get("roomId")).longValue();
                    buildingMap.put(rid, new String[]{
                            (String) info.get("buildingName"),
                            null // roomCode and roomName filled separately
                    });
                }
                roomInfoMap = new HashMap<>();
                for (Map<String, Object> info : roomInfos) {
                    Long rid = ((Number) info.get("roomId")).longValue();
                    String[] b = buildingMap.getOrDefault(rid, new String[]{null, null});
                    roomInfoMap.put(rid, new String[]{
                            b[0], // buildingName
                            (String) info.get("roomCode"),
                            (String) info.get("roomName")
                    });
                }
            }

            // 批量查询业主信息
            List<Long> ownerIds = records.stream().map(PaymentOrderEntity::getOwnerId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!ownerIds.isEmpty()) {
                List<Map<String, Object>> ownerInfos = ownerService.getOwnerInfoBatch(ownerIds);
                ownerInfoMap = new HashMap<>();
                for (Map<String, Object> info : ownerInfos) {
                    Long oid = ((Number) info.get("ownerId")).longValue();
                    ownerInfoMap.put(oid, new String[]{
                            (String) info.get("ownerName"),
                            (String) info.get("ownerPhone")
                    });
                }
            }

            // 批量查询账单信息
            List<Long> billIds = records.stream().map(PaymentOrderEntity::getBillId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!billIds.isEmpty()) {
                List<BillEntity> bills = billMapper.selectBatchIds(billIds);
                billMap = bills.stream().collect(Collectors.toMap(BillEntity::getId, b -> b));
            }
        }

        final Map<Long, String[]> finalRoomInfoMap = roomInfoMap;
        final Map<Long, String[]> finalOwnerInfoMap = ownerInfoMap;
        final Map<Long, BillEntity> finalBillMap = billMap;
        return entityPage.convert(entity -> toVO(entity, finalRoomInfoMap, finalOwnerInfoMap, finalBillMap));
    }

    @Override
    public PaymentOrderVO getDetail(Long id) {
        PaymentOrderEntity entity = paymentOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "支付记录不存在");
        }
        Map<Long, String[]> roomInfoMap = new HashMap<>();
        Map<Long, String[]> ownerInfoMap = new HashMap<>();
        Map<Long, BillEntity> billMap = new HashMap<>();
        if (entity.getRoomId() != null) {
            roomInfoMap.put(entity.getRoomId(), new String[]{
                    roomDataService.getBuildingNameByRoomId(entity.getRoomId()),
                    roomDataService.getRoomCodeByRoomId(entity.getRoomId()),
                    roomDataService.getRoomNameByRoomId(entity.getRoomId())
            });
        }
        if (entity.getOwnerId() != null) {
            ownerInfoMap.put(entity.getOwnerId(), new String[]{
                    ownerService.getOwnerNameById(entity.getOwnerId()),
                    ownerService.getOwnerPhoneById(entity.getOwnerId())
            });
        }
        if (entity.getBillId() != null) {
            BillEntity bill = billMapper.selectById(entity.getBillId());
            if (bill != null) billMap.put(bill.getId(), bill);
        }
        return toVO(entity, roomInfoMap, ownerInfoMap, billMap);
    }

    private PaymentOrderVO toVO(PaymentOrderEntity entity,
                                 Map<Long, String[]> roomInfoMap,
                                 Map<Long, String[]> ownerInfoMap,
                                 Map<Long, BillEntity> billMap) {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setId(entity.getId());
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setBillId(entity.getBillId());
        vo.setRoomId(entity.getRoomId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setPaymentMethod(entity.getPaymentMethod());
        vo.setPaymentMethodName(getPaymentMethodName(entity.getPaymentMethod()));
        vo.setPaymentAmount(entity.getPaymentAmount());
        vo.setPaymentTime(entity.getPaymentTime());
        vo.setTransactionId(entity.getTransactionId());
        vo.setPaymentStatus(entity.getPaymentStatus());
        vo.setPaymentStatusName(getPaymentStatusName(entity.getPaymentStatus()));
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setPayerName(entity.getPayerName());
        vo.setPayerPhone(entity.getPayerPhone());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        // 从批量预加载的Map中填充，避免逐条查询
        if (entity.getRoomId() != null) {
            String[] roomInfo = roomInfoMap.get(entity.getRoomId());
            if (roomInfo != null) {
                vo.setBuildingName(roomInfo[0]);
                vo.setRoomCode(roomInfo[1]);
                vo.setRoomName(roomInfo[2]);
            }
        }
        if (entity.getOwnerId() != null) {
            String[] ownerInfo = ownerInfoMap.get(entity.getOwnerId());
            if (ownerInfo != null) {
                vo.setOwnerName(ownerInfo[0]);
                vo.setOwnerPhone(ownerInfo[1]);
            }
        }
        if (entity.getBillId() != null) {
            BillEntity bill = billMap.get(entity.getBillId());
            if (bill != null) {
                vo.setBillPeriod(bill.getBillPeriod());
                vo.setBillNo(bill.getBillNo());
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
