package com.ai4.classroom.repository;

import com.ai4.classroom.dto.ClassOptionResponse;
import com.ai4.classroom.model.ClassInfo;

import java.util.List;
import java.util.Optional;

/** 班级数据访问边界。 */
public interface ClassRepository {

    List<ClassOptionResponse> findOptions();

    Optional<ClassInfo> findActiveById(String classId);
}
