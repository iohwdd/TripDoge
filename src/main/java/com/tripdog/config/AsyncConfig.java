package com.tripdog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 自定义 @Async 线程池
     */
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("async-task-");

        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);

        // 等待所有任务完成的时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 是否等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 拒绝策略：CallerRunsPolicy（调用方线程执行）
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();
        return executor;
    }
}
