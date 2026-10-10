package com.ai4.task.service;

import com.ai4.ai.client.AiClient;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.file.service.OssStorageService;
import com.ai4.project.model.Project;
import com.ai4.project.repository.ProjectRepository;
import com.ai4.task.dto.*;
import com.ai4.task.model.ProjectTask;
import com.ai4.task.model.ProjectTaskPartnerSkills;
import com.ai4.task.repository.ProjectTaskRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 项目领取任务配置及公司名称生成业务 */
@Service
public class ProjectTaskService {

    private static final int COMPANY_NAME_COUNT = 60;
    private static final int MAX_GENERATION_ATTEMPTS = 2;
    private static final Map<String, Set<String>> ALLOWED_TASK_TYPES = Map.of(
            "IMAGE", Set.of("png", "jpg", "jpeg"),
            "TEXT", Set.of("txt", "md", "html", "json", "css"),
            "FILE", Set.of("docx", "pdf", "xlsx", "pptx"),
            "MEDIA", Set.of("mp4", "mp3")
    );

    private final ProjectTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final OssStorageService ossStorageService;

    public ProjectTaskService(
            ProjectTaskRepository taskRepository,
            ProjectRepository projectRepository,
            AiClient aiClient,
            ObjectMapper objectMapper,
            OssStorageService ossStorageService
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.ossStorageService = ossStorageService;
    }

    /** 按项目查询完整任务配置 */
    @Transactional(readOnly = true)
    public ProjectTaskResponse get(Long projectId) {
        findProject(projectId);
        return toResponse(findTask(projectId));
    }

