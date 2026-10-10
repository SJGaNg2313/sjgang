package com.ai4.livecourse.dto;

import com.ai4.common.response.PageQuery;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 直播课分页查询条件。 */
public record LiveCoursePageQuery(
        String roomId,
        String className,
        String teacherName,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime,

        @Min(value = 0, message = "直播课状态不能小于0")
        @Max(value = 3, message = "直播课状态不能大于3")
        Integer status,

        @Min(value = 1, message = "页码不能小于1")
        Integer pageNum,

        @Min(value = 1, message = "每页数量不能小于1")
        @Max(value = 100, message = "每页数量不能超过100")
        Integer pageSize
) implements PageQuery {

    @AssertTrue(message = "上课时间范围不合法")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || !startTime.isAfter(endTime);
    }
}
