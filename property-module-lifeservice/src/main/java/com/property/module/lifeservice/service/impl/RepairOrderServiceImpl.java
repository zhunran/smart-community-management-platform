package com.property.module.lifeservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.enums.RepairCategoryEnum;
import com.property.common.enums.RepairStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.common.exception.ForbiddenException;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAssignRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderAuditRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCompleteRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderCreateRequest;
import com.property.module.lifeservice.dto.repair.request.RepairOrderQuery;
import com.property.module.lifeservice.dto.repair.request.RepairOrderRateRequest;
import com.property.module.lifeservice.dto.repair.response.RepairOrderVO;
import com.property.module.lifeservice.dto.repair.response.RepairStatisticsVO;
import com.property.module.lifeservice.entity.RepairOrderEntity;
import com.property.module.lifeservice.repository.RepairOrderMapper;
import com.property.module.lifeservice.service.RepairOrderService;
import com.property.module.lifeservice.service.impl.converter.RepairConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrderEntity>
        implements RepairOrderService {

    private static final String[] URGENCY_NAMES = {"", "普通", "紧急", "特急"};

    private final RepairOrderMapper repairOrderMapper;
    private final RepairConverter repairConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderVO create(RepairOrderCreateRequest request, Long ownerId) {
        RepairOrderEntity entity = repairConverter.toEntity(request);
        entity.setOwnerId(ownerId);
        entity.setOrderNo(generateOrderNo());
        entity.setStatus(RepairStatusEnum.PENDING_AUDIT.getValue());
        entity.setTimeoutFlag(0);
        this.save(entity);
        log.info("业主提交报修工单 [orderNo={}, ownerId={}]", entity.getOrderNo(), ownerId);
        return fillName(repairConverter.toVO(entity));
    }

    @Override
    public IPage<RepairOrderVO> myPage(RepairOrderQuery query, Long ownerId) {
        LambdaQueryWrapper<RepairOrderEntity> wrapper = new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(RepairOrderEntity::getOwnerId, ownerId)
                .eq(query.getCategory() != null, RepairOrderEntity::getCategory, query.getCategory())
                .eq(query.getStatus() != null, RepairOrderEntity::getStatus, query.getStatus())
                .orderByDesc(RepairOrderEntity::getCreateTime);
        Page<RepairOrderEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<RepairOrderEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(e -> fillName(repairConverter.toVO(e)));
    }

    @Override
    public RepairOrderVO getById(Long id, Long ownerId) {
        RepairOrderEntity entity = getByIdOrThrow(id);
        checkOwner(entity, ownerId);
        return fillName(repairConverter.toVO(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long ownerId) {
        RepairOrderEntity entity = getByIdOrThrow(id);
        checkOwner(entity, ownerId);
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canCancel()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "当前状态不可取消");
        }
        entity.setStatus(RepairStatusEnum.CANCELED.getValue());
        this.updateById(entity);
        log.info("业主取消工单 [id={}, ownerId={}]", id, ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rate(Long id, RepairOrderRateRequest request, Long ownerId) {
        RepairOrderEntity entity = getByIdOrThrow(id);
        checkOwner(entity, ownerId);
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canRate()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅已完成工单可评价");
        }
        entity.setStatus(RepairStatusEnum.RATED.getValue());
        entity.setRating(request.getRating());
        entity.setRatingComment(request.getRatingComment());
        this.updateById(entity);
        log.info("业主评价工单 [id={}, ownerId={}, rating={}]", id, ownerId, request.getRating());
    }

    @Override
    public IPage<RepairOrderVO> adminPage(RepairOrderQuery query) {
        LambdaQueryWrapper<RepairOrderEntity> wrapper = new LambdaQueryWrapper<RepairOrderEntity>()
                .eq(query.getCategory() != null, RepairOrderEntity::getCategory, query.getCategory())
                .eq(query.getStatus() != null, RepairOrderEntity::getStatus, query.getStatus())
                .eq(query.getTimeoutFlag() != null, RepairOrderEntity::getTimeoutFlag, query.getTimeoutFlag());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(RepairOrderEntity::getTitle, query.getKeyword())
                    .or().like(RepairOrderEntity::getOrderNo, query.getKeyword()));
        }
        wrapper.orderByDesc(RepairOrderEntity::getCreateTime);
        Page<RepairOrderEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<RepairOrderEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(e -> fillName(repairConverter.toVO(e)));
    }

    @Override
    public RepairOrderVO adminGetDetail(Long id) {
        return fillName(repairConverter.toVO(getByIdOrThrow(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, RepairOrderAuditRequest request) {
        // SELECT ... FOR UPDATE 行级锁：双管理员并发审核时串行处理
        RepairOrderEntity entity = repairOrderMapper.selectByIdForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "工单不存在");
        }
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canAudit()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅待审核状态可审核");
        }
        if (Boolean.TRUE.equals(request.getApproved())) {
            entity.setStatus(RepairStatusEnum.PENDING_ASSIGN.getValue());
            entity.setRejectReason(null);
        } else {
            if (!StringUtils.hasText(request.getReason())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回原因不能为空");
            }
            entity.setStatus(RepairStatusEnum.REJECTED.getValue());
            entity.setRejectReason(request.getReason());
        }
        this.updateById(entity);
        log.info("管理端审核工单 [id={}, approved={}]", id, request.getApproved());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairOrderAssignRequest request) {
        RepairOrderEntity entity = repairOrderMapper.selectByIdForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "工单不存在");
        }
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canAssign()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅待派单状态可指派");
        }
        entity.setStatus(RepairStatusEnum.ASSIGNED.getValue());
        entity.setHandlerId(request.getHandlerId());
        this.updateById(entity);
        log.info("管理端派单 [id={}, handlerId={}]", id, request.getHandlerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id, Long handlerId) {
        RepairOrderEntity entity = repairOrderMapper.selectByIdForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "工单不存在");
        }
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canAccept()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅已派单状态可接单");
        }
        checkHandler(entity, handlerId);
        entity.setStatus(RepairStatusEnum.REPAIRING.getValue());
        this.updateById(entity);
        log.info("维修员接单 [id={}, handlerId={}]", id, handlerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, RepairOrderCompleteRequest request, Long handlerId) {
        RepairOrderEntity entity = repairOrderMapper.selectByIdForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "工单不存在");
        }
        RepairStatusEnum current = RepairStatusEnum.fromValue(entity.getStatus());
        if (current == null || !current.canComplete()) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "仅维修中状态可完工");
        }
        checkHandler(entity, handlerId);
        entity.setStatus(RepairStatusEnum.COMPLETED.getValue());
        entity.setHandleNote(request.getHandleNote());
        this.updateById(entity);
        log.info("维修员完工 [id={}, handlerId={}]", id, handlerId);
    }

    @Override
    public RepairStatisticsVO statistics() {
        RepairStatisticsVO vo = new RepairStatisticsVO();
        // 各状态数量
        Map<Integer, Long> statusCounts = new HashMap<>();
        long total = 0;
        for (Map<String, Object> row : repairOrderMapper.countByStatus()) {
            Integer status = ((Number) row.get("status")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            statusCounts.put(status, count);
            total += count;
        }
        vo.setStatusCounts(statusCounts);
        vo.setTotal(total);
        // 平均处理时长
        BigDecimal avg = repairOrderMapper.avgHandleHours();
        vo.setAvgHandleHours(avg != null ? avg.setScale(1, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return vo;
    }

    /**
     * 生成工单号：RP + yyyyMMdd + 4位流水（查当日最大号自增，唯一索引 uk_order_no 兜底）
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RP" + datePart;
        LambdaQueryWrapper<RepairOrderEntity> wrapper = new LambdaQueryWrapper<RepairOrderEntity>()
                .likeRight(RepairOrderEntity::getOrderNo, prefix)
                .orderByDesc(RepairOrderEntity::getOrderNo)
                .last("LIMIT 1");
        RepairOrderEntity last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getOrderNo() != null) {
            String lastSeq = last.getOrderNo().substring(last.getOrderNo().length() - 4);
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private void checkOwner(RepairOrderEntity entity, Long ownerId) {
        if (!Objects.equals(entity.getOwnerId(), ownerId)) {
            throw new ForbiddenException("无权操作他人工单");
        }
    }

    private void checkHandler(RepairOrderEntity entity, Long handlerId) {
        if (entity.getHandlerId() == null || !Objects.equals(entity.getHandlerId(), handlerId)) {
            throw new ForbiddenException("仅被指派的维修员可操作该工单");
        }
    }

    private RepairOrderEntity getByIdOrThrow(Long id) {
        RepairOrderEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "工单不存在");
        }
        return entity;
    }

    private RepairOrderVO fillName(RepairOrderVO vo) {
        vo.setStatusName(Optional.ofNullable(vo.getStatus())
                .map(RepairStatusEnum::fromValue)
                .map(RepairStatusEnum::getLabel)
                .orElse(null));
        vo.setCategoryName(Optional.ofNullable(vo.getCategory())
                .map(RepairCategoryEnum::fromValue)
                .map(RepairCategoryEnum::getLabel)
                .orElse(null));
        if (vo.getUrgency() != null && vo.getUrgency() >= 0 && vo.getUrgency() < URGENCY_NAMES.length) {
            vo.setUrgencyName(URGENCY_NAMES[vo.getUrgency()]);
        }
        return vo;
    }
}
