// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("retest_record")
public class RetestRecord {
    @TableId(type = IdType.AUTO)
    private Long retestId;
    private Long recordId;
    private LocalDate retestDate;
    private String retestValues;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
