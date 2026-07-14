package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("workstation_media_indicator")
public class WorkstationMediaIndicator {
    @TableId(type = IdType.AUTO)
    private Long wmiId;
    private Long wmId;
    private Long indicatorId;
    private BigDecimal standardMin;
    private BigDecimal standardMax;
    private BigDecimal warnMin;
    private BigDecimal warnMax;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
