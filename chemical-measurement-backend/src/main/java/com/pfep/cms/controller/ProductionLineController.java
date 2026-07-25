// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.ProductionLine;
import com.pfep.cms.mapper.ProductionLineMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "产线管理")
@RestController
@RequestMapping("/api/production-line")
@RequiredArgsConstructor
public class ProductionLineController {

    private final ProductionLineMapper productionLineMapper;

    @Operation(summary = "获取全部产线")
    @GetMapping
    public Result<List<ProductionLine>> list() {
        return Result.success(productionLineMapper.selectList(null));
    }

    @Operation(summary = "根据ID获取产线")
    @GetMapping("/{id}")
    public Result<ProductionLine> getById(@PathVariable Long id) {
        return Result.success(productionLineMapper.selectById(id));
    }

    @Operation(summary = "新增产线")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@Valid @RequestBody ProductionLine line) {
        productionLineMapper.insert(line);
        return Result.success();
    }

    @Operation(summary = "修改产线")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody ProductionLine line) {
        line.setLineId(id);
        productionLineMapper.updateById(line);
        return Result.success();
    }

    @Operation(summary = "删除产线")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        productionLineMapper.deleteById(id);
        return Result.success();
    }
}
