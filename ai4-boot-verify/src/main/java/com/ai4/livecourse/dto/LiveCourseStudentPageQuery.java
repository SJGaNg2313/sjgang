package com.ai4.livecourse.dto;

import com.ai4.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 直播课详情中的学员分页参数。
 */
public record LiveCourseStudentPageQuery(
        @Min(value = 1, message = "页码不能小于1")
        Integer pageNum,

        @Min(value = 1, message = "每页数量不能小于1")
        @Max(value = 100, message = "每页数量不能超过100")
        Integer pageSize
) implements PageQuery {
}
