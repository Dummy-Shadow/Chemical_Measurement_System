package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("production_line")
public class ProductionLine {
    @TableId(type = IdType.AUTO)
    private Long lineId;
    private String lineCode;
    private String lineName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
