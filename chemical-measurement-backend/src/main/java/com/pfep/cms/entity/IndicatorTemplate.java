package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("indicator_template")
public class IndicatorTemplate {
    @TableId(type = IdType.AUTO)
    private Long templateId;
    private Long categoryId;
    private String indicatorName;
    private String indicatorUnit;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
