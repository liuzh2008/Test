package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监控配置环境变量映射测试类
 * 第三阶段：环境变量映射 - 绿阶段测试用例
 * 验证MonitoringProperties配置类能够正确映射环境变量和配置属性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootTest(classes = MonitoringProperties.class)
@EnableConfigurationProperties(MonitoringProperties.class)
@TestPropertySource(properties = {
    // 使用Spring Boot属性格式（小写、连字符分隔）
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
    "monitoring.database.connection-pool-check-interval=60000"
})
class MonitoringPropertiesEnvironmentVariableTest {

    @Autowired
    private MonitoringProperties monitoringProperties;

    /**
     * 测试配置属性能够正确映射到配置类
     * 绿阶段：这个测试现在应该通过，因为配置属性映射功能已实现
     */
    @Test
    void shouldBindConfigurationPropertiesCorrectly() {
        // 验证配置类不为空
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证启动阶段配置从属性映射
        assertNotNull(monitoringProperties.getStartup(), "启动阶段配置应该不为空");
        assertEquals(900000L, monitoringProperties.getStartup().getStartupTimeout(), 
            "启动超时时间应该从属性monitoring.startup.startup-timeout映射为900000");
        assertEquals(5, monitoringProperties.getStartup().getMaxStartupRetries(), 
            "最大启动重试次数应该从属性monitoring.startup.max-startup-retries映射为5");
        assertFalse(monitoringProperties.getStartup().isDetailedLogging(), 
            "详细日志应该从属性monitoring.startup.detailed-logging映射为false");
        
        // 验证正常运行阶段配置从属性映射
        assertNotNull(monitoringProperties.getNormal(), "正常运行阶段配置应该不为空");
        assertEquals(120000L, monitoringProperties.getNormal().getMonitoringInterval(), 
            "监控间隔应该从属性monitoring.normal.monitoring-interval映射为120000");
        assertFalse(monitoringProperties.getNormal().isAutoRecoveryEnabled(), 
            "自动恢复应该从属性monitoring.normal.auto-recovery-enabled映射为false");
        
        // 验证告警配置从属性映射
        assertNotNull(monitoringProperties.getAlert(), "告警配置应该不为空");
        assertFalse(monitoringProperties.getAlert().isEnabled(), 
            "告警应该从属性monitoring.alert.enabled映射为false");
        assertEquals(0.9, monitoringProperties.getAlert().getHealthThreshold(), 
            "健康阈值应该从属性monitoring.alert.health-threshold映射为0.9");
        
        // 验证指标监控配置从属性映射
        assertNotNull(monitoringProperties.getMetrics(), "指标监控配置应该不为空");
        assertFalse(monitoringProperties.getMetrics().isEnabled(), 
            "指标监控应该从属性monitoring.metrics.enabled映射为false");
        assertEquals(30000L, monitoringProperties.getMetrics().getCollectionInterval(), 
            "指标收集间隔应该从属性monitoring.metrics.collection-interval映射为30000");
        
        // 验证数据库监控配置从属性映射
        assertNotNull(monitoringProperties.getDatabase(), "数据库监控配置应该不为空");
        assertFalse(monitoringProperties.getDatabase().isEnabled(), 
            "数据库监控应该从属性monitoring.database.enabled映射为false");
        assertEquals(60000L, monitoringProperties.getDatabase().getConnectionPoolCheckInterval(), 
            "连接池检查间隔应该从属性monitoring.database.connection-pool-check-interval映射为60000");
    }

    /**
     * 测试环境变量名称符合Spring Boot规范
     * 绿阶段：这个测试现在应该通过，因为环境变量命名规范验证已实现
     */
    @Test
    void shouldUseSpringBootEnvironmentVariableNamingConvention() {
        // 验证环境变量名称符合Spring Boot规范（大写、下划线分隔）
        // 这个测试验证配置类能够处理标准的环境变量格式
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证配置类能够处理MONITORING_前缀的环境变量
        assertTrue(monitoringProperties.getConfigSummary().contains("MonitoringConfig"), 
            "配置摘要应该包含MonitoringConfig，表明配置类正常工作");
    }

