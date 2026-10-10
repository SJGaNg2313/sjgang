CREATE TABLE `ai4_question_bank` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `question_type` VARCHAR(32) DEFAULT NULL COMMENT 'single_choice:单选题,其他题型待定',
    `content` TEXT DEFAULT NULL COMMENT '题干信息',
    `image_urls` JSON DEFAULT NULL COMMENT '题目用图',
    `del_flag` INT NOT NULL DEFAULT 1 COMMENT '逻辑删除，1=正常，2=删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai4_question_bank_type` (`question_type`),
    KEY `idx_ai4_question_bank_del_flag` (`del_flag`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='[AI4]题库';

CREATE TABLE `ai4_question_choice` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `question_id` VARCHAR(64) NOT NULL COMMENT '题目ID',
    `option_key` VARCHAR(255) DEFAULT NULL COMMENT '题目标识',
    `option_value` JSON DEFAULT NULL COMMENT '选项值',
    `correct` INT NOT NULL DEFAULT 2 COMMENT '是否为正确答案，1=是，2=否',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_ai4_question_choice_question_id` (`question_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin
  COMMENT='[AI4]题目选项';
