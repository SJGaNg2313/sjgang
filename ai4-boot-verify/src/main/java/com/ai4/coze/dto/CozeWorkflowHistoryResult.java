package com.ai4.coze.dto;

/**
 * Coze 异步工作流执行状态和输出。
 */
public record CozeWorkflowHistoryResult(
        String executeId,
        String status,
        String output,
        String errorCode,
        String errorMessage,
        String debugUrl
) {
}
