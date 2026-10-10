-- 新增关于选项影响以及答题前后提示词字段
ALTER TABLE `ai4_question_bank`
    MODIFY COLUMN `question_type` VARCHAR(32) NOT NULL
        COMMENT '题目类型：single_choice=单选题，multiple_choice=多选题',
    ADD COLUMN `pre_answer_guidance` TEXT DEFAULT NULL
        COMMENT '答题前引导词'
        AFTER `image_urls`,
    ADD COLUMN `post_answer_guidance` TEXT DEFAULT NULL
        COMMENT '答题后引导词'
        AFTER `pre_answer_guidance`;

ALTER TABLE `ai4_question_choice`
    ADD COLUMN `effect_text` TEXT DEFAULT NULL
        COMMENT '选择该选项后的影响'
        AFTER `correct`,
    ADD CONSTRAINT `chk_ai4_question_choice_option_key`
          CHECK (`option_key` IN ('A', 'B', 'C', 'D')),
    ADD UNIQUE KEY `uk_ai4_question_choice_question_option`(
        `question_id`,
        `option_key`
    );
