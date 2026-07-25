// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.InspectionRecord;
import com.pfep.cms.mapper.InspectionRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final InspectionRecordMapper recordMapper;

    @Operation(summary = "获取首页统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LocalDate today = LocalDate.now();
        List<InspectionRecord> allToday = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>().eq(InspectionRecord::getInspectionDate, today)
                .orderByAsc(InspectionRecord::getCreateTime));

        Map<String, List<InspectionRecord>> groups = new LinkedHashMap<>();
        for (InspectionRecord r : allToday) {
            String key = r.getStationId() + "_" + r.getMediaId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        long normalCount = 0, warnCount = 0, overCount = 0, retestOkCount = 0;
        for (List<InspectionRecord> list : groups.values()) {
            InspectionRecord latest = list.get(list.size() - 1);
            if (list.size() == 1) {
                if (latest.getStatus() == 3) overCount++;
                else if (latest.getStatus() == 2) warnCount++;
                else normalCount++;
            } else {
                // 有复测：看最早一次是否异常
                InspectionRecord first = list.get(0);
                boolean wasAbnormal = first.getStatus() == 3 || first.getStatus() == 2;
                if (latest.getStatus() == 1 && wasAbnormal) retestOkCount++;
                else if (latest.getStatus() == 3) overCount++;
                else if (latest.getStatus() == 2) warnCount++;
                else normalCount++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayTotal", allToday.size());
        data.put("normalCount", normalCount);
        data.put("warnCount", warnCount);
        data.put("overCount", overCount);
        data.put("retestOkCount", retestOkCount);
        return Result.success(data);
    }

    @Operation(summary = "获取近7天趋势数据")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        List<InspectionRecord> allInRange = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .ge(InspectionRecord::getInspectionDate, sevenDaysAgo)
                .le(InspectionRecord::getInspectionDate, today)
                .orderByAsc(InspectionRecord::getCreateTime));

        // 先按日期分组，再按(station,media)分组
        Map<LocalDate, Map<String, List<InspectionRecord>>> dateGroups = new LinkedHashMap<>();
        for (InspectionRecord r : allInRange) {
            dateGroups.computeIfAbsent(r.getInspectionDate(), k -> new LinkedHashMap<>())
                .computeIfAbsent(r.getStationId() + "_" + r.getMediaId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, List<InspectionRecord>> dayData = dateGroups.getOrDefault(date, new LinkedHashMap<>());
            long normal = 0, warn = 0, over = 0, retestOk = 0, total = 0;
            for (List<InspectionRecord> list : dayData.values()) {
                total++;
                InspectionRecord latest = list.get(list.size() - 1);
                if (list.size() == 1) {
                    if (latest.getStatus() == 3) over++;
                    else if (latest.getStatus() == 2) warn++;
                    else normal++;
                } else {
                    boolean wasAbnormal = list.get(0).getStatus() == 3 || list.get(0).getStatus() == 2;
                    if (latest.getStatus() == 1 && wasAbnormal) retestOk++;
                    else if (latest.getStatus() == 3) over++;
                    else if (latest.getStatus() == 2) warn++;
                    else normal++;
                }
            }
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("total", total);
            day.put("normal", normal);
            day.put("warn", warn);
            day.put("over", over);
            day.put("retestOk", retestOk);
            result.add(day);
        }
        return Result.success(result);
    }
}
