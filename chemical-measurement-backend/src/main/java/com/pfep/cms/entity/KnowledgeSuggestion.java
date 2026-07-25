package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_suggestion")
public class KnowledgeSuggestion {
    @TableId(type = IdType.AUTO)
    private Long suggestionId;
    private Long lineId;
    private Long stationId;
    private Long mediaId;
    private Long indicatorId;
    private String symptom;
    private String cause;
    private String proposedSolution;
    private Long suggestedBy;
    private String status;
    private Long reviewedBy;
    private String reviewComment;
    private Long resultKbId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
