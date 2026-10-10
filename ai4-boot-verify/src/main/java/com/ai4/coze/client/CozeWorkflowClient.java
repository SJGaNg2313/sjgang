package com.ai4.coze.client;

import com.ai4.coze.config.CozeProperties;
import com.ai4.coze.dto.CozeWorkflowHistoryResult;
import com.ai4.coze.dto.CozeWorkflowRunResult;
import com.coze.openapi.client.workflows.run.RetrieveRunHistoryReq;
import com.coze.openapi.client.workflows.run.RetrieveRunHistoryResp;
import com.coze.openapi.client.workflows.run.RunWorkflowReq;
import com.coze.openapi.client.workflows.run.RunWorkflowResp;
import com.coze.openapi.client.workflows.run.model.WorkflowRunHistory;
import com.coze.openapi.service.service.CozeAPI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 封装 Coze 工作流的同步、异步提交和异步结果查询。
 */
@Component
@ConditionalOnProperty(prefix = "ai4.coze", name = "enabled", havingValue = "true")
public class CozeWorkflowClient {

    private final CozeAPI cozeApi;
    private final int timeoutMillis;

    public CozeWorkflowClient(CozeAPI cozeApi, CozeProperties properties) {
        this.cozeApi = cozeApi;
        this.timeoutMillis = Math.multiplyExact(properties.timeoutSeconds(), 1000);
    }

    /**
     * 同步执行工作流，接口直接等待最终输出。
     */
    public CozeWorkflowRunResult runSync(
            String workflowId,
            Map<String, Object> parameters
    ) {
        return run(workflowId, parameters, false);
    }

    /**
     * 异步提交工作流，成功后通过 executeId 查询执行状态。
     */
    public CozeWorkflowRunResult runAsync(
            String workflowId,
            Map<String, Object> parameters
    ) {
        return run(workflowId, parameters, true);
    }

    /**
     * 查询异步工作流的最新执行状态和最终输出。
     */
    public CozeWorkflowHistoryResult getRunHistory(
            String workflowId,
            String executeId
    ) {
        requireText(workflowId, "workflowId 不能为空");
        requireText(executeId, "executeId 不能为空");

        RetrieveRunHistoryReq request = RetrieveRunHistoryReq.of(
                workflowId.trim(),
                executeId.trim()
        );
        applyTimeout(request);

        RetrieveRunHistoryResp response = cozeApi.workflows()
                .runs()
                .histories()
                .retrieve(request);

        List<WorkflowRunHistory> histories = response == null
                ? null
                : response.getHistories();
        if (histories == null || histories.isEmpty()) {
            throw new IllegalStateException("Coze 未返回工作流执行记录");
        }

        WorkflowRunHistory history = histories.get(0);
        String status = history.getExecuteStatus() == null
                ? null
                : history.getExecuteStatus().getValue();
        return new CozeWorkflowHistoryResult(
                history.getExecuteID(),
                status,
                history.getOutput(),
                history.getErrorCode(),
                history.getErrorMessage(),
                history.getDebugUrl()
        );
    }

    private CozeWorkflowRunResult run(
            String workflowId,
            Map<String, Object> parameters,
            boolean async
    ) {
        requireText(workflowId, "workflowId 不能为空");

        RunWorkflowReq request = new RunWorkflowReq();
        request.setWorkflowID(workflowId.trim());
        request.setParameters(parameters == null ? Map.of() : parameters);
        request.setIsAsync(async);
        applyTimeout(request);

        RunWorkflowResp response = cozeApi.workflows().runs().create(request);
        if (response == null) {
            throw new IllegalStateException("Coze 返回空响应");
        }
        if (response.getCode() != null && response.getCode() != 0) {
            throw new IllegalStateException(
                    "Coze 工作流执行失败，code=" + response.getCode()
                            + "，message=" + response.getMsg()
            );
        }
        if (async && !StringUtils.hasText(response.getExecuteID())) {
            throw new IllegalStateException("Coze 异步工作流未返回 executeId");
        }

        return new CozeWorkflowRunResult(
                response.getExecuteID(),
                response.getData(),
                response.getDebugURL()
        );
    }

    private void applyTimeout(com.coze.openapi.client.common.BaseReq request) {
        request.setConnectTimeout(timeoutMillis);
        request.setReadTimeout(timeoutMillis);
        request.setWriteTimeout(timeoutMillis);
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
