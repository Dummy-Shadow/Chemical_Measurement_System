package com.pfep.cms.controller;

import com.pfep.cms.common.Result;
import com.pfep.cms.entity.Media;
import com.pfep.cms.mapper.MediaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "介质管理")
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaMapper mediaMapper;

    @Operation(summary = "获取介质列表")
    @GetMapping
    public Result<List<Media>> list(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return Result.success(mediaMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Media>()
                            .eq(Media::getCategoryId, categoryId)));
        }
        return Result.success(mediaMapper.selectList(null));
    }

    @Operation(summary = "新增介质")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> create(@Valid @RequestBody Media media) {
        mediaMapper.insert(media);
        return Result.success();
    }

    @Operation(summary = "修改介质")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Media media) {
        media.setMediaId(id);
        mediaMapper.updateById(media);
        return Result.success();
    }

    @Operation(summary = "删除介质")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        mediaMapper.deleteById(id);
        return Result.success();
    }
}
