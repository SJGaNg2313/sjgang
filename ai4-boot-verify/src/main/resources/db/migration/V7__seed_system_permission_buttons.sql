-- Ensure the built-in super administrator role exists.
INSERT INTO sys_role (role_name, role_code, status, role_sort)
SELECT '超级管理员', 'super_admin', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'super_admin'
);

-- Ensure the system directory and its three management pages exist.
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'DIR', '系统管理', 0, '/system', 'Layout', 1, NULL, 'settings', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/system'
);

SET @system_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/system'
);

UPDATE sys_menu
SET menu_type = 'DIR',
    menu_name = '系统管理',
    parent_id = 0,
    component = 'Layout',
    order_num = 1,
    perms = NULL,
    icon = 'settings'
WHERE menu_id = @system_menu_id;

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '用户管理', @system_menu_id, '/users', 'system/user/index',
       1, NULL, 'user', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/users'
);

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '角色管理', @system_menu_id, '/roles', 'system/role/index',
       2, NULL, 'shield', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/roles'
);

INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'MENU', '菜单管理', @system_menu_id, '/menus', 'system/menu/index',
       3, NULL, 'menu', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE path = '/menus'
);

SET @user_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/users'
);
SET @role_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/roles'
);
SET @menu_menu_id = (
    SELECT MIN(menu_id) FROM sys_menu WHERE path = '/menus'
);

-- Normalize existing pages and remove the legacy user:list permission.
UPDATE sys_menu
SET menu_type = 'MENU', menu_name = '用户管理', parent_id = @system_menu_id,
    component = 'system/user/index', order_num = 1, perms = NULL,
    icon = 'user'
WHERE menu_id = @user_menu_id;

UPDATE sys_menu
SET menu_type = 'MENU', menu_name = '角色管理', parent_id = @system_menu_id,
    component = 'system/role/index', order_num = 2, perms = NULL,
    icon = 'shield'
WHERE menu_id = @role_menu_id;

UPDATE sys_menu
SET menu_type = 'MENU', menu_name = '菜单管理', parent_id = @system_menu_id,
    component = 'system/menu/index', order_num = 3, perms = NULL,
    icon = 'menu'
WHERE menu_id = @menu_menu_id;

-- Seed all built-in button permissions without relying on menu IDs.
INSERT INTO sys_menu (
    menu_type, menu_name, parent_id, path, component,
    order_num, perms, icon, status
)
SELECT 'BUTTON', seed.menu_name, seed.parent_id, NULL, NULL,
       seed.order_num, seed.perms, NULL, 0
FROM (
    SELECT '查询用户' AS menu_name, @user_menu_id AS parent_id, 1 AS order_num,
           'system:user:list' AS perms
    UNION ALL SELECT '新增用户', @user_menu_id, 2, 'system:user:create'
    UNION ALL SELECT '修改用户状态', @user_menu_id, 3, 'system:user:update-status'
    UNION ALL SELECT '重置用户密码', @user_menu_id, 4, 'system:user:reset-password'
    UNION ALL SELECT '分配角色', @user_menu_id, 5, 'system:user:assign-role'
    UNION ALL SELECT '查询角色', @role_menu_id, 1, 'system:role:list'
    UNION ALL SELECT '新增角色', @role_menu_id, 2, 'system:role:create'
    UNION ALL SELECT '修改角色', @role_menu_id, 3, 'system:role:update'
    UNION ALL SELECT '删除角色', @role_menu_id, 4, 'system:role:delete'
    UNION ALL SELECT '分配菜单', @role_menu_id, 5, 'system:role:assign-menu'
    UNION ALL SELECT '查询菜单', @menu_menu_id, 1, 'system:menu:list'
    UNION ALL SELECT '新增菜单', @menu_menu_id, 2, 'system:menu:create'
    UNION ALL SELECT '修改菜单', @menu_menu_id, 3, 'system:menu:update'
    UNION ALL SELECT '删除菜单', @menu_menu_id, 4, 'system:menu:delete'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms
);

-- Explicitly grant all built-in pages and permissions to active super admins.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m
  ON m.menu_id IN (
      @system_menu_id, @user_menu_id, @role_menu_id, @menu_menu_id
  )
  OR m.perms IN (
      'system:user:list',
      'system:user:create',
      'system:user:update-status',
      'system:user:reset-password',
      'system:user:assign-role',
      'system:role:list',
      'system:role:create',
      'system:role:update',
      'system:role:delete',
      'system:role:assign-menu',
      'system:menu:list',
      'system:menu:create',
      'system:menu:update',
      'system:menu:delete'
  )
WHERE r.role_code = 'super_admin'
  AND r.status = 0;
