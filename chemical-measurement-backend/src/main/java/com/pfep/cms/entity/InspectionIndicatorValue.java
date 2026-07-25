// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inspection_indicator_value")
public class InspectionIndicatorValue {
    @TableId(type = IdType.AUTO)
    private Long detailId;
    private Long recordId;
    private Long indicatorId;
    private BigDecimal ocrValue;
    private BigDecimal correctedValue;
    private BigDecimal finalValue;
    private Integer warnStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
