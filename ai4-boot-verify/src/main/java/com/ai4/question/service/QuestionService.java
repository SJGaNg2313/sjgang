package com.ai4.question.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.response.PageResponse;
import com.ai4.file.dto.UploadFileResponse;
import com.ai4.file.service.OssStorageService;
import com.ai4.question.dto.CreateQuestionRequest;
import com.ai4.question.dto.ProjectQuestionListItemResponse;
import com.ai4.question.dto.QuestionDetailResponse;
import com.ai4.question.dto.QuestionImageUploadResponse;
import com.ai4.question.dto.QuestionPageQuery;
import com.ai4.question.dto.QuestionProjectAttributeResponse;
import com.ai4.question.dto.UpdateQuestionRequest;
import com.ai4.question.model.ProjectQuestion;
import com.ai4.question.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 处理项目题目管理的查询、保存和上传逻辑。 */
@Service
public class QuestionService {

    private static final Integer ENABLED_STATUS = 0;
    private static final int ENABLED_LIMIT = 36;
    private static final String QUESTION_IMAGE_DIRECTORY = "questions/images";

    private final QuestionRepository questionRepository;
    private final OssStorageService ossStorageService;

    public QuestionService(
            QuestionRepository questionRepository,
            OssStorageService ossStorageService
    ) {
        this.questionRepository = questionRepository;
        this.ossStorageService = ossStorageService;
    }

    /** 查询项目名称和项目沙盘属性名称，用于题目管理页面初始化。 */
    @Transactional(readOnly = true)
    public List<QuestionProjectAttributeResponse> projectAttributes() {
        return questionRepository.findProjectAttributes();
    }

    /** 分页查询指定项目下未删除题目，支持题目文本和备注模糊筛选。 */
    @Transactional(readOnly = true)
    public PageResponse<ProjectQuestionListItemResponse> list(QuestionPageQuery query) {
        long total = questionRepository.countPage(query);
        return new PageResponse<>(
                questionRepository.findPage(query),
                total,
                query.normalizedPageNum(),
                QuestionPageQuery.PAGE_SIZE
        );
    }

    /** 查询题目详情，用于修改页面回填。 */
    @Transactional(readOnly = true)
    public QuestionDetailResponse get(Long questionId) {
        ProjectQuestion question = findQuestion(questionId);
        return toDetailResponse(question);
    }

    /** 新增题目，选项和属性影响整体保存。 */
    @Transactional
    public QuestionDetailResponse create(CreateQuestionRequest request) {
        ensureEnabledLimit(request.projectId(), request.status());
        Long questionId = questionRepository.save(request);
        request.options().forEach(option -> questionRepository.insertOption(questionId, option));
        return get(questionId);
    }

    /** 修改题目，项目不允许修改，选项和属性影响整体重建。 */
    @Transactional
    public QuestionDetailResponse update(Long questionId, UpdateQuestionRequest request) {
        ProjectQuestion existing = findQuestion(questionId);
        if (!ENABLED_STATUS.equals(existing.status()) && ENABLED_STATUS.equals(request.status())) {
            ensureEnabledLimit(existing.projectId(), request.status());
        }
        if (questionRepository.update(questionId, request) == 0) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionRepository.deleteOptions(questionId);
        request.options().forEach(option -> questionRepository.insertOption(questionId, option));
        return get(questionId);
    }

    /** 删除题目的选项和属性影响，并逻辑删除题目主表。 */
    @Transactional
    public void delete(Long questionId) {
        findQuestion(questionId);
        questionRepository.deleteOptions(questionId);
        if (questionRepository.softDelete(questionId) == 0) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }
    }

    /** 上传题目图片到 OSS，返回图片访问地址。 */
    public QuestionImageUploadResponse uploadImage(MultipartFile image) {
        UploadFileResponse response = ossStorageService.upload(image, QUESTION_IMAGE_DIRECTORY);
        return new QuestionImageUploadResponse(response.url());
    }

    private ProjectQuestion findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new BizException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private void ensureEnabledLimit(Long projectId, Integer status) {
        if (!ENABLED_STATUS.equals(status)) {
            return;
        }
        if (questionRepository.countEnabled(projectId) >= ENABLED_LIMIT) {
            throw new BizException(
                    "QUESTION_ENABLED_LIMIT_EXCEEDED",
                    "启用题目数量不能超过36个"
            );
        }
    }

    private QuestionDetailResponse toDetailResponse(ProjectQuestion question) {
        return new QuestionDetailResponse(
                question.id(),
                question.projectId(),
                question.imageUrl(),
                question.imageDesc(),
                question.questionText(),
                question.month(),
                question.beforeGuide(),
                question.status(),
                question.remark(),
                questionRepository.findOptionsByQuestionId(question.id())
        );
    }
}
