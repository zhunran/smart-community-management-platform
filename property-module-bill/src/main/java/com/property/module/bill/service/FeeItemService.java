package com.property.module.bill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.bill.dto.request.FeeItemCreateRequest;
import com.property.module.bill.dto.request.FeeItemPageQuery;
import com.property.module.bill.dto.request.FeeItemUpdateRequest;
import com.property.module.bill.dto.response.FeeItemVO;

import java.util.List;

/**
 * 费用项服务接口
 */
public interface FeeItemService {

    IPage<FeeItemVO> page(FeeItemPageQuery query);

    FeeItemVO getDetail(Long id);

    List<FeeItemVO> listAll();

    Long create(FeeItemCreateRequest request);

    void update(FeeItemUpdateRequest request);

    void delete(Long id);
}
