package com.pfep.cms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("media_category")
public class MediaCategory {
    @TableId(type = IdType.AUTO)
    private Long categoryId;
    private String categoryName;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