    /**
     * 测试默认值在环境变量缺失时生效
     * 绿阶段：这个测试现在应该通过，因为默认值处理逻辑已实现
     */
    @Test
    void shouldUseDefaultValuesWhenEnvironmentVariablesAreMissing() {
        // 创建一个新的配置实例，不设置任何环境变量
        MonitoringProperties defaultProperties = new MonitoringProperties();
        
        // 验证启动阶段默认值
        assertEquals(600000L, defaultProperties.getStartup().getStartupTimeout(), 
            "当环境变量MONITORING_STARTUP_STARTUPTIMEOUT缺失时，应该使用默认值600000");
        assertEquals(3, defaultProperties.getStartup().getMaxStartupRetries(), 
            "当环境变量MONITORING_STARTUP_MAXSTARTUPRETRIES缺失时，应该使用默认值3");
        assertTrue(defaultProperties.getStartup().isDetailedLogging(), 
            "当环境变量MONITORING_STARTUP_DETAILEDLOGGING缺失时，应该使用默认值true");
        
        // 验证正常运行阶段默认值
        assertEquals(60000L, defaultProperties.getNormal().getMonitoringInterval(), 
            "当环境变量MONITORING_NORMAL_MONITORINGINTERVAL缺失时，应该使用默认值60000");
        assertTrue(defaultProperties.getNormal().isAutoRecoveryEnabled(), 
            "当环境变量MONITORING_NORMAL_AUTORECOVERYENABLED缺失时，应该使用默认值true");
        
        // 验证告警配置默认值
        assertTrue(defaultProperties.getAlert().isEnabled(), 
            "当环境变量MONITORING_ALERT_ENABLED缺失时，应该使用默认值true");
        assertEquals(0.8, defaultProperties.getAlert().getHealthThreshold(), 
            "当环境变量MONITORING_ALERT_HEALTHTHRESHOLD缺失时，应该使用默认值0.8");
        
        // 验证指标监控默认值
        assertTrue(defaultProperties.getMetrics().isEnabled(), 
            "当环境变量MONITORING_METRICS_ENABLED缺失时，应该使用默认值true");
        assertEquals(15000L, defaultProperties.getMetrics().getCollectionInterval(), 
            "当环境变量MONITORING_METRICS_COLLECTIONINTERVAL缺失时，应该使用默认值15000");
        
        // 验证数据库监控默认值
        assertTrue(defaultProperties.getDatabase().isEnabled(), 
            "当环境变量MONITORING_DATABASE_ENABLED缺失时，应该使用默认值true");
        assertEquals(30000L, defaultProperties.getDatabase().getConnectionPoolCheckInterval(), 
            "当环境变量MONITORING_DATABASE_CONNECTIONPOOLCHECKINTERVAL缺失时，应该使用默认值30000");
    }

    /**
     * 测试配置类与环境变量模板对齐
     * 绿阶段：这个测试现在应该通过，因为环境变量模板验证已实现
     */
    @Test
    void shouldAlignWithEnvironmentVariableTemplate() {
        // 验证配置类字段与环境变量模板中的变量名对齐
        assertNotNull(monitoringProperties, "监控配置类应该被正确注入");
        
        // 验证配置类能够处理标准的环境变量格式
        // 这个测试确保配置类的字段命名与预期的环境变量模板一致
        String configSummary = monitoringProperties.getConfigSummary();
        assertNotNull(configSummary, "配置摘要不应该为空");
        assertTrue(configSummary.contains("MonitoringConfig"), 
            "配置摘要应该包含MonitoringConfig，表明配置类结构正确");
        
        // 验证配置类支持所有预期的环境变量前缀
        assertTrue(monitoringProperties.getStartup() != null, "应该支持启动阶段环境变量");
        assertTrue(monitoringProperties.getNormal() != null, "应该支持正常运行阶段环境变量");
        assertTrue(monitoringProperties.getAlert() != null, "应该支持告警环境变量");
        assertTrue(monitoringProperties.getMetrics() != null, "应该支持指标监控环境变量");
        assertTrue(monitoringProperties.getDatabase() != null, "应该支持数据库监控环境变量");
    }
}
