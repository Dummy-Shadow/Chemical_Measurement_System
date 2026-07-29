// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.*;
import com.pfep.cms.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "复测管理")
@RestController
@RequestMapping("/api/retest")
@RequiredArgsConstructor
public class RetestController {

    private final InspectionRecordMapper recordMapper;
    private final InspectionIndicatorValueMapper valueMapper;
    private final RetestRecordMapper retestRecordMapper;
    private final WorkstationMediaMapper wmMapper;
    private final WorkstationMediaIndicatorMapper wmiMapper;
    private final IndicatorTemplateMapper indicatorMapper;
    private final OperationLogMapper logMapper;
    private final ObjectMapper om;

    @Operation(summary = "提交复测")
    @PostMapping
    @Transactional
    public Result<?> submitRetest(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        Long originalRecordId = toLong(body.get("originalRecordId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> values = (List<Map<String, Object>>) body.get("values");

        InspectionRecord original = recordMapper.selectById(originalRecordId);
        if (original == null) return Result.error("原记录不存在");

        // 检查是否已正常完结（与 entry() 一致）
        List<InspectionRecord> previous = recordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getStationId, original.getStationId())
                .eq(InspectionRecord::getMediaId, original.getMediaId())
                .eq(InspectionRecord::getInspectionDate, LocalDate.now())
                .orderByDesc(InspectionRecord::getCreateTime)
                .last("LIMIT 1"));
        if (!previous.isEmpty() && previous.get(0).getStatus() != null && previous.get(0).getStatus() != 3) {
            return Result.error("该工位介质今日已检测正常，无需重复复测");
        }

        // 新建复测记录
        InspectionRecord retest = new InspectionRecord();
        retest.setStationId(original.getStationId());
        retest.setMediaId(original.getMediaId());
        retest.setInspectionDate(LocalDate.now());
        retest.setStatus(1);
        retest.setEntryType("RETEST");
        retest.setEntryUserId(getUserId());
        recordMapper.insert(retest);

        int warnCount = 0, overCount = 0;
        for (Map<String, Object> v : values) {
            Long indicatorId = toLong(v.get("indicatorId"));
            BigDecimal val = v.get("value") != null ? new BigDecimal(v.get("value").toString()) : null;

            InspectionIndicatorValue detail = new InspectionIndicatorValue();
            detail.setRecordId(retest.getRecordId());
            detail.setIndicatorId(indicatorId);
            detail.setFinalValue(val);
            detail.setWarnStatus(0);

            if (val != null) {
                WorkstationMedia wm = wmMapper.selectOne(
                    new LambdaQueryWrapper<WorkstationMedia>()
                        .eq(WorkstationMedia::getStationId, retest.getStationId())
                        .eq(WorkstationMedia::getMediaId, retest.getMediaId()));
                if (wm != null) {
                    WorkstationMediaIndicator wmi = wmiMapper.selectOne(
                        new LambdaQueryWrapper<WorkstationMediaIndicator>()
                            .eq(WorkstationMediaIndicator::getWmId, wm.getWmId())
                            .eq(WorkstationMediaIndicator::getIndicatorId, indicatorId));
                    if (wmi != null) {
                        if (wmi.getStandardMin() != null && val.compareTo(wmi.getStandardMin()) < 0) detail.setWarnStatus(2);
                        else if (wmi.getStandardMax() != null && val.compareTo(wmi.getStandardMax()) > 0) detail.setWarnStatus(2);
                        else if (wmi.getWarnMin() != null && val.compareTo(wmi.getWarnMin()) < 0) detail.setWarnStatus(1);
                        else if (wmi.getWarnMax() != null && val.compareTo(wmi.getWarnMax()) > 0) detail.setWarnStatus(1);
                    }
                }
            }
            valueMapper.insert(detail);
            if (detail.getWarnStatus() == 1) warnCount++;
            if (detail.getWarnStatus() == 2) overCount++;
        }

        int fs = 1;
        if (overCount > 0) fs = 3;
        else if (warnCount > 0) fs = 2;
        retest.setStatus(fs);
        recordMapper.updateById(retest);

        // 写入 retest_record 关联
        RetestRecord rr = new RetestRecord();
        rr.setRecordId(originalRecordId);
        rr.setRetestDate(LocalDate.now());
        rr.setRetestValues(om.writeValueAsString(Map.of("retestRecordId", retest.getRecordId())));
        rr.setCreatedBy(getUserId());
        retestRecordMapper.insert(rr);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("recordId", retest.getRecordId());
        resp.put("status", fs);
        resp.put("warnCount", warnCount);
        resp.put("overCount", overCount);
        return Result.success(resp);
    }

