package com.property.adminapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.adminapi.dto.request.BuildingCreateRequest;
import com.property.adminapi.dto.request.BuildingPageQuery;
import com.property.adminapi.dto.request.BuildingUpdateRequest;
import com.property.adminapi.dto.response.BuildingVO;

import java.util.List;

/**
 * 楼栋服务接口
 */
public interface BuildingService {

    /**
     * 分页查询楼栋
     */
    IPage<BuildingVO> page(BuildingPageQuery query);

    /**
     * 查询楼栋详情
     */
    BuildingVO getDetail(Long id);

    /**
     * 查询全部启用的楼栋
     */
    List<BuildingVO> listAll();

    /**
     * 新增楼栋
     */
    Long create(BuildingCreateRequest request);

    /**
     * 修改楼栋
     * @return 更新后的楼栋信息
     */
    BuildingVO update(BuildingUpdateRequest request);

    /**
     * 删除楼栋（逻辑删除）
     */
    void delete(Long id);
}
