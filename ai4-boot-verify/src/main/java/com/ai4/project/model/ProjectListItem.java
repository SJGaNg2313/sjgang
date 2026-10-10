package com.ai4.project.model;

/** 项目列表及其四个业务模块的整体配置状态。 */
public record ProjectListItem(
        Long id,
        String projectName,
        Integer sort,
        String configurationStatus
) {
}
