package com.property.module.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.parking.dto.request.WarningHandleRequest;
import com.property.module.parking.dto.response.ParkingWarningVO;
import com.property.module.parking.entity.ParkingWarningEntity;
import com.property.module.parking.repository.ParkingWarningMapper;
import com.property.module.parking.service.ParkingReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 车位双轨对账服务实现
 *
 * 通过 SQL 级比对，发现以下 5 类异常并自动生成预警：
 * 1. LEASE_EXPIRED    — 租赁合同到期但车位未更新（HIGH）
 * 2. SPACE_IDLE       — 已售/已租车位长期无使用记录（MEDIUM）
 * 3. PAYMENT_PENDING  — 临时停车结束但未支付（MEDIUM）
 * 4. OCCUPANCY_ANOMALY — 空闲车位有占用记录（HIGH）
 * 5. LEASE_EXPIRING   — 租赁合同即将到期（LOW，提前提醒）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingReconciliationServiceImpl implements ParkingReconciliationService {

    private final ParkingWarningMapper warningMapper;

    /** 闲置判定天数（超过此天数无使用记录视为闲置） */
    private static final int IDLE_DAYS = 30;

    /** 租赁到期提前预警天数 */
    private static final int EXPIRE_ADVANCE_DAYS = 15;

    @Override
    public int reconcile() {
        LocalDateTime now = LocalDateTime.now();
        String batchNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int totalWarnings = 0;

        log.info("====== 车位双轨对账开始 [batchNo={}] ======", batchNo);

        // 1. 租赁到期未更新
        totalWarnings += checkExpiredLeases(now, batchNo);

        // 2. 已售/已租车位闲置
        totalWarnings += checkIdleSpaces(now, batchNo);

        // 3. 临时停车未支付
        totalWarnings += checkUnpaidUsage(batchNo);

        // 4. 空闲车位占用异常
        totalWarnings += checkOccupancyAnomaly(batchNo);

        // 5. 租赁即将到期
        totalWarnings += checkLeasesExpiring(now, batchNo);

        log.info("====== 车位双轨对账完成 [batchNo={}, 预警={}] ======", batchNo, totalWarnings);
        return totalWarnings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, WarningHandleRequest request) {
        ParkingWarningEntity warning = warningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "预警不存在");
        }
        if (warning.getStatus() != 0 && warning.getStatus() != 1) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "该预警已处理或关闭");
        }

        warning.setStatus(2);   // 已处理
        warning.setHandler(SecurityUtil.getUsername() != null ? SecurityUtil.getUsername() : "system");
        warning.setHandleRemark(request.getHandleRemark());
        warning.setHandleTime(LocalDateTime.now());
        warningMapper.updateById(warning);

        log.info("预警已处理 [id={}, handler={}, remark={}]", id, warning.getHandler(), request.getHandleRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String remark) {
        ParkingWarningEntity warning = warningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "预警不存在");
        }
        if (warning.getStatus() == 3) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "该预警已关闭");
        }

        warning.setStatus(3);   // 已关闭
        warning.setHandler(SecurityUtil.getUsername() != null ? SecurityUtil.getUsername() : "system");
        warning.setHandleRemark(remark);
        warning.setHandleTime(LocalDateTime.now());
        warningMapper.updateById(warning);

        log.info("预警已关闭 [id={}]", id);
    }

    @Override
    public IPage<ParkingWarningVO> page(int current, int size, String warningType, Integer status) {
        Page<ParkingWarningEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ParkingWarningEntity> wrapper = new LambdaQueryWrapper<ParkingWarningEntity>()
                .eq(warningType != null && !warningType.isEmpty(), ParkingWarningEntity::getWarningType, warningType)
                .eq(status != null, ParkingWarningEntity::getStatus, status)
                .orderByDesc(ParkingWarningEntity::getCreateTime);

        IPage<ParkingWarningEntity> entityPage = warningMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    // ========== 对账检查方法 ==========

    private int checkExpiredLeases(LocalDateTime now, String batchNo) {
        List<Map<String, Object>> expired = warningMapper.selectExpiredLeases(now);
        int inserted = 0;
        for (Map<String, Object> row : expired) {
            Long spaceId = toLong(row.get("space_id"));
            if (isDuplicate(spaceId, "LEASE_EXPIRED")) continue;
            ParkingWarningEntity w = new ParkingWarningEntity();
            w.setId(IdWorker.getId());
            w.setSpaceId(spaceId);
            w.setWarningType("LEASE_EXPIRED");
            w.setWarningLevel("HIGH");
            w.setDescription(String.format("车位 [%s] %s 的租赁合同已于 %s 到期，但车位仍标记为已租状态，需手动更新车位状态或续签合同",
                    row.get("space_code"), row.get("space_name"), row.get("lease_end")));
            w.setStatus(0);
            w.setBatchNo(batchNo);
            warningMapper.insert(w);
            inserted++;
        }
        if (inserted > 0) {
            log.warn("对账发现 {} 条租赁到期未更新", inserted);
        }
        return inserted;
    }

    private int checkIdleSpaces(LocalDateTime now, String batchNo) {
        LocalDateTime since = now.minusDays(IDLE_DAYS);
        List<Map<String, Object>> idle = warningMapper.selectIdleSpaces(since);
        int inserted = 0;
        for (Map<String, Object> row : idle) {
            Long spaceId = toLong(row.get("space_id"));
            if (isDuplicate(spaceId, "SPACE_IDLE")) continue;
            ParkingWarningEntity w = new ParkingWarningEntity();
            w.setId(IdWorker.getId());
            w.setSpaceId(spaceId);
            w.setWarningType("SPACE_IDLE");
            w.setWarningLevel("MEDIUM");
            w.setDescription(String.format("车位 [%s] %s 状态为已使用，但近 %d 天无任何出入记录，请核实是否空置",
                    row.get("space_code"), row.get("space_name"), IDLE_DAYS));
            w.setStatus(0);
            w.setBatchNo(batchNo);
            warningMapper.insert(w);
            inserted++;
        }
        if (inserted > 0) {
            log.info("对账发现 {} 条闲置车位", inserted);
        }
        return inserted;
    }

    private int checkUnpaidUsage(String batchNo) {
        List<Map<String, Object>> unpaid = warningMapper.selectUnpaidTemporaryUsage();
        int inserted = 0;
        for (Map<String, Object> row : unpaid) {
            Long spaceId = toLong(row.get("space_id"));
            if (isDuplicate(spaceId, "PAYMENT_PENDING")) continue;
            ParkingWarningEntity w = new ParkingWarningEntity();
            w.setId(IdWorker.getId());
            w.setSpaceId(spaceId);
            w.setWarningType("PAYMENT_PENDING");
            w.setWarningLevel("MEDIUM");
            w.setDescription(String.format("车位 [%s] %s 临时停车（%s）已于 %s 结束，费用 ¥%s 未支付",
                    row.get("space_code"), row.get("space_name"),
                    row.get("plate_no"), row.get("end_time"), row.get("fee_amount")));
            w.setStatus(0);
            w.setBatchNo(batchNo);
            warningMapper.insert(w);
            inserted++;
        }
        if (inserted > 0) {
            log.info("对账发现 {} 条未支付临时停车", inserted);
        }
        return inserted;
    }

    private int checkOccupancyAnomaly(String batchNo) {
        List<Map<String, Object>> anomaly = warningMapper.selectOccupancyAnomaly();
        int inserted = 0;
        for (Map<String, Object> row : anomaly) {
            Long spaceId = toLong(row.get("space_id"));
            if (isDuplicate(spaceId, "OCCUPANCY_ANOMALY")) continue;
            ParkingWarningEntity w = new ParkingWarningEntity();
            w.setId(IdWorker.getId());
            w.setSpaceId(spaceId);
            w.setWarningType("OCCUPANCY_ANOMALY");
            w.setWarningLevel("HIGH");
            w.setDescription(String.format("车位 [%s] %s 系统状态为「空闲」，但存在进行中的使用记录（%s），请核实是否异常占用",
                    row.get("space_code"), row.get("space_name"), row.get("plate_no")));
            w.setStatus(0);
            w.setBatchNo(batchNo);
            warningMapper.insert(w);
            inserted++;
        }
        if (inserted > 0) {
            log.warn("对账发现 {} 条空闲车位占用异常", inserted);
        }
        return inserted;
    }

    private int checkLeasesExpiring(LocalDateTime now, String batchNo) {
        LocalDateTime deadline = now.plusDays(EXPIRE_ADVANCE_DAYS);
        List<Map<String, Object>> expiring = warningMapper.selectLeasesAboutToExpire(now, deadline);
        int inserted = 0;
        for (Map<String, Object> row : expiring) {
            Long spaceId = toLong(row.get("space_id"));
            if (isDuplicate(spaceId, "LEASE_EXPIRING")) continue;
            ParkingWarningEntity w = new ParkingWarningEntity();
            w.setId(IdWorker.getId());
            w.setSpaceId(spaceId);
            w.setWarningType("LEASE_EXPIRING");
            w.setWarningLevel("LOW");
            w.setDescription(String.format("车位 [%s] %s 的租赁合同（%s）将于 %s 到期，请提前处理续签",
                    row.get("space_code"), row.get("space_name"),
                    row.get("contract_no"), row.get("lease_end")));
            w.setStatus(0);
            w.setBatchNo(batchNo);
            warningMapper.insert(w);
            inserted++;
        }
        if (inserted > 0) {
            log.info("对账发现 {} 条即将到期租赁", inserted);
        }
        return inserted;
    }

    // ========== 工具方法 ==========

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.valueOf(obj.toString());
    }

    /**
     * 去重检查：同一车位同一预警类型是否已有活跃预警
     */
    private boolean isDuplicate(Long spaceId, String warningType) {
        return warningMapper.countActiveBySpaceAndType(spaceId, warningType) > 0;
    }

    private ParkingWarningVO toVO(ParkingWarningEntity entity) {
        ParkingWarningVO vo = new ParkingWarningVO();
        vo.setId(entity.getId());
        vo.setSpaceId(entity.getSpaceId());
        vo.setWarningType(entity.getWarningType());
        vo.setWarningTypeName(getWarningTypeName(entity.getWarningType()));
        vo.setWarningLevel(entity.getWarningLevel());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        vo.setHandler(entity.getHandler());
        vo.setHandleRemark(entity.getHandleRemark());
        vo.setHandleTime(entity.getHandleTime());
        vo.setBatchNo(entity.getBatchNo());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String getWarningTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "LEASE_EXPIRED" -> "租赁到期未更新";
            case "SPACE_IDLE" -> "车位闲置";
            case "PAYMENT_PENDING" -> "欠费未支付";
            case "OCCUPANCY_ANOMALY" -> "占用异常";
            case "LEASE_EXPIRING" -> "租赁即将到期";
            default -> type;
        };
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已处理";
            case 3 -> "已关闭";
            default -> "未知";
        };
    }
}
