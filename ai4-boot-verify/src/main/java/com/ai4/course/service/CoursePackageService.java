package com.ai4.course.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.course.model.CoursePackage;
import com.ai4.course.repository.CoursePackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 查询系统中唯一的有效课包。 */
@Service
public class CoursePackageService {

    private final CoursePackageRepository packageRepository;

    public CoursePackageService(CoursePackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    @Transactional(readOnly = true)
    public CoursePackage getOnly() {
        return packageRepository.findOnlyActive()
                .orElseThrow(() ->
                        new BizException(ErrorCode.COURSE_PACKAGE_CONFIGURATION_INVALID));
    }
}
