-- 修改合伙人表
ALTER TABLE `ai4_partner`
        DROP COLUMN `partner_image`,
        ADD COLUMN `title` VARCHAR(32) NOT NULL
             COMMENT '头衔'
             AFTER `partner_name`,
        ADD COLUMN `declaration` VARCHAR(32) NOT NULL
             COMMENT '宣言'
             AFTER `title`,
        ADD COLUMN `opening_message` TEXT NOT NULL
             COMMENT '初始开场白'
             AFTER `declaration`,
        ADD COLUMN `theme_color` VARCHAR(32) NOT NULL
          COMMENT '主题色，例如#1677FF'
          AFTER `opening_message`,
        ADD COLUMN `large` VARCHAR(1024) DEFAULT NULL
            COMMENT '形象大图'
            AFTER `theme_color`,
        ADD COLUMN `medium` VARCHAR(1024) DEFAULT NULL
            COMMENT '形象中图'
             AFTER `large`,
        ADD COLUMN `small` VARCHAR(1024) DEFAULT NULL
            COMMENT '形象小图'
            AFTER `medium`,
        ADD COLUMN `full_body` VARCHAR(1024) DEFAULT NULL
            COMMENT '全身图'
            AFTER `small`,
        ADD COLUMN `avatar` VARCHAR(1024) DEFAULT NULL
            COMMENT '头像'
            AFTER `full_body`;

-- 修改skill表
ALTER TABLE `ai4_skill`
        DROP COLUMN `skill_prompt`,
        ADD COLUMN `skill_icon` VARCHAR(255)
             COMMENT '图标'
             AFTER `skill_content`,
        ADD COLUMN `trigger_conditions` TEXT NOT NULL
             COMMENT '触发条件'
             AFTER `skill_icon`,
        ADD COLUMN `input_parameters` TEXT NOT NULL
             COMMENT '输入参数'
             AFTER `trigger_conditions`,
        ADD COLUMN `follow_step` TEXT NOT NULL
             COMMENT '执行步骤'
             AFTER `input_parameters`,
        ADD COLUMN `output_format` TEXT NOT NULL
             COMMENT '输出格式'
             AFTER `follow_step`,
        ADD COLUMN `constraints` TEXT NOT NULL
             COMMENT '约束'
             AFTER `output_format`;
