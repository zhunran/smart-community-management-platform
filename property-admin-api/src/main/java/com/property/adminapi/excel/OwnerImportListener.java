package com.property.adminapi.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.property.adminapi.dto.response.OwnerExcelVO;
import com.property.module.owner.dto.request.OwnerCreateRequest;
import com.property.module.owner.service.OwnerService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * EasyExcel 导入监听器
 * 逐行读取 + 批量校验 + 批量入库
 */
@RequiredArgsConstructor
public class OwnerImportListener implements ReadListener<OwnerExcelVO> {

    private static final Logger log = LoggerFactory.getLogger(OwnerImportListener.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int BATCH_SIZE = 500;

    private final OwnerService ownerService;
    private final TransactionTemplate txTemplate;
    private final List<OwnerExcelVO> cachedList = new ArrayList<>();

    @Getter
    private int successCount = 0;

    @Getter
    private int failCount = 0;

    @Getter
    private final List<OwnerExcelVO> failList = new ArrayList<>();

    @Override
    public void invoke(OwnerExcelVO row, AnalysisContext context) {
        // 校验单行
        validateRow(row);

        if (row.isValid()) {
            cachedList.add(row);
        } else {
            failCount++;
            failList.add(row);
        }

        // 达到批次阈值，批量入库
        if (cachedList.size() >= BATCH_SIZE) {
            batchInsert();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余数据
        if (!cachedList.isEmpty()) {
            batchInsert();
        }
        log.info("导入完成：成功{}条，失败{}条", successCount, failCount);
    }

    /**
     * 批量入库（每行一个独立事务，成功行提交、失败行回滚）
     */
    private void batchInsert() {
        for (OwnerExcelVO row : cachedList) {
            try {
                txTemplate.executeWithoutResult(status -> {
                    OwnerCreateRequest request = buildCreateRequest(row);
                    ownerService.create(request);
                });
                successCount++;
            } catch (Exception e) {
                log.warn("导入业主失败 [phone={}]: {}", row.getPhone(), e.getMessage());
                row.setValid(false);
                row.setErrorMsg(e.getMessage());
                failCount++;
                failList.add(row);
            }
        }
        cachedList.clear();
    }

    /**
     * 校验单行数据
     */
    private void validateRow(OwnerExcelVO row) {
        StringBuilder errors = new StringBuilder();

        if (row.getOwnerName() == null || row.getOwnerName().isBlank()) {
            errors.append("业主姓名不能为空；");
        }
        if (row.getPhone() == null || !row.getPhone().matches("^1[3-9]\\d{9}$")) {
            errors.append("手机号格式不正确；");
        }
        if (row.getIdCardNo() == null || row.getIdCardNo().isBlank()) {
            errors.append("证件号码不能为空；");
        }

        // 转换枚举标签为值
        row.setIdCardType(parseLabel("证件类型", row.getIdCardTypeLabel(),
                new String[]{"身份证", "护照", "港澳台证"}, new Integer[]{1, 2, 3}));
        row.setGender(parseLabel("性别", row.getGenderLabel(),
                new String[]{"未知", "男", "女"}, new Integer[]{0, 1, 2}));
        row.setOwnerType(parseLabel("业主类型", row.getOwnerTypeLabel(),
                new String[]{"个人", "公司", "共有"}, new Integer[]{1, 2, 3}));
        row.setStatus(parseLabel("状态", row.getStatusLabel(),
                new String[]{"禁用", "正常", "冻结"}, new Integer[]{0, 1, 2}));

        if (row.getIdCardType() == null) {
            errors.append("证件类型无效（身份证/护照/港澳台证）；");
        }
        if (row.getStatus() == null) {
            errors.append("状态无效（禁用/正常/冻结）；");
        }

        if (!errors.isEmpty()) {
            row.setValid(false);
            row.setErrorMsg(errors.toString());
        }
    }

    /**
     * 根据中文标签解析为枚举值
     */
    private Integer parseLabel(String fieldName, String label, String[] labels, Integer[] values) {
        if (label == null || label.isBlank()) {
            return null;
        }
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(label.trim())) {
                return values[i];
            }
        }
        return null;
    }

    /**
     * 构建创建请求
     */
    private OwnerCreateRequest buildCreateRequest(OwnerExcelVO row) {
        OwnerCreateRequest request = new OwnerCreateRequest();
        request.setOwnerName(row.getOwnerName());
        request.setPhone(row.getPhone());
        request.setIdCardType(row.getIdCardType() != null ? row.getIdCardType() : 1);
        request.setIdCardNo(row.getIdCardNo());
        request.setGender(row.getGender());
        request.setOwnerType(row.getOwnerType() != null ? row.getOwnerType() : 1);
        request.setStatus(row.getStatus() != null ? row.getStatus() : 1);
        request.setEmail(row.getEmail());
        request.setEmergencyContact(row.getEmergencyContact());
        request.setEmergencyPhone(row.getEmergencyPhone());

        if (row.getBirthday() != null && !row.getBirthday().isBlank()) {
            try {
                request.setBirthday(LocalDate.parse(row.getBirthday(), DATE_FMT));
            } catch (Exception ignored) {
            }
        }

        return request;
    }
}
