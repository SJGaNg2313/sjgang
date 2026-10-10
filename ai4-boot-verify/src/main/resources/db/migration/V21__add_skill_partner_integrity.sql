-- 合伙人增加乐观锁版本号
ALTER TABLE `ai4_partner`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0
        COMMENT '乐观锁版本号'
        AFTER `avatar`;

-- 只限制未删除 Skill 的名称唯一，已删除名称允许重新使用
ALTER TABLE `ai4_skill`
    ADD COLUMN `active_skill_name` VARCHAR(128)
        GENERATED ALWAYS AS (
            CASE WHEN `del_flag` = 1 THEN `skill_name` ELSE NULL END
        ) STORED
        COMMENT '未删除Skill名称唯一键',
    ADD UNIQUE KEY `uk_ai4_skill_active_name` (`active_skill_name`);

-- 只限制未删除合伙人的名称唯一，已删除名称允许重新使用
ALTER TABLE `ai4_partner`
    ADD COLUMN `active_partner_name` VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE WHEN `del_flag` = 1 THEN `partner_name` ELSE NULL END
        ) STORED
        COMMENT '未删除合伙人名称唯一键',
    ADD UNIQUE KEY `uk_ai4_partner_active_name` (`active_partner_name`);
