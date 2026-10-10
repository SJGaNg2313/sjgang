package com.ai4.question.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.coze.client.CozeWorkflowClient;
import com.ai4.coze.config.CozeWorkflowProperties;
import com.ai4.coze.dto.CozeWorkflowRunResult;
import com.ai4.project.model.Project;
import com.ai4.project.repository.ProjectRepository;
import com.ai4.question.dto.GenerateBeforeGuideAttributeRequest;
import com.ai4.question.dto.GenerateBeforeGuideOptionRequest;
import com.ai4.question.dto.GenerateBeforeGuideRequest;
import com.ai4.question.dto.GenerateBeforeGuideResponse;
import com.ai4.question.dto.GeneratedOptionGuideResponse;
import com.ai4.question.dto.GenerateQuestionGuidesRequest;
import com.ai4.question.dto.GenerateQuestionGuidesResponse;
import com.ai4.question.dto.QuestionAttributeResponse;
import com.ai4.question.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/** 负责组织题目前置信息并调用 Coze 生成题前引导。 */
@Service
public class QuestionGuideGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGuideGenerationService.class);

    private final ObjectProvider<CozeWorkflowClient> cozeWorkflowClientProvider;
    private final CozeWorkflowProperties workflowProperties;
    private final ProjectRepository projectRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService cozeGuideExecutor;

    public QuestionGuideGenerationService(
            ObjectProvider<CozeWorkflowClient> cozeWorkflowClientProvider,
            CozeWorkflowProperties workflowProperties,
            ProjectRepository projectRepository,
            QuestionRepository questionRepository,
            ObjectMapper objectMapper,
            @Qualifier("cozeGuideExecutor") ExecutorService cozeGuideExecutor
    ) {
        this.cozeWorkflowClientProvider = cozeWorkflowClientProvider;
        this.workflowProperties = workflowProperties;
        this.projectRepository = projectRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.cozeGuideExecutor = cozeGuideExecutor;
    }

    /** 同步生成题前引导，结果交给前端回填并由管理员确认。 */
    public GenerateBeforeGuideResponse generate(GenerateBeforeGuideRequest request) {
        CozeWorkflowClient workflowClient = cozeWorkflowClientProvider.getIfAvailable();
        String workflowId = workflowProperties.questionBeforeGuideId();
        if (workflowClient == null || !StringUtils.hasText(workflowId)) {
            throw new BizException(ErrorCode.COZE_NOT_CONFIGURED);
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
        Map<Long, String> attributeNames = questionRepository
                .findAttributesByProjectId(request.projectId())
                .stream()
                .collect(Collectors.toMap(
                        QuestionAttributeResponse::attributeId,
                        QuestionAttributeResponse::attributeName
                ));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("Project_Introduction", project.projectIntro());
        parameters.put("imageDesc", trimToEmpty(request.imageDesc()));
        parameters.put("option_information", buildOptionInformation(request.options(), attributeNames));
        parameters.put("questionText", request.questionText().trim());

        try {
            CozeWorkflowRunResult result = workflowClient.runSync(workflowId, parameters);
            return new GenerateBeforeGuideResponse(readOutput(result.data()));
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("调用Coze题前引导工作流失败，workflowId={}", workflowId, exception);
            throw new BizException(ErrorCode.QUESTION_GUIDE_GENERATION_FAILED);
        }
    }

    /** 并行生成题前引导和全部选项的选后引导，结果由前端确认后再随题目保存。 */
    public GenerateQuestionGuidesResponse generateGuides(GenerateQuestionGuidesRequest request) {
        CozeWorkflowClient workflowClient = cozeWorkflowClientProvider.getIfAvailable();
        String beforeGuideWorkflowId = workflowProperties.questionBeforeGuideId();
        String afterGuideWorkflowId = workflowProperties.questionAfterGuideId();
        if (workflowClient == null
                || !StringUtils.hasText(beforeGuideWorkflowId)
                || !StringUtils.hasText(afterGuideWorkflowId)) {
            throw new BizException(ErrorCode.COZE_NOT_CONFIGURED);
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
        Map<Long, String> attributeNames = findAttributeNames(request.projectId());
        List<Map<String, Object>> optionInformation = buildOptionInformation(
                request.options(),
                attributeNames
        );

        List<Callable<String>> tasks = new ArrayList<>();
        tasks.add(() -> runWorkflow(
                workflowClient,
                beforeGuideWorkflowId,
                buildWorkflowParameters(project, request.imageDesc(), request.questionText(), optionInformation)
        ));
        for (Map<String, Object> selectedOption : optionInformation) {
            tasks.add(() -> {
                Map<String, Object> parameters = buildWorkflowParameters(
                        project,
                        request.imageDesc(),
                        request.questionText(),
                        optionInformation
                );
                parameters.put("user_answer", List.of(selectedOption));
                return runWorkflow(workflowClient, afterGuideWorkflowId, parameters);
            });
        }

        try {
            List<Future<String>> results = cozeGuideExecutor.invokeAll(tasks);
            String beforeGuide = results.get(0).get();
            List<GeneratedOptionGuideResponse> optionGuides = new ArrayList<>();
            for (int optionIndex = 0; optionIndex < request.options().size(); optionIndex++) {
                optionGuides.add(new GeneratedOptionGuideResponse(
                        optionIndex,
                        request.options().get(optionIndex).optionText().trim(),
                        results.get(optionIndex + 1).get()
                ));
            }
            return new GenerateQuestionGuidesResponse(beforeGuide, optionGuides);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("生成题目引导时线程被中断", exception);
            throw new BizException(ErrorCode.QUESTION_GUIDE_GENERATION_FAILED);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof BizException bizException) {
                throw bizException;
            }
            log.error("调用Coze题目引导工作流失败", cause);
            throw new BizException(ErrorCode.QUESTION_GUIDE_GENERATION_FAILED);
        }
    }

    private Map<Long, String> findAttributeNames(Long projectId) {
        return questionRepository.findAttributesByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(
                        QuestionAttributeResponse::attributeId,
                        QuestionAttributeResponse::attributeName
                ));
    }

    private Map<String, Object> buildWorkflowParameters(
            Project project,
            String imageDesc,
            String questionText,
            List<Map<String, Object>> optionInformation
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("Project_Introduction", project.projectIntro());
        parameters.put("imageDesc", trimToEmpty(imageDesc));
        parameters.put("option_information", optionInformation);
        parameters.put("questionText", questionText.trim());
        return parameters;
    }

    private String runWorkflow(
            CozeWorkflowClient workflowClient,
            String workflowId,
            Map<String, Object> parameters
    ) throws Exception {
        CozeWorkflowRunResult result = workflowClient.runSync(workflowId, parameters);
        return readOutput(result.data());
    }

    private List<Map<String, Object>> buildOptionInformation(
            List<GenerateBeforeGuideOptionRequest> options,
            Map<Long, String> attributeNames
    ) {
        return options.stream()
                .map(option -> buildOption(option, attributeNames))
                .toList();
    }

    private Map<String, Object> buildOption(
            GenerateBeforeGuideOptionRequest option,
            Map<Long, String> attributeNames
    ) {
        Map<String, Object> optionInformation = new LinkedHashMap<>();
        optionInformation.put("optionText", option.optionText().trim());
        optionInformation.put("impactType", option.impactType().trim());
        optionInformation.put(
                "attributeEffects",
                option.attributes().stream()
                        .map(attribute -> buildAttributeEffect(attribute, attributeNames))
                        .toList()
        );
        return optionInformation;
    }

    private Map<String, Object> buildAttributeEffect(
            GenerateBeforeGuideAttributeRequest attribute,
            Map<Long, String> attributeNames
    ) {
        String attributeName = attributeNames.get(attribute.attributeId());
        if (attributeName == null) {
            throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_INVALID);
        }

        Map<String, Object> attributeEffect = new LinkedHashMap<>();
        attributeEffect.put("attributeName", attributeName);
        attributeEffect.put("attributeEffect", attribute.attributeEffect().trim());
        return attributeEffect;
    }

    private String readOutput(String data) throws Exception {
        if (!StringUtils.hasText(data)) {
            throw new IllegalStateException("Coze工作流未返回输出内容");
        }

        JsonNode root = objectMapper.readTree(data);
        JsonNode output = root.get("output");
        if (output == null || !StringUtils.hasText(output.asText())) {
            throw new IllegalStateException("Coze工作流返回数据中缺少output字段");
        }
        return output.asText().trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