    @Operation(summary = "查记录详情（含指标+范围）")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long recordId) {
        InspectionRecord r = recordMapper.selectById(recordId);
        if (r == null) return Result.error("记录不存在");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("recordId", r.getRecordId());
        resp.put("stationId", r.getStationId());
        resp.put("mediaId", r.getMediaId());
        resp.put("status", r.getStatus());

        List<InspectionIndicatorValue> vals = valueMapper.selectList(
            new LambdaQueryWrapper<InspectionIndicatorValue>().eq(InspectionIndicatorValue::getRecordId, recordId));
        List<Map<String, Object>> vList = new ArrayList<>();
        WorkstationMedia wm = wmMapper.selectOne(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, r.getStationId()).eq(WorkstationMedia::getMediaId, r.getMediaId()));
        for (InspectionIndicatorValue v : vals) {
            IndicatorTemplate it = indicatorMapper.selectById(v.getIndicatorId());
            if (it == null) continue;
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("indicatorId", it.getTemplateId());
            vm.put("indicatorName", it.getIndicatorName());
            vm.put("indicatorUnit", it.getIndicatorUnit());
            vm.put("value", v.getFinalValue());
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

    @Operation(summary = "查询复测链")
    @GetMapping("/chain")
    public Result<List<Map<String, Object>>> chain(@RequestParam Long recordId) {
        List<InspectionRecord> all = new ArrayList<>();

        InspectionRecord first = recordMapper.selectById(recordId);
        if (first != null) all.add(first);

        List<RetestRecord> retests = retestRecordMapper.selectList(
            new LambdaQueryWrapper<RetestRecord>().eq(RetestRecord::getRecordId, recordId)
                .orderByAsc(RetestRecord::getCreateTime));
        for (RetestRecord rr : retests) {
            try {
                String json = rr.getRetestValues();
                if (json == null) continue;
                Map<String, Object> vals = om.readValue(json, Map.class);
                Long rid = toLong(vals.get("retestRecordId"));
                if (rid != null) {
                    InspectionRecord r = recordMapper.selectById(rid);
                    if (r != null) all.add(r);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 按时间排序
        all.sort(Comparator.comparing(r -> r.getCreateTime() != null ? r.getCreateTime() : java.time.LocalDateTime.MIN));

        List<Map<String, Object>> result = new ArrayList<>();
        for (InspectionRecord r : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", r.getRecordId());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            m.put("status", r.getStatus());
            m.put("entryType", r.getEntryType());

            List<InspectionIndicatorValue> vals = valueMapper.selectList(
                new LambdaQueryWrapper<InspectionIndicatorValue>().eq(InspectionIndicatorValue::getRecordId, r.getRecordId()));
            List<Map<String, Object>> vList = new ArrayList<>();
            for (InspectionIndicatorValue v : vals) {
                IndicatorTemplate it = indicatorMapper.selectById(v.getIndicatorId());
                if (it == null) continue;
                Map<String, Object> vm = new LinkedHashMap<>();
                vm.put("indicatorName", it.getIndicatorName());
                vm.put("indicatorUnit", it.getIndicatorUnit());
                vm.put("value", v.getFinalValue());
                vm.put("warnStatus", v.getWarnStatus());
                vList.add(vm);
            }
            m.put("values", vList);
            result.add(m);
        }
        return Result.success(result);
    }

    private Long getUserId() {
        try {
            return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) { return 2L; }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
}
