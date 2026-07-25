// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfep.cms.common.PageResult;
import com.pfep.cms.common.Result;
import com.pfep.cms.dto.ManualEntryDTO;
import com.pfep.cms.entity.*;
import com.pfep.cms.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "手动录入")
@RestController
@RequestMapping("/api/manual")
@RequiredArgsConstructor
public class ManualEntryController {

    private final WorkstationMapper workstationMapper;
    private final MediaMapper mediaMapper;
    private final ProductionLineMapper lineMapper;
    private final WorkstationMediaMapper wmMapper;
    private final WorkstationMediaIndicatorMapper wmiMapper;
    private final IndicatorTemplateMapper indicatorTemplateMapper;
    private final InspectionRecordMapper recordMapper;
    private final InspectionIndicatorValueMapper valueMapper;
    private final OperationLogMapper logMapper;
    private final UserMapper userMapper;
    private final RetestRecordMapper retestRecordMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "按产线获取工位列表")
    @GetMapping("/workstations")
    public Result<List<Map<String, Object>>> workstations(@RequestParam Long lineId) {
        List<Workstation> list = workstationMapper.selectList(
                new LambdaQueryWrapper<Workstation>().eq(Workstation::getLineId, lineId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Workstation ws : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("stationId", ws.getStationId());
            map.put("stationCode", ws.getStationCode());
            map.put("stationName", ws.getStationName());
            result.add(map);
        }
        return Result.success(result);
    }

    @Operation(summary = "按工位获取可用介质列表")
    @GetMapping("/media-options")
    public Result<List<Map<String, Object>>> mediaOptions(@RequestParam Long stationId) {
        List<WorkstationMedia> wmList = wmMapper.selectList(
                new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, stationId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkstationMedia wm : wmList) {
            Media m = mediaMapper.selectById(wm.getMediaId());
            if (m != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("mediaId", m.getMediaId());
                map.put("mediaCode", m.getMediaCode());
                map.put("mediaName", m.getMediaName());
                result.add(map);
            }
        }
        return Result.success(result);
    }

    @Operation(summary = "获取工位+介质对应的指标模板和范围")
    @GetMapping("/indicators")
    public Result<List<Map<String, Object>>> indicators(@RequestParam Long stationId, @RequestParam Long mediaId) {
        WorkstationMedia wm = wmMapper.selectOne(
                new LambdaQueryWrapper<WorkstationMedia>()
                        .eq(WorkstationMedia::getStationId, stationId)
                        .eq(WorkstationMedia::getMediaId, mediaId));
        if (wm == null) return Result.error("该工位未配置此介质");

        List<WorkstationMediaIndicator> wmiList = wmiMapper.selectList(
                new LambdaQueryWrapper<WorkstationMediaIndicator>().eq(WorkstationMediaIndicator::getWmId, wm.getWmId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkstationMediaIndicator wmi : wmiList) {
            IndicatorTemplate it = indicatorTemplateMapper.selectById(wmi.getIndicatorId());
            if (it == null) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("indicatorId", it.getTemplateId());
            map.put("indicatorName", it.getIndicatorName());
            map.put("indicatorUnit", it.getIndicatorUnit());
            map.put("standardMin", wmi.getStandardMin());
            map.put("standardMax", wmi.getStandardMax());
            map.put("warnMin", wmi.getWarnMin());
            map.put("warnMax", wmi.getWarnMax());
            result.add(map);
        }
        return Result.success(result);
    }

    @Operation(summary = "查询今日该工位介质最近一次异常记录")
    @GetMapping("/latest-abnormal")
    public Result<Map<String, Object>> latestAbnormal(@RequestParam Long stationId, @RequestParam Long mediaId) {
        List<InspectionRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getStationId, stationId)
                .eq(InspectionRecord::getMediaId, mediaId)
                .eq(InspectionRecord::getInspectionDate, LocalDate.now())
                .eq(InspectionRecord::getStatus, 3)
                .orderByDesc(InspectionRecord::getCreateTime)
                .last("LIMIT 1"));

        if (records.isEmpty()) return Result.error("无异常记录");

        InspectionRecord r = records.get(0);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("recordId", r.getRecordId());
        resp.put("status", r.getStatus());

        List<InspectionIndicatorValue> vals = valueMapper.selectList(
            new LambdaQueryWrapper<InspectionIndicatorValue>().eq(InspectionIndicatorValue::getRecordId, r.getRecordId()));
        List<Map<String, Object>> vList = new ArrayList<>();
        WorkstationMedia wm = wmMapper.selectOne(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, stationId).eq(WorkstationMedia::getMediaId, mediaId));
        for (InspectionIndicatorValue v : vals) {
            IndicatorTemplate it = indicatorTemplateMapper.selectById(v.getIndicatorId());
            if (it == null) continue;
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("indicatorId", it.getTemplateId());
            vm.put("indicatorName", it.getIndicatorName());
            vm.put("indicatorUnit", it.getIndicatorUnit());
            vm.put("originalValue", v.getFinalValue());
            vm.put("warnStatus", v.getWarnStatus());
            if (wm != null) {
                WorkstationMediaIndicator wmi = wmiMapper.selectOne(
                    new LambdaQueryWrapper<WorkstationMediaIndicator>()
                        .eq(WorkstationMediaIndicator::getWmId, wm.getWmId())
                        .eq(WorkstationMediaIndicator::getIndicatorId, it.getTemplateId()));
                if (wmi != null) {
                    vm.put("standardMin", wmi.getStandardMin());
                    vm.put("standardMax", wmi.getStandardMax());
                    vm.put("warnMin", wmi.getWarnMin());
                    vm.put("warnMax", wmi.getWarnMax());
                }
            }
            vList.add(vm);
        }
        resp.put("values", vList);
        return Result.success(resp);
    }

    @Operation(summary = "手动录入检测数据")
    @PostMapping("/entry")
    @Transactional
    public Result<?> entry(@Valid @RequestBody ManualEntryDTO dto, HttpServletRequest request) {
        WorkstationMedia wm = wmMapper.selectOne(
                new LambdaQueryWrapper<WorkstationMedia>()
                        .eq(WorkstationMedia::getStationId, dto.getStationId())
                        .eq(WorkstationMedia::getMediaId, dto.getMediaId()));
        if (wm == null) return Result.error("工位-介质未配置");

        // 检查是否已正常完结（锁定）
        List<InspectionRecord> previous = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getStationId, dto.getStationId())
                .eq(InspectionRecord::getMediaId, dto.getMediaId())
                .eq(InspectionRecord::getInspectionDate, dto.getInspectionDate())
                .orderByDesc(InspectionRecord::getCreateTime)
                .last("LIMIT 1"));
        if (!previous.isEmpty() && previous.get(0).getStatus() != 3) {
            return Result.error("该工位介质今日已检测正常，无需重复录入");
        }

        // 强制使用今天日期
        dto.setInspectionDate(LocalDate.now());

        InspectionRecord record = new InspectionRecord();
        record.setStationId(dto.getStationId());
        record.setMediaId(dto.getMediaId());
        record.setInspectionDate(dto.getInspectionDate());
        record.setStatus(1);
        record.setEntryType("MANUAL");
        String username = getCurrentUsername();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user != null) record.setEntryUserId(user.getUserId());
        recordMapper.insert(record);

        int warnCount = 0, overCount = 0;
        Map<String, Object> logDetail = new LinkedHashMap<>();
        List<Map<String, Object>> logValues = new ArrayList<>();

        for (ManualEntryDTO.IndicatorValue iv : dto.getValues()) {
            InspectionIndicatorValue val = new InspectionIndicatorValue();
            val.setRecordId(record.getRecordId());
            val.setIndicatorId(iv.getIndicatorId());
            val.setFinalValue(iv.getValue());
            val.setWarnStatus(0);

            if (iv.getValue() != null) {
                WorkstationMediaIndicator wmi = wmiMapper.selectOne(
                        new LambdaQueryWrapper<WorkstationMediaIndicator>()
                                .eq(WorkstationMediaIndicator::getWmId, wm.getWmId())
                                .eq(WorkstationMediaIndicator::getIndicatorId, iv.getIndicatorId()));
                if (wmi != null) {
                    BigDecimal v = iv.getValue();
                    if (wmi.getStandardMin() != null && v.compareTo(wmi.getStandardMin()) < 0)
                        val.setWarnStatus(2);
                    else if (wmi.getStandardMax() != null && v.compareTo(wmi.getStandardMax()) > 0)
                        val.setWarnStatus(2);
                    else if (wmi.getWarnMin() != null && v.compareTo(wmi.getWarnMin()) < 0)
                        val.setWarnStatus(1);
                    else if (wmi.getWarnMax() != null && v.compareTo(wmi.getWarnMax()) > 0)
                        val.setWarnStatus(1);
                }
            }
            valueMapper.insert(val);

            if (val.getWarnStatus() == 1) warnCount++;
            if (val.getWarnStatus() == 2) overCount++;

            Map<String, Object> logVal = new LinkedHashMap<>();
            logVal.put("indicatorId", iv.getIndicatorId());
            logVal.put("value", iv.getValue());
            logVal.put("warnStatus", val.getWarnStatus());
            logValues.add(logVal);
        }

        int finalStatus = 1;
        if (overCount > 0) finalStatus = 3;
        else if (warnCount > 0) finalStatus = 2;
        record.setStatus(finalStatus);
        recordMapper.updateById(record);

        logDetail.put("recordId", record.getRecordId());
        logDetail.put("stationId", dto.getStationId());
        logDetail.put("mediaId", dto.getMediaId());
        logDetail.put("values", logValues);
        logDetail.put("resultStatus", finalStatus);
        writeLog("INSERT", "INSPECTION", record.getRecordId(), logDetail, request);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("recordId", record.getRecordId());
        resp.put("status", finalStatus);
        resp.put("warnCount", warnCount);
        resp.put("overCount", overCount);
        return Result.success(resp);
    }

    @Operation(summary = "查询检测记录列表（按产线分）")
    @GetMapping("/records")
    public Result<PageResult<Map<String, Object>>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(InspectionRecord::getStatus, status);
        if (dateFrom != null) wrapper.ge(InspectionRecord::getInspectionDate, LocalDate.parse(dateFrom));
        if (dateTo != null) wrapper.le(InspectionRecord::getInspectionDate, LocalDate.parse(dateTo));
        if (lineId != null)
            wrapper.inSql(InspectionRecord::getStationId,
                "SELECT station_id FROM workstation WHERE line_id = " + lineId);
        wrapper.orderByDesc(InspectionRecord::getInspectionDate, InspectionRecord::getCreateTime);
        IPage<InspectionRecord> pageResult = recordMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", r.getRecordId());
            m.put("inspectionDate", r.getInspectionDate());
            m.put("status", r.getStatus());
            m.put("entryType", r.getEntryType());
            Workstation ws = workstationMapper.selectById(r.getStationId());
            if (ws != null) {
                m.put("stationCode", ws.getStationCode());
                ProductionLine pl = lineMapper.selectById(ws.getLineId());
                m.put("lineCode", pl != null ? pl.getLineCode() : "");
                m.put("lineName", pl != null ? pl.getLineName() : "");
            }
            Media med = mediaMapper.selectById(r.getMediaId());
            if (med != null) m.put("mediaCode", med.getMediaCode());
            return m;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setTotal(pageResult.getTotal());
        pr.setSize(pageResult.getSize());
        pr.setCurrent(pageResult.getCurrent());
        pr.setRecords(records);
        return Result.success(pr);
    }

    @Operation(summary = "今日各产线统计：需检测X/已检测Y/异常未复测Z")
    @GetMapping("/today-stats")
    public Result<List<Map<String, Object>>> todayStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<ProductionLine> lines = lineMapper.selectList(null);
        LocalDate today = LocalDate.now();
        for (ProductionLine line : lines) {
            List<Workstation> stations = workstationMapper.selectList(
                new LambdaQueryWrapper<Workstation>().eq(Workstation::getLineId, line.getLineId()));
            long needCount = 0, doneCount = 0, abCount = 0;
            for (Workstation ws : stations) {
                List<WorkstationMedia> wmList = wmMapper.selectList(
                    new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, ws.getStationId()));
                if (wmList.isEmpty()) continue;

                for (WorkstationMedia wm : wmList) {
                    needCount++;
                    // 找最近一条记录（按时间倒序）
                    List<InspectionRecord> latestList = recordMapper.selectList(
                        new LambdaQueryWrapper<InspectionRecord>()
                            .eq(InspectionRecord::getStationId, ws.getStationId())
                            .eq(InspectionRecord::getMediaId, wm.getMediaId())
                            .eq(InspectionRecord::getInspectionDate, today)
                            .orderByDesc(InspectionRecord::getCreateTime)
                            .last("LIMIT 1"));
                    if (!latestList.isEmpty()) {
                        doneCount++;
                        InspectionRecord latest = latestList.get(0);
                        if (latest.getStatus() == 3) {
                            abCount++;
                        }
                    }
                }
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("lineId", line.getLineId());
            map.put("lineCode", line.getLineCode());
            map.put("lineName", line.getLineName());
            map.put("needCount", needCount);
            map.put("doneCount", doneCount);
            map.put("abNormalUnfixed", abCount);
            result.add(map);
        }
        return Result.success(result);
    }

    @Operation(summary = "某产线下各工位介质今日统计")
    @GetMapping("/station-today-stats")
    public Result<List<Map<String, Object>>> stationTodayStats(@RequestParam Long lineId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Workstation> stations = workstationMapper.selectList(
            new LambdaQueryWrapper<Workstation>().eq(Workstation::getLineId, lineId));
        LocalDate today = LocalDate.now();
        for (Workstation ws : stations) {
            List<WorkstationMedia> wmList = wmMapper.selectList(
                new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, ws.getStationId()));
            int total = wmList.size(), done = 0, needRetest = 0;
            for (WorkstationMedia wm : wmList) {
                // 找最近一条
                List<InspectionRecord> latestList = recordMapper.selectList(
                    new LambdaQueryWrapper<InspectionRecord>()
                        .eq(InspectionRecord::getStationId, ws.getStationId())
                        .eq(InspectionRecord::getMediaId, wm.getMediaId())
                        .eq(InspectionRecord::getInspectionDate, today)
                        .orderByDesc(InspectionRecord::getCreateTime)
                        .last("LIMIT 1"));
                if (!latestList.isEmpty()) {
                    done++;
                    if (latestList.get(0).getStatus() == 3) needRetest++;
                }
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("stationId", ws.getStationId());
            map.put("stationCode", ws.getStationCode());
            map.put("total", total);
            map.put("doneCount", done);
            map.put("needRetest", needRetest);
            result.add(map);
        }
        return Result.success(result);
    }

    @Operation(summary = "查询某工位介质今日录入/复测次数及锁定状态")
    @GetMapping("/entry-history")
    public Result<Map<String, Object>> entryHistory(
            @RequestParam Long stationId, @RequestParam Long mediaId) {
        List<InspectionRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getStationId, stationId)
                .eq(InspectionRecord::getMediaId, mediaId)
                .eq(InspectionRecord::getInspectionDate, LocalDate.now())
                .orderByAsc(InspectionRecord::getCreateTime));

        int entryCount = records.size();
        String label;
        boolean locked = false;

        if (entryCount == 0) {
            label = "未录入";
        } else {
            InspectionRecord latest = records.get(records.size() - 1);
            int status = latest.getStatus();
            if (entryCount == 1) {
                if (status == 3) {
                    label = "待复测";
                } else {
                    label = "已录入";
                    locked = true;
                }
            } else {
                int retestNum = entryCount - 1;
                if (status == 3) {
                    label = "复测" + retestNum + "次，异常";
                } else {
                    label = "复测" + retestNum + "次，正常";
                    locked = true;
                }
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("entryCount", entryCount);
        resp.put("label", label);
        resp.put("locked", locked);
        return Result.success(resp);
    }

    @Operation(summary = "历史记录查询（只读）")
    @GetMapping("/history-records")
    public Result<List<Map<String, Object>>> historyRecords(
            @RequestParam Long stationId, @RequestParam Long mediaId, @RequestParam String date) {
        List<InspectionRecord> records = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getStationId, stationId)
                .eq(InspectionRecord::getMediaId, mediaId)
                .eq(InspectionRecord::getInspectionDate, LocalDate.parse(date))
                .orderByAsc(InspectionRecord::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (InspectionRecord r : records) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("recordId", r.getRecordId());
            rec.put("inspectionDate", r.getInspectionDate().toString());
            rec.put("createTime", r.getCreateTime().toString());
            rec.put("status", r.getStatus());
            rec.put("entryType", r.getEntryType());

            List<InspectionIndicatorValue> vals = valueMapper.selectList(
                new LambdaQueryWrapper<InspectionIndicatorValue>()
                    .eq(InspectionIndicatorValue::getRecordId, r.getRecordId()));
            List<Map<String, Object>> valList = new ArrayList<>();
            for (InspectionIndicatorValue v : vals) {
                IndicatorTemplate it = indicatorTemplateMapper.selectById(v.getIndicatorId());
                if (it == null) continue;
                WorkstationMediaIndicator wmi = wmiMapper.selectOne(
                    new LambdaQueryWrapper<WorkstationMediaIndicator>()
                        .eq(WorkstationMediaIndicator::getWmId,
                            wmMapper.selectOne(new LambdaQueryWrapper<WorkstationMedia>()
                                .eq(WorkstationMedia::getStationId, stationId)
                                .eq(WorkstationMedia::getMediaId, mediaId)).getWmId())
                        .eq(WorkstationMediaIndicator::getIndicatorId, v.getIndicatorId()));

                Map<String, Object> vm = new LinkedHashMap<>();
                vm.put("indicatorName", it.getIndicatorName());
                vm.put("indicatorUnit", it.getIndicatorUnit());
                vm.put("standardMin", wmi != null ? wmi.getStandardMin() : null);
                vm.put("standardMax", wmi != null ? wmi.getStandardMax() : null);
                vm.put("warnMin", wmi != null ? wmi.getWarnMin() : null);
                vm.put("warnMax", wmi != null ? wmi.getWarnMax() : null);
                vm.put("finalValue", v.getFinalValue());
                vm.put("warnStatus", v.getWarnStatus());
                valList.add(vm);
            }
            rec.put("values", valList);
            result.add(rec);
        }
        return Result.success(result);
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof Long) {
                User user = userMapper.selectById((Long) principal);
                return user != null ? user.getUsername() : "unknown";
            }
            return String.valueOf(principal);
        } catch (Exception e) {
            return "system";
        }
    }

    private void writeLog(String action, String targetType, Long targetId, Object detail, HttpServletRequest req) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            log.setUsername(getCurrentUsername());
            log.setAction(action);
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setDetail(objectMapper.writeValueAsString(detail));
            log.setCreateTime(LocalDateTime.now());
            logMapper.insert(log);
        } catch (JsonProcessingException ignored) {}
    }
}
