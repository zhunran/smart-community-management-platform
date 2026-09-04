package com.property.module.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.dto.request.FeeItemCreateRequest;
import com.property.module.bill.dto.request.FeeItemPageQuery;
import com.property.module.bill.dto.request.FeeItemUpdateRequest;
import com.property.module.bill.dto.response.FeeItemVO;
import com.property.module.bill.entity.FeeItemEntity;
import com.property.module.bill.repository.FeeItemMapper;
import com.property.module.bill.service.FeeItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 费用项服务实现
 */
@Service
@RequiredArgsConstructor
public class FeeItemServiceImpl implements FeeItemService {

    private final FeeItemMapper feeItemMapper;

    @Override
    public IPage<FeeItemVO> page(FeeItemPageQuery query) {
        Page<FeeItemEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<FeeItemEntity> wrapper = new LambdaQueryWrapper<FeeItemEntity>()
                .like(query.getItemCode() != null, FeeItemEntity::getItemCode, query.getItemCode())
                .like(query.getItemName() != null, FeeItemEntity::getItemName, query.getItemName())
                .eq(query.getBillingCycle() != null, FeeItemEntity::getBillingCycle, query.getBillingCycle())
                .eq(query.getCalcType() != null, FeeItemEntity::getCalcType, query.getCalcType())
                .eq(query.getStatus() != null, FeeItemEntity::getStatus, query.getStatus())
                .orderByAsc(FeeItemEntity::getSortOrder);

        IPage<FeeItemEntity> entityPage = feeItemMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public FeeItemVO getDetail(Long id) {
        FeeItemEntity entity = feeItemMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用项不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<FeeItemVO> listAll() {
        List<FeeItemEntity> entities = feeItemMapper.selectList(
                new LambdaQueryWrapper<FeeItemEntity>()
                        .eq(FeeItemEntity::getStatus, 1)
                        .orderByAsc(FeeItemEntity::getSortOrder)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeeItemCreateRequest request) {
        Long count = feeItemMapper.selectCount(
                new LambdaQueryWrapper<FeeItemEntity>()
                        .eq(FeeItemEntity::getItemCode, request.getItemCode())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "费用项编码已存在");
        }

        FeeItemEntity entity = new FeeItemEntity();
        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setBillingCycle(request.getBillingCycle() != null ? request.getBillingCycle() : 1);
        entity.setCalcType(request.getCalcType() != null ? request.getCalcType() : 1);
        entity.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : java.math.BigDecimal.ZERO);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        feeItemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FeeItemUpdateRequest request) {
        FeeItemEntity entity = feeItemMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用项不存在");
        }

        if (!entity.getItemCode().equals(request.getItemCode())) {
            Long count = feeItemMapper.selectCount(
                    new LambdaQueryWrapper<FeeItemEntity>()
                            .eq(FeeItemEntity::getItemCode, request.getItemCode())
                            .ne(FeeItemEntity::getId, request.getId())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "费用项编码已存在");
            }
        }

        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setBillingCycle(request.getBillingCycle() != null ? request.getBillingCycle() : 1);
        entity.setCalcType(request.getCalcType() != null ? request.getCalcType() : 1);
        entity.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : java.math.BigDecimal.ZERO);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());

        feeItemMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FeeItemEntity entity = feeItemMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用项不存在");
        }
        feeItemMapper.deleteById(id);
    }

    private FeeItemVO toVO(FeeItemEntity entity) {
        FeeItemVO vo = new FeeItemVO();
        vo.setId(entity.getId());
        vo.setItemCode(entity.getItemCode());
        vo.setItemName(entity.getItemName());
        vo.setBillingCycle(entity.getBillingCycle());
        vo.setCalcType(entity.getCalcType());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
