package com.property.adminapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.common.result.ApiResult;
import com.property.framework.entity.SysConfigEntity;
import com.property.framework.repository.SysConfigMapper;
import com.property.framework.service.SysConfigService;
import com.property.framework.web.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置管理 Controller（管理端）
 */
@Tag(name = "系统配置管理", description = "系统配置项维护")
@RestController
@RequestMapping("/api/admin/sys-config")
@RequiredArgsConstructor
public class AdminSysConfigController {

    private final SysConfigMapper sysConfigMapper;
    private final SysConfigService sysConfigService;

    @Operation(summary = "分页查询系统配置")
    @GetMapping("/page")
    public ApiResult<Page<SysConfigEntity>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String configKey) {
        LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(groupName)) {
            wrapper.eq(SysConfigEntity::getGroupName, groupName);
        }
        if (StringUtils.hasText(configKey)) {
            wrapper.like(SysConfigEntity::getConfigKey, configKey);
        }
        wrapper.orderByAsc(SysConfigEntity::getGroupName, SysConfigEntity::getConfigKey);
        return ApiResult.success(sysConfigMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @OperationLog(module = "系统配置管理", action = "修改配置项")
    @Operation(summary = "修改配置项")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody SysConfigUpdateRequest request) {
        SysConfigEntity entity = new SysConfigEntity();
        entity.setId(id);
        entity.setConfigValue(request.getConfigValue());
        entity.setStatus(request.getStatus());
        sysConfigMapper.updateById(entity);
        sysConfigService.refreshCache(request.getConfigKey());
        return ApiResult.success();
    }

    @OperationLog(module = "系统配置管理", action = "刷新配置缓存")
    @Operation(summary = "刷新单个配置缓存")
    @PostMapping("/{id}/refresh-cache")
    public ApiResult<Void> refreshCache(@PathVariable Long id, @RequestParam String configKey) {
        sysConfigService.refreshCache(configKey);
        return ApiResult.success();
    }

    @Data
    public static class SysConfigUpdateRequest {
        @NotBlank(message = "配置键不能为空")
        private String configKey;
        @NotBlank(message = "配置值不能为空")
        private String configValue;
        private Integer status;
    }
}