package com.pfep.cms.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long size;
    private long current;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.total = page.getTotal();
        result.size = page.getSize();
        result.current = page.getCurrent();
        result.records = page.getRecords();
        return result;
    }
}
