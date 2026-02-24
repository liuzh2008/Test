package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulingProperties TDD测试类
 * 按照TDD红-绿-重构流程实现配置类开发
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = SchedulingProperties.class,
    properties = {
        // 测试配置属性
        "scheduling.timer.dailyTime=0 0 8 * * *",
        "scheduling.timer.maxConcurrency=3",
        "scheduling.timer.enabled=true",
        "scheduling.auto-execute.interval=3000",
        "scheduling.auto-execute.maxThreads=5",
        "scheduling.execution-polling.maxConcurrency=2",
        "scheduling.execution-polling.timeoutSeconds=600",
        
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
@DisplayName("SchedulingProperties TDD测试")
class SchedulingPropertiesTddTest {

    private SchedulingProperties schedulingProperties;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
    }

    /**
     * 测试定时任务配置默认值
     */
    @Test
    @DisplayName("应该创建定时任务配置并设置默认值")
    void shouldCreateTimerConfigWithDefaultValues() {
        // 给定：创建新的定时任务配置
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        
        // 当：检查默认值
        // 那么：应该设置正确的默认值
        assertNotNull(timerConfig, "定时任务配置不应该为null");
        assertEquals("0 0 7 * * *", timerConfig.getDailyTime(), "每日任务执行时间默认值应该正确");
        assertEquals(5, timerConfig.getMaxConcurrency(), "最大并发数默认值应该正确");
        assertTrue(timerConfig.isEnabled(), "定时任务应该默认启用");
        assertFalse(timerConfig.isDepartmentFilterEnabled(), "科室过滤应该默认禁用");
        assertTrue(timerConfig.getTargetDepartments().isEmpty(), "目标科室列表应该默认为空");
    }

    /**
     * 测试自动执行配置默认值
     */
    @Test
    @DisplayName("应该创建自动执行配置并设置默认值")
    void shouldCreateAutoExecuteConfigWithDefaultValues() {
        // 给定：创建新的自动执行配置
        SchedulingProperties.AutoExecuteConfig autoExecuteConfig = new SchedulingProperties.AutoExecuteConfig();
        
        // 当：检查默认值
        // 那么：应该设置正确的默认值
        assertNotNull(autoExecuteConfig, "自动执行配置不应该为null");
        assertEquals(5000L, autoExecuteConfig.getInterval(), "执行间隔默认值应该正确");
        assertEquals(10, autoExecuteConfig.getMaxThreads(), "最大线程数默认值应该正确");
        assertTrue(autoExecuteConfig.isEnabled(), "自动执行应该默认启用");
        assertEquals("Asia/Shanghai", autoExecuteConfig.getTimezone(), "时区默认值应该正确");
    }

    /**
     * 测试执行轮询配置默认值
     */
    @Test
    @DisplayName("应该创建执行轮询配置并设置默认值")
    void shouldCreateExecutionPollingConfigWithDefaultValues() {
        // 给定：创建新的执行轮询配置
        SchedulingProperties.ExecutionPollingConfig executionPollingConfig = new SchedulingProperties.ExecutionPollingConfig();
        
        // 当：检查默认值
        // 那么：应该设置正确的默认值
        assertNotNull(executionPollingConfig, "执行轮询配置不应该为null");
        assertEquals(5, executionPollingConfig.getMaxConcurrency(), "最大并发数默认值应该正确");
        assertEquals(900, executionPollingConfig.getTimeoutSeconds(), "超时时间默认值应该正确");
        assertTrue(executionPollingConfig.isConcurrentEnabled(), "并发轮询应该默认启用");
        assertEquals(3, executionPollingConfig.getMaxRetryCount(), "最大重试次数默认值应该正确");
    }

    /**
     * 测试配置验证逻辑
     */
    @Test
    @DisplayName("当定时任务最大并发数为0时应该抛出异常")
    void shouldThrowExceptionWhenTimerMaxConcurrencyIsZero() {
        // 给定：设置无效的定时任务配置
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setMaxConcurrency(0);
        schedulingProperties.setTimer(timerConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "定时任务最大并发数为0时应该抛出异常");
    }

    /**
     * 测试配置摘要功能
     */
    @Test
    @DisplayName("应该生成配置摘要信息")
    void shouldGenerateConfigurationSummary() {
        // 给定：设置配置属性
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDailyTime("0 0 8 * * *");
        timerConfig.setMaxConcurrency(3);
        schedulingProperties.setTimer(timerConfig);
        
        // 当：生成配置摘要
        // 那么：应该返回正确的摘要信息
        String summary = schedulingProperties.getConfigurationSummary();
        assertNotNull(summary, "配置摘要不应该为null");
        assertTrue(summary.contains("定时任务"), "配置摘要应该包含定时任务信息");
        assertTrue(summary.contains("0 0 8 * * *"), "配置摘要应该包含执行时间");
        assertTrue(summary.contains("最大并发数: 3"), "配置摘要应该包含最大并发数");
    }

    /**
     * 测试线程池监控配置默认值
     */
    @Test
    @DisplayName("应该创建线程池监控配置并设置默认值")
    void shouldCreateThreadPoolMonitorConfigWithDefaultValues() {
        // 给定：创建新的监控配置
        SchedulingProperties.MonitoringConfig monitoringConfig = new SchedulingProperties.MonitoringConfig();
        
        // 当：检查默认值
        // 那么：应该设置正确的默认值
        assertNotNull(monitoringConfig, "监控配置不应该为null");
        assertTrue(monitoringConfig.isEnabled(), "监控应该默认启用");
        assertEquals(60000, monitoringConfig.getMetricsInterval(), "指标收集间隔默认值应该正确");
        assertEquals(80, monitoringConfig.getWarningThreshold(), "警告阈值默认值应该正确");
        assertEquals(95, monitoringConfig.getCriticalThreshold(), "严重警告阈值默认值应该正确");
        assertTrue(monitoringConfig.getAlertEmails().isEmpty(), "告警邮箱列表应该默认为空");
    }

    /**
     * 测试边界条件 - 监控阈值验证
     */
    @Test
    @DisplayName("当监控警告阈值大于等于严重警告阈值时应该抛出异常")
    void shouldThrowExceptionWhenWarningThresholdGreaterThanCritical() {
        // 给定：设置无效的监控配置
        SchedulingProperties.MonitoringConfig monitoringConfig = new SchedulingProperties.MonitoringConfig();
        monitoringConfig.setWarningThreshold(90);
        monitoringConfig.setCriticalThreshold(90);
        schedulingProperties.setMonitoring(monitoringConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "监控警告阈值大于等于严重警告阈值时应该抛出异常");
    }

    /**
     * 测试边界条件 - 自动执行间隔验证
     */
    @Test
    @DisplayName("当自动执行间隔为0时应该抛出异常")
    void shouldThrowExceptionWhenAutoExecuteIntervalIsZero() {
        // 给定：设置无效的自动执行配置
        SchedulingProperties.AutoExecuteConfig autoExecuteConfig = new SchedulingProperties.AutoExecuteConfig();
        autoExecuteConfig.setInterval(0);
        schedulingProperties.setAutoExecute(autoExecuteConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "自动执行间隔为0时应该抛出异常");
    }

    /**
     * 测试边界条件 - 执行轮询重试次数验证
     */
    @Test
    @DisplayName("当执行轮询最大重试次数为负数时应该抛出异常")
    void shouldThrowExceptionWhenExecutionPollingMaxRetryCountIsNegative() {
        // 给定：设置无效的执行轮询配置
        SchedulingProperties.ExecutionPollingConfig executionPollingConfig = new SchedulingProperties.ExecutionPollingConfig();
        executionPollingConfig.setMaxRetryCount(-1);
        schedulingProperties.setExecutionPolling(executionPollingConfig);
        
        // 当：验证配置
        // 那么：应该抛出异常
        assertThrows(IllegalStateException.class, 
            () -> schedulingProperties.validateConfiguration(),
            "执行轮询最大重试次数为负数时应该抛出异常");
    }

    /**
     * 测试配置摘要包含所有配置信息
     */
    @Test
    @DisplayName("配置摘要应该包含所有配置信息")
    void configurationSummaryShouldContainAllConfigurationInformation() {
        // 给定：设置完整的配置
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDailyTime("0 0 9 * * *");
        timerConfig.setMaxConcurrency(10);
        timerConfig.setDepartmentFilterEnabled(true);
        timerConfig.setTargetDepartments(java.util.Arrays.asList("心血管一病区", "心血管二病区"));
        schedulingProperties.setTimer(timerConfig);
        
        SchedulingProperties.AutoExecuteConfig autoExecuteConfig = new SchedulingProperties.AutoExecuteConfig();
        autoExecuteConfig.setInterval(10000);
        autoExecuteConfig.setMaxThreads(20);
        schedulingProperties.setAutoExecute(autoExecuteConfig);
        
        // 当：生成配置摘要
        String summary = schedulingProperties.getConfigurationSummary();
        
        // 那么：摘要应该包含所有重要信息
        assertNotNull(summary, "配置摘要不应该为null");
        assertTrue(summary.contains("定时任务"), "配置摘要应该包含定时任务信息");
        assertTrue(summary.contains("0 0 9 * * *"), "配置摘要应该包含执行时间");
        assertTrue(summary.contains("最大并发数: 10"), "配置摘要应该包含最大并发数");
        assertTrue(summary.contains("科室过滤: 启用"), "配置摘要应该包含科室过滤状态");
        assertTrue(summary.contains("目标科室: 心血管一病区, 心血管二病区"), "配置摘要应该包含目标科室");
        assertTrue(summary.contains("自动执行"), "配置摘要应该包含自动执行信息");
        assertTrue(summary.contains("执行间隔: 10000ms"), "配置摘要应该包含执行间隔");
        assertTrue(summary.contains("最大线程数: 20"), "配置摘要应该包含最大线程数");
    }
}
