package com.ai4.common.response;

import java.util.List;

/**
 * 通用分页响应数据。
 */
public record PageResponse<T>(
        List<T> records,
        long total,
        int pageNum,
        int pageSize,
        long totalPages
) {

    public PageResponse(
            List<T> records,
            long total,
            int pageNum,
            int pageSize
    ) {
        this(
                records,
                total,
                pageNum,
                pageSize,
                total == 0 ? 0 : (total + pageSize - 1) / pageSize
        );
    }
}
