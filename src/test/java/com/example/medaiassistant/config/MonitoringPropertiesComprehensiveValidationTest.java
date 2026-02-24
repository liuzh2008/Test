package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监控配置全面验证测试类
 * 验证所有配置验证场景，确保验证逻辑完整
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootTest(classes = MonitoringProperties.class)
@EnableConfigurationProperties(MonitoringProperties.class)
@TestPropertySource(properties = {
    // 提供有效的配置值
    "monitoring.startup.startup-timeout=600000",
    "monitoring.startup.max-startup-retries=3",
    "monitoring.normal.monitoring-interval=60000",
    "monitoring.normal.health-check-timeout=5000",
    "monitoring.alert.enabled=true",
    "monitoring.alert.health-threshold=0.8",
    "monitoring.alert.alert-retry-count=3",
    "monitoring.metrics.enabled=true",
    "monitoring.metrics.collection-interval=15000",
    "monitoring.metrics.retention-days=30",
    "monitoring.database.enabled=true",
    "monitoring.database.connection-pool-check-interval=30000",
    "monitoring.database.slow-query-threshold=1000"
})
class MonitoringPropertiesComprehensiveValidationTest {

    @Autowired
    private MonitoringProperties monitoringProperties;

    /**
     * 绿阶段测试：验证启动阶段配置验证逻辑
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateStartupConfigurationComprehensively() {
        // 测试无效启动超时时间
        MonitoringProperties.StartupPhase invalidStartupTimeout = new MonitoringProperties.StartupPhase();
        invalidStartupTimeout.setStartupTimeout(0);
        invalidStartupTimeout.setMaxStartupRetries(3); // 有效的重试次数
        
        monitoringProperties.setStartup(invalidStartupTimeout);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "启动配置验证应该对无效启动超时时间抛出异常");

        // 测试无效最大启动重试次数
        MonitoringProperties.StartupPhase invalidMaxRetries = new MonitoringProperties.StartupPhase();
        invalidMaxRetries.setStartupTimeout(600000); // 有效的超时时间
        invalidMaxRetries.setMaxStartupRetries(0);
        
        monitoringProperties.setStartup(invalidMaxRetries);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "启动配置验证应该对无效最大启动重试次数抛出异常");

        // 测试有效配置
        MonitoringProperties.StartupPhase validStartup = new MonitoringProperties.StartupPhase();
        validStartup.setStartupTimeout(600000);
        validStartup.setMaxStartupRetries(3);
        
        monitoringProperties.setStartup(validStartup);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "启动配置验证应该对有效配置通过");
    }

    /**
     * 绿阶段测试：验证正常运行阶段配置验证逻辑
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateNormalConfigurationComprehensively() {
        // 测试无效监控间隔
        MonitoringProperties.NormalPhase invalidMonitoringInterval = new MonitoringProperties.NormalPhase();
        invalidMonitoringInterval.setMonitoringInterval(0);
        invalidMonitoringInterval.setHealthCheckTimeout(5000); // 有效的健康检查超时
        
        monitoringProperties.setNormal(invalidMonitoringInterval);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "正常运行配置验证应该对无效监控间隔抛出异常");

        // 测试无效健康检查超时
        MonitoringProperties.NormalPhase invalidHealthCheckTimeout = new MonitoringProperties.NormalPhase();
        invalidHealthCheckTimeout.setMonitoringInterval(60000); // 有效的监控间隔
        invalidHealthCheckTimeout.setHealthCheckTimeout(0);
        
        monitoringProperties.setNormal(invalidHealthCheckTimeout);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "正常运行配置验证应该对无效健康检查超时抛出异常");

        // 测试有效配置
        MonitoringProperties.NormalPhase validNormal = new MonitoringProperties.NormalPhase();
        validNormal.setMonitoringInterval(60000);
        validNormal.setHealthCheckTimeout(5000);
        
        monitoringProperties.setNormal(validNormal);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "正常运行配置验证应该对有效配置通过");
    }

    /**
     * 绿阶段测试：验证告警配置验证逻辑
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateAlertConfigurationComprehensively() {
        // 测试启用告警但健康阈值为0
        MonitoringProperties.AlertPhase invalidHealthThreshold = new MonitoringProperties.AlertPhase();
        invalidHealthThreshold.setEnabled(true);
        invalidHealthThreshold.setHealthThreshold(0);
        invalidHealthThreshold.setAlertRetryCount(3); // 有效的重试次数
        
        monitoringProperties.setAlert(invalidHealthThreshold);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "告警配置验证应该对无效健康阈值抛出异常");

        // 测试无效告警重试次数
        MonitoringProperties.AlertPhase invalidAlertRetryCount = new MonitoringProperties.AlertPhase();
        invalidAlertRetryCount.setEnabled(true);
        invalidAlertRetryCount.setHealthThreshold(0.8); // 有效的健康阈值
        invalidAlertRetryCount.setAlertRetryCount(0);
        
        monitoringProperties.setAlert(invalidAlertRetryCount);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "告警配置验证应该对无效告警重试次数抛出异常");

        // 测试禁用告警时的无效配置（应该通过，因为验证只对启用状态检查）
        MonitoringProperties.AlertPhase disabledAlertWithInvalidConfig = new MonitoringProperties.AlertPhase();
        disabledAlertWithInvalidConfig.setEnabled(false);
        disabledAlertWithInvalidConfig.setHealthThreshold(0); // 无效的健康阈值
        disabledAlertWithInvalidConfig.setAlertRetryCount(0); // 无效的重试次数
        
        monitoringProperties.setAlert(disabledAlertWithInvalidConfig);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "告警配置验证应该对禁用状态的无效配置通过");

        // 测试有效配置
        MonitoringProperties.AlertPhase validAlert = new MonitoringProperties.AlertPhase();
        validAlert.setEnabled(true);
        validAlert.setHealthThreshold(0.8);
        validAlert.setAlertRetryCount(3);
        
        monitoringProperties.setAlert(validAlert);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "告警配置验证应该对有效配置通过");
    }

    /**
     * 绿阶段测试：验证指标监控配置验证逻辑
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateMetricsConfigurationComprehensively() {
        // 测试启用指标监控但收集间隔为0
        MonitoringProperties.MetricsPhase invalidCollectionInterval = new MonitoringProperties.MetricsPhase();
        invalidCollectionInterval.setEnabled(true);
        invalidCollectionInterval.setCollectionInterval(0);
        invalidCollectionInterval.setRetentionDays(30); // 有效的保留天数
        
        monitoringProperties.setMetrics(invalidCollectionInterval);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "指标监控配置验证应该对无效收集间隔抛出异常");

        // 测试无效保留天数
        MonitoringProperties.MetricsPhase invalidRetentionDays = new MonitoringProperties.MetricsPhase();
        invalidRetentionDays.setEnabled(true);
        invalidRetentionDays.setCollectionInterval(15000); // 有效的收集间隔
        invalidRetentionDays.setRetentionDays(0);
        
        monitoringProperties.setMetrics(invalidRetentionDays);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "指标监控配置验证应该对无效保留天数抛出异常");

        // 测试禁用指标监控时的无效配置（应该通过，因为验证只对启用状态检查）
        MonitoringProperties.MetricsPhase disabledMetricsWithInvalidConfig = new MonitoringProperties.MetricsPhase();
        disabledMetricsWithInvalidConfig.setEnabled(false);
        disabledMetricsWithInvalidConfig.setCollectionInterval(0); // 无效的收集间隔
        disabledMetricsWithInvalidConfig.setRetentionDays(0); // 无效的保留天数
        
        monitoringProperties.setMetrics(disabledMetricsWithInvalidConfig);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "指标监控配置验证应该对禁用状态的无效配置通过");

        // 测试有效配置
        MonitoringProperties.MetricsPhase validMetrics = new MonitoringProperties.MetricsPhase();
        validMetrics.setEnabled(true);
        validMetrics.setCollectionInterval(15000);
        validMetrics.setRetentionDays(30);
        
        monitoringProperties.setMetrics(validMetrics);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "指标监控配置验证应该对有效配置通过");
    }

    /**
     * 绿阶段测试：验证数据库监控配置验证逻辑
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateDatabaseConfigurationComprehensively() {
        // 测试启用数据库监控但连接池检查间隔为0
        MonitoringProperties.DatabaseMonitoringPhase invalidConnectionPoolInterval = new MonitoringProperties.DatabaseMonitoringPhase();
        invalidConnectionPoolInterval.setEnabled(true);
        invalidConnectionPoolInterval.setConnectionPoolCheckInterval(0);
        invalidConnectionPoolInterval.setSlowQueryThreshold(1000); // 有效的慢查询阈值
        
        monitoringProperties.setDatabase(invalidConnectionPoolInterval);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "数据库监控配置验证应该对无效连接池检查间隔抛出异常");

        // 测试无效慢查询阈值
        MonitoringProperties.DatabaseMonitoringPhase invalidSlowQueryThreshold = new MonitoringProperties.DatabaseMonitoringPhase();
        invalidSlowQueryThreshold.setEnabled(true);
        invalidSlowQueryThreshold.setConnectionPoolCheckInterval(30000); // 有效的连接池检查间隔
        invalidSlowQueryThreshold.setSlowQueryThreshold(0);
        
        monitoringProperties.setDatabase(invalidSlowQueryThreshold);
        assertThrows(IllegalStateException.class, 
            () -> monitoringProperties.validateConfiguration(null),
            "数据库监控配置验证应该对无效慢查询阈值抛出异常");

        // 测试禁用数据库监控时的无效配置（应该通过，因为验证只对启用状态检查）
        MonitoringProperties.DatabaseMonitoringPhase disabledDatabaseWithInvalidConfig = new MonitoringProperties.DatabaseMonitoringPhase();
        disabledDatabaseWithInvalidConfig.setEnabled(false);
        disabledDatabaseWithInvalidConfig.setConnectionPoolCheckInterval(0); // 无效的连接池检查间隔
        disabledDatabaseWithInvalidConfig.setSlowQueryThreshold(0); // 无效的慢查询阈值
        
        monitoringProperties.setDatabase(disabledDatabaseWithInvalidConfig);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "数据库监控配置验证应该对禁用状态的无效配置通过");

        // 测试有效配置
        MonitoringProperties.DatabaseMonitoringPhase validDatabase = new MonitoringProperties.DatabaseMonitoringPhase();
        validDatabase.setEnabled(true);
        validDatabase.setConnectionPoolCheckInterval(30000);
        validDatabase.setSlowQueryThreshold(1000);
        
        monitoringProperties.setDatabase(validDatabase);
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "数据库监控配置验证应该对有效配置通过");
    }

    /**
     * 绿阶段测试：验证完整配置验证
     * 这个测试应该通过，因为验证逻辑已经实现
     */
    @Test
    void shouldValidateCompleteConfiguration() {
        // 验证注入的配置对象
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证完整配置
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null),
            "完整配置验证应该通过");
        
        // 验证配置摘要
        String summary = monitoringProperties.getConfigSummary();
        assertNotNull(summary, "配置摘要不应该为空");
        assertTrue(summary.contains("MonitoringConfig"), "配置摘要应该包含MonitoringConfig");
    }
}
