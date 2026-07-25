// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.*;
import com.pfep.cms.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "排班管理")
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleMapper scheduleMapper;
    private final ScheduleChangeRequestMapper changeRequestMapper;
    private final ProductionLineMapper lineMapper;
    private final UserMapper userMapper;
    private final InspectionRecordMapper recordMapper;
    private final OperationLogMapper logMapper;

    @Operation(summary = "查看某日全部排班（分区管理者）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        List<Schedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<Schedule>().eq(Schedule::getScheduleDate, d));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Schedule s : schedules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("scheduleId", s.getScheduleId());
            m.put("lineId", s.getLineId());
            ProductionLine pl = lineMapper.selectById(s.getLineId());
            m.put("lineCode", pl != null ? pl.getLineCode() : "");
            m.put("lineName", pl != null ? pl.getLineName() : "");
            User u = userMapper.selectById(s.getInspectorId());
            m.put("inspectorId", s.getInspectorId());
            m.put("inspectorName", u != null ? u.getRealName() : "");
            m.put("status", s.getStatus());
            result.add(m);
        }
        return Result.success(result);
    }

    @Operation(summary = "获取审核者的今日排班")
    @GetMapping("/my-today")
    public Result<List<Map<String, Object>>> myToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Schedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getInspectorId, userId)
                .eq(Schedule::getScheduleDate, LocalDate.now())
                .eq(Schedule::getStatus, "ACTIVE"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Schedule s : schedules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("scheduleId", s.getScheduleId());
            m.put("lineId", s.getLineId());
            ProductionLine pl = lineMapper.selectById(s.getLineId());
            m.put("lineCode", pl != null ? pl.getLineCode() : "");
            m.put("lineName", pl != null ? pl.getLineName() : "");
            m.put("status", s.getStatus());
            result.add(m);
        }
        return Result.success(result);
    }

    @Operation(summary = "创建/更新排班（分区管理者）")
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body) {
        Long inspectorId = ((Number) body.get("inspectorId")).longValue();
        Long lineId = ((Number) body.get("lineId")).longValue();
        LocalDate sdate = LocalDate.parse((String) body.get("date"));

        // 查找该产线+日期已有记录（含CANCELLED）
        Schedule exist = scheduleMapper.selectOne(
            new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getLineId, lineId)
                .eq(Schedule::getScheduleDate, sdate));

        if (exist != null) {
            // 已有记录，无论状态如何直接更新为ACTIVE+新审核者
            exist.setInspectorId(inspectorId);
            exist.setStatus("ACTIVE");
            scheduleMapper.updateById(exist);
        } else {
            Schedule s = new Schedule();
            s.setInspectorId(inspectorId);
            s.setLineId(lineId);
            s.setScheduleDate(sdate);
            s.setScheduledBy(1L);
            s.setStatus("ACTIVE");
            scheduleMapper.insert(s);
        }
        return Result.success();
    }

    @Operation(summary = "一键排班：将某天全部产线排给同一人")
    @PostMapping("/batch-assign")
    public Result<?> batchAssign(@RequestBody Map<String, Object> body) {
        Long inspectorId = ((Number) body.get("inspectorId")).longValue();
        LocalDate sdate = LocalDate.parse((String) body.get("date"));
        List<ProductionLine> lines = lineMapper.selectList(null);
        int count = 0;
        for (ProductionLine line : lines) {
            Schedule exist = scheduleMapper.selectOne(
                new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getLineId, line.getLineId())
                    .eq(Schedule::getScheduleDate, sdate));
            if (exist != null) {
                exist.setInspectorId(inspectorId);
                exist.setStatus("ACTIVE");
                scheduleMapper.updateById(exist);
            } else {
                Schedule s = new Schedule();
                s.setInspectorId(inspectorId);
                s.setLineId(line.getLineId());
                s.setScheduleDate(sdate);
                s.setScheduledBy(1L);
                s.setStatus("ACTIVE");
                scheduleMapper.insert(s);
            }
            count++;
        }
        return Result.success(Map.of("assignedCount", count));
    }

    @Operation(summary = "取消排班（分区管理者）")
    @PutMapping("/cancel")
    public Result<?> cancel(@RequestParam Long scheduleId) {
        Schedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) return Result.error("排班不存在");
        s.setStatus("CANCELLED");
        scheduleMapper.updateById(s);
        return Result.success();
    }

    @Operation(summary = "提交变更申请（审核者）")
    @PostMapping("/change-request")
    public Result<?> submitChangeRequest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long scheduleId = body.get("scheduleId") != null ? ((Number) body.get("scheduleId")).longValue() : null;
        String type = (String) body.get("requestType");
        String reason = (String) body.get("reason");
        Long proposedInspector = body.get("proposedInspector") != null ?
            ((Number) body.get("proposedInspector")).longValue() : null;

        ScheduleChangeRequest cr = new ScheduleChangeRequest();
        cr.setScheduleId(scheduleId);
        cr.setRequestedBy(getUserId(request));
        cr.setRequestType(type);
        cr.setReason(reason);
        cr.setProposedInspector(proposedInspector);
        cr.setStatus("PENDING");
        cr.setCreateTime(LocalDateTime.now());
        changeRequestMapper.insert(cr);
        return Result.success(Map.of("requestId", cr.getRequestId()));
    }

    @Operation(summary = "待审批变更列表（分区管理者）")
    @GetMapping("/pending-requests")
    public Result<List<Map<String, Object>>> pendingRequests() {
        List<ScheduleChangeRequest> list = changeRequestMapper.selectList(
            new LambdaQueryWrapper<ScheduleChangeRequest>()
                .eq(ScheduleChangeRequest::getStatus, "PENDING")
                .orderByDesc(ScheduleChangeRequest::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScheduleChangeRequest cr : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("requestId", cr.getRequestId());
            m.put("requestType", cr.getRequestType());
            m.put("reason", cr.getReason());
            m.put("status", cr.getStatus());
            m.put("createTime", cr.getCreateTime() != null ? cr.getCreateTime().toString() : "");
            User reqUser = userMapper.selectById(cr.getRequestedBy());
            m.put("requestedByName", reqUser != null ? reqUser.getRealName() : "");
            User propUser = cr.getProposedInspector() != null ? userMapper.selectById(cr.getProposedInspector()) : null;
            m.put("proposedInspectorName", propUser != null ? propUser.getRealName() : "");
            if (cr.getScheduleId() != null) {
                Schedule s = scheduleMapper.selectById(cr.getScheduleId());
                if (s != null) {
                    ProductionLine pl = lineMapper.selectById(s.getLineId());
                    m.put("lineCode", pl != null ? pl.getLineCode() : "");
                    m.put("lineName", pl != null ? pl.getLineName() : "");
                    m.put("scheduleId", cr.getScheduleId());
                }
            }
            result.add(m);
        }
        return Result.success(result);
    }

    @Operation(summary = "我的变更申请历史（审核者）")
    @GetMapping("/my-requests")
    public Result<List<Map<String, Object>>> myRequests(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<ScheduleChangeRequest> list = changeRequestMapper.selectList(
            new LambdaQueryWrapper<ScheduleChangeRequest>()
                .eq(ScheduleChangeRequest::getRequestedBy, userId)
                .orderByDesc(ScheduleChangeRequest::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScheduleChangeRequest cr : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("requestId", cr.getRequestId());
            m.put("requestType", cr.getRequestType());
            m.put("reason", cr.getReason());
            m.put("status", cr.getStatus());
            m.put("createTime", cr.getCreateTime() != null ? cr.getCreateTime().toString() : "");
            m.put("reviewComment", cr.getReviewComment());
            if (cr.getScheduleId() != null) {
                Schedule s = scheduleMapper.selectById(cr.getScheduleId());
                if (s != null) {
                    ProductionLine pl = lineMapper.selectById(s.getLineId());
                    m.put("lineCode", pl != null ? pl.getLineCode() : "");
                }
            }
            result.add(m);
        }
        return Result.success(result);
    }

    @Operation(summary = "审批变更申请（分区管理者）")
    @PutMapping("/approve-request")
    public Result<?> approveRequest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long requestId = ((Number) body.get("requestId")).longValue();
        String action = (String) body.get("action"); // APPROVED / REJECTED
        String comment = (String) body.get("comment");

        ScheduleChangeRequest cr = changeRequestMapper.selectById(requestId);
        if (cr == null) return Result.error("申请不存在");
        if (!"PENDING".equals(cr.getStatus())) return Result.error("该申请已处理");

        cr.setStatus(action);
        cr.setReviewedBy(getUserId(request));
        cr.setReviewComment(comment);
        cr.setReviewTime(LocalDateTime.now());

        // 通过：换人则直接更新审核者，取消则设CANCELLED
        if ("APPROVED".equals(action) && cr.getScheduleId() != null) {
            Schedule oldSchedule = scheduleMapper.selectById(cr.getScheduleId());
            if (oldSchedule != null) {
                if ("REASSIGN".equals(cr.getRequestType()) && cr.getProposedInspector() != null) {
                    oldSchedule.setInspectorId(cr.getProposedInspector());
                    oldSchedule.setScheduledBy(getUserId(request));
                } else {
                    oldSchedule.setStatus("CANCELLED");
                }
                scheduleMapper.updateById(oldSchedule);
            }
        }
        // 拒绝：旧排班保持不变，仅记录拒绝

        changeRequestMapper.updateById(cr);
        return Result.success();
    }

    @Operation(summary = "检查用户今日是否已排班")
    @GetMapping("/check-today")
    public Result<List<Long>> checkToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Schedule> list = scheduleMapper.selectList(
            new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getInspectorId, userId)
                .eq(Schedule::getScheduleDate, LocalDate.now())
                .eq(Schedule::getStatus, "ACTIVE"));
        List<Long> lineIds = list.stream().map(Schedule::getLineId).collect(Collectors.toList());
        return Result.success(lineIds);
    }

    @Operation(summary = "获取审核者列表（供排班选择）")
    @GetMapping("/inspectors")
    public Result<List<Map<String, Object>>> inspectors() {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getRole, "INSPECTOR"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("realName", u.getRealName());
            m.put("username", u.getUsername());
            result.add(m);
        }
        return Result.success(result);
    }

    private Long getUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr != null) return (Long) attr;
        // fallback: 从 SecurityContext 取（principal 现在是 userId）
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        return 1L;
    }

    private void logSchedule(String action, Long sid, String detail) {
        try {
            OperationLog log = new OperationLog();
            Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User u = userMapper.selectById(uid);
            log.setUsername(u != null ? u.getUsername() : "unknown");
            log.setAction(action);
            log.setTargetType("SCHEDULE");
            log.setTargetId(sid);
            log.setDetail(detail);
            log.setCreateTime(java.time.LocalDateTime.now());
            logMapper.insert(log);
        } catch (Exception ignored) {}
    }
}
