package com.example.medaiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 * 
 * 配置专用线程池，为手术分析任务提供独立的执行环境
 * 避免任务间相互影响，提高系统稳定性
 * 
 * @author Cline
 * @since 2025-09-27
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    private final SchedulingProperties schedulingProperties;
    
    public AsyncConfig(SchedulingProperties schedulingProperties) {
        this.schedulingProperties = schedulingProperties;
    }
    
    /**
     * Prompt生成专用线程池
     * 使用配置化的参数，避免硬编码
     * 拒绝策略：CallerRunsPolicy（由调用线程执行）
     * 
     * @return Prompt生成专用线程池
     */
    @Bean("promptGenerationExecutor")
    public TaskExecutor promptGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 使用配置化的参数
        executor.setCorePoolSize(schedulingProperties.getPromptGenerationPool().getCorePoolSize());
        executor.setMaxPoolSize(schedulingProperties.getPromptGenerationPool().getMaxPoolSize());
        executor.setQueueCapacity(schedulingProperties.getPromptGenerationPool().getQueueCapacity());
        
        // 线程配置
        executor.setThreadNamePrefix(schedulingProperties.getPromptGenerationPool().getThreadNamePrefix() + "prompt-");
        executor.setDaemon(true);
        
        // 拒绝策略：当队列满时，由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 手术分析专用线程池
     * 使用配置化的参数，避免硬编码
     * 拒绝策略：CallerRunsPolicy（由调用线程执行）
     * 
     * @return 手术分析专用线程池
     */
    @Bean("surgeryAnalysisExecutor")
    public TaskExecutor surgeryAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 使用配置化的参数
        executor.setCorePoolSize(schedulingProperties.getSurgeryAnalysisPool().getCorePoolSize());
        executor.setMaxPoolSize(schedulingProperties.getSurgeryAnalysisPool().getMaxPoolSize());
        executor.setQueueCapacity(schedulingProperties.getSurgeryAnalysisPool().getQueueCapacity());
        
        // 线程配置
        executor.setThreadNamePrefix(schedulingProperties.getSurgeryAnalysisPool().getThreadNamePrefix() + "surgery-");
        executor.setDaemon(true);
        
        // 拒绝策略：当队列满时，由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 通用异步执行线程池
     * 用于其他异步任务，避免与手术分析任务竞争资源
     * 
     * @return 通用异步线程池
     */
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 使用统一配置
        executor.setCorePoolSize(schedulingProperties.getExecutor().getCorePoolSize());
        executor.setMaxPoolSize(schedulingProperties.getExecutor().getMaxPoolSize());
        executor.setQueueCapacity(schedulingProperties.getExecutor().getQueueCapacity());
        
        // 线程配置
        executor.setThreadNamePrefix("async-task-");
        executor.setDaemon(true);
        
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 监控线程池
     * 用于系统监控和健康检查任务
     * 
     * @return 监控线程池
     */
    @Bean("monitoringExecutor")
    public TaskExecutor monitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 监控任务通常较少，配置较小的线程池
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(10);
        
        // 线程配置
        executor.setThreadNamePrefix("monitoring-");
        executor.setDaemon(true);
        
        // 拒绝策略：直接丢弃，监控任务可以容忍丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        executor.initialize();
        return executor;
    }
}
