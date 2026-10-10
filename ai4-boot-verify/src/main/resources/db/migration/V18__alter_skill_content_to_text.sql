-- Skill 内容改为普通长文本，不再要求 JSON 格式
ALTER TABLE `ai4_skill`
    MODIFY COLUMN `skill_content` TEXT NOT NULL
        COMMENT '技能介绍';
