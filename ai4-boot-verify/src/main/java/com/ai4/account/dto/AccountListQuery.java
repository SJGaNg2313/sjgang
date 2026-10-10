package com.ai4.account.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 服务账户分页查询条件。
 */
public record AccountListQuery(
        String userId,
        String nick,
        String identity,
        String phone,
        String className,

        @Min(value = 1, message = "页码不能小于 1")
        Integer pageNum,

        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100")
        Integer pageSize
) {
    public int normalizedPageNum() {
        return pageNum == null ? 1 : pageNum;
    }

    public int normalizedPageSize() {
        return pageSize == null ? 10 : pageSize;
    }
}
