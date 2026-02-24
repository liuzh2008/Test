package com.example.medaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * 监控配置映射类
 * 映射现有监控配置键，避免与Actuator冲突
 * 与宿主机环境变量模板对齐，支持全自动化部署
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@ConfigurationProperties(prefix = "monitoring")
@Validated
@Data
public class MonitoringProperties {
    
    /**
     * 启动阶段监控配置 - 映射现有 startup.* 配置
     */
    private StartupPhase startup = new StartupPhase();
    
    /**
     * 正常运行阶段监控配置 - 映射现有 normal.* 配置
     */
    private NormalPhase normal = new NormalPhase();
    
    /**
     * 告警配置 - 映射现有 leak-detection.*, health.* 配置
     */
    private AlertPhase alert = new AlertPhase();
    
    /**
     * 指标监控配置 - 新增指标监控配置
     */
    private MetricsPhase metrics = new MetricsPhase();
    
    /**
     * 数据库监控配置 - 新增数据库监控配置
     */
    private DatabaseMonitoringPhase database = new DatabaseMonitoringPhase();
    
    @Data
    public static class StartupPhase {
        @Positive
        private long leakDetectionThreshold = 120000;
        
        @Positive
        private long healthCheckTimeout = 30000;
        
        @Positive
        private long monitoringDuration = 300000;
        
        @Positive
        private long componentReadinessInterval = 10000;
        
        @Positive
        private long startupTimeout = 600000; // 10分钟启动超时
        
        @Positive
        private int maxStartupRetries = 3;
        
        private boolean detailedLogging = true;
    }
    
    @Data
    public static class NormalPhase {
        @Positive
        private long leakDetectionThreshold = 30000;
        
        @Positive
        private long healthCheckTimeout = 5000;
        
        @Positive
        private long monitoringInterval = 60000;
        
        @Positive
        private long componentHealthInterval = 30000;
        
        @Positive
        private long performanceCheckInterval = 30000;
        
        @Positive
        private long resourceUsageInterval = 60000;
        
        private boolean autoRecoveryEnabled = true;
    }
    
    @Data
    public static class AlertPhase {
        @Min(1)
        private int leakDetectionThreshold = 3;
        
        @Positive
        private long leakDetectionInterval = 300000;
        
        @Min(0)
        private double healthThreshold = 0.8;
        
        @Positive
        private long healthInterval = 120000;
        
        private boolean enabled = true;
        
        @Positive
        private int alertRetryCount = 3;
        
        @Positive
        private long alertRetryInterval = 30000;
        
        private String alertChannels = "LOG,EMAIL"; // LOG, EMAIL, SMS, WEBHOOK
        
        private boolean criticalAlertsOnly = false;
    }
    
    @Data
    public static class MetricsPhase {
        @Positive
        private long collectionInterval = 15000;
        
        @Positive
        private int retentionDays = 30;
        
        @Positive
        private int batchSize = 1000;
        
        private boolean enabled = true;
        
        private String storageType = "MEMORY"; // MEMORY, DATABASE, EXTERNAL
        
        @Positive
        private int maxMetricsPerBatch = 1000;
        
        private boolean compressionEnabled = true;
    }
    
    @Data
    public static class DatabaseMonitoringPhase {
        @Positive
        private long connectionPoolCheckInterval = 30000;
        
        @Positive
        private long queryPerformanceThreshold = 5000;
        
        @Positive
        private int slowQueryThreshold = 1000;
        
        @Positive
        private int connectionLeakThreshold = 10;
        
        private boolean enabled = true;
        
        @Positive
        private long deadlockCheckInterval = 60000;
        
        @Positive
        private int maxConnectionsThreshold = 80; // 80%使用率警告
    }
    
    /**
     * 配置验证
     * 验证所有监控配置参数的合法性
     * 
     * @param env Spring环境对象，可用于获取环境变量
     * @throws IllegalStateException 当配置参数不合法时抛出
     */
    public void validateConfiguration(Environment env) {
        validateStartupConfiguration();
        validateNormalConfiguration();
        validateAlertConfiguration();
        validateMetricsConfiguration();
        validateDatabaseConfiguration();
    }
    
    /**
     * 验证启动阶段配置
     */
    private void validateStartupConfiguration() {
        if (startup.startupTimeout <= 0) {
            throw new IllegalStateException("启动超时时间必须大于0");
        }
        if (startup.maxStartupRetries <= 0) {
            throw new IllegalStateException("最大启动重试次数必须大于0");
        }
    }
    
    /**
     * 验证正常运行阶段配置
     */
    private void validateNormalConfiguration() {
        if (normal.monitoringInterval <= 0) {
            throw new IllegalStateException("监控间隔必须大于0");
        }
        if (normal.healthCheckTimeout <= 0) {
            throw new IllegalStateException("健康检查超时必须大于0");
        }
    }
    
    /**
     * 验证告警配置
     */
    private void validateAlertConfiguration() {
        if (alert.enabled) {
            if (alert.healthThreshold <= 0) {
                throw new IllegalStateException("健康阈值必须大于0");
            }
            if (alert.alertRetryCount <= 0) {
                throw new IllegalStateException("告警重试次数必须大于0");
            }
        }
    }
    