    /** 首次创建任务配置并保存 2 至 4 个合伙人及其 Skill 关联 */
    @Transactional
    public ProjectTaskResponse create(Long projectId, CreateProjectTaskRequest request) {
        findProject(projectId);
        if (taskRepository.findByProjectId(projectId).isPresent()) {
            throw new BizException(ErrorCode.TASK_ALREADY_EXISTS);
        }
        List<TaskTypeRequest> taskTypes = normalizeTaskTypes(request.taskTypes());
        List<String> taskKeywords = normalizeTaskKeywords(request.taskKeywords());
        List<ProjectTaskPartnerSkills> partnerSkills = normalizePartnerSkills(request.partners());
        ProjectTask task = buildTask(
                projectId, taskTypes, request.companyPositioning(), request.companyCard(),
                request.taskName(), request.taskImage(), request.taskObjective(), taskKeywords,
                request.submissionIcon(), request.submissionName(), request.submissionDescription()
        );
        try {
            ProjectTask saved = taskRepository.save(task);
            taskRepository.replacePartnerRelations(saved.id(), partnerSkills);
            return toResponse(saved);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.TASK_ALREADY_EXISTS);
        }
    }

    /** 完整修改任务配置并整体替换合伙人及其 Skill 关联 */
    @Transactional
    public ProjectTaskResponse update(Long projectId, UpdateProjectTaskRequest request) {
        findProject(projectId);
        ProjectTask existing = findTask(projectId);
        List<TaskTypeRequest> taskTypes = normalizeTaskTypes(request.taskTypes());
        List<String> taskKeywords = normalizeTaskKeywords(request.taskKeywords());
        List<ProjectTaskPartnerSkills> partnerSkills = normalizePartnerSkills(request.partners());
        ProjectTask task = buildTask(
                projectId, taskTypes, request.companyPositioning(),
                keepExistingIfBlank(request.companyCard(), existing.companyCard()),
                request.taskName(),
                keepExistingIfBlank(request.taskImage(), existing.taskImage()),
                request.taskObjective(), taskKeywords,
                keepExistingIfBlank(request.submissionIcon(), existing.submissionIcon()),
                request.submissionName(), request.submissionDescription()
        );
        if (taskRepository.update(task) == 0) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        taskRepository.replacePartnerRelations(existing.id(), partnerSkills);
        return get(projectId);
    }

    /** 根据项目和任务配置生成 60 个合法且不重复的公司名称 */
    public CompanyNamesResponse generateCompanyNames(Long projectId) {
        Project project = findProject(projectId);
        ProjectTask task = findTask(projectId);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS && names.size() < COMPANY_NAME_COUNT;
             attempt++) {
            int remaining = COMPANY_NAME_COUNT - names.size();
            String output = aiClient.generateText(
                    buildPrompt(project, task, remaining, names)
            );
            parseCompanyNames(output).stream()
                    .map(String::trim)
                    .filter(this::isValidCompanyName)
                    .forEach(names::add);
        }
        if (names.size() < COMPANY_NAME_COUNT) {
            throw new BizException(ErrorCode.TASK_COMPANY_NAME_GENERATION_FAILED);
        }
        List<String> companyNames = names.stream().limit(COMPANY_NAME_COUNT).toList();
        if (taskRepository.updateCompanyNames(projectId, writeCompanyNames(companyNames)) == 0) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return new CompanyNamesResponse(companyNames);
    }

    /** 修改指定下标的公司名称候选并整体写回 JSON。 */
    public CompanyNamesResponse updateCompanyName(
            Long projectId,
            int index,
            UpdateCompanyNameRequest request
    ) {
        findProject(projectId);
        ProjectTask task = findTask(projectId);
        List<String> companyNames = new ArrayList<>(readCompanyNames(task.companyNames()));
        if (index < 0 || index >= companyNames.size()) {
            throw new BizException(ErrorCode.TASK_COMPANY_NAME_INDEX_INVALID);
        }
        String companyName = request.companyName().trim();
        validateEditableCompanyName(companyName);
        boolean duplicated = companyNames.stream()
                .anyMatch(existing -> existing.equals(companyName))
                && !companyNames.get(index).equals(companyName);
        if (duplicated) {
            throw new BizException(ErrorCode.TASK_COMPANY_NAME_DUPLICATED);
        }
        companyNames.set(index, companyName);
        if (taskRepository.updateCompanyNames(projectId, writeCompanyNames(companyNames)) == 0) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return new CompanyNamesResponse(List.copyOf(companyNames));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectTask findTask(Long projectId) {
        return taskRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND));
    }

    private List<TaskTypeRequest> normalizeTaskTypes(List<TaskTypeRequest> requestedTypes) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (TaskTypeRequest requestedType : requestedTypes) {
            String category = requestedType.category().trim().toUpperCase(Locale.ROOT);
            Set<String> allowedFormats = ALLOWED_TASK_TYPES.get(category);
            if (allowedFormats == null || normalized.containsKey(category)) {
                throw new BizException(ErrorCode.TASK_TYPE_INVALID);
            }
            List<String> formats = requestedType.formats().stream()
                    .map(format -> format.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (new LinkedHashSet<>(formats).size() != formats.size()
                    || formats.stream().anyMatch(format -> !allowedFormats.contains(format))) {
                throw new BizException(ErrorCode.TASK_TYPE_INVALID);
            }
            normalized.put(category, formats);
        }
        return normalized.entrySet().stream()
                .map(entry -> new TaskTypeRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ProjectTaskPartnerSkills> normalizePartnerSkills(
            List<ProjectTaskPartnerRequest> requestedPartners
    ) {
        Set<String> seenPartnerIds = new LinkedHashSet<>();
        Set<String> allSkillIds = new LinkedHashSet<>();
        List<ProjectTaskPartnerSkills> partnerSkills = new ArrayList<>();
        for (ProjectTaskPartnerRequest requestedPartner : requestedPartners) {
            String partnerId = trimToNull(requestedPartner.partnerId());
            if (partnerId == null) {
                throw new BizException(ErrorCode.TASK_PARTNER_INVALID);
            }
            if (!seenPartnerIds.add(partnerId)) {
                throw new BizException(ErrorCode.TASK_PARTNER_DUPLICATED);
            }
            List<String> skillIds = requestedPartner.skillIds().stream()
                    .map(this::trimToNull)
                    .toList();
            if (skillIds.stream().anyMatch(java.util.Objects::isNull)) {
                throw new BizException(ErrorCode.TASK_PARTNER_SKILL_INVALID);
            }
            if (new LinkedHashSet<>(skillIds).size() != skillIds.size()) {
                throw new BizException(ErrorCode.TASK_PARTNER_SKILL_DUPLICATED);
            }
            allSkillIds.addAll(skillIds);
            partnerSkills.add(new ProjectTaskPartnerSkills(partnerId, skillIds));
        }
        List<String> partnerIds = new ArrayList<>(seenPartnerIds);
        if (taskRepository.lockActivePartnerIds(partnerIds).size() != partnerIds.size()) {
            throw new BizException(ErrorCode.TASK_PARTNER_INVALID);
        }
        if (taskRepository.lockActiveSkillIds(new ArrayList<>(allSkillIds)).size()
                != allSkillIds.size()) {
            throw new BizException(ErrorCode.TASK_PARTNER_SKILL_INVALID);
        }
        return partnerSkills;
    }

    private List<String> normalizeTaskKeywords(List<String> requestedKeywords) {
        if (requestedKeywords == null || requestedKeywords.size() < 2
                || requestedKeywords.size() > 5) {
            throw new BizException(ErrorCode.TASK_KEYWORDS_INVALID);
        }
        List<String> keywords = requestedKeywords.stream()
                .map(this::trimToNull)
                .toList();
        if (keywords.stream().anyMatch(keyword -> keyword == null
                || keyword.codePointCount(0, keyword.length()) < 2
                || keyword.codePointCount(0, keyword.length()) > 4)) {
            throw new BizException(ErrorCode.TASK_KEYWORDS_INVALID);
        }
        return keywords;
    }

    private ProjectTask buildTask(
            Long projectId,
            List<TaskTypeRequest> taskTypes,
            String companyPositioning,
            String companyCard,
            String taskName,
            String taskImage,
            String taskObjective,
            List<String> taskKeywords,
            String submissionIcon,
            String submissionName,
            String submissionDescription
    ) {
        return new ProjectTask(
                null, projectId, writeTaskTypes(taskTypes), companyPositioning.trim(),
                trimToNull(companyCard), null, taskName.trim(), trimToNull(taskImage),
                taskObjective.trim(), writeTaskKeywords(taskKeywords),
                trimToNull(submissionIcon), submissionName.trim(),
                submissionDescription.trim(), null, null
        );
    }

    private ProjectTaskResponse toResponse(ProjectTask task) {
        return new ProjectTaskResponse(
                task.id(), task.projectId(), readTaskTypes(task.taskTypes()),
                task.companyPositioning(), task.companyCard(), readCompanyNames(task.companyNames()),
                task.taskName(), task.taskImage(), task.taskObjective(),
                readTaskKeywords(task.taskKeywords()), task.submissionIcon(),
                task.submissionName(), task.submissionDescription(),
                taskRepository.findPartnerSkills(task.id()).stream()
                        .map(partner -> new ProjectTaskPartnerResponse(
                                partner.partnerId(), partner.skillIds()
                        ))
                        .toList(),
                task.createdAt(), task.updatedAt()
        );
    }

    private String buildPrompt(
            Project project,
            ProjectTask task,
            int count,
            Set<String> existingNames
    ) {
        return """
                直接生成%d个简单中文企业名称，不要分析。
                只返回JSON字符串数组，数组必须正好包含%d项。
                每项最多7个汉字，必须以“公司”或“工作室”结尾。
                名称不能重复，不要返回已有名称：%s。
                不要Markdown、序号、说明或其他字段。
                业务参考：%s；%s；%s；%s。
                """.formatted(
                count,
                count,
                existingNames,
                project.projectName(), project.projectIntro(), task.taskName(),
                task.companyPositioning()
        );
    }

    private List<String> parseCompanyNames(String output) {
        String json = output == null ? "" : output.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JacksonException exception) {
            return new ArrayList<>();
        }
    }

    private void validateEditableCompanyName(String companyName) {
        if (companyName == null
                || companyName.isBlank()
                || companyName.trim().codePointCount(0, companyName.trim().length()) > 7) {
            throw new BizException(ErrorCode.TASK_COMPANY_NAME_INVALID);
        }
    }

    private boolean isValidCompanyName(String companyName) {
        return companyName != null
                && !companyName.isBlank()
                && companyName.codePointCount(0, companyName.length()) <= 7
                && (companyName.endsWith("公司") || companyName.endsWith("工作室"));
    }

    private String writeTaskTypes(List<TaskTypeRequest> taskTypes) {
        try {
            return objectMapper.writeValueAsString(taskTypes);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize task types", exception);
        }
    }

    private List<TaskTypeRequest> readTaskTypes(String taskTypes) {
        try {
            return objectMapper.readValue(taskTypes, new TypeReference<>() { });
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize task types", exception);
        }
    }

    private String writeCompanyNames(List<String> companyNames) {
        try {
            return objectMapper.writeValueAsString(companyNames);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize company names", exception);
        }
    }

    private String writeTaskKeywords(List<String> taskKeywords) {
        try {
            return objectMapper.writeValueAsString(taskKeywords);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize task keywords", exception);
        }
    }

    private List<String> readCompanyNames(String companyNames) {
        if (companyNames == null || companyNames.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(companyNames, new TypeReference<>() { });
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize company names", exception);
        }
    }

    private List<String> readTaskKeywords(String taskKeywords) {
        if (taskKeywords == null || taskKeywords.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(taskKeywords, new TypeReference<>() { });
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize task keywords", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String keepExistingIfBlank(String requested, String existing) {
        return requested == null || requested.isBlank() ? existing : requested;
    }

    /**
     * 上传任务相关图片
     * @param taskImage
     * @param submissionIcon
     * @param companyCard
     * @return
     */
    public TaskImageUploadResponse uploadImages(
            Long projectId,
            MultipartFile taskImage,
            MultipartFile submissionIcon,
            MultipartFile companyCard
    ){
        findProject(projectId);
        if (!hasFile(taskImage) && !hasFile(submissionIcon) && !hasFile(companyCard)) {
            throw new BizException("TASK_IMAGE_EMPTY", "至少上传一张任务图片");
        }

        return new TaskImageUploadResponse(
                uploadImageIfPresent(
                        taskImage, "task/" + projectId + "/task-image"
                ),
                uploadImageIfPresent(
                        submissionIcon, "task/" + projectId + "/submission-icon"
                ),
                uploadImageIfPresent(
                        companyCard, "task/" + projectId + "/company-card"
                )
        );
    }

    private String uploadImageIfPresent(MultipartFile file, String directory) {
        if (!hasFile(file)) {
            return null;
        }
        return ossStorageService.upload(file, directory).url();
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
