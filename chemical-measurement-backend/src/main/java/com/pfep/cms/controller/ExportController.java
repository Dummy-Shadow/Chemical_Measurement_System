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

@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final InspectionRecordMapper recordMapper;
    private final InspectionIndicatorValueMapper valueMapper;
    private final IndicatorTemplateMapper indicatorTemplateMapper;
    private final WorkstationMapper workstationMapper;
    private final MediaMapper mediaMapper;
    private final ProductionLineMapper lineMapper;
    private final UserMapper userMapper;

    @Operation(summary = "导出管理者抽检数据（一周内）")
    @GetMapping("/spot-check")
    public Result<List<Map<String, Object>>> exportSpotCheck(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        if ("INSPECTOR".equals(getCurrentRole())) return Result.error("权限不足");

        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : today.with(DayOfWeek.MONDAY);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : today;

        Long userId = getCurrentUserId();
        List<InspectionRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getInspectionType, "SPOT_CHECK")
                .eq(InspectionRecord::getEntryUserId, userId)
                .ge(InspectionRecord::getInspectionDate, from)
                .le(InspectionRecord::getInspectionDate, to)
                .orderByDesc(InspectionRecord::getInspectionDate, InspectionRecord::getCreateTime));

        return Result.success(buildExportRows(records));
    }

    @Operation(summary = "导出审核者日常检测数据（一周内）")
    @GetMapping("/daily-inspection")
    public Result<List<Map<String, Object>>> exportDailyInspection(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        if ("INSPECTOR".equals(getCurrentRole())) return Result.error("权限不足");

        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : today.with(DayOfWeek.MONDAY);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : today;

        List<InspectionRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getInspectionType, "DAILY")
                .ge(InspectionRecord::getInspectionDate, from)
                .le(InspectionRecord::getInspectionDate, to)
                .orderByDesc(InspectionRecord::getInspectionDate, InspectionRecord::getCreateTime));

        return Result.success(buildExportRows(records));
    }

    private List<Map<String, Object>> buildExportRows(List<InspectionRecord> records) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, String> stationCache = new HashMap<>();
        Map<Long, String> mediaCache = new HashMap<>();
        Map<Long, String> lineCache = new HashMap<>();
        Map<Long, String> userCache = new HashMap<>();

        for (InspectionRecord r : records) {
            String stationName = stationCache.computeIfAbsent(r.getStationId(), id -> {
                Workstation ws = workstationMapper.selectById(id);
                return ws != null ? ws.getStationName() : "未知";
            });
            String mediaName = mediaCache.computeIfAbsent(r.getMediaId(), id -> {
                Media m = mediaMapper.selectById(id);
                return m != null ? m.getMediaName() : "未知";
            });
            String lineName = stationCache.computeIfAbsent(r.getStationId() + 10000L, id -> {
                Workstation ws = workstationMapper.selectById(r.getStationId());
                if (ws != null && ws.getLineId() != null) {
                    return lineCache.computeIfAbsent(ws.getLineId(), lid -> {
                        ProductionLine pl = lineMapper.selectById(lid);
                        return pl != null ? pl.getLineName() : "未知";
                    });
                }
                return "未知";
            });
            String inspectorName = "";
            if (r.getEntryUserId() != null) {
                inspectorName = userCache.computeIfAbsent(r.getEntryUserId(), id -> {
                    User u = userMapper.selectById(id);
                    return u != null ? u.getRealName() : "未知";
                });
            }

            List<InspectionIndicatorValue> vals = valueMapper.selectList(
                new LambdaQueryWrapper<InspectionIndicatorValue>().eq(InspectionIndicatorValue::getRecordId, r.getRecordId()));

            for (InspectionIndicatorValue v : vals) {
                IndicatorTemplate it = indicatorTemplateMapper.selectById(v.getIndicatorId());
                if (it == null) continue;

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("recordId", r.getRecordId());
                row.put("lineName", lineName);
                row.put("stationName", stationName);
                row.put("mediaName", mediaName);
                row.put("inspectorName", inspectorName);
                row.put("date", r.getInspectionDate() != null ? r.getInspectionDate().toString() : "");
                row.put("indicatorName", it.getIndicatorName());
                row.put("indicatorUnit", it.getIndicatorUnit() != null ? it.getIndicatorUnit() : "");
                row.put("value", v.getFinalValue());
                String statusLabel = r.getStatus() == null ? "" : (r.getStatus() == 3 ? "超差" : (r.getStatus() == 2 ? "预警" : "正常"));
                row.put("status", statusLabel);
                row.put("inspectionType", "SPOT_CHECK".equals(r.getInspectionType()) ? "抽检" : "日常");
                rows.add(row);
            }
            if (vals.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("recordId", r.getRecordId());
                row.put("lineName", lineName);
                row.put("stationName", stationName);
                row.put("mediaName", mediaName);
                row.put("inspectorName", inspectorName);
                row.put("date", r.getInspectionDate() != null ? r.getInspectionDate().toString() : "");
                row.put("indicatorName", "");
                row.put("indicatorUnit", "");
                row.put("value", "");
                String statusLabel = r.getStatus() == null ? "" : (r.getStatus() == 3 ? "超差" : (r.getStatus() == 2 ? "预警" : "正常"));
                row.put("status", statusLabel);
                row.put("inspectionType", "SPOT_CHECK".equals(r.getInspectionType()) ? "抽检" : "日常");
                rows.add(row);
            }
        }
        return rows;
    }

    private String getCurrentRole() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().map(Object::toString).filter(s -> s.startsWith("ROLE_"))
                .map(s -> s.substring(5)).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private Long getCurrentUserId() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return p instanceof Long ? (Long) p : 1L;
        } catch (Exception e) { return 2L; }
    }
}
