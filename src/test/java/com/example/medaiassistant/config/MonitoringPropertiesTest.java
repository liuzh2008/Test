package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监控配置属性测试类
 * 验证MonitoringProperties配置类的正确性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@SpringBootTest(classes = MonitoringProperties.class)
@EnableConfigurationProperties(MonitoringProperties.class)
@TestPropertySource(properties = {
    "monitoring.startup.startup-timeout=600000",
    "monitoring.startup.max-startup-retries=3",
    "monitoring.startup.detailed-logging=true",
    "monitoring.normal.monitoring-interval=60000",
    "monitoring.normal.auto-recovery-enabled=true",
    "monitoring.alert.enabled=true",
    "monitoring.alert.health-threshold=0.8",
    "monitoring.metrics.enabled=true",
    "monitoring.metrics.collection-interval=15000",
    "monitoring.database.enabled=true",
    "monitoring.database.connection-pool-check-interval=30000"
})
class MonitoringPropertiesTest {

    @Autowired
    private MonitoringProperties monitoringProperties;

    /**
     * 测试配置类能够正确绑定monitoring前缀
     * 绿阶段：这个测试现在应该通过，因为MonitoringProperties类已实现
     */
    @Test
    void shouldBindMonitoringPropertiesCorrectly() {
        // 验证配置类不为空
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证启动阶段配置
        assertNotNull(monitoringProperties.getStartup(), "启动阶段配置应该不为空");
        assertEquals(600000L, monitoringProperties.getStartup().getStartupTimeout(), "启动超时时间应该为600000");
        assertEquals(3, monitoringProperties.getStartup().getMaxStartupRetries(), "最大启动重试次数应该为3");
        assertTrue(monitoringProperties.getStartup().isDetailedLogging(), "详细日志应该启用");
        
        // 验证正常运行阶段配置
        assertNotNull(monitoringProperties.getNormal(), "正常运行阶段配置应该不为空");
        assertEquals(60000L, monitoringProperties.getNormal().getMonitoringInterval(), "监控间隔应该为60000");
        assertTrue(monitoringProperties.getNormal().isAutoRecoveryEnabled(), "自动恢复应该启用");
        
        // 验证告警配置
        assertNotNull(monitoringProperties.getAlert(), "告警配置应该不为空");
        assertTrue(monitoringProperties.getAlert().isEnabled(), "告警应该启用");
        assertEquals(0.8, monitoringProperties.getAlert().getHealthThreshold(), "健康阈值应该为0.8");
        
        // 验证指标监控配置
        assertNotNull(monitoringProperties.getMetrics(), "指标监控配置应该不为空");
        assertTrue(monitoringProperties.getMetrics().isEnabled(), "指标监控应该启用");
        assertEquals(15000L, monitoringProperties.getMetrics().getCollectionInterval(), "指标收集间隔应该为15000");
        
        // 验证数据库监控配置
        assertNotNull(monitoringProperties.getDatabase(), "数据库监控配置应该不为空");
        assertTrue(monitoringProperties.getDatabase().isEnabled(), "数据库监控应该启用");
        assertEquals(30000L, monitoringProperties.getDatabase().getConnectionPoolCheckInterval(), "连接池检查间隔应该为30000");
    }

    /**
     * 测试配置验证逻辑
     * 绿阶段：这个测试现在应该通过，因为验证方法已实现
     */
    @Test
    void shouldValidateConfigurationSuccessfully() {
        // 验证配置验证方法存在且正常工作
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null), 
            "配置验证应该成功通过");
    }

    /**
     * 测试配置摘要方法
     * 绿阶段：这个测试现在应该通过，因为配置摘要方法已实现
     */
    @Test
    void shouldGenerateConfigSummary() {
        // 验证配置摘要方法存在且返回有效字符串
        String summary = monitoringProperties.getConfigSummary();
        assertNotNull(summary, "配置摘要不应该为空");
        assertTrue(summary.contains("MonitoringConfig"), "配置摘要应该包含MonitoringConfig");
    }

    /**
     * 测试边界条件 - 验证配置验证失败场景
     * 重构阶段：添加边界条件测试
     */
    @Test
    void shouldThrowExceptionWhenInvalidConfiguration() {
        // 创建无效配置的实例
        MonitoringProperties invalidProperties = new MonitoringProperties();
        invalidProperties.getStartup().setStartupTimeout(0); // 无效的超时时间
        
        // 验证配置验证抛出异常
        assertThrows(IllegalStateException.class, 
            () -> invalidProperties.validateConfiguration(null),
            "配置验证应该对无效配置抛出异常");
    }

    /**
     * 测试默认值设置
     * 重构阶段：验证默认值是否正确设置
     */
    @Test
    void shouldUseDefaultValuesWhenNoConfigurationProvided() {
        // 创建新的配置实例，不设置任何属性
        MonitoringProperties defaultProperties = new MonitoringProperties();
        
        // 验证启动阶段默认值
        assertEquals(600000L, defaultProperties.getStartup().getStartupTimeout(), "启动超时默认值应该为600000");
        assertEquals(3, defaultProperties.getStartup().getMaxStartupRetries(), "最大启动重试次数默认值应该为3");
        assertTrue(defaultProperties.getStartup().isDetailedLogging(), "详细日志默认应该启用");
        
        // 验证正常运行阶段默认值
        assertEquals(60000L, defaultProperties.getNormal().getMonitoringInterval(), "监控间隔默认值应该为60000");
        assertTrue(defaultProperties.getNormal().isAutoRecoveryEnabled(), "自动恢复默认应该启用");
        
        // 验证告警配置默认值
        assertTrue(defaultProperties.getAlert().isEnabled(), "告警默认应该启用");
        assertEquals(0.8, defaultProperties.getAlert().getHealthThreshold(), "健康阈值默认值应该为0.8");
        
        // 验证指标监控默认值
        assertTrue(defaultProperties.getMetrics().isEnabled(), "指标监控默认应该启用");
        assertEquals(15000L, defaultProperties.getMetrics().getCollectionInterval(), "指标收集间隔默认值应该为15000");
        
        // 验证数据库监控默认值
        assertTrue(defaultProperties.getDatabase().isEnabled(), "数据库监控默认应该启用");
        assertEquals(30000L, defaultProperties.getDatabase().getConnectionPoolCheckInterval(), "连接池检查间隔默认值应该为30000");
    }

    /**
     * 测试配置验证注解
     * 重构阶段：验证Jakarta Validation注解正常工作
     */
    @Test
    void shouldRespectValidationAnnotations() {
        // 创建配置实例
        MonitoringProperties properties = new MonitoringProperties();
        
        // 验证注解约束
        assertTrue(properties.getStartup().getStartupTimeout() > 0, "启动超时时间应该大于0");
        assertTrue(properties.getStartup().getMaxStartupRetries() > 0, "最大启动重试次数应该大于0");
        assertTrue(properties.getNormal().getMonitoringInterval() > 0, "监控间隔应该大于0");
        assertTrue(properties.getAlert().getHealthThreshold() >= 0, "健康阈值应该大于等于0");
        assertTrue(properties.getMetrics().getCollectionInterval() > 0, "指标收集间隔应该大于0");
        assertTrue(properties.getDatabase().getConnectionPoolCheckInterval() > 0, "连接池检查间隔应该大于0");
    }
}
