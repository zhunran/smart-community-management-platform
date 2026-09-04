package com.property.module.lifeservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.property.common.dto.PageQuery;
import com.property.common.enums.VisitorPassStatusEnum;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.common.exception.ForbiddenException;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassCreateRequest;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassQuery;
import com.property.module.lifeservice.dto.visitor.request.VisitorPassVerifyRequest;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVO;
import com.property.module.lifeservice.dto.visitor.response.VisitorPassVerifyVO;
import com.property.module.lifeservice.entity.VisitorPassEntity;
import com.property.module.lifeservice.repository.VisitorPassMapper;
import com.property.module.lifeservice.service.VisitorPassService;
import com.property.module.lifeservice.service.impl.converter.VisitorConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorPassServiceImpl extends ServiceImpl<VisitorPassMapper, VisitorPassEntity>
        implements VisitorPassService {

    private final VisitorConverter visitorConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorPassVO create(VisitorPassCreateRequest request, Long ownerId) {
        if (request.getValidUntil() != null && request.getValidFrom() != null
                && !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "失效时间必须晚于生效时间");
        }

        VisitorPassEntity entity = visitorConverter.toEntity(request);
        entity.setOwnerId(ownerId);
        entity.setPassCode(generatePassCode());
        entity.setUsedCount(0);
        entity.setStatus(VisitorPassStatusEnum.VALID.getValue());
        if (entity.getMaxUse() == null) {
            entity.setMaxUse(1);
        }
        this.save(entity);

        log.info("生成访客通行码 [passCode={}, ownerId={}]", entity.getPassCode(), ownerId);
        return fillName(visitorConverter.toVO(entity));
    }

    @Override
    public IPage<VisitorPassVO> myPage(PageQuery query, Long ownerId) {
        Page<VisitorPassEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<VisitorPassEntity> wrapper = new LambdaQueryWrapper<VisitorPassEntity>()
                .eq(VisitorPassEntity::getOwnerId, ownerId)
                .orderByDesc(VisitorPassEntity::getCreateTime);
        IPage<VisitorPassEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(e -> fillName(visitorConverter.toVO(e)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, Long ownerId) {
        VisitorPassEntity entity = getByIdOrThrow(id);
        checkOwner(entity, ownerId);
        if (VisitorPassStatusEnum.REVOKED.getValue().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_ERROR, "通行码已撤销");
        }
        entity.setStatus(VisitorPassStatusEnum.REVOKED.getValue());
        this.updateById(entity);
        log.info("撤销访客通行码 [id={}, ownerId={}]", id, ownerId);
    }

    @Override
    public IPage<VisitorPassVO> adminPage(VisitorPassQuery query) {
        LambdaQueryWrapper<VisitorPassEntity> wrapper = new LambdaQueryWrapper<VisitorPassEntity>()
                .eq(query.getStatus() != null, VisitorPassEntity::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(VisitorPassEntity::getVisitorName, query.getKeyword())
                    .or().like(VisitorPassEntity::getVisitorPhone, query.getKeyword())
                    .or().like(VisitorPassEntity::getPlateNo, query.getKeyword()));
        }
        wrapper.orderByDesc(VisitorPassEntity::getCreateTime);
        Page<VisitorPassEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<VisitorPassEntity> entityPage = this.page(page, wrapper);
        return entityPage.convert(e -> fillName(visitorConverter.toVO(e)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorPassVerifyVO verify(VisitorPassVerifyRequest request) {
        VisitorPassEntity entity = this.getOne(
                new LambdaQueryWrapper<VisitorPassEntity>()
                        .eq(VisitorPassEntity::getPassCode, request.getPassCode())
                        .orderByDesc(VisitorPassEntity::getCreateTime)
                        .last("LIMIT 1"),
                false);

        VisitorPassVerifyVO result = new VisitorPassVerifyVO();
        result.setPassCode(request.getPassCode());

        if (entity == null) {
            result.setValid(false);
            result.setMessage("通行码不存在");
            return result;
        }

        result.setVisitorName(entity.getVisitorName());
        result.setPlateNo(entity.getPlateNo());
        result.setUsedCount(entity.getUsedCount());
        result.setMaxUse(entity.getMaxUse());

        VisitorPassStatusEnum statusEnum = VisitorPassStatusEnum.fromValue(entity.getStatus());
        if (statusEnum == VisitorPassStatusEnum.REVOKED) {
            return invalid(result, "通行码已撤销");
        }
        if (statusEnum == VisitorPassStatusEnum.USED_UP) {
            return invalid(result, "通行码已用尽");
        }
        if (statusEnum == VisitorPassStatusEnum.EXPIRED) {
            return invalid(result, "通行码已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        // 到期置为已过期
        if (entity.getValidUntil() != null && now.isAfter(entity.getValidUntil())) {
            entity.setStatus(VisitorPassStatusEnum.EXPIRED.getValue());
            this.updateById(entity);
            return invalid(result, "通行码已过期");
        }
        // 未到生效时间
        if (entity.getValidFrom() != null && now.isBefore(entity.getValidFrom())) {
            return invalid(result, "通行码未到生效时间");
        }
        // 超次置为已用尽
        if (entity.getMaxUse() != null && entity.getMaxUse() > 0
                && entity.getUsedCount() != null && entity.getUsedCount() >= entity.getMaxUse()) {
            entity.setStatus(VisitorPassStatusEnum.USED_UP.getValue());
            this.updateById(entity);
            return invalid(result, "通行码已用尽");
        }

        // 核销通过：使用次数+1，达到上限置为已用尽
        int usedCount = entity.getUsedCount() == null ? 1 : entity.getUsedCount() + 1;
        entity.setUsedCount(usedCount);
        if (entity.getMaxUse() != null && entity.getMaxUse() > 0 && usedCount >= entity.getMaxUse()) {
            entity.setStatus(VisitorPassStatusEnum.USED_UP.getValue());
        }
        this.updateById(entity);

        result.setValid(true);
        result.setMessage("核销成功");
        result.setUsedCount(usedCount);
        return result;
    }

    private String generatePassCode() {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 3; i++) {
            int code = 100000 + random.nextInt(900000);
            String passCode = String.valueOf(code);
            long exists = this.count(new LambdaQueryWrapper<VisitorPassEntity>()
                    .eq(VisitorPassEntity::getPassCode, passCode));
            if (exists == 0) {
                return passCode;
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_FAILED, "通行码生成失败，请重试");
    }

    private VisitorPassVerifyVO invalid(VisitorPassVerifyVO result, String message) {
        result.setValid(false);
        result.setMessage(message);
        return result;
    }

    private VisitorPassVO fillName(VisitorPassVO vo) {
        vo.setStatusName(Optional.ofNullable(vo.getStatus())
                .map(VisitorPassStatusEnum::fromValue)
                .map(VisitorPassStatusEnum::getLabel)
                .orElse(null));
        return vo;
    }

    private void checkOwner(VisitorPassEntity entity, Long ownerId) {
        if (!Objects.equals(entity.getOwnerId(), ownerId)) {
            throw new ForbiddenException("无权操作他人通行码");
        }
    }

    private VisitorPassEntity getByIdOrThrow(Long id) {
        VisitorPassEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "通行码不存在");
        }
        return entity;
    }
}
