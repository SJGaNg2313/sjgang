package com.ai4.file.dto;

/**
 * 文件上传结果
 * @param objectKey OSS中的对象路径
 * @param url 文件访问URL
 */
public record UploadFileResponse (
        String objectKey,
        String url
){
}
