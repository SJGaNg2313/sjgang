package com.ai4.externalcrm.dto;

/**
 * 班级绑定学员结果。
 */
public record BindCrmClassStudentsResponse(
        int addedCount,
        int alreadyBoundCount
) {
}
