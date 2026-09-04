package com.property.adminapi.controller;

import com.property.adminapi.excel.OwnerExcelService;
import com.property.adminapi.dto.response.OwnerExcelVO;
import com.property.adminapi.excel.OwnerImportListener;
import com.property.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 管理员端：业主 Excel 导入导出
 */
@Tag(name = "业主Excel导入导出")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/admin/owner/excel")
@RequiredArgsConstructor
public class AdminOwnerExcelController {

    private final OwnerExcelService ownerExcelService;

    @Operation(summary = "导出业主列表（流式）")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        ownerExcelService.exportExcel(response);
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ownerExcelService.exportTemplate(response);
    }

    @Operation(summary = "导入业主", description = "上传Excel文件，批量导入业主（校验+入库）")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<ImportResult> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        OwnerImportListener listener = ownerExcelService.importExcel(file.getInputStream());

        ImportResult result = new ImportResult();
        result.setSuccessCount(listener.getSuccessCount());
        result.setFailCount(listener.getFailCount());
        result.setFailList(listener.getFailList());

        return ApiResult.success("导入完成", result);
    }

    @lombok.Data
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private List<OwnerExcelVO> failList;
    }
}
