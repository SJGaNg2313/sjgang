CREATE TABLE `ai4_project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目 ID',
    `package_id` BIGINT NOT NULL COMMENT '所属课包 ID',
    `project_name` VARCHAR(10) NOT NULL COMMENT '项目名称',
    `project_intro` VARCHAR(25) NOT NULL COMMENT '项目介绍',
    `project_code` VARCHAR(64) NOT NULL COMMENT '项目编码',
    `del_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '删除标志：1 正常，2 删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai4_project_code` (`project_code`),
    KEY `idx_ai4_project_package_del` (`package_id`, `del_flag`, `id`),
    KEY `idx_ai4_project_name` (`project_name`),
    CONSTRAINT `fk_ai4_project_package`
        FOREIGN KEY (`package_id`) REFERENCES `ai4_course_package` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI4 项目表';

DROP TABLE `ai4_lesson_content`;
DROP TABLE `ai4_lesson`;

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'DIR', '系统管理', 0, '/system', 'Layout',
       1, NULL, 'settings', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/system'
);

SET @system_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/system'
);

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '项目管理', @system_menu_id, '/projects',
       'project/index', 8, NULL, 'folder-kanban', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/projects'
);

SET @project_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/projects'
);

UPDATE sys_menu
SET menu_type = 'MENU',
    menu_name = '项目管理',
    parent_id = @system_menu_id,
    component = 'project/index',
    order_num = 8,
    perms = NULL,
    icon = 'folder-kanban',
    status = 0
WHERE menu_id = @project_menu_id;

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @project_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询项目' AS menu_name, 1 AS order_num, 'project:list' AS perms
    UNION ALL
    SELECT '新增项目', 2, 'project:create'
    UNION ALL
    SELECT '修改项目', 3, 'project:update'
    UNION ALL
    SELECT '删除项目', 4, 'project:delete'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms
);

UPDATE sys_menu
SET menu_type = 'BUTTON',
    parent_id = @project_menu_id,
    path = NULL,
    component = NULL,
    icon = NULL,
    status = 0
WHERE perms IN (
    'project:list', 'project:create', 'project:update', 'project:delete'
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu
  ON menu.menu_id = @project_menu_id
  OR menu.perms IN (
      'project:list', 'project:create', 'project:update', 'project:delete'
  )
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
