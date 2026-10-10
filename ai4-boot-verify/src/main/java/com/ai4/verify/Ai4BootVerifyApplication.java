package com.ai4.verify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI4 后台管理后端的启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.ai4")
@ConfigurationPropertiesScan(basePackages = "com.ai4")
@EnableScheduling
public class Ai4BootVerifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(Ai4BootVerifyApplication.class, args);
    }

}
