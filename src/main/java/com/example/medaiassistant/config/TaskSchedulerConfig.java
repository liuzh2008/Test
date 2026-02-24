package com.example.medaiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {
    
    private final SchedulingProperties schedulingProperties;

    public TaskSchedulerConfig(SchedulingProperties schedulingProperties) {
        this.schedulingProperties = schedulingProperties;
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        
        // 使用统一配置
        taskScheduler.setPoolSize(schedulingProperties.getExecutor().getPoolSize());
        taskScheduler.setThreadNamePrefix(schedulingProperties.getExecutor().getThreadNamePrefix());
        taskScheduler.setDaemon(true); // 设置为守护线程
        
        taskScheduler.initialize();
        return taskScheduler;
    }
}
