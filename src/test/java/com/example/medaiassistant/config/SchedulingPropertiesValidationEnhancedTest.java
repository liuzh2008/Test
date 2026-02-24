package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulingProperties 配置验证增强测试类
 * 按照TDD红-绿-重构流程实现配置验证增强
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = SchedulingProperties.class,
    properties = {
        // 禁用不必要的组件
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        "prompt.submission.enabled=false",
        "prompt.polling.enabled=false",
        "monitoring.metrics.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
@EnableConfigurationProperties(SchedulingProperties.class)
@DisplayName("SchedulingProperties 配置验证增强测试")
class SchedulingPropertiesValidationEnhancedTest {

    private SchedulingProperties schedulingProperties;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
    }

    /**
     * 验证线程池配置边界值
     * 当线程池核心线程数为0时应该抛出异常
     */
    @Test
    @DisplayName("当线程池核心线程数为0时应该抛出异常")
    void shouldThrowExceptionWhenThreadPoolCoreSizeIsZero() {
        // 给定：设置无效的线程池配置
        SchedulingProperties.ThreadPoolConfig threadPoolConfig = new SchedulingProperties.ThreadPoolConfig();
        threadPoolConfig.setCorePoolSize(0);
        schedulingProperties.setPromptGenerationPool(threadPoolConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "线程池核心线程数为0时应该抛出异常");
    }

    /**
     * 验证线程池最大线程数小于核心线程数
     * 当线程池最大线程数小于核心线程数时应该抛出异常
     */
    @Test
    @DisplayName("当线程池最大线程数小于核心线程数时应该抛出异常")
    void shouldThrowExceptionWhenMaxPoolSizeLessThanCorePoolSize() {
        // 给定：设置无效的线程池配置
        SchedulingProperties.ThreadPoolConfig threadPoolConfig = new SchedulingProperties.ThreadPoolConfig();
        threadPoolConfig.setCorePoolSize(5);
        threadPoolConfig.setMaxPoolSize(3);
        schedulingProperties.setSurgeryAnalysisPool(threadPoolConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "线程池最大线程数小于核心线程数时应该抛出异常");
    }

    /**
     * 验证队列容量边界值
     * 当线程池队列容量为0时应该抛出异常
     */
    @Test
    @DisplayName("当线程池队列容量为0时应该抛出异常")
    void shouldThrowExceptionWhenQueueCapacityIsZero() {
        // 给定：设置无效的线程池配置
        SchedulingProperties.ThreadPoolConfig threadPoolConfig = new SchedulingProperties.ThreadPoolConfig();
        threadPoolConfig.setQueueCapacity(0);
        schedulingProperties.setPromptGenerationPool(threadPoolConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "线程池队列容量为0时应该抛出异常");
    }

    /**
     * 验证监控指标收集间隔边界值
     * 当监控指标收集间隔小于1000毫秒时应该抛出异常
     */
    @Test
    @DisplayName("当监控指标收集间隔小于1000毫秒时应该抛出异常")
    void shouldThrowExceptionWhenMetricsIntervalTooSmall() {
        // 给定：设置无效的监控配置
        SchedulingProperties.MonitoringConfig monitoringConfig = new SchedulingProperties.MonitoringConfig();
        monitoringConfig.setMetricsInterval(500);
        schedulingProperties.setMonitoring(monitoringConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "监控指标收集间隔小于1000毫秒时应该抛出异常");
    }

    /**
     * 验证定时任务cron表达式格式
     * 当定时任务cron表达式为空时应该抛出异常
     */
    @Test
    @DisplayName("当定时任务cron表达式为空时应该抛出异常")
    void shouldThrowExceptionWhenCronExpressionIsEmpty() {
        // 给定：设置无效的定时任务配置
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDailyTime("");
        schedulingProperties.setTimer(timerConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "定时任务cron表达式为空时应该抛出异常");
    }

    /**
     * 验证时区配置格式
     * 当时区配置为空时应该抛出异常
     */
    @Test
    @DisplayName("当时区配置为空时应该抛出异常")
    void shouldThrowExceptionWhenTimezoneIsEmpty() {
        // 给定：设置无效的自动执行配置
        SchedulingProperties.AutoExecuteConfig autoExecuteConfig = new SchedulingProperties.AutoExecuteConfig();
        autoExecuteConfig.setTimezone("");
        schedulingProperties.setAutoExecute(autoExecuteConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "时区配置为空时应该抛出异常");
    }

    /**
     * 验证执行轮询超时时间边界值
     * 当执行轮询超时时间小于10秒时应该抛出异常
     */
    @Test
    @DisplayName("当执行轮询超时时间小于10秒时应该抛出异常")
    void shouldThrowExceptionWhenPollingTimeoutTooSmall() {
        // 给定：设置无效的执行轮询配置
        SchedulingProperties.ExecutionPollingConfig executionPollingConfig = new SchedulingProperties.ExecutionPollingConfig();
        executionPollingConfig.setTimeoutSeconds(5);
        schedulingProperties.setExecutionPolling(executionPollingConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "执行轮询超时时间小于10秒时应该抛出异常");
    }

    /**
     * 验证线程池配置默认值合理性
     * 线程池配置默认值应该合理
     */
    @Test
    @DisplayName("线程池配置默认值应该合理")
    void threadPoolConfigDefaultValuesShouldBeReasonable() {
        // 给定：创建新的线程池配置
        SchedulingProperties.ThreadPoolConfig threadPoolConfig = new SchedulingProperties.ThreadPoolConfig();
        
        // 当：检查默认值
        // 那么：默认值应该合理
        assertTrue(threadPoolConfig.getCorePoolSize() > 0, "线程池核心线程数默认值应该大于0");
        assertTrue(threadPoolConfig.getMaxPoolSize() >= threadPoolConfig.getCorePoolSize(), 
            "线程池最大线程数默认值应该大于等于核心线程数");
        assertTrue(threadPoolConfig.getQueueCapacity() > 0, "线程池队列容量默认值应该大于0");
        assertNotNull(threadPoolConfig.getThreadNamePrefix(), "线程名称前缀默认值不应该为null");
        assertFalse(threadPoolConfig.getThreadNamePrefix().isEmpty(), "线程名称前缀默认值不应该为空");
    }
}
