package com.ai4.livecourse.agora.dto.recording;

import jakarta.validation.constraints.NotBlank;

/** 录制网页暂停、继续和心跳使用的请求参数。 */
public record VideoRecordUpdateRequest(
        @NotBlank(message = "房间号不能为空")
        String roomId,

        @NotBlank(message = "录制密钥不能为空")
        String secretKey,

        Integer pause
) {
}
