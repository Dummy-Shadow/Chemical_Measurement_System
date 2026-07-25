// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.pfep.cms.common.Result;
import com.pfep.cms.entity.Workstation;
import com.pfep.cms.mapper.WorkstationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "工位管理")
@RestController
@RequestMapping("/api/workstation")
@RequiredArgsConstructor
public class WorkstationController {

    private final WorkstationMapper workstationMapper;

    @Operation(summary = "获取工位列表")
    @GetMapping
    public Result<List<Workstation>> list(@RequestParam(required = false) Long lineId) {
        if (lineId != null) {
            return Result.success(workstationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Workstation>()
                            .eq(Workstation::getLineId, lineId)));
        }
        return Result.success(workstationMapper.selectList(null));
    }

    @Operation(summary = "新增工位")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@Valid @RequestBody Workstation workstation) {
        workstationMapper.insert(workstation);
        return Result.success();
    }

    @Operation(summary = "修改工位")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Workstation workstation) {
        workstation.setStationId(id);
        workstationMapper.updateById(workstation);
        return Result.success();
    }

    @Operation(summary = "删除工位")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        workstationMapper.deleteById(id);
        return Result.success();
    }
}
