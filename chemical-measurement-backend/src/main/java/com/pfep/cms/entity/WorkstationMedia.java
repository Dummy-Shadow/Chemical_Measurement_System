package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workstation_media")
public class WorkstationMedia {
    @TableId(type = IdType.AUTO)
    private Long wmId;
    private Long stationId;
    private Long mediaId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
