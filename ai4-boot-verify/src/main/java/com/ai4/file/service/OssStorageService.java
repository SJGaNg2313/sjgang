package com.ai4.file.service;

import com.ai4.file.config.OssProperties;
import com.ai4.file.dto.UploadFileResponse;
import com.aliyun.oss.OSS;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class OssStorageService {
    private final OssProperties ossProperties;
    private final ObjectProvider<OSS> ossClientProvider;

    public OssStorageService(
            OssProperties ossProperties,
            ObjectProvider<OSS> ossClientProvider
    ) {
        this.ossProperties = ossProperties;
        this.ossClientProvider = ossClientProvider;
    }

    /**
     * 将文件上传至OSS
     */
    public UploadFileResponse upload(MultipartFile file,String directory){
        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("文件不能为空");
        }

        String objectKey = buildObjectKey(directory,file.getOriginalFilename());
        OSS ossClient = ossClientProvider.getObject();

        try(InputStream inputStream = file.getInputStream()){
            ossClient.putObject(ossProperties.bucketName(),objectKey,inputStream);
        }catch(IOException ioException){
            throw new IllegalStateException("上传文件失败",ioException);
        }catch (RuntimeException runtimeException){
            throw new IllegalStateException("上传文件失败",runtimeException);
        }

        String url = ossProperties.publicBaseUrl()
                + "/"
                + objectKey;

        return new UploadFileResponse(objectKey, url);
    }

    /**
     * 生成不会因原始文件重名而相互覆盖的对象路径。
     */
    private String buildObjectKey(String directory, String originalFilename) {
        String extension = getExtension(originalFilename);
        LocalDate today = LocalDate.now();

        return directory
                + "/"
                + today.getYear()
                + "/"
                + String.format("%02d", today.getMonthValue())
                + "/"
                + String.format("%02d", today.getDayOfMonth())
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    //获取文件扩展名
    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int index = originalFilename.lastIndexOf('.');
        return index >= 0 ? originalFilename.substring(index) : "";
    }

}
