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
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/system'
);

-- 路由地址和组件路径为临时值，前端确认后可以修改
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '合伙人管理', @system_menu_id, '/partners',
       'partner/index', 7, NULL, 'users', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/partners'
);

SET @partner_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/partners'
);

UPDATE sys_menu
SET menu_type = 'MENU',
    menu_name = '合伙人管理',
    parent_id = @system_menu_id,
    component = 'partner/index',
    order_num = 7,
    perms = NULL,
    icon = 'users',
    status = 0
WHERE menu_id = @partner_menu_id;

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, @partner_menu_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询合伙人' AS menu_name, 1 AS order_num,
           'partner:list' AS perms
    UNION ALL
    SELECT '新增合伙人', 2, 'partner:create'
    UNION ALL
    SELECT '修改合伙人', 3, 'partner:update'
    UNION ALL
    SELECT '删除合伙人', 4, 'partner:delete'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms
);

UPDATE sys_menu
SET menu_type = 'BUTTON',
    parent_id = @partner_menu_id,
    path = NULL,
    component = NULL,
    icon = NULL,
    status = 0
WHERE perms IN (
    'partner:list', 'partner:create', 'partner:update', 'partner:delete'
);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu
  ON menu.menu_id = @partner_menu_id
  OR menu.perms IN (
      'partner:list', 'partner:create', 'partner:update', 'partner:delete'
  )
WHERE role.role_code = 'super_admin'
  AND role.status = 0;
