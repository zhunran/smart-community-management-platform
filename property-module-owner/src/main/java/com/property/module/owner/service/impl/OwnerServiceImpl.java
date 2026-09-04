package com.property.module.owner.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.module.owner.dto.request.OwnerCreateRequest;
import com.property.module.owner.dto.request.OwnerPageQuery;
import com.property.module.owner.dto.request.OwnerProfileUpdateRequest;
import com.property.module.owner.dto.request.OwnerUpdateRequest;
import com.property.module.owner.dto.response.OwnerDetailVO;
import com.property.module.owner.dto.response.OwnerVO;
import com.property.module.owner.entity.OwnerEntity;
import com.property.module.owner.repository.OwnerMapper;
import com.property.module.owner.service.OwnerService;
import com.property.module.owner.service.impl.converter.OwnerConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 业主服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private final OwnerMapper ownerMapper;
    private final OwnerConverter ownerConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<OwnerVO> page(OwnerPageQuery query) {
        Page<OwnerEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<OwnerEntity> wrapper = new LambdaQueryWrapper<OwnerEntity>()
                .like(query.getOwnerName() != null, OwnerEntity::getOwnerName, query.getOwnerName())
                .like(query.getPhone() != null, OwnerEntity::getPhone, query.getPhone())
                .eq(query.getIdCardType() != null, OwnerEntity::getIdCardType, query.getIdCardType())
                .like(query.getIdCardNo() != null, OwnerEntity::getIdCardNo, query.getIdCardNo())
                .eq(query.getGender() != null, OwnerEntity::getGender, query.getGender())
                .ge(query.getBirthdayStart() != null, OwnerEntity::getBirthday, query.getBirthdayStart())
                .le(query.getBirthdayEnd() != null, OwnerEntity::getBirthday, query.getBirthdayEnd())
                .eq(query.getOwnerType() != null, OwnerEntity::getOwnerType, query.getOwnerType())
                .eq(query.getStatus() != null, OwnerEntity::getStatus, query.getStatus())
                .ge(query.getRegisterTimeStart() != null, OwnerEntity::getRegisterTime, query.getRegisterTimeStart())
                .le(query.getRegisterTimeEnd() != null, OwnerEntity::getRegisterTime, query.getRegisterTimeEnd())
                .orderByDesc(OwnerEntity::getCreateTime);

        // 房号筛选：通过关联表查询业主ID列表
        if (query.getRoomCode() != null && !query.getRoomCode().isBlank()) {
            List<Long> ownerIds = ownerMapper.selectOwnerIdsByRoomCode(query.getRoomCode());
            if (ownerIds.isEmpty()) {
                // 无匹配业主，返回空结果
                return new Page<>();
            }
            wrapper.in(OwnerEntity::getId, ownerIds);
        }

        IPage<OwnerEntity> entityPage = ownerMapper.selectPage(page, wrapper);
        return entityPage.convert(ownerConverter::toVO);
    }

    @Override
    public OwnerDetailVO getDetail(Long id) {
        OwnerEntity entity = ownerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }
        return ownerConverter.toDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OwnerCreateRequest request) {
        // 校验手机号唯一（包括逻辑删除的记录，避免 @TableLogic 过滤导致唯一键冲突）
        if (ownerMapper.countByPhoneIncludeDeleted(request.getPhone()) > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "手机号已被注册");
        }

        // 校验证件号码唯一（包括逻辑删除的记录）
        if (ownerMapper.countByIdCardNoIncludeDeleted(request.getIdCardNo()) > 0) {
            throw new BusinessException(ErrorCode.DATA_EXISTS, "证件号码已存在");
        }

        OwnerEntity entity = ownerConverter.toEntity(request);

        // 设置默认密码（手机号后6位）
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            String phone = request.getPhone();
            if (phone == null || phone.length() < 11) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
            }
            String defaultPwd = phone.substring(phone.length() - 6);
            entity.setPassword(passwordEncoder.encode(defaultPwd));
            log.info("业主创建，默认密码已设置为手机号后6位 [phone={}]", phone);
        } else {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // 设置默认属性
        if (entity.getOwnerType() == null) {
            entity.setOwnerType(1);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }

        ownerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(OwnerUpdateRequest request) {
        log.info("修改业主，接收到的id={}, type={}", request.getId(), request.getId().getClass().getSimpleName());
        OwnerEntity entity = ownerMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }

        // 校验手机号唯一（排除自身，包括逻辑删除的记录）
        if (!entity.getPhone().equals(request.getPhone())) {
            if (ownerMapper.countByPhoneIncludeDeleted(request.getPhone()) > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "手机号已被占用");
            }
        }

        // 校验证件号码唯一（排除自身，包括逻辑删除的记录）
        if (!Objects.equals(entity.getIdCardNo(), request.getIdCardNo())) {
            if (ownerMapper.countByIdCardNoIncludeDeleted(request.getIdCardNo()) > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "证件号码已存在");
            }
        }

        ownerConverter.updateEntity(request, entity);
        ownerMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long ownerId, OwnerProfileUpdateRequest request) {
        OwnerEntity entity = ownerMapper.selectById(ownerId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }

        // 校验手机号唯一（排除自身，包括逻辑删除的记录）
        if (!entity.getPhone().equals(request.getPhone())) {
            if (ownerMapper.countByPhoneIncludeDeleted(request.getPhone()) > 0) {
                throw new BusinessException(ErrorCode.DATA_EXISTS, "手机号已被占用");
            }
        }

        ownerConverter.updateProfileEntity(request, entity);
        ownerMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除业主，接收到的id={}", id);
        OwnerEntity entity = ownerMapper.selectById(id);
        if (entity == null) {
            log.warn("删除业主失败，selectById({}) 返回null，请检查数据库中是否存在该id且del_flag=0", id);
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "业主不存在");
        }
        ownerMapper.deleteById(id);
    }

    @Override
    public String getOwnerNameById(Long ownerId) {
        OwnerEntity owner = ownerMapper.selectById(ownerId);
        return owner != null ? owner.getOwnerName() : null;
    }

    @Override
    public String getOwnerPhoneById(Long ownerId) {
        OwnerEntity owner = ownerMapper.selectById(ownerId);
        return owner != null ? owner.getPhone() : null;
    }

    @Override
    public List<Map<String, Object>> getOwnerInfoBatch(List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) return List.of();
        List<OwnerEntity> owners = ownerMapper.selectBatchIds(ownerIds);
        return owners.stream().map(owner -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ownerId", owner.getId());
            map.put("ownerName", owner.getOwnerName());
            map.put("ownerPhone", owner.getPhone());
            return map;
        }).toList();
    }
}
