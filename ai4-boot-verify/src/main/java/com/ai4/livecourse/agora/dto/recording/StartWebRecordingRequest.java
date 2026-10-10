package com.ai4.livecourse.agora.dto.recording;

import java.util.List;

/** 启动声网网页录制的请求参数。 */
public record StartWebRecordingRequest(
        String cname,
        String uid,
        ClientRequest clientRequest
) {

    public static StartWebRecordingRequest create(
            String roomId,
            int recordingUid,
            String rtcToken,
            String recordPageUrl,
            int maxRecordingHour,
            StorageConfig storageConfig
    ) {
        ServiceParam serviceParam = new ServiceParam(
                recordPageUrl,
                30,
                0,
                1280,
                720,
                maxRecordingHour,
                true,
                true,
                0
        );
        ExtensionService extensionService = new ExtensionService(
                "web_recorder_service",
                "error_abort",
                serviceParam
        );
        ClientRequest clientRequest = new ClientRequest(
                rtcToken,
                new ExtensionServiceConfig("error_abort", List.of(extensionService)),
                new RecordingFileConfig(List.of("hls", "mp4")),
                storageConfig
        );
        return new StartWebRecordingRequest(roomId, Integer.toString(recordingUid), clientRequest);
    }

    public record ClientRequest(
            String token,
            ExtensionServiceConfig extensionServiceConfig,
            RecordingFileConfig recordingFileConfig,
            StorageConfig storageConfig
    ) {
    }

    public record ExtensionServiceConfig(
            String errorHandlePolicy,
            List<ExtensionService> extensionServices
    ) {
    }

    public record ExtensionService(
            String serviceName,
            String errorHandlePolicy,
            ServiceParam serviceParam
    ) {
    }

    public record ServiceParam(
            String url,
            int videoFps,
            int audioProfile,
            int videoWidth,
            int videoHeight,
            int maxRecordingHour,
            boolean enableWebGL,
            boolean onhold,
            int readyTimeout
    ) {
    }

    public record RecordingFileConfig(List<String> avFileType) {
    }

    public record StorageConfig(
            int vendor,
            int region,
            String bucket,
            String accessKey,
            String secretKey,
            List<String> fileNamePrefix
    ) {

        public static StorageConfig aliyun(
                int region,
                String bucket,
                String accessKey,
                String secretKey
        ) {
            return new StorageConfig(
                    2,
                    region,
                    bucket,
                    accessKey,
                    secretKey,
                    List.of("videorecord", "mp4")
            );
        }
    }
}
