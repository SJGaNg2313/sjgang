CREATE TABLE `edu2_ai_course_package` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课包 ID',
    `package_name` VARCHAR(30) NOT NULL COMMENT '课包名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `del_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '删除标志：1 正常，2 删除',
    `remark` VARCHAR(2000) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_package_del_sort` (`del_flag`, `sort_order`, `id`),
    KEY `idx_course_package_name` (`package_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课包信息表';

CREATE TABLE `edu2_ai_lesson` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课节 ID',
    `package_id` BIGINT NOT NULL COMMENT '所属课包 ID',
    `lessons_name` VARCHAR(100) NOT NULL COMMENT '课节名称',
    `lessons_icon` VARCHAR(255) DEFAULT NULL COMMENT '课节图标 URL',
    `unlock_interval` INT NOT NULL DEFAULT 1 COMMENT '解锁间隔，单位：天',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `del_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '删除标志：1 正常，2 删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `lessons_described` VARCHAR(1000) DEFAULT NULL COMMENT '课节描述',
    `tag` VARCHAR(50) DEFAULT NULL COMMENT '课节标签',
    PRIMARY KEY (`id`),
    KEY `idx_lesson_package_del_sort` (`package_id`, `del_flag`, `sort_order`, `id`),
    KEY `idx_lesson_package_name` (`package_id`, `lessons_name`, `del_flag`),
    CONSTRAINT `fk_lesson_package` FOREIGN KEY (`package_id`)
        REFERENCES `edu2_ai_course_package` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课节表';

CREATE TABLE `edu2_ai_lesson_content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程内容 ID',
    `lesson_id` BIGINT NOT NULL COMMENT '所属课节 ID',
    `content_name` VARCHAR(100) NOT NULL COMMENT '内容名称',
    `content_type` VARCHAR(30) NOT NULL COMMENT '内容类型',
    `ref_id` VARCHAR(64) DEFAULT NULL COMMENT '关联业务资源 ID',
    `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面 URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `del_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '删除标志：1 正常，2 删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_lesson_content_lesson_del_sort` (`lesson_id`, `del_flag`, `sort_order`, `id`),
    CONSTRAINT `fk_lesson_content_lesson` FOREIGN KEY (`lesson_id`)
        REFERENCES `edu2_ai_lesson` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课节内容表';
