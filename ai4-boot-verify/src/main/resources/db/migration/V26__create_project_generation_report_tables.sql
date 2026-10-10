CREATE TABLE `ai4_project_generation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目生成配置 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `scoring_criteria` TEXT NOT NULL COMMENT '评分标准',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai4_project_generation_project` (`project_id`),
    CONSTRAINT `fk_ai4_project_generation_project`
        FOREIGN KEY (`project_id`) REFERENCES `ai4_project` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目生成配置';

CREATE TABLE `ai4_project_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '创业报告配置 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `review_criteria` TEXT NOT NULL COMMENT '点评标准',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai4_project_report_project` (`project_id`),
    CONSTRAINT `fk_ai4_project_report_project`
        FOREIGN KEY (`project_id`) REFERENCES `ai4_project` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='创业报告配置';

SET @project_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/projects'
);

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @project_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询项目生成配置' AS menu_name, 9 AS order_num, 'generation:list' AS perms
    UNION ALL SELECT '新增项目生成配置', 10, 'generation:create'
    UNION ALL SELECT '修改项目生成配置', 11, 'generation:update'
    UNION ALL SELECT '查询创业报告配置', 12, 'report:list'
    UNION ALL SELECT '新增创业报告配置', 13, 'report:create'
    UNION ALL SELECT '修改创业报告配置', 14, 'report:update'
) seed
WHERE @project_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu ON menu.perms IN (
    'generation:list', 'generation:create', 'generation:update',
    'report:list', 'report:create', 'report:update'
)
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
