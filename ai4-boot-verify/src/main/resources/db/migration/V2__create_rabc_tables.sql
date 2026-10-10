CREATE TABLE `sys_user` (
                            `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
                            `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
                            `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
                            `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1禁用',
                            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`user_id`),
                            UNIQUE KEY `uk_sys_user_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `sys_role` (
                            `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
                            `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
                            `role_code` VARCHAR(50) NOT NULL COMMENT '角色权限字符串',
                            `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1禁用',
                            `role_sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
                            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`role_id`),
                            UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `sys_menu` (
                            `menu_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单 ID',
                            `menu_type` VARCHAR(20) NOT NULL COMMENT '菜单类型：DIR目录，MENU菜单，BUTTON按钮',
                            `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
                            `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单 ID',
                            `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
                            `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
                            `order_num` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
                            `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
                            `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
                            `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常，1禁用',
                            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`menu_id`),
                            KEY `idx_sys_menu_parent_id` (`parent_id`),
                            KEY `idx_sys_menu_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

CREATE TABLE `sys_user_role` (
                                 `user_id` BIGINT NOT NULL COMMENT '用户 ID',
                                 `role_id` BIGINT NOT NULL COMMENT '角色 ID',
                                 PRIMARY KEY (`user_id`, `role_id`),
                                 KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `sys_role_menu` (
                                 `role_id` BIGINT NOT NULL COMMENT '角色 ID',
                                 `menu_id` BIGINT NOT NULL COMMENT '菜单 ID',
                                 PRIMARY KEY (`role_id`, `menu_id`),
                                 KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';
