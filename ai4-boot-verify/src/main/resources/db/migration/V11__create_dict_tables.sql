CREATE TABLE `sys_dict_type`(
    `id` VARCHAR(64) NOT NULL COMMENT '主键id',
    `dict_name` VARCHAR(100) NOT NULL COMMENT '字典名称',
    `dict_code` VARCHAR(100) NOT NULL COMMENT '字典编码',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态 0=启用 1=禁用',
    `remark` VARCHAR(255) NOT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_type_code` (`dict_code`),
    KEY `idx_sys_dict_type_status` (`status`)
)ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_bin
COMMENT='字典类型';

CREATE TABLE `sys_dict_item` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `type_id` VARCHAR(64) NOT NULL COMMENT '字典类型ID',
    `item_label` VARCHAR(100) NOT NULL COMMENT '字典项显示名称',
    `item_value` VARCHAR(500) NOT NULL COMMENT '字典项值',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态，0=启用，1=禁用',
    `extra` JSON DEFAULT NULL COMMENT '扩展信息',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_item_value` (`type_id`, `item_value`),
    KEY `idx_sys_dict_item_query` (`type_id`, `status`, `sort_order`)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_bin
COMMENT='字典项';
