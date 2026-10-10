package com.ai4.common.response;

/**
 * 通用分页查询参数。
 */
public interface PageQuery {

    int DEFAULT_PAGE_NUM = 1;
    int DEFAULT_PAGE_SIZE = 10;

    Integer pageNum();

    Integer pageSize();

    default int normalizedPageNum() {
        return pageNum() == null ? DEFAULT_PAGE_NUM : pageNum();
    }

    default int normalizedPageSize() {
        return pageSize() == null ? DEFAULT_PAGE_SIZE : pageSize();
    }

    default int offset() {
        return (normalizedPageNum() - 1) * normalizedPageSize();
    }
}
