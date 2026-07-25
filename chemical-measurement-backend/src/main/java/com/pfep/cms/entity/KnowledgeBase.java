// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long kbId;
    private String title;
    private String category;
    private Long mediaId;
    private Long indicatorId;
    private String symptom;
    private String cause;
    private String solution;
    private Integer priority;
    private Integer usageCount;
    private Long lineId;
    private Long stationId;
    private String sourceType;
    private Long sourceId;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
