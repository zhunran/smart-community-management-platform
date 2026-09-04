package com.property.module.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.bill.dto.request.FeeStandardCreateRequest;
import com.property.module.bill.dto.request.FeeStandardUpdateRequest;
import com.property.module.bill.dto.response.FeeStandardVO;
import com.property.module.bill.entity.FeeItemEntity;
import com.property.module.bill.entity.FeeStandardEntity;
import com.property.module.bill.repository.FeeItemMapper;
import com.property.module.bill.repository.FeeStandardMapper;
import com.property.module.bill.service.FeeStandardService;
import com.property.module.housing.service.RoomDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 费用标准服务实现
 *
 * 费用标准支持按房屋、费用项、时间范围进行精细化管理。
 * 季节性调价可通过设置 startDate/endDate 实现：
 * - 夏季（6-8月）空调附加费：startDate=2026-06-01, endDate=2026-08-31
 * - 冬季取暖费：startDate=2026-11-01, endDate=2027-03-31
 * 账单生成时自动按日期范围匹配生效的标准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeStandardServiceImpl implements FeeStandardService {

    private final FeeStandardMapper feeStandardMapper;
    private final FeeItemMapper feeItemMapper;
    private final RoomDataService roomDataService;

    @Override
    public IPage<FeeStandardVO> page(int current, int size, Long feeItemId, Long roomId, Integer status) {
        Page<FeeStandardEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<FeeStandardEntity> wrapper = new LambdaQueryWrapper<FeeStandardEntity>()
                .eq(feeItemId != null, FeeStandardEntity::getFeeItemId, feeItemId)
                .eq(roomId != null, FeeStandardEntity::getRoomId, roomId)
                .eq(status != null, FeeStandardEntity::getStatus, status)
                .orderByDesc(FeeStandardEntity::getCreateTime);

        IPage<FeeStandardEntity> entityPage = feeStandardMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public List<FeeStandardVO> listByFeeItemId(Long feeItemId) {
        List<FeeStandardEntity> entities = feeStandardMapper.selectList(
                new LambdaQueryWrapper<FeeStandardEntity>()
                        .eq(FeeStandardEntity::getFeeItemId, feeItemId)
                        .eq(FeeStandardEntity::getStatus, 1)
                        .orderByDesc(FeeStandardEntity::getStartDate)
        );
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeeStandardCreateRequest request) {
        // 校验费用项存在
        FeeItemEntity feeItem = feeItemMapper.selectById(request.getFeeItemId());
        if (feeItem == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用项不存在");
        }

        FeeStandardEntity entity = new FeeStandardEntity();
        entity.setRoomId(request.getRoomId());
        entity.setFeeItemId(request.getFeeItemId());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(1);  // 默认启用
        entity.setRemark(request.getRemark());

        feeStandardMapper.insert(entity);
        log.info("新增费用标准 [feeItemId={}, unitPrice={}, startDate={}, endDate={}]",
                request.getFeeItemId(), request.getUnitPrice(), request.getStartDate(), request.getEndDate());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FeeStandardUpdateRequest request) {
        FeeStandardEntity entity = feeStandardMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用标准不存在");
        }

        if (request.getUnitPrice() != null) entity.setUnitPrice(request.getUnitPrice());
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());

        feeStandardMapper.updateById(entity);
        log.info("更新费用标准 [id={}]", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FeeStandardEntity entity = feeStandardMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "费用标准不存在");
        }
        feeStandardMapper.deleteById(id);
        log.info("删除费用标准 [id={}]", id);
    }

    private FeeStandardVO toVO(FeeStandardEntity entity) {
        FeeStandardVO vo = new FeeStandardVO();
        vo.setId(entity.getId());
        vo.setRoomId(entity.getRoomId());
        vo.setFeeItemId(entity.getFeeItemId());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        // 查询费用项名称
        if (entity.getFeeItemId() != null) {
            FeeItemEntity feeItem = feeItemMapper.selectById(entity.getFeeItemId());
            if (feeItem != null) {
                vo.setFeeItemName(feeItem.getItemName());
            }
        }

        // 查询房屋编号
        if (entity.getRoomId() != null) {
            vo.setRoomCode(roomDataService.getRoomCodeByRoomId(entity.getRoomId()));
        }

        return vo;
    }
}
