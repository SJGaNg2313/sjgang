-- Ensure the system directory exists.
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
    SELECT MIN(menu_id)
    FROM sys_menu
    WHERE path = '/system'
);

-- Create the provisional question management page.
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '题目管理', @system_menu_id, '/questions',
       'question/index', 5, NULL, 'circle-help', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/questions'
);

SET @question_menu_id = (
    SELECT MIN(menu_id)
    FROM sys_menu
    WHERE path = '/questions'
);

-- Normalize an existing question page if it was created manually.
UPDATE sys_menu
SET menu_type = 'MENU',
    menu_name = '题目管理',
    parent_id = @system_menu_id,
    component = 'question/index',
    order_num = 5,
    perms = NULL,
    icon = 'circle-help',
    status = 0
WHERE menu_id = @question_menu_id;

-- Create question operation permissions.
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @question_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询题目' AS menu_name, 1 AS order_num,
           'question:list' AS perms
    UNION ALL
    SELECT '修改题目', 2, 'question:update'
    UNION ALL
    SELECT '删除题目', 3, 'question:delete'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.perms = seed.perms
);

-- Normalize permissions that may already exist.
UPDATE sys_menu
SET menu_type = 'BUTTON',
    parent_id = @question_menu_id,
    path = NULL,
    component = NULL,
    icon = NULL,
    status = 0
WHERE perms IN (
    'question:list',
    'question:update',
    'question:delete'
);

-- Grant the page and all question permissions to active super administrators.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu
  ON menu.menu_id = @question_menu_id
  OR menu.perms IN (
      'question:list',
      'question:update',
      'question:delete'
  )
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
