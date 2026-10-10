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

-- 创造字典管理权限页
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '字典管理', @system_menu_id, '/dicts',
       'system/dict/index', 4, NULL, 'book-open', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/dicts'
);

SET @dict_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/dicts'
);

-- 将临时页面规范化。前端可能会在稍后更新路径和组件。
UPDATE sys_menu
SET menu_type = 'MENU',
    menu_name = '字典管理',
    parent_id = @system_menu_id,
    component = 'system/dict/index',
    order_num = 4,
    perms = NULL,
    icon = 'book-open',
    status = 0
WHERE menu_id = @dict_menu_id;

-- 创建字典按钮的权限。
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @dict_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询字典' AS menu_name, 1 AS order_num,
           'system:dict:list' AS perms
    UNION ALL
    SELECT '新增字典', 2, 'system:dict:create'
    UNION ALL
    SELECT '修改字典', 3, 'system:dict:update'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.perms = seed.perms
);

-- 规范现有字典的权限
UPDATE sys_menu
SET menu_type = 'BUTTON',
    parent_id = @dict_menu_id,
    path = NULL,
    component = NULL,
    icon = NULL,
    status = 0
WHERE perms IN (
    'system:dict:list',
    'system:dict:create',
    'system:dict:update'
);

-- 授权给super_admin
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu
  ON menu.menu_id = @dict_menu_id
  OR menu.perms IN (
      'system:dict:list',
      'system:dict:create',
      'system:dict:update'
  )
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
