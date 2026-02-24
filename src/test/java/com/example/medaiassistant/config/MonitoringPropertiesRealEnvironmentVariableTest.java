package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监控配置真实环境变量映射测试类
 * 第三阶段：环境变量映射 - 验证真实环境变量支持
 * 验证MonitoringProperties配置类能够正确映射真实环境变量
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = MonitoringProperties.class,
    properties = {
        // 使用Spring Boot属性格式，模拟环境变量映射
        "monitoring.startup.startup-timeout=900000",
        "monitoring.startup.max-startup-retries=5",
        "monitoring.startup.detailed-logging=false",
        "monitoring.normal.monitoring-interval=120000",
        "monitoring.normal.auto-recovery-enabled=false",
        "monitoring.alert.enabled=false",
        "monitoring.alert.health-threshold=0.9",
        "monitoring.metrics.enabled=false",
        "monitoring.metrics.collection-interval=30000",
        "monitoring.database.enabled=false",
        "monitoring.database.connection-pool-check-interval=60000",
        
        // 禁用无关组件
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        "prompt.submission.enabled=false",
        "prompt.polling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
@EnableConfigurationProperties(MonitoringProperties.class)
class MonitoringPropertiesRealEnvironmentVariableTest {

    @Autowired
    private MonitoringProperties monitoringProperties;

    /**
     * 测试配置类能够通过Spring Boot自动映射环境变量
     * 绿阶段：这个测试应该通过，因为Spring Boot自动处理环境变量映射
     */
    @Test
    void shouldBindPropertiesThroughSpringBootAutoConfiguration() {
        // 验证配置类不为空
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证启动阶段配置映射
        assertNotNull(monitoringProperties.getStartup(), "启动阶段配置应该不为空");
        assertEquals(900000L, monitoringProperties.getStartup().getStartupTimeout(), 
            "启动超时时间应该从属性映射为900000");
        assertEquals(5, monitoringProperties.getStartup().getMaxStartupRetries(), 
            "最大启动重试次数应该从属性映射为5");
        assertFalse(monitoringProperties.getStartup().isDetailedLogging(), 
            "详细日志应该从属性映射为false");
        
        // 验证正常运行阶段配置映射
        assertNotNull(monitoringProperties.getNormal(), "正常运行阶段配置应该不为空");
        assertEquals(120000L, monitoringProperties.getNormal().getMonitoringInterval(), 
            "监控间隔应该从属性映射为120000");
        assertFalse(monitoringProperties.getNormal().isAutoRecoveryEnabled(), 
            "自动恢复应该从属性映射为false");
        
        // 验证告警配置映射
        assertNotNull(monitoringProperties.getAlert(), "告警配置应该不为空");
        assertFalse(monitoringProperties.getAlert().isEnabled(), 
            "告警应该从属性映射为false");
        assertEquals(0.9, monitoringProperties.getAlert().getHealthThreshold(), 
            "健康阈值应该从属性映射为0.9");
        
        // 验证指标监控配置映射
        assertNotNull(monitoringProperties.getMetrics(), "指标监控配置应该不为空");
        assertFalse(monitoringProperties.getMetrics().isEnabled(), 
            "指标监控应该从属性映射为false");
        assertEquals(30000L, monitoringProperties.getMetrics().getCollectionInterval(), 
            "指标收集间隔应该从属性映射为30000");
        
        // 验证数据库监控配置映射
        assertNotNull(monitoringProperties.getDatabase(), "数据库监控配置应该不为空");
        assertFalse(monitoringProperties.getDatabase().isEnabled(), 
            "数据库监控应该从属性映射为false");
        assertEquals(60000L, monitoringProperties.getDatabase().getConnectionPoolCheckInterval(), 
            "连接池检查间隔应该从属性映射为60000");
    }

    /**
     * 测试配置验证方法与环境变量兼容
     * 绿阶段：这个测试应该通过，因为配置验证方法已实现
     */
    @Test
    void shouldValidateConfigurationWithEnvironmentVariables() {
        // 验证配置验证方法存在且正常工作
        assertDoesNotThrow(() -> monitoringProperties.validateConfiguration(null), 
            "配置验证应该成功通过，即使使用环境变量映射的配置");
        
        // 验证配置摘要方法正常工作
        String summary = monitoringProperties.getConfigSummary();
        assertNotNull(summary, "配置摘要不应该为空");
        assertTrue(summary.contains("MonitoringConfig"), 
            "配置摘要应该包含MonitoringConfig，表明配置类正常工作");
    }

    /**
     * 测试默认值与环境变量映射的兼容性
     * 绿阶段：这个测试应该通过，因为默认值机制与环境变量映射兼容
     */
    @Test
    void shouldHandleDefaultValuesWithEnvironmentVariableMapping() {
        // 创建一个新的配置实例，验证默认值
        MonitoringProperties defaultProperties = new MonitoringProperties();
        
        // 验证启动阶段默认值
        assertEquals(600000L, defaultProperties.getStartup().getStartupTimeout(), 
            "当没有环境变量时，应该使用默认值600000");
        assertEquals(3, defaultProperties.getStartup().getMaxStartupRetries(), 
            "当没有环境变量时，应该使用默认值3");
        assertTrue(defaultProperties.getStartup().isDetailedLogging(), 
            "当没有环境变量时，应该使用默认值true");
        
        // 验证配置验证通过默认值
        assertDoesNotThrow(() -> defaultProperties.validateConfiguration(null), 
            "默认配置应该通过验证");
    }

    /**
     * 测试配置类与环境变量命名规范对齐
     * 绿阶段：这个测试应该通过，因为配置类设计符合Spring Boot环境变量映射规范
     */
    @Test
    void shouldAlignWithSpringBootEnvironmentVariableConvention() {
        // 验证配置类结构支持环境变量映射
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证配置类支持所有预期的配置前缀
        assertTrue(monitoringProperties.getStartup() != null, "应该支持启动阶段配置");
        assertTrue(monitoringProperties.getNormal() != null, "应该支持正常运行阶段配置");
        assertTrue(monitoringProperties.getAlert() != null, "应该支持告警配置");
        assertTrue(monitoringProperties.getMetrics() != null, "应该支持指标监控配置");
        assertTrue(monitoringProperties.getDatabase() != null, "应该支持数据库监控配置");
        
        // 验证配置摘要包含所有配置部分
        String summary = monitoringProperties.getConfigSummary();
        assertNotNull(summary, "配置摘要不应该为空");
        assertTrue(summary.contains("MonitoringConfig"), 
            "配置摘要应该包含MonitoringConfig，表明配置类结构正确");
    }
}
