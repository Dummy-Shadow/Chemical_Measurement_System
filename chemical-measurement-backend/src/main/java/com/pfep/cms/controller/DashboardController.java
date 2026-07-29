package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.*;
import com.pfep.cms.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final InspectionRecordMapper recordMapper;
    private final WorkstationMapper workstationMapper;
    private final MediaMapper mediaMapper;
    private final InspectionIndicatorValueMapper valueMapper;
    private final IndicatorTemplateMapper indicatorTemplateMapper;
    private final ProductionLineMapper lineMapper;
    private final WorkstationMediaMapper wmMapper;

    @Operation(summary = "获取首页统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<InspectionRecord> w = new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getInspectionDate, today);
        if ("INSPECTOR".equals(getCurrentRole())) {
            w.eq(InspectionRecord::getInspectionType, "DAILY");
        }
        List<InspectionRecord> allToday = recordMapper.selectList(w.orderByAsc(InspectionRecord::getCreateTime));

        Map<String, List<InspectionRecord>> groups = new LinkedHashMap<>();
        for (InspectionRecord r : allToday) {
            if ("SPOT_CHECK".equals(r.getInspectionType())) continue;
            String key = r.getStationId() + "_" + r.getMediaId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        long detectionCount = 0, completedCount = 0, pendingRetestCount = 0;
        long abnormalCount = 0, normalCount = 0, retestOkCount = 0, warnCount = 0, overCount = 0;

        for (List<InspectionRecord> list : groups.values()) {
            detectionCount += list.size();
            completedCount++;
            InspectionRecord first = list.get(0);
            InspectionRecord latest = list.get(list.size() - 1);

            if (latest.getStatus() != null && latest.getStatus() == 3) {
                pendingRetestCount++;
            }

            boolean firstAbnormal = first.getStatus() != null && (first.getStatus() == 2 || first.getStatus() == 3);
            int lastStatus = latest.getStatus() != null ? latest.getStatus() : 1;

            if (lastStatus == 2) { warnCount++; abnormalCount++; }
            else if (lastStatus == 3) { overCount++; abnormalCount++; }
            if (list.size() == 1 && lastStatus == 1) normalCount++;
            if (list.size() >= 2 && lastStatus == 1 && firstAbnormal) retestOkCount++;
            if (list.size() >= 2 && lastStatus == 1 && !firstAbnormal) normalCount++;
        }

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<InspectionRecord> spotChecks = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .ge(InspectionRecord::getInspectionDate, weekStart)
                .le(InspectionRecord::getInspectionDate, today)
                .eq(InspectionRecord::getInspectionType, "SPOT_CHECK"));
        Set<String> checkedCombos = new HashSet<>();
        for (InspectionRecord r : spotChecks) {
            checkedCombos.add(r.getStationId() + "_" + r.getMediaId());
        }

        List<ProductionLine> allLines = lineMapper.selectList(null);
        long completedSpotCheckLines = 0;
        for (ProductionLine line : allLines) {
            List<Workstation> lineStations = workstationMapper.selectList(
                new LambdaQueryWrapper<Workstation>().eq(Workstation::getLineId, line.getLineId()));
            if (lineStations.isEmpty()) { completedSpotCheckLines++; continue; }
            boolean allDone = true;
            for (Workstation ws : lineStations) {
                List<WorkstationMedia> wms = wmMapper.selectList(
                    new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, ws.getStationId()));
                for (WorkstationMedia wm : wms) {
                    if (!checkedCombos.contains(wm.getStationId() + "_" + wm.getMediaId())) {
                        allDone = false;
                        break;
                    }
                }
                if (!allDone) break;
            }
            if (allDone) completedSpotCheckLines++;
        }
        long pendingSpotCheckLines = allLines.size() - completedSpotCheckLines;
        boolean spotCheckAllDone = (completedSpotCheckLines == allLines.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("detectionCount", detectionCount);
        data.put("completedCount", completedCount);
        data.put("pendingRetestCount", pendingRetestCount);
        data.put("abnormalCount", abnormalCount);
        data.put("normalCount", normalCount);
        data.put("retestOkCount", retestOkCount);
        data.put("warnCount", warnCount);
        data.put("overCount", overCount);
        data.put("completedSpotCheckLines", completedSpotCheckLines);
        data.put("pendingSpotCheckLines", pendingSpotCheckLines);
        data.put("totalLines", (long) allLines.size());
        data.put("spotCheckAllDone", spotCheckAllDone);
        return Result.success(data);
    }

    @Operation(summary = "获取本周异常项目清单")
    @GetMapping("/weekly-abnormal")
    public Result<List<Map<String, Object>>> weeklyAbnormal() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<InspectionRecord> weekRecords = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .ge(InspectionRecord::getInspectionDate, weekStart)
                .le(InspectionRecord::getInspectionDate, today)
                .orderByAsc(InspectionRecord::getCreateTime));

        Map<String, List<InspectionRecord>> groups = new LinkedHashMap<>();
        for (InspectionRecord r : weekRecords) {
            String key = r.getStationId() + "_" + r.getMediaId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<InspectionRecord> list : groups.values()) {
            InspectionRecord latest = list.get(list.size() - 1);
            int lastStatus = latest.getStatus() != null ? latest.getStatus() : 1;
            if (lastStatus != 2 && lastStatus != 3) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("recordId", latest.getRecordId());
            item.put("stationId", latest.getStationId());
            item.put("mediaId", latest.getMediaId());
            item.put("status", lastStatus);
            item.put("inspectionType", "SPOT_CHECK".equals(latest.getInspectionType()) ? "抽检" : "日常");
            item.put("date", latest.getInspectionDate() != null ? latest.getInspectionDate().toString() : "");

            Workstation ws = workstationMapper.selectById(latest.getStationId());
            item.put("stationName", ws != null ? ws.getStationName() : "未知工位");
            Media m = mediaMapper.selectById(latest.getMediaId());
            item.put("mediaName", m != null ? m.getMediaName() : "未知介质");

            List<InspectionIndicatorValue> vals = valueMapper.selectList(
                new LambdaQueryWrapper<InspectionIndicatorValue>().eq(InspectionIndicatorValue::getRecordId, latest.getRecordId()));
            List<Map<String, Object>> abnormalIndicators = new ArrayList<>();
            for (InspectionIndicatorValue v : vals) {
                if (v.getWarnStatus() == null || v.getWarnStatus() == 0) continue;
                IndicatorTemplate it = indicatorTemplateMapper.selectById(v.getIndicatorId());
                Map<String, Object> ind = new LinkedHashMap<>();
                ind.put("indicatorName", it != null ? it.getIndicatorName() : "未知指标");
                ind.put("indicatorUnit", it != null ? it.getIndicatorUnit() : "");
                ind.put("value", v.getFinalValue());
                ind.put("warnStatus", v.getWarnStatus());
                ind.put("warnLabel", v.getWarnStatus() == 2 ? "超差" : "预警");
                abnormalIndicators.add(ind);
            }
            item.put("abnormalIndicators", abnormalIndicators);
            result.add(item);
        }

        result.sort((a, b) -> b.get("date").toString().compareTo(a.get("date").toString()));
        return Result.success(result);
    }

    @Operation(summary = "获取近7天趋势数据")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        LambdaQueryWrapper<InspectionRecord> w = new LambdaQueryWrapper<InspectionRecord>()
                .ge(InspectionRecord::getInspectionDate, sevenDaysAgo)
                .le(InspectionRecord::getInspectionDate, today);
        if ("INSPECTOR".equals(getCurrentRole())) {
            w.eq(InspectionRecord::getInspectionType, "DAILY");
        }
        List<InspectionRecord> allInRange = recordMapper.selectList(w.orderByAsc(InspectionRecord::getCreateTime));

        Map<LocalDate, Map<String, List<InspectionRecord>>> dateGroups = new LinkedHashMap<>();
        for (InspectionRecord r : allInRange) {
            if ("SPOT_CHECK".equals(r.getInspectionType())) continue;
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
                InspectionRecord first = list.get(0);
                InspectionRecord latest = list.get(list.size() - 1);
                boolean firstAbnormal = first.getStatus() != null && (first.getStatus() == 2 || first.getStatus() == 3);
                int lastStatus = latest.getStatus() != null ? latest.getStatus() : 1;
                if (list.size() == 1) {
                    if (lastStatus == 3) over++;
                    else if (lastStatus == 2) warn++;
                    else normal++;
                } else {
                    if (lastStatus == 1 && firstAbnormal) retestOk++;
                    else if (lastStatus == 3) over++;
                    else if (lastStatus == 2) warn++;
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

    private String getCurrentRole() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().map(Object::toString).filter(s -> s.startsWith("ROLE_"))
                .map(s -> s.substring(5)).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }
}
