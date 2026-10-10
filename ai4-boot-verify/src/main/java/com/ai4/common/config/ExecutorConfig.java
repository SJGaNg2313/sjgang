package com.ai4.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 项目共享线程池配置。 */
@Configuration
public class ExecutorConfig {

    /**
     * Coze题目引导共享线程池，限制外部工作流的最大并发量。
     */
    @Bean(name = "cozeGuideExecutor", destroyMethod = "shutdown")
    public ExecutorService cozeGuideExecutor() {
        return new ThreadPoolExecutor(
                10,
                10,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(50),
                new CustomizableThreadFactory("coze-guide-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /** CRM 出站同步共享线程池，避免外部接口调用阻塞业务事务线程。 */
    @Bean(name = "crmCallbackExecutor", destroyMethod = "shutdown")
    public ExecutorService crmCallbackExecutor() {
        return new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new CustomizableThreadFactory("crm-callback-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
