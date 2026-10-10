-- 确保系统管理目录存在
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

-- 创建临时 Skill 管理页面，前端确定路由后可以修改路径和组件
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', 'Skill管理', @system_menu_id, '/skills',
       'skill/index', 6, NULL, 'sparkles', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/skills'
);

SET @skill_menu_id = (
    SELECT MIN(menu_id)
    FROM sys_menu
    WHERE path = '/skills'
);

-- 规范可能已由管理员手动创建的 Skill 页面
UPDATE sys_menu
SET menu_type = 'MENU',
    menu_name = 'Skill管理',
    parent_id = @system_menu_id,
    component = 'skill/index',
    order_num = 6,
    perms = NULL,
    icon = 'sparkles',
    status = 0
WHERE menu_id = @skill_menu_id;

-- 创建 Skill 操作权限
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @skill_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询Skill' AS menu_name, 1 AS order_num,
           'skill:list' AS perms
    UNION ALL
    SELECT '新增Skill', 2, 'skill:create'
    UNION ALL
    SELECT '修改Skill', 3, 'skill:update'
    UNION ALL
    SELECT '删除Skill', 4, 'skill:delete'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.perms = seed.perms
);

-- 规范可能已经存在的 Skill 按钮权限
UPDATE sys_menu
SET menu_type = 'BUTTON',
    parent_id = @skill_menu_id,
    path = NULL,
    component = NULL,
    icon = NULL,
    status = 0
WHERE perms IN (
    'skill:list',
    'skill:create',
    'skill:update',
    'skill:delete'
);

-- 将 Skill 页面和操作权限授权给当前启用的超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu
  ON menu.menu_id = @skill_menu_id
  OR menu.perms IN (
      'skill:list',
      'skill:create',
      'skill:update',
      'skill:delete'
  )
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