    /**
     * 验证指标监控配置
     */
    private void validateMetricsConfiguration() {
        if (metrics.enabled) {
            if (metrics.collectionInterval <= 0) {
                throw new IllegalStateException("指标收集间隔必须大于0");
            }
            if (metrics.retentionDays <= 0) {
                throw new IllegalStateException("数据保留天数必须大于0");
            }
        }
    }
    
    /**
     * 验证数据库监控配置
     */
    private void validateDatabaseConfiguration() {
        if (database.enabled) {
            if (database.connectionPoolCheckInterval <= 0) {
                throw new IllegalStateException("数据库连接池检查间隔必须大于0");
            }
            if (database.slowQueryThreshold <= 0) {
                throw new IllegalStateException("慢查询阈值必须大于0");
            }
        }
    }
    
    /**
     * 获取监控配置摘要（用于日志记录）
     */
    public String getConfigSummary() {
        return String.format("MonitoringConfig{startup=%b, normal=%b, alert=%b, metrics=%b, database=%b}", 
                           startup.detailedLogging, normal.autoRecoveryEnabled, alert.enabled, 
                           metrics.enabled, database.enabled);
    }
    
    /**
     * 获取环境变量映射信息
     * 提供配置类字段与环境变量的映射关系
     * 
     * @return 环境变量映射信息字符串
     */
    public String getEnvironmentVariableMappingInfo() {
        return """
               监控配置环境变量映射信息：
               =========================
               启动阶段配置 (MONITORING_STARTUP_*):
                 - MONITORING_STARTUP_STARTUPTIMEOUT -> startup.startup-timeout
                 - MONITORING_STARTUP_MAXSTARTUPRETRIES -> startup.max-startup-retries
                 - MONITORING_STARTUP_DETAILEDLOGGING -> startup.detailed-logging
                 - MONITORING_STARTUP_LEAKDETECTIONTHRESHOLD -> startup.leak-detection-threshold
                 - MONITORING_STARTUP_HEALTHCHECKTIMEOUT -> startup.health-check-timeout
                 
               正常运行阶段配置 (MONITORING_NORMAL_*):
                 - MONITORING_NORMAL_MONITORINGINTERVAL -> normal.monitoring-interval
                 - MONITORING_NORMAL_AUTORECOVERYENABLED -> normal.auto-recovery-enabled
                 - MONITORING_NORMAL_LEAKDETECTIONTHRESHOLD -> normal.leak-detection-threshold
                 - MONITORING_NORMAL_HEALTHCHECKTIMEOUT -> normal.health-check-timeout
                 
               告警配置 (MONITORING_ALERT_*):
                 - MONITORING_ALERT_ENABLED -> alert.enabled
                 - MONITORING_ALERT_HEALTHTHRESHOLD -> alert.health-threshold
                 - MONITORING_ALERT_LEAKDETECTIONTHRESHOLD -> alert.leak-detection-threshold
                 - MONITORING_ALERT_ALERTRETRYCOUNT -> alert.alert-retry-count
                 
               指标监控配置 (MONITORING_METRICS_*):
                 - MONITORING_METRICS_ENABLED -> metrics.enabled
                 - MONITORING_METRICS_COLLECTIONINTERVAL -> metrics.collection-interval
                 - MONITORING_METRICS_RETENTIONDAYS -> metrics.retention-days
                 
               数据库监控配置 (MONITORING_DATABASE_*):
                 - MONITORING_DATABASE_ENABLED -> database.enabled
                 - MONITORING_DATABASE_CONNECTIONPOOLCHECKINTERVAL -> database.connection-pool-check-interval
                 - MONITORING_DATABASE_SLOWQUERYTHRESHOLD -> database.slow-query-threshold
                 
               注意：Spring Boot会自动将大写、下划线分隔的环境变量转换为小写、连字符分隔的配置属性。
               """;
    }
    
    /**
     * 验证环境变量配置的完整性
     * 检查是否所有必要的环境变量都已设置
     * 
     * @param env Spring环境对象
     * @return 验证结果，包含缺失的环境变量信息
     */
    public String validateEnvironmentVariables(Environment env) {
        StringBuilder result = new StringBuilder();
        result.append("环境变量配置验证结果：\n");
        result.append("====================\n");
        
        // 检查关键环境变量
        String[] criticalEnvVars = {
            "MONITORING_STARTUP_STARTUPTIMEOUT",
            "MONITORING_NORMAL_MONITORINGINTERVAL", 
            "MONITORING_ALERT_ENABLED",
            "MONITORING_METRICS_ENABLED",
            "MONITORING_DATABASE_ENABLED"
        };
        
        boolean allCriticalVarsPresent = true;
        for (String envVar : criticalEnvVars) {
            String value = env.getProperty(envVar);
            if (value == null || value.trim().isEmpty()) {
                result.append(String.format("❌ 缺失关键环境变量: %s (使用默认值)\n", envVar));
                allCriticalVarsPresent = false;
            } else {
                result.append(String.format("✅ 环境变量已设置: %s = %s\n", envVar, value));
            }
        }
        
        if (allCriticalVarsPresent) {
            result.append("\n✅ 所有关键环境变量都已正确设置\n");
        } else {
            result.append("\n⚠️  部分关键环境变量缺失，使用默认值\n");
        }
        
        return result.toString();
    }
}
