package com.ai4.livecourse.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.common.response.PageResponse;
import com.ai4.livecourse.dto.CreateLiveCourseRequest;
import com.ai4.livecourse.dto.CreateLiveCourseResponse;
import com.ai4.livecourse.dto.LiveCourseDetailResponse;
import com.ai4.livecourse.dto.LiveCourseListItemResponse;
import com.ai4.livecourse.dto.LiveCoursePageQuery;
import com.ai4.livecourse.dto.LiveCourseStudentPageQuery;
import com.ai4.livecourse.dto.StudentSubmissionFileResponse;
import com.ai4.livecourse.service.LiveCourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 直播课管理接口。 */
@RestController
@RequestMapping("/live-courses")
public class LiveCourseController {

    private final LiveCourseService liveCourseService;

    public LiveCourseController(LiveCourseService liveCourseService) {
        this.liveCourseService = liveCourseService;
    }

    /** 按房间、班级、教师、上课时间和状态分页查询直播课。 */
    @GetMapping
    public ApiResponse<PageResponse<LiveCourseListItemResponse>> list(
            @Valid @ModelAttribute LiveCoursePageQuery query
    ) {
        return ApiResponse.success(liveCourseService.list(query));
    }

    /**
     * 查询直播课详情和分页学员明细。
     */
    @GetMapping("/{courseId}")
    public ApiResponse<LiveCourseDetailResponse> detail(
            @PathVariable String courseId,
            @Valid @ModelAttribute LiveCourseStudentPageQuery query
    ) {
        return ApiResponse.success(liveCourseService.detail(courseId, query));
    }

    /** 查询学员在当前直播课对应项目下提交的全部文件。 */
    @GetMapping("/{courseId}/students/{studentId}/submission-files")
    public ApiResponse<List<StudentSubmissionFileResponse>> listStudentSubmissionFiles(
            @PathVariable String courseId,
            @PathVariable String studentId
    ) {
        return ApiResponse.success(
                liveCourseService.listStudentSubmissionFiles(courseId, studentId)
        );
    }

    /** 新增直播课。 */
    @PostMapping
    public ApiResponse<CreateLiveCourseResponse> create(
            @Valid @RequestBody CreateLiveCourseRequest request
    ) {
        return ApiResponse.success(liveCourseService.create(request));
    }

    /** 逻辑删除未开始的直播课。 */
    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> delete(@PathVariable String courseId) {
        liveCourseService.delete(courseId);
        return ApiResponse.success(null);
    }
}
