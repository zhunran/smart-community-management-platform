package com.property.module.bill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.bill.dto.request.FeeStandardCreateRequest;
import com.property.module.bill.dto.request.FeeStandardUpdateRequest;
import com.property.module.bill.dto.response.FeeStandardVO;

import java.util.List;

/**
 * 费用标准服务接口
 */
public interface FeeStandardService {

    /**
     * 分页查询费用标准
     */
    IPage<FeeStandardVO> page(int current, int size, Long feeItemId, Long roomId, Integer status);

    /**
     * 查询指定费用项的所有生效标准
     */
    List<FeeStandardVO> listByFeeItemId(Long feeItemId);

    /**
     * 新增费用标准
     */
    Long create(FeeStandardCreateRequest request);

    /**
     * 修改费用标准
     */
    void update(FeeStandardUpdateRequest request);

    /**
     * 删除费用标准
     */
    void delete(Long id);
}
