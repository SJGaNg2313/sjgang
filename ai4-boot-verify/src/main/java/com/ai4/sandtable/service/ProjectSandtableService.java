package com.ai4.sandtable.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.file.service.OssStorageService;
import com.ai4.project.repository.ProjectRepository;
import com.ai4.sandtable.dto.ProjectSandtableResponse;
import com.ai4.sandtable.dto.SandtableAttributeRequest;
import com.ai4.sandtable.dto.SandtableAttributeResponse;
import com.ai4.sandtable.dto.SandtableGainRequest;
import com.ai4.sandtable.dto.SandtableGainResponse;
import com.ai4.sandtable.dto.SandtableSegmentRequest;
import com.ai4.sandtable.dto.SandtableSegmentResponse;
import com.ai4.sandtable.dto.SandtableTrendUploadResponse;
import com.ai4.sandtable.dto.SandtableVideoUploadResponse;
import com.ai4.sandtable.dto.SaveProjectSandtableRequest;
import com.ai4.sandtable.model.ProjectSandtable;
import com.ai4.sandtable.model.SandtableAttribute;
import com.ai4.sandtable.model.SandtableGain;
import com.ai4.sandtable.repository.ProjectSandtableRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 项目沙盘配置业务。 */
@Service
public class ProjectSandtableService {

    private final ProjectSandtableRepository sandtableRepository;
    private final ProjectRepository projectRepository;
    private final OssStorageService ossStorageService;

    public ProjectSandtableService(
            ProjectSandtableRepository sandtableRepository,
            ProjectRepository projectRepository,
            OssStorageService ossStorageService
    ) {
        this.sandtableRepository = sandtableRepository;
        this.projectRepository = projectRepository;
        this.ossStorageService = ossStorageService;
    }

    /** 查询项目沙盘完整配置。 */
    @Transactional(readOnly = true)
    public ProjectSandtableResponse get(Long projectId) {
        requireProject(projectId);
        return toResponse(requireSandtable(projectId));
    }

