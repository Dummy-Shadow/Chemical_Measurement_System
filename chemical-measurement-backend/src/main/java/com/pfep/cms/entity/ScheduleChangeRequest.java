package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("schedule_change_request")
public class ScheduleChangeRequest {
    @TableId(type = IdType.AUTO)
    private Long requestId;
    private Long scheduleId;
    private Long requestedBy;
    private String requestType;
    private String reason;
    private Long proposedInspector;
    private String status;
    private Long reviewedBy;
    private String reviewComment;
    private LocalDateTime reviewTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
