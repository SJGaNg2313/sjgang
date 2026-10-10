CREATE TABLE `ai4_partner`(
                              `id` VARCHAR(64) NOT NULL COMMENT '主键id',
                              `partner_name` VARCHAR(64) NOT NULL COMMENT '合伙人名称',
                              `partner_image` VARCHAR(255) NOT NULL COMMENT '合伙人外观',
                              `skill_name` JSON NOT NULL COMMENT '合伙人技能名称',
                              `del_flag` INT NOT NULL DEFAULT 1 COMMENT '逻辑删除，1=正常，2=删除',
                              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`)
)ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='[AI4]合伙人';

CREATE TABLE `ai4_partner_skill`(
                                    `id` VARCHAR(64) NOT NULL COMMENT '主键id',
                                    `partner_id` VARCHAR(64) NOT NULL COMMENT '合伙人id',
                                    `skill_content` JSON NOT NULL COMMENT '技能内容',
                                    `skill_prompt` VARCHAR(255) NOT NULL COMMENT '技能提示词',
                                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_ai4_partner_partner_id` (`partner_id`)
)ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='[AI4]合伙人-技能';
