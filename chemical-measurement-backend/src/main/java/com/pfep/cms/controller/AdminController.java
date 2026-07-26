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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductionLineMapper lineMapper;
    private final WorkstationMapper workstationMapper;
    private final MediaMapper mediaMapper;
    private final MediaCategoryMapper categoryMapper;
    private final WorkstationMediaMapper wmMapper;
    private final WorkstationMediaIndicatorMapper wmiMapper;
    private final KnowledgeBaseMapper kbMapper;
    private final InspectionRecordMapper recordMapper;
    private final IndicatorTemplateMapper indicatorMapper;
    private final UserMapper userMapper;
    private final ScheduleMapper scheduleMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    // ========== 产线 ==========

    @GetMapping("/production-lines")
    public Result<List<ProductionLine>> listLines() {
        return Result.success(lineMapper.selectList(null));
    }

    @PostMapping("/production-lines")
    public Result<?> createLine(@RequestBody ProductionLine line, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        lineMapper.insert(line);
        return Result.success(Map.of("lineId", line.getLineId()));
    }

    @PutMapping("/production-lines/{id}")
    public Result<?> updateLine(@PathVariable Long id, @RequestBody ProductionLine line, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        line.setLineId(id);
        lineMapper.updateById(line);
        return Result.success();
    }

    @DeleteMapping("/production-lines/{id}")
    @Transactional
    public Result<?> deleteLine(@PathVariable Long id, HttpServletRequest req) {
        if (!isDev(req)) return authErr();
        // 级联删除工位
        List<Workstation> stations = workstationMapper.selectList(
            new LambdaQueryWrapper<Workstation>().eq(Workstation::getLineId, id));
        for (Workstation ws : stations) {
            deleteWorkstationCascade(ws.getStationId());
        }
        lineMapper.deleteById(id);
        return Result.success(Map.of("deletedStations", stations.size()));
    }

    // ========== 工位 ==========

    @GetMapping("/workstations")
    public Result<List<Map<String, Object>>> listWorkstations(@RequestParam(required = false) Long lineId) {
        LambdaQueryWrapper<Workstation> w = new LambdaQueryWrapper<>();
        if (lineId != null) w.eq(Workstation::getLineId, lineId);
        List<Workstation> list = workstationMapper.selectList(w);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Workstation ws : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stationId", ws.getStationId());
            m.put("stationCode", ws.getStationCode());
            m.put("stationName", ws.getStationName());
            m.put("lineId", ws.getLineId());
            ProductionLine pl = lineMapper.selectById(ws.getLineId());
            m.put("lineCode", pl != null ? pl.getLineCode() : "");
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/workstations")
    public Result<?> createWorkstation(@RequestBody Workstation ws, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        workstationMapper.insert(ws);
        return Result.success(Map.of("stationId", ws.getStationId()));
    }

    @PutMapping("/workstations/{id}")
    public Result<?> updateWorkstation(@PathVariable Long id, @RequestBody Workstation ws, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        ws.setStationId(id);
        workstationMapper.updateById(ws);
        return Result.success();
    }

    @DeleteMapping("/workstations/{id}")
    @Transactional
    public Result<?> deleteWorkstation(@PathVariable Long id, HttpServletRequest req) {
        if (!isDev(req)) return authErr();
        int cnt = deleteWorkstationCascade(id);
        return Result.success(Map.of("deletedAssociations", cnt));
    }

    // ========== 介质 ==========

    @GetMapping("/media")
    public Result<List<Map<String, Object>>> listMedia() {
        List<Media> list = mediaMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Media m : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("mediaId", m.getMediaId());
            map.put("mediaCode", m.getMediaCode());
            map.put("mediaName", m.getMediaName());
            map.put("categoryId", m.getCategoryId());
            MediaCategory mc = categoryMapper.selectById(m.getCategoryId());
            map.put("categoryName", mc != null ? mc.getCategoryName() : "");
            result.add(map);
        }
        return Result.success(result);
    }

    @GetMapping("/media-categories")
    public Result<List<MediaCategory>> listCategories() {
        return Result.success(categoryMapper.selectList(null));
    }

    @PostMapping("/media")
    public Result<?> createMedia(@RequestBody Media media, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        mediaMapper.insert(media);
        return Result.success(Map.of("mediaId", media.getMediaId()));
    }

    @Operation(summary = "查询介质关联数据（知识库+工位关联数）")
    @GetMapping("/media/{id}/relations")
    public Result<Map<String, Object>> mediaRelations(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long kbCount = kbMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getMediaId, id));
        Long suggestionCount = kbMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeBase>() // knowledge_suggestion
                .eq(KnowledgeBase::getMediaId, id)); // won't work for suggestion table
        Long wmCount = wmMapper.selectCount(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getMediaId, id));

        result.put("kbCount", kbCount);
        result.put("wmCount", wmCount);
        return Result.success(result);
    }

    @PutMapping("/media/{id}")
    public Result<?> updateMedia(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        String action = (String) body.get("action"); // UPDATE / REPLACE / DELETE_KNOWLEDGE

        Media media = mediaMapper.selectById(id);
        if (media == null) return Result.error("介质不存在");

        if ("REPLACE".equals(action)) {
            // 替换模式：知识库中的旧media_id替换为新id
            Long newMediaId = toLong(body.get("newMediaId"));
            if (newMediaId == null) return Result.error("未指定替换介质");
            List<KnowledgeBase> kbList = kbMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getMediaId, id));
            for (KnowledgeBase kb : kbList) {
                kb.setMediaId(newMediaId);
                kbMapper.updateById(kb);
            }
            // 删除旧介质
            mediaMapper.deleteById(id);
            return Result.success(Map.of("replacedKbCount", kbList.size()));
        } else if ("DELETE_KNOWLEDGE".equals(action)) {
            // 一并删除知识库
            kbMapper.delete(new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getMediaId, id));
            mediaMapper.deleteById(id);
            return Result.success();
        } else {
            // 仅更新
            if (body.containsKey("mediaCode")) media.setMediaCode((String) body.get("mediaCode"));
            if (body.containsKey("mediaName")) media.setMediaName((String) body.get("mediaName"));
            if (body.containsKey("categoryId")) media.setCategoryId(toLong(body.get("categoryId")));
            mediaMapper.updateById(media);
            return Result.success();
        }
    }

    @DeleteMapping("/media/{id}")
    public Result<?> deleteMedia(@PathVariable Long id, HttpServletRequest req) {
        if (!isDev(req)) return authErr();
        mediaMapper.deleteById(id);
        return Result.success();
    }

    // ========== 工位介质关联 ==========

    @GetMapping("/workstation-media")
    public Result<List<Map<String, Object>>> listWm(@RequestParam Long stationId) {
        List<WorkstationMedia> list = wmMapper.selectList(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, stationId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkstationMedia wm : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("wmId", wm.getWmId());
            m.put("mediaId", wm.getMediaId());
            Media med = mediaMapper.selectById(wm.getMediaId());
            m.put("mediaCode", med != null ? med.getMediaCode() : "");
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/workstation-media")
    @Transactional
    public Result<?> createWm(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        Long stationId = toLong(body.get("stationId"));
        Long mediaId = toLong(body.get("mediaId"));
        Long exist = wmMapper.selectCount(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, stationId).eq(WorkstationMedia::getMediaId, mediaId));
        if (exist > 0) return Result.error("该工位介质关联已存在");

        WorkstationMedia wm = new WorkstationMedia();
        wm.setStationId(stationId); wm.setMediaId(mediaId);
        wmMapper.insert(wm);

        // 自动为该关联复制同类别介质的指标模板
        Media med = mediaMapper.selectById(mediaId);
        if (med != null) {
            List<IndicatorTemplate> templates = indicatorMapper.selectList(
                new LambdaQueryWrapper<IndicatorTemplate>().eq(IndicatorTemplate::getCategoryId, med.getCategoryId()));
            for (IndicatorTemplate it : templates) {
                WorkstationMediaIndicator wmi = new WorkstationMediaIndicator();
                wmi.setWmId(wm.getWmId()); wmi.setIndicatorId(it.getTemplateId());
                wmiMapper.insert(wmi);
            }
        }
        return Result.success(Map.of("wmId", wm.getWmId()));
    }

    @DeleteMapping("/workstation-media/{wmId}")
    public Result<?> deleteWm(@PathVariable Long wmId, HttpServletRequest req) {
        if (!isDev(req)) return authErr();
        wmiMapper.delete(new LambdaQueryWrapper<WorkstationMediaIndicator>().eq(WorkstationMediaIndicator::getWmId, wmId));
        wmMapper.deleteById(wmId);
        return Result.success();
    }

    @PutMapping("/workstation-media-indicator/{wmiId}")
    public Result<?> updateWmi(@PathVariable Long wmiId, @RequestBody Map<String, Object> body) {
        WorkstationMediaIndicator wmi = wmiMapper.selectById(wmiId);
        if (wmi == null) return Result.error("不存在");
        if (body.containsKey("standardMin")) wmi.setStandardMin(toBigDecimal(body.get("standardMin")));
        if (body.containsKey("standardMax")) wmi.setStandardMax(toBigDecimal(body.get("standardMax")));
        if (body.containsKey("warnMin")) wmi.setWarnMin(toBigDecimal(body.get("warnMin")));
        if (body.containsKey("warnMax")) wmi.setWarnMax(toBigDecimal(body.get("warnMax")));
        wmiMapper.updateById(wmi);
        return Result.success();
    }

    @GetMapping("/workstation-media/{wmId}/indicators")
    public Result<List<Map<String, Object>>> listWmiIndicators(@PathVariable Long wmId) {
        List<WorkstationMediaIndicator> list = wmiMapper.selectList(
            new LambdaQueryWrapper<WorkstationMediaIndicator>().eq(WorkstationMediaIndicator::getWmId, wmId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkstationMediaIndicator wmi : list) {
            IndicatorTemplate it = indicatorMapper.selectById(wmi.getIndicatorId());
            if (it == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("wmiId", wmi.getWmiId());
            m.put("indicatorId", it.getTemplateId());
            m.put("indicatorName", it.getIndicatorName());
            m.put("indicatorUnit", it.getIndicatorUnit());
            m.put("standardMin", wmi.getStandardMin());
            m.put("standardMax", wmi.getStandardMax());
            m.put("warnMin", wmi.getWarnMin());
            m.put("warnMax", wmi.getWarnMax());
            result.add(m);
        }
        return Result.success(result);
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getUserId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole());
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping("/users")
    public Result<?> createUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        if (!isManagerOrDev(req)) return authErr();
        String username = (String) body.get("username");
        // 用户名唯一
        Long exist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exist > 0) return Result.error("用户名已存在");

        String role = (String) body.get("role");
        // 非开发者只能创建审核者
        if (!isDev(req) && !"INSPECTOR".equals(role)) {
            role = "INSPECTOR";
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode((String) body.get("password")));
        u.setRealName((String) body.get("realName"));
        u.setRole(role);
        userMapper.insert(u);
        logAction("INSERT", "USER", u.getUserId(), "创建用户: " + username + " 角色: " + role);
        return Result.success(Map.of("userId", u.getUserId()));
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        User u = userMapper.selectById(id);
        if (u == null) return Result.error("不存在");

        boolean isDev = isDev(req);
        boolean isSelf = getUserId(req).equals(id);

        // 非开发者且非本人不能修改
        if (!isDev && !isSelf) return authErr();

        // 开发者可以改任何字段
        if (isDev) {
            if (body.containsKey("realName")) u.setRealName((String) body.get("realName"));
            if (body.containsKey("role")) {
                String oldRole = u.getRole();
                u.setRole((String) body.get("role"));
                if (!oldRole.equals(u.getRole())) {
                    logAction("UPDATE", "USER", id, "角色变更: " + oldRole + " -> " + u.getRole());
                }
            }
            if (body.containsKey("password") && body.get("password") != null && !((String) body.get("password")).isEmpty()) {
                u.setPassword(passwordEncoder.encode((String) body.get("password")));
            }
        } else {
            // 非开发者（管理者/审核者）只能改自己的名字和密码
            if (body.containsKey("realName")) u.setRealName((String) body.get("realName"));
            if (body.containsKey("password") && body.get("password") != null && !((String) body.get("password")).isEmpty()) {
                // 需验证旧密码
                String oldPassword = (String) body.get("oldPassword");
                if (oldPassword == null || !passwordEncoder.matches(oldPassword, u.getPassword())) {
                    return Result.error("旧密码错误");
                }
                u.setPassword(passwordEncoder.encode((String) body.get("password")));
            }
        }
        userMapper.updateById(u);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest req) {
        if (!isDev(req)) return authErr();
        User u = userMapper.selectById(id);
        if (u == null) return Result.error("不存在");

        // 取消该用户所有排班
        scheduleMapper.update(null,
            new LambdaUpdateWrapper<Schedule>().eq(Schedule::getInspectorId, id)
                .set(Schedule::getStatus, "CANCELLED"));

        // 清空检测记录中的关联
        int cnt = 0;
        cnt += recordMapper.update(null,
            new LambdaUpdateWrapper<InspectionRecord>().eq(InspectionRecord::getEntryUserId, id)
                .set(InspectionRecord::getEntryUserId, null));

        // 清空知识库创建人
        kbMapper.update(null,
            new LambdaUpdateWrapper<KnowledgeBase>().eq(KnowledgeBase::getCreatedBy, id)
                .set(KnowledgeBase::getCreatedBy, null));

        userMapper.deleteById(id);
        logAction("DELETE", "USER", id, "删除用户: " + u.getUsername() + " 排班已取消");
        return Result.success(Map.of("cancelledSchedules", true, "nullifiedRecords", cnt));
    }

    // ========== 操作日志 ==========

    @GetMapping("/logs")
    public Result<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String action,
            HttpServletRequest req) {

        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        if (!isDev(req)) {
            String selfName = getCurrentUsername();
            w.and(wrapper -> wrapper.eq(OperationLog::getUsername, selfName)
                .or().isNull(OperationLog::getUserId)
                .or().inSql(OperationLog::getUserId,
                    "SELECT user_id FROM user WHERE role = 'INSPECTOR'"));
        }
        if (dateFrom != null) w.ge(OperationLog::getCreateTime, java.time.LocalDateTime.parse(dateFrom + "T00:00:00"));
        if (dateTo != null) w.le(OperationLog::getCreateTime, java.time.LocalDateTime.parse(dateTo + "T23:59:59"));
        if (action != null) w.eq(OperationLog::getAction, action);
        w.orderByDesc(OperationLog::getCreateTime);

        com.baomidou.mybatisplus.core.metadata.IPage<OperationLog> ip =
            operationLogMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), w);

        List<Map<String, Object>> records = new ArrayList<>();
        for (OperationLog log : ip.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("logId", log.getLogId());
            m.put("username", log.getUsername());
            m.put("action", log.getAction());
            m.put("targetType", log.getTargetType());
            m.put("targetId", log.getTargetId());
            m.put("detail", log.getDetail());
            m.put("createTime", log.getCreateTime() != null ? log.getCreateTime().toString() : "");
            records.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", ip.getTotal());
        return Result.success(result);
    }

    private Long getUserId(HttpServletRequest req) {
        Object attr = req.getAttribute("userId");
        if (attr instanceof Long) return (Long) attr;
        try {
            return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new RuntimeException("无法获取当前用户ID，请重新登录", e);
        }
    }

    private java.math.BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        return new java.math.BigDecimal(v.toString());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private boolean isDev(HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        return "DEVELOPER".equals(role);
    }

    private boolean isManagerOrDev(HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        return "DEVELOPER".equals(role) || "AREA_MANAGER".equals(role);
    }

    private Result<?> authErr() { return Result.error(403, "权限不足"); }

    private void logAction(String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.setUsername(getCurrentUsername());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreateTime(java.time.LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String getCurrentUsername() {
        try {
            Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User u = userMapper.selectById(uid);
            return u != null ? u.getUsername() : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private int deleteWorkstationCascade(Long stationId) {
        int cnt = 0;
        List<WorkstationMedia> wmList = wmMapper.selectList(
            new LambdaQueryWrapper<WorkstationMedia>().eq(WorkstationMedia::getStationId, stationId));
        for (WorkstationMedia wm : wmList) {
            wmiMapper.delete(new LambdaQueryWrapper<WorkstationMediaIndicator>().eq(WorkstationMediaIndicator::getWmId, wm.getWmId()));
            wmMapper.deleteById(wm.getWmId());
            cnt++;
        }
        workstationMapper.deleteById(stationId);
        return cnt;
    }
}
