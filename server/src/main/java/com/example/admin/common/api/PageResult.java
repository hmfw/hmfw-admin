package com.example.admin.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应体。不直接把 MyBatis-Plus 的 IPage 暴露给前端，避免绑定持久层实现。
 */
@Getter
@Setter
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page, Function.identity());
    }

    /** 分页实体转 VO */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        PageResult<V> r = new PageResult<>();
        r.setRecords(page.getRecords().stream().map(mapper).toList());
        r.setTotal(page.getTotal());
        r.setPageNum(page.getCurrent());
        r.setPageSize(page.getSize());
        r.setPages(page.getPages());
        return r;
    }
}
