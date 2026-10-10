package com.ai4.question.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.common.response.PageResponse;
import com.ai4.question.dto.CreateQuestionRequest;
import com.ai4.question.dto.GenerateBeforeGuideRequest;
import com.ai4.question.dto.GenerateBeforeGuideResponse;
import com.ai4.question.dto.GenerateQuestionGuidesRequest;
import com.ai4.question.dto.GenerateQuestionGuidesResponse;
import com.ai4.question.dto.ProjectQuestionListItemResponse;
import com.ai4.question.dto.QuestionDetailResponse;
import com.ai4.question.dto.QuestionImageUploadResponse;
import com.ai4.question.dto.QuestionPageQuery;
import com.ai4.question.dto.QuestionProjectAttributeResponse;
import com.ai4.question.dto.UpdateQuestionRequest;
import com.ai4.question.service.QuestionService;
import com.ai4.question.service.QuestionGuideGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 题目管理接口。 */
@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionGuideGenerationService questionGuideGenerationService;

    public QuestionController(
            QuestionService questionService,
            QuestionGuideGenerationService questionGuideGenerationService
    ) {
        this.questionService = questionService;
        this.questionGuideGenerationService = questionGuideGenerationService;
    }

    /** 查询所有项目名称和项目沙盘属性名称。 */
    @GetMapping("/project-attributes")
    public ApiResponse<List<QuestionProjectAttributeResponse>> projectAttributes() {
        return ApiResponse.success(questionService.projectAttributes());
    }

    /** 分页查询指定项目下的题目列表，固定每页10条。 */
    @GetMapping
    public ApiResponse<PageResponse<ProjectQuestionListItemResponse>> list(
            @Valid @ModelAttribute QuestionPageQuery query
    ) {
        return ApiResponse.success(questionService.list(query));
    }

    /** 查询题目详情，用于修改页面回填。 */
    @GetMapping("/{questionId}")
    public ApiResponse<QuestionDetailResponse> get(@PathVariable Long questionId) {
        return ApiResponse.success(questionService.get(questionId));
    }

    /** 新增题目，选项和属性影响整体保存。 */
    @PostMapping
    public ApiResponse<QuestionDetailResponse> create(
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        return ApiResponse.success(questionService.create(request));
    }

    /** 修改题目 */
    @PutMapping("/{questionId}")
    public ApiResponse<QuestionDetailResponse> update(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        return ApiResponse.success(questionService.update(questionId, request));
    }

    /** 删除题目选项及属性影响，逻辑删除题目主表 */
    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(@PathVariable Long questionId) {
        questionService.delete(questionId);
        return ApiResponse.success(null);
    }

    /** 上传题目图片 */
    @PostMapping("/image/upload")
    public ApiResponse<QuestionImageUploadResponse> uploadImage(
            @RequestParam("image") MultipartFile image
    ) {
        return ApiResponse.success(questionService.uploadImage(image));
    }

    /** 调用 Coze 工作流生成题前引导，生成结果不直接保存。 */
    @PostMapping("/before-guide/generate")
    public ApiResponse<GenerateBeforeGuideResponse> generateBeforeGuide(
            @Valid @RequestBody GenerateBeforeGuideRequest request
    ) {
        return ApiResponse.success(questionGuideGenerationService.generate(request));
    }

    /** 一次生成题前引导和每个选项的选后引导，生成结果不直接保存。 */
    @PostMapping("/guides/generate")
    public ApiResponse<GenerateQuestionGuidesResponse> generateGuides(
            @Valid @RequestBody GenerateQuestionGuidesRequest request
    ) {
        return ApiResponse.success(questionGuideGenerationService.generateGuides(request));
    }
}
