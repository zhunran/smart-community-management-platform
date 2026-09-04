package com.property.adminapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.UnitCreateRequest;
import com.property.adminapi.dto.request.UnitPageQuery;
import com.property.adminapi.dto.request.UnitUpdateRequest;
import com.property.adminapi.dto.response.UnitVO;

import java.util.List;

/**
 * 单元服务接口
 */
public interface UnitService {

    /**
     * 分页查询单元
     */
    IPage<UnitVO> page(UnitPageQuery query);

    /**
     * 查询单元详情
     */
    UnitVO getDetail(Long id);

    /**
     * 查询某楼栋下的全部单元
     */
    List<UnitVO> listByBuildingId(Long buildingId);

    /**
     * 新增单元
     */
    Long create(UnitCreateRequest request);

    /**
     * 修改单元
     */
    void update(UnitUpdateRequest request);

    /**
     * 删除单元（逻辑删除）
     */
    void delete(Long id);
}
