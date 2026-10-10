package com.ai4.livecourse.agora.exception;

/** 调用声网 HTTP 服务失败。 */
public class AgoraClientException extends RuntimeException {

    private final String operation;
    private final Integer httpStatus;
    private final String requestId;

    public AgoraClientException(
            String operation,
            Integer httpStatus,
            String requestId,
            Throwable cause
    ) {
        super(buildMessage(operation, httpStatus, requestId), cause);
        this.operation = operation;
        this.httpStatus = httpStatus;
        this.requestId = requestId;
    }

    public AgoraClientException(String operation, String message) {
        super(message);
        this.operation = operation;
        this.httpStatus = null;
        this.requestId = null;
    }

    public String operation() {
        return operation;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String requestId() {
        return requestId;
    }

    private static String buildMessage(
            String operation,
            Integer httpStatus,
            String requestId
    ) {
        String statusText = httpStatus == null ? "无HTTP状态" : "HTTP " + httpStatus;
        String requestText = requestId == null || requestId.isBlank()
                ? ""
                : "，requestId=" + requestId;
        return "声网请求失败：" + operation + "，" + statusText + requestText;
    }
}
