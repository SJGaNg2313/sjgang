package com.ai4.classroom.service;

import com.ai4.classroom.dto.ClassOptionResponse;
import com.ai4.classroom.repository.ClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 班级查询业务。 */
@Service
public class ClassService {

    private final ClassRepository classRepository;

    public ClassService(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    /** 查询建课页面可选择的有效班级。 */
    @Transactional(readOnly = true)
    public List<ClassOptionResponse> options() {
        return classRepository.findOptions();
    }
}
