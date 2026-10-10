package com.ai4.file.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** OSS客户端配置。 */
@Configuration
public class OssClientConfiguration {

    /**
     * 延迟创建并复用OSS客户端，应用关闭时由Spring统一释放。
     */
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public OSS ossClient(OssProperties properties) {
        return new OSSClientBuilder().build(
                properties.endpoint(),
                properties.accessKeyId(),
                properties.accessKeySecret()
        );
    }
}
