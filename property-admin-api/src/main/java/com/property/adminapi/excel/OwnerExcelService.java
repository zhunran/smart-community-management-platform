package com.property.adminapi.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.adminapi.dto.response.OwnerExcelVO;
import com.property.module.owner.entity.OwnerEntity;
import com.property.module.owner.repository.OwnerMapper;
import com.property.module.owner.service.OwnerService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 业主 Excel 导入导出服务
 */
@Service
@RequiredArgsConstructor
public class OwnerExcelService {

    private final OwnerMapper ownerMapper;
    private final OwnerService ownerService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 流式导出业主列表到 Excel（分批写入，不缓存全部数据到内存）
     */
    public void exportExcel(HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("业主列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        long total = ownerMapper.selectCount(null);
        int pageSize = 2000;

        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), OwnerExcelVO.class).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("业主列表").build();

            for (int i = 0; i * pageSize < total; i++) {
                List<OwnerEntity> entities = ownerMapper.selectList(
                        new LambdaQueryWrapper<OwnerEntity>()
                                .orderByDesc(OwnerEntity::getCreateTime)
                                .last("LIMIT " + (i * pageSize) + "," + pageSize)
                );
                List<OwnerExcelVO> voList = entities.stream().map(this::toExcelVO).toList();
                excelWriter.write(voList, writeSheet);
            }
        }
    }

    /**
     * 导入业主 Excel（每行独立事务）
     */
    public OwnerImportListener importExcel(InputStream inputStream) throws IOException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        OwnerImportListener listener = new OwnerImportListener(ownerService, txTemplate);
        EasyExcel.read(inputStream, OwnerExcelVO.class, listener)
                .sheet()
                .doRead();
        return listener;
    }

    /**
     * 导出模板（带示例数据）
     */
    public void exportTemplate(HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("业主导入模板", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        OwnerExcelVO example = new OwnerExcelVO();
        example.setOwnerName("张三");
        example.setPhone("13812345678");
        example.setIdCardTypeLabel("身份证");
        example.setIdCardNo("110101199001011234");
        example.setGenderLabel("男");
        example.setBirthday("1990-01-01");
        example.setEmail("zhangsan@example.com");
        example.setEmergencyContact("李四");
        example.setEmergencyPhone("13912345678");
        example.setOwnerTypeLabel("个人");
        example.setStatusLabel("正常");

        EasyExcel.write(response.getOutputStream(), OwnerExcelVO.class)
                .sheet("导入模板")
                .doWrite(List.of(example));
    }

    private OwnerExcelVO toExcelVO(OwnerEntity entity) {
        OwnerExcelVO vo = new OwnerExcelVO();
        vo.setOwnerName(entity.getOwnerName());
        vo.setPhone(entity.getPhone());
        vo.setIdCardTypeLabel(convertLabel(entity.getIdCardType(), new String[]{"身份证", "护照", "港澳台证"}));
        vo.setIdCardNo(entity.getIdCardNo());
        vo.setGenderLabel(convertLabel(entity.getGender(), new String[]{"未知", "男", "女"}));
        vo.setBirthday(entity.getBirthday() != null ? entity.getBirthday().toString() : "");
        vo.setEmail(entity.getEmail());
        vo.setEmergencyContact(entity.getEmergencyContact());
        vo.setEmergencyPhone(entity.getEmergencyPhone());
        vo.setOwnerTypeLabel(convertLabel(entity.getOwnerType(), new String[]{"未知", "个人", "公司", "共有"}));
        vo.setStatusLabel(convertLabel(entity.getStatus(), new String[]{"禁用", "正常", "冻结"}));
        vo.setRegisterTime(entity.getRegisterTime() != null ? entity.getRegisterTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        return vo;
    }

    private String convertLabel(Integer value, String[] labels) {
        if (value == null || value < 0 || value >= labels.length) return "";
        return labels[value];
    }
}
