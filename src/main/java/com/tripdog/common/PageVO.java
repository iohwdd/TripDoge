package com.tripdog.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
    private long total;
    private int pages;
    private int pageSize;
    private int page;
    private List<T> rows;

    public static <T> PageVO<T> of(List<T> all, int page, int pageSize) {
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int total = all == null ? 0 : all.size();
        int pages = pageSize > 0 ? (int) Math.ceil(total * 1.0 / pageSize) : 0;
        int fromIndex = Math.max(0, total - page * pageSize);
        int toIndex = Math.max(0, Math.min(total, total - (page - 1) * pageSize));
        List<T> sub = all == null ? List.of() : all.subList(fromIndex, toIndex);
        return new PageVO<>(total, pages, pageSize, page, sub);
    }
}
