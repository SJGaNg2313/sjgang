CREATE TABLE `ai4_project_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `task_types` JSON NOT NULL COMMENT '任务大类及格式配置',
    `company_positioning` VARCHAR(20) NOT NULL COMMENT '公司定位',
    `company_card` VARCHAR(1000) DEFAULT NULL COMMENT '公司名片图片 URL',
    `company_name` VARCHAR(7) DEFAULT NULL COMMENT '公司名称，最多 7 个字且以公司或工作室结尾',
    `public_task` VARCHAR(1000) DEFAULT NULL COMMENT '发布任务图 URL',
    `claim_task` VARCHAR(1000) DEFAULT NULL COMMENT '领取任务图 URL',
    `task_name` VARCHAR(8) NOT NULL COMMENT '任务名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai4_project_task_project_id` (`project_id`),
    CONSTRAINT `fk_ai4_project_task_project`
        FOREIGN KEY (`project_id`) REFERENCES `ai4_project` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目领取任务配置';

CREATE TABLE `ai4_project_task_partner` (
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `partner_id` VARCHAR(64) NOT NULL COMMENT '合伙人 ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`task_id`, `partner_id`),
    KEY `idx_ai4_project_task_partner_partner` (`partner_id`, `task_id`),
    CONSTRAINT `fk_ai4_project_task_partner_task`
        FOREIGN KEY (`task_id`) REFERENCES `ai4_project_task` (`id`),
    CONSTRAINT `fk_ai4_project_task_partner_partner`
        FOREIGN KEY (`partner_id`) REFERENCES `ai4_partner` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='项目任务与合伙人关联';

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
    SELECT '查询任务配置' AS menu_name, 5 AS order_num, 'task:list' AS perms
    UNION ALL
    SELECT '新增任务配置', 6, 'task:create'
    UNION ALL
    SELECT '修改任务配置', 7, 'task:update'
    UNION ALL
    SELECT '生成公司名称', 8, 'task:generate'
) seed
WHERE @project_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu ON menu.perms IN (
    'task:list', 'task:create', 'task:update', 'task:generate'
)
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
