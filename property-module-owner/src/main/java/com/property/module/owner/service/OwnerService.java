package com.property.module.owner.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.owner.dto.request.OwnerCreateRequest;
import com.property.module.owner.dto.request.OwnerPageQuery;
import com.property.module.owner.dto.request.OwnerProfileUpdateRequest;
import com.property.module.owner.dto.request.OwnerUpdateRequest;
import com.property.module.owner.dto.response.OwnerDetailVO;
import com.property.module.owner.dto.response.OwnerVO;

import java.util.List;
import java.util.Map;

/**
 * 业主服务接口
 */
public interface OwnerService {

    /**
     * 分页查询业主
     */
    IPage<OwnerVO> page(OwnerPageQuery query);

    /**
     * 查询业主详情
     */
    OwnerDetailVO getDetail(Long id);

    /**
     * 新增业主
     */
    Long create(OwnerCreateRequest request);

    /**
     * 修改业主
     */
    void update(OwnerUpdateRequest request);

    /**
     * 业主端修改个人信息
     */
    void updateProfile(Long ownerId, OwnerProfileUpdateRequest request);

    /**
     * 删除业主（逻辑删除）
     */
    void delete(Long id);

    /**
     * 查询业主姓名
     */
    String getOwnerNameById(Long ownerId);

    /**
     * 查询业主手机号
     */
    String getOwnerPhoneById(Long ownerId);

    /**
     * 批量查询业主信息
     */
    List<Map<String, Object>> getOwnerInfoBatch(List<Long> ownerIds);
}
