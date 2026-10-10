package com.ai4.course.repository;

import com.ai4.course.model.CoursePackage;

import java.util.Optional;

/** 唯一有效课包的数据访问边界。 */
public interface CoursePackageRepository {

    /**
     * 仅当数据库中恰好存在一个有效课包时返回该课包。
     */
    Optional<CoursePackage> findOnlyActive();
}
