// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pfep.cms.common.PageResult;
import com.pfep.cms.common.Result;
import com.pfep.cms.entity.*;
import com.pfep.cms.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "知识库")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseMapper kbMapper;
    private final KnowledgeSuggestionMapper suggestionMapper;
    private final ProductionLineMapper lineMapper;
    private final WorkstationMapper workstationMapper;
    private final MediaMapper mediaMapper;
    private final IndicatorTemplateMapper indicatorMapper;
    private final UserMapper userMapper;

    // ============ 知识条目 ============

    @Operation(summary = "分页查询知识条目")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long lineId) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        if (lineId != null) w.eq(KnowledgeBase::getLineId, lineId);
        w.orderByDesc(KnowledgeBase::getPriority, KnowledgeBase::getCreateTime);
        IPage<KnowledgeBase> ip = kbMapper.selectPage(new Page<>(page, size), w);

        List<Map<String, Object>> records = ip.getRecords().stream().map(kb -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kbId", kb.getKbId());
            m.put("title", kb.getTitle());
            m.put("symptom", kb.getSymptom());
            m.put("cause", kb.getCause());
            m.put("solution", kb.getSolution());
            m.put("sourceType", kb.getSourceType());
            if (kb.getLineId() != null) {
                ProductionLine pl = lineMapper.selectById(kb.getLineId());
                m.put("lineCode", pl != null ? pl.getLineCode() : "");
            }
            if (kb.getStationId() != null) {
                Workstation ws = workstationMapper.selectById(kb.getStationId());
                m.put("stationCode", ws != null ? ws.getStationCode() : "");
            }
            if (kb.getMediaId() != null) {
                Media med = mediaMapper.selectById(kb.getMediaId());
                m.put("mediaCode", med != null ? med.getMediaCode() : "");
            }
            if (kb.getCreatedBy() != null) {
                User u = userMapper.selectById(kb.getCreatedBy());
                m.put("createdByName", u != null ? u.getRealName() : "");
            }
            return m;
        }).collect(Collectors.toList());

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setTotal(ip.getTotal()); pr.setSize(ip.getSize()); pr.setCurrent(ip.getCurrent()); pr.setRecords(records);
        return Result.success(pr);
    }

    @Operation(summary = "开发者/管理者直接添加知识")
    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body) {
        if ("INSPECTOR".equals(getCurrentRole())) return Result.error("权限不足");
        KnowledgeBase kb = new KnowledgeBase();
        fillKbFromBody(kb, body);
        kb.setSourceType("DIRECT");
        kb.setCreatedBy(getUserId());
        kb.setPriority(3);
        kbMapper.insert(kb);
        return Result.success(Map.of("kbId", kb.getKbId()));
    }

    @Operation(summary = "编辑知识条目")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if ("INSPECTOR".equals(getCurrentRole())) return Result.error("权限不足");
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) return Result.error("不存在");
        fillKbFromBody(kb, body);
        kbMapper.updateById(kb);
        return Result.success();
    }

    @Operation(summary = "删除知识条目")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if ("INSPECTOR".equals(getCurrentRole())) return Result.error("权限不足");
        kbMapper.deleteById(id);
        return Result.success();
    }

    private void fillKbFromBody(KnowledgeBase kb, Map<String, Object> body) {
        if (body.containsKey("title")) kb.setTitle((String) body.get("title"));
        if (body.containsKey("symptom")) kb.setSymptom((String) body.get("symptom"));
        if (body.containsKey("cause")) kb.setCause((String) body.get("cause"));
        if (body.containsKey("solution")) kb.setSolution((String) body.get("solution"));
        if (body.containsKey("lineId")) kb.setLineId(toLong(body.get("lineId")));
        if (body.containsKey("stationId")) kb.setStationId(toLong(body.get("stationId")));
        if (body.containsKey("mediaId")) kb.setMediaId(toLong(body.get("mediaId")));
        if (body.containsKey("indicatorId")) kb.setIndicatorId(toLong(body.get("indicatorId")));
    }

    // ============ 知识建议 ============

    @Operation(summary = "审核者提交知识建议")
    @PostMapping("/suggestions")
    public Result<?> submitSuggestion(@RequestBody Map<String, Object> body) {
        KnowledgeSuggestion s = new KnowledgeSuggestion();
        s.setLineId(toLong(body.get("lineId")));
        s.setStationId(toLong(body.get("stationId")));
        s.setMediaId(toLong(body.get("mediaId")));
        s.setIndicatorId(toLong(body.get("indicatorId")));
        s.setSymptom((String) body.get("symptom"));
        s.setCause((String) body.get("cause"));
        s.setProposedSolution((String) body.get("proposedSolution"));
        s.setSuggestedBy(getUserId());
        s.setStatus("PENDING");
        suggestionMapper.insert(s);
        return Result.success(Map.of("suggestionId", s.getSuggestionId()));
    }

    @Operation(summary = "我的建议（审核者）")
    @GetMapping("/suggestions/my")
    public Result<List<Map<String, Object>>> mySuggestions() {
        List<KnowledgeSuggestion> list = suggestionMapper.selectList(
            new LambdaQueryWrapper<KnowledgeSuggestion>().eq(KnowledgeSuggestion::getSuggestedBy, getUserId())
                .orderByDesc(KnowledgeSuggestion::getCreateTime));
        return Result.success(buildSuggestionList(list));
    }

    @Operation(summary = "待审批建议（管理者）")
    @GetMapping("/suggestions/pending")
    public Result<List<Map<String, Object>>> pendingSuggestions() {
        List<KnowledgeSuggestion> list = suggestionMapper.selectList(
            new LambdaQueryWrapper<KnowledgeSuggestion>().eq(KnowledgeSuggestion::getStatus, "PENDING")
                .orderByDesc(KnowledgeSuggestion::getCreateTime));
        return Result.success(buildSuggestionList(list));
    }

    @Operation(summary = "通过建议→生成知识条目")
    @PutMapping("/suggestions/{id}/approve")
    public Result<?> approveSuggestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KnowledgeSuggestion s = suggestionMapper.selectById(id);
        if (s == null) return Result.error("不存在");

        KnowledgeBase kb = new KnowledgeBase();
        kb.setTitle((String) body.getOrDefault("title", "建议采纳"));
        kb.setSymptom(s.getSymptom());
        kb.setCause((String) body.getOrDefault("cause", s.getCause()));
        kb.setSolution((String) body.getOrDefault("solution", s.getProposedSolution()));
        kb.setLineId(s.getLineId()); kb.setStationId(s.getStationId());
        kb.setMediaId(s.getMediaId()); kb.setIndicatorId(s.getIndicatorId());
        kb.setSourceType("SUGGESTION"); kb.setSourceId(id);
        kb.setCreatedBy(getUserId()); kb.setPriority(2);
        kbMapper.insert(kb);

        s.setStatus("APPROVED"); s.setReviewedBy(getUserId());
        s.setResultKbId(kb.getKbId());
        String comment = (String) body.get("reviewComment");
        s.setReviewComment(comment != null ? comment : "已采纳");
        suggestionMapper.updateById(s);
        return Result.success(Map.of("kbId", kb.getKbId()));
    }

    @Operation(summary = "拒绝建议")
    @PutMapping("/suggestions/{id}/reject")
    public Result<?> rejectSuggestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KnowledgeSuggestion s = suggestionMapper.selectById(id);
        if (s == null) return Result.error("不存在");
        s.setStatus("REJECTED"); s.setReviewedBy(getUserId());
        s.setReviewComment((String) body.get("reviewComment"));
        suggestionMapper.updateById(s);
        return Result.success();
    }

    private List<Map<String, Object>> buildSuggestionList(List<KnowledgeSuggestion> list) {
        return list.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("suggestionId", s.getSuggestionId());
            m.put("symptom", s.getSymptom());
            m.put("cause", s.getCause());
            m.put("proposedSolution", s.getProposedSolution());
            m.put("status", s.getStatus());
            m.put("reviewComment", s.getReviewComment());
            m.put("createTime", s.getCreateTime() != null ? s.getCreateTime().toString() : "");
            if (s.getLineId() != null) { ProductionLine pl = lineMapper.selectById(s.getLineId()); m.put("lineCode", pl != null ? pl.getLineCode() : ""); }
            if (s.getStationId() != null) { Workstation ws = workstationMapper.selectById(s.getStationId()); m.put("stationCode", ws != null ? ws.getStationCode() : ""); }
            if (s.getMediaId() != null) { Media med = mediaMapper.selectById(s.getMediaId()); m.put("mediaCode", med != null ? med.getMediaCode() : ""); }
            User reqUser = userMapper.selectById(s.getSuggestedBy());
            m.put("suggestedByName", reqUser != null ? reqUser.getRealName() : "");
            return m;
        }).collect(Collectors.toList());
    }

    private String getCurrentRole() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().map(Object::toString).filter(s -> s.startsWith("ROLE_"))
                .map(s -> s.substring(5)).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private Long getUserId() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return p instanceof Long ? (Long) p : 1L;
        } catch (Exception e) { return 2L; }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
}