    /** 首次创建沙盘、分段、属性和预计学习收获。 */
    @Transactional
    public ProjectSandtableResponse create(Long projectId, SaveProjectSandtableRequest request) {
        requireProject(projectId);
        if (sandtableRepository.findByProjectId(projectId).isPresent()) {
            throw new BizException(ErrorCode.SANDTABLE_ALREADY_EXISTS);
        }
        ensureCreateAttributesHaveNoId(request.attributes());
        ensureCreateGainsHaveNoId(request.gains());
        try {
            ProjectSandtable sandtable = sandtableRepository.save(
                    projectId,
                    trimToNull(request.videoUrl()),
                    request.trendUrl().trim()
            );
            sandtableRepository.replaceSegments(
                    sandtable.id(),
                    normalizeSegments(request.segments())
            );
            request.attributes().stream()
                    .map(this::normalizeAttribute)
                    .forEach(attribute -> sandtableRepository.insertAttribute(
                            sandtable.id(),
                            attribute
                    ));
            request.gains().stream()
                    .map(this::normalizeGain)
                    .forEach(gain -> sandtableRepository.insertGain(sandtable.id(), gain));
            return toResponse(sandtable);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.SANDTABLE_ALREADY_EXISTS);
        }
    }

    /** 完整修改沙盘；分段整体同步，属性只处理新增、删除和真正变化的记录。 */
    @Transactional
    public ProjectSandtableResponse update(Long projectId, SaveProjectSandtableRequest request) {
        requireProject(projectId);
        ProjectSandtable sandtable = requireSandtable(projectId);
        if (sandtableRepository.updateMain(
                sandtable.id(),
                trimToNull(request.videoUrl()),
                request.trendUrl().trim()
        ) == 0) {
            throw new BizException(ErrorCode.SANDTABLE_NOT_FOUND);
        }
        sandtableRepository.replaceSegments(
                sandtable.id(),
                normalizeSegments(request.segments())
        );
        syncAttributes(sandtable.id(), request.attributes());
        syncGains(sandtable.id(), request.gains());
        return toResponse(requireSandtable(projectId));
    }

    /** 上传一个项目沙盘视频并返回OSS地址。 */
    public SandtableVideoUploadResponse uploadVideo(Long projectId, MultipartFile video) {
        requireProject(projectId);
        if (video == null || video.isEmpty()) {
            throw new BizException(ErrorCode.SANDTABLE_VIDEO_EMPTY);
        }
        return new SandtableVideoUploadResponse(
                ossStorageService.upload(video, "sandtable/" + projectId + "/video").url()
        );
    }

    /** 上传项目沙盘收益趋势图并返回OSS地址。 */
    public SandtableTrendUploadResponse uploadTrend(Long projectId, MultipartFile trend) {
        requireProject(projectId);
        if (trend == null || trend.isEmpty()) {
            throw new BizException(ErrorCode.SANDTABLE_TREND_IMAGE_EMPTY);
        }
        return new SandtableTrendUploadResponse(
                ossStorageService.upload(trend, "sandtable/" + projectId + "/trend").url()
        );
    }

    private void syncAttributes(Long sandtableId, List<SandtableAttributeRequest> requested) {
        Map<Long, SandtableAttribute> existingById = new HashMap<>();
        for (SandtableAttribute attribute : sandtableRepository.findAttributes(sandtableId)) {
            existingById.put(attribute.id(), attribute);
        }

        Set<Long> submittedIds = new HashSet<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        List<SandtableAttributeRequest> insertAttributes = new java.util.ArrayList<>();

        for (SandtableAttributeRequest rawAttribute : requested) {
            SandtableAttributeRequest attribute = normalizeAttribute(rawAttribute);
            if (attribute.id() == null) {
                insertAttributes.add(attribute);
                continue;
            }
            if (!submittedIds.add(attribute.id())) {
                throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_INVALID);
            }
            SandtableAttribute existing = existingById.get(attribute.id());
            if (existing == null) {
                throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_INVALID);
            }
            if (!sameAttribute(existing, attribute)) {
                if (sandtableRepository.updateAttribute(sandtableId, attribute) == 0) {
                    throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_INVALID);
                }
            }
        }

        existingById.keySet().stream()
                .filter(existingId -> !submittedIds.contains(existingId))
                .forEach(deleteIds::add);
        if (sandtableRepository.hasQuestionReferences(deleteIds)) {
            throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_IN_USE);
        }
        sandtableRepository.deleteAttributes(sandtableId, deleteIds);
        insertAttributes.forEach(attribute ->
                sandtableRepository.insertAttribute(sandtableId, attribute)
        );
    }

    private boolean sameAttribute(
            SandtableAttribute existing,
            SandtableAttributeRequest requested
    ) {
        return existing.attributeName().equals(requested.attributeName())
                && Objects.equals(existing.icon(), requested.icon())
                && sameNumber(existing.target(), requested.target())
                && sameNumber(existing.initial(), requested.initial());
    }

    private boolean sameNumber(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.compareTo(second) == 0;
    }

    private void syncGains(Long sandtableId, List<SandtableGainRequest> requested) {
        Map<Long, SandtableGain> existingById = new HashMap<>();
        for (SandtableGain gain : sandtableRepository.findGains(sandtableId)) {
            existingById.put(gain.id(), gain);
        }

        Set<Long> submittedIds = new HashSet<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        List<SandtableGainRequest> insertGains = new java.util.ArrayList<>();

        for (SandtableGainRequest rawGain : requested) {
            SandtableGainRequest gain = normalizeGain(rawGain);
            if (gain.id() == null) {
                insertGains.add(gain);
                continue;
            }
            if (!submittedIds.add(gain.id())) {
                throw new BizException(ErrorCode.SANDTABLE_GAIN_INVALID);
            }
            SandtableGain existing = existingById.get(gain.id());
            if (existing == null) {
                throw new BizException(ErrorCode.SANDTABLE_GAIN_INVALID);
            }
            if (!sameGain(existing, gain)) {
                deleteIds.add(existing.id());
                insertGains.add(new SandtableGainRequest(null, gain.icon(), gain.gain()));
            }
        }

        existingById.keySet().stream()
                .filter(existingId -> !submittedIds.contains(existingId))
                .forEach(deleteIds::add);
        sandtableRepository.deleteGains(sandtableId, deleteIds);
        insertGains.forEach(gain -> sandtableRepository.insertGain(sandtableId, gain));
    }

    private boolean sameGain(SandtableGain existing, SandtableGainRequest requested) {
        return Objects.equals(existing.icon(), requested.icon())
                && existing.gain().equals(requested.gain());
    }

    private List<SandtableSegmentRequest> normalizeSegments(
            List<SandtableSegmentRequest> segments
    ) {
        if (segments == null) {
            return List.of();
        }
        return segments.stream()
                .map(segment -> new SandtableSegmentRequest(
                        segment.segmentTime(),
                        trimToNull(segment.segmentName())
                ))
                .toList();
    }

    private SandtableAttributeRequest normalizeAttribute(SandtableAttributeRequest attribute) {
        return new SandtableAttributeRequest(
                attribute.id(),
                attribute.attributeName().trim(),
                trimToNull(attribute.icon()),
                attribute.target(),
                attribute.initial()
        );
    }

    private SandtableGainRequest normalizeGain(SandtableGainRequest gain) {
        return new SandtableGainRequest(
                gain.id(),
                trimToNull(gain.icon()),
                gain.gain().trim()
        );
    }

    private void ensureCreateAttributesHaveNoId(List<SandtableAttributeRequest> attributes) {
        if (attributes.stream().anyMatch(attribute -> attribute.id() != null)) {
            throw new BizException(ErrorCode.SANDTABLE_ATTRIBUTE_INVALID);
        }
    }

    private void ensureCreateGainsHaveNoId(List<SandtableGainRequest> gains) {
        if (gains.stream().anyMatch(gain -> gain.id() != null)) {
            throw new BizException(ErrorCode.SANDTABLE_GAIN_INVALID);
        }
    }

    private ProjectSandtable requireSandtable(Long projectId) {
        return sandtableRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.SANDTABLE_NOT_FOUND));
    }

    private void requireProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectSandtableResponse toResponse(ProjectSandtable sandtable) {
        List<SandtableSegmentResponse> segments = sandtableRepository
                .findSegments(sandtable.id()).stream()
                .map(segment -> new SandtableSegmentResponse(
                        segment.id(),
                        segment.segmentTime(),
                        segment.segmentName()
                ))
                .toList();
        List<SandtableAttributeResponse> attributes = sandtableRepository
                .findAttributes(sandtable.id()).stream()
                .map(attribute -> new SandtableAttributeResponse(
                        attribute.id(),
                        attribute.attributeName(),
                        attribute.icon(),
                        attribute.target(),
                        attribute.initial()
                ))
                .toList();
        List<SandtableGainResponse> gains = sandtableRepository
                .findGains(sandtable.id()).stream()
                .map(gain -> new SandtableGainResponse(
                        gain.id(),
                        gain.icon(),
                        gain.gain()
                ))
                .toList();
        return new ProjectSandtableResponse(
                sandtable.id(),
                sandtable.projectId(),
                sandtable.videoUrl(),
                sandtable.trendUrl(),
                segments,
                attributes,
                gains,
                sandtable.createdAt(),
                sandtable.updatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
