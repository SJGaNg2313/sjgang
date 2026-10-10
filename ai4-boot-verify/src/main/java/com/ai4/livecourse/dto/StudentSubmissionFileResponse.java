package com.ai4.livecourse.dto;

/**
 * 学员项目提交中的文件。
 */
public record StudentSubmissionFileResponse(
        String fileName,
        String fileUrl
) {
}
