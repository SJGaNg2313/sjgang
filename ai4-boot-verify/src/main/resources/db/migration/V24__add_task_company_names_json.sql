ALTER TABLE `ai4_project_task`
    ADD COLUMN `company_names` JSON DEFAULT NULL
        COMMENT 'AI生成的60个公司名称候选，重新生成时整体替换'
        AFTER `company_name`;
