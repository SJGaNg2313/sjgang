-- 合伙人不在保存技能名称
ALTER TABLE `ai4_partner` DROP COLUMN `skill_name`,
ADD KEY `idx_ai4_partner_del_flag`(`del_flag`);

-- 原合伙人技能表调整为单独技能表
RENAME TABLE `ai4_partner_skill` TO `ai4_skill`;

-- 技能不再直接属于某一个合伙人
ALTER TABLE `ai4_skill`
DROP INDEX `idx_ai4_partner_partner_id`,
DROP COLUMN `partner_id`,
      ADD COLUMN `skill_name` VARCHAR(128) NOT NULL
          COMMENT '技能名称'
          AFTER `id`,
      MODIFY COLUMN `skill_prompt` TEXT NOT NULL
          COMMENT '技能提示词',
      ADD COLUMN `del_flag` INT NOT NULL DEFAULT 1
          COMMENT '逻辑删除，1=正常，2=删除'
          AFTER `skill_prompt`,
      ADD KEY `idx_ai4_skill_del_flag` (`del_flag`);

-- 合伙人技能关联表
CREATE TABLE `ai4_partner_skill_relation`(
    `partner_id` VARCHAR(64) NOT NULL COMMENT '合伙人id',
    `skill_id` VARCHAR(64) NOT NULL COMMENT '技能id',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '技能在当前合伙人下的排序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`partner_id`,`skill_id`),
    KEY `idx_ai4_partner_skill_rel_skill_id` (`skill_id`)
)ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_bin
COMMENT='[AI4]合伙人技能关联表';
