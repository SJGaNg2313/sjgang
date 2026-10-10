package com.ai4.sandtable.repository;

import com.ai4.sandtable.dto.SandtableAttributeRequest;
import com.ai4.sandtable.dto.SandtableGainRequest;
import com.ai4.sandtable.dto.SandtableSegmentRequest;
import com.ai4.sandtable.model.ProjectSandtable;
import com.ai4.sandtable.model.SandtableAttribute;
import com.ai4.sandtable.model.SandtableGain;
import com.ai4.sandtable.model.SandtableSegment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 项目沙盘数据访问边界。 */
public interface ProjectSandtableRepository {
    Optional<ProjectSandtable> findByProjectId(Long projectId);

    List<SandtableSegment> findSegments(Long sandtableId);

    List<SandtableAttribute> findAttributes(Long sandtableId);

    List<SandtableGain> findGains(Long sandtableId);

    ProjectSandtable save(Long projectId, String videoUrl, String trendUrl);

    int updateMain(Long sandtableId, String videoUrl, String trendUrl);

    void replaceSegments(Long sandtableId, List<SandtableSegmentRequest> segments);

    void insertAttribute(Long sandtableId, SandtableAttributeRequest attribute);

    int updateAttribute(Long sandtableId, SandtableAttributeRequest attribute);

    boolean hasQuestionReferences(Collection<Long> attributeIds);

    void deleteAttributes(Long sandtableId, Collection<Long> attributeIds);

    void insertGain(Long sandtableId, SandtableGainRequest gain);

    void deleteGains(Long sandtableId, Collection<Long> gainIds);
}
