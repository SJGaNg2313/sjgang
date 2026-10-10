package com.ai4.externalcrm.dto;

/**
 * 班级移除学员结果。
 */
public record RemoveCrmClassStudentsResponse(
        int removedCount,
        int notInClassCount
) {
}
