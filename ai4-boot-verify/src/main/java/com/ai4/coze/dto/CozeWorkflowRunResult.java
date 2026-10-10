package com.ai4.coze.dto;

/**
 * Coze 工作流提交结果。
 */
public record CozeWorkflowRunResult(
        String executeId,
        String data,
        String debugUrl
) {
}
