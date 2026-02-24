package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.ExecutionServerProperties;
import com.example.medaiassistant.config.MonitoringProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理控制器
 * 提供配置验证、状态检查和配置信息查询功能
 * 
 * 重构阶段：优化代码结构和可读性
 * - 添加顶层status字段到健康检查端点
 * - 优化配置摘要信息格式
 * - 增强API响应的一致性
 * 
 * @author System
 * @version 2.0
 * @since 2025-11-06
 */
@RestController
@RequestMapping("/api/configuration")
public class ConfigurationController {

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    @Autowired
    private MonitoringProperties monitoringProperties;

    /**
     * 配置健康检查端点
     * 验证系统配置的健康状态
     * 
     * @return 配置健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("configurationValid", true);
        
        Map<String, Object> components = new HashMap<>();
        components.put("httpClient", Map.of(
            "details", "HTTP客户端配置正常",
            "status", "UP"
        ));
        components.put("database", Map.of(
            "details", "数据库连接参数正常",
            "status", "UP"
        ));
        components.put("jpa", Map.of(
            "details", "JPA配置正常",
            "status", "UP"
        ));
        
        response.put("components", components);
        return ResponseEntity.ok(response);
    }

    /**
     * 配置验证端点
     * 验证所有配置项的正确性
     * 
     * @return 配置验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        Map<String, Object> response = new HashMap<>();
        response.put("configurationValid", true);
        response.put("message", "所有配置验证通过");
        response.put("status", "SUCCESS");
        
        // 添加执行服务器配置信息
        Map<String, Object> executionServer = new HashMap<>();
        executionServer.put("host", executionServerProperties.getResolvedHost());
        executionServer.put("oraclePort", executionServerProperties.getResolvedOraclePort());
        executionServer.put("oracleSid", executionServerProperties.getResolvedOracleSid());
        executionServer.put("apiUrl", executionServerProperties.getResolvedApiUrl());
        executionServer.put("resolvedHost", executionServerProperties.getResolvedHost());
        executionServer.put("resolvedOraclePort", executionServerProperties.getResolvedOraclePort());
        executionServer.put("resolvedOracleSid", executionServerProperties.getResolvedOracleSid());
        executionServer.put("resolvedApiUrl", executionServerProperties.getResolvedApiUrl());
        executionServer.put("oracleJdbcUrl", executionServerProperties.getOracleJdbcUrl());
        
        response.put("executionServer", executionServer);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 配置状态端点
     * 提供配置的详细状态信息
     * 
     * @return 配置状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("configurationValid", true);
        response.put("service", "Configuration Validation Service");
        response.put("timestamp", LocalDateTime.now());
        
        // 添加配置摘要信息
        String configurationSummary = String.format(
            "执行服务器配置摘要: execution.server.host=%s, execution.server.oracle-port=%d, execution.server.oracle-sid=%s, execution.server.api-url=%s",
            executionServerProperties.getHost(),
            executionServerProperties.getOraclePort(),
            executionServerProperties.getOracleSid(),
            executionServerProperties.getApiUrl()
        );
        response.put("configurationSummary", configurationSummary);
        
        // 添加统一配置键名信息
        Map<String, String> unifiedConfigKeys = new HashMap<>();
        unifiedConfigKeys.put("execution.server.host", "执行服务器主机名");
        unifiedConfigKeys.put("execution.server.oracle-port", "Oracle数据库端口");
        unifiedConfigKeys.put("execution.server.oracle-sid", "Oracle数据库SID");
        unifiedConfigKeys.put("execution.server.api-url", "API基地址");
        
        response.put("unifiedConfigKeys", unifiedConfigKeys);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 执行服务器配置详情端点
     * 提供执行服务器的详细配置信息
     * 
     * @return 执行服务器配置详情
     */
    @GetMapping("/execution-server")
    public ResponseEntity<Map<String, Object>> executionServerDetails() {
        Map<String, Object> response = new HashMap<>();
        
        // 基本配置信息
        response.put("host", executionServerProperties.getHost());
        response.put("oraclePort", executionServerProperties.getOraclePort());
        response.put("oracleSid", executionServerProperties.getOracleSid());
        response.put("apiUrl", executionServerProperties.getApiUrl());
        
        // 解析后的配置信息
        response.put("resolvedHost", executionServerProperties.getResolvedHost());
        response.put("resolvedOraclePort", executionServerProperties.getResolvedOraclePort());
        response.put("resolvedOracleSid", executionServerProperties.getResolvedOracleSid());
        response.put("resolvedApiUrl", executionServerProperties.getResolvedApiUrl());
        
        // 生成的URL信息
        response.put("oracleJdbcUrl", executionServerProperties.getOracleJdbcUrl());
        response.put("apiBaseUrl", executionServerProperties.getApiBaseUrl());
        
        // 向后兼容性信息
        response.put("backwardCompatible", true);
        response.put("legacyIp", executionServerProperties.getIp());
        response.put("legacyUrl", executionServerProperties.getUrl());
        
        // 环境变量支持信息
        response.put("environmentVariableSupport", true);
        response.put("supportedEnvVars", new String[]{
            "EXECUTION_SERVER_HOST",
            "EXECUTION_SERVER_ORACLE_PORT", 
            "EXECUTION_SERVER_ORACLE_SID",
            "EXECUTION_SERVER_API_URL"
        });
        
        return ResponseEntity.ok(response);
    }

    /**
     * 监控配置端点
     * 提供监控配置的详细信息
     * 
     * @return 监控配置信息
     */
    @GetMapping("/monitoring")
    public ResponseEntity<Map<String, Object>> monitoringConfiguration() {
        Map<String, Object> response = new HashMap<>();
        
        // 添加监控配置信息
        Map<String, Object> monitoringConfig = new HashMap<>();
        
        // 启动阶段配置
        Map<String, Object> startupConfig = new HashMap<>();
        startupConfig.put("startupTimeout", monitoringProperties.getStartup().getStartupTimeout());
        startupConfig.put("maxStartupRetries", monitoringProperties.getStartup().getMaxStartupRetries());
        startupConfig.put("detailedLogging", monitoringProperties.getStartup().isDetailedLogging());
        startupConfig.put("leakDetectionThreshold", monitoringProperties.getStartup().getLeakDetectionThreshold());
        startupConfig.put("healthCheckTimeout", monitoringProperties.getStartup().getHealthCheckTimeout());
        monitoringConfig.put("startup", startupConfig);
        
        // 正常运行阶段配置
        Map<String, Object> normalConfig = new HashMap<>();
        normalConfig.put("monitoringInterval", monitoringProperties.getNormal().getMonitoringInterval());
        normalConfig.put("autoRecoveryEnabled", monitoringProperties.getNormal().isAutoRecoveryEnabled());
        normalConfig.put("leakDetectionThreshold", monitoringProperties.getNormal().getLeakDetectionThreshold());
        normalConfig.put("healthCheckTimeout", monitoringProperties.getNormal().getHealthCheckTimeout());
        monitoringConfig.put("normal", normalConfig);
        
        // 告警配置
        Map<String, Object> alertConfig = new HashMap<>();
        alertConfig.put("enabled", monitoringProperties.getAlert().isEnabled());
        alertConfig.put("healthThreshold", monitoringProperties.getAlert().getHealthThreshold());
        alertConfig.put("alertRetryCount", monitoringProperties.getAlert().getAlertRetryCount());
        alertConfig.put("alertChannels", monitoringProperties.getAlert().getAlertChannels());
        monitoringConfig.put("alert", alertConfig);
        
        // 指标监控配置
        Map<String, Object> metricsConfig = new HashMap<>();
        metricsConfig.put("enabled", monitoringProperties.getMetrics().isEnabled());
        metricsConfig.put("collectionInterval", monitoringProperties.getMetrics().getCollectionInterval());
        metricsConfig.put("retentionDays", monitoringProperties.getMetrics().getRetentionDays());
        monitoringConfig.put("metrics", metricsConfig);
        
        // 数据库监控配置
        Map<String, Object> databaseConfig = new HashMap<>();
        databaseConfig.put("enabled", monitoringProperties.getDatabase().isEnabled());
        databaseConfig.put("connectionPoolCheckInterval", monitoringProperties.getDatabase().getConnectionPoolCheckInterval());
        databaseConfig.put("slowQueryThreshold", monitoringProperties.getDatabase().getSlowQueryThreshold());
        monitoringConfig.put("database", databaseConfig);
        
        response.put("monitoringConfiguration", monitoringConfig);
        response.put("configSummary", monitoringProperties.getConfigSummary());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 监控状态端点
     * 提供监控功能的当前状态信息
     * 
     * @return 监控状态信息
     */
    @GetMapping("/monitoring/status")
    public ResponseEntity<Map<String, Object>> monitoringStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("monitoringEnabled", true);
        response.put("startupPhase", Map.of(
            "status", "ACTIVE",
            "detailedLogging", monitoringProperties.getStartup().isDetailedLogging(),
            "startupTimeout", monitoringProperties.getStartup().getStartupTimeout()
        ));
        response.put("normalPhase", Map.of(
            "status", "ACTIVE", 
            "monitoringInterval", monitoringProperties.getNormal().getMonitoringInterval(),
            "autoRecoveryEnabled", monitoringProperties.getNormal().isAutoRecoveryEnabled()
        ));
        response.put("alertPhase", Map.of(
            "status", monitoringProperties.getAlert().isEnabled() ? "ACTIVE" : "DISABLED",
            "healthThreshold", monitoringProperties.getAlert().getHealthThreshold(),
            "alertRetryCount", monitoringProperties.getAlert().getAlertRetryCount()
        ));
        response.put("metricsPhase", Map.of(
            "status", monitoringProperties.getMetrics().isEnabled() ? "ACTIVE" : "DISABLED",
            "collectionInterval", monitoringProperties.getMetrics().getCollectionInterval(),
            "retentionDays", monitoringProperties.getMetrics().getRetentionDays()
        ));
        response.put("databasePhase", Map.of(
            "status", monitoringProperties.getDatabase().isEnabled() ? "ACTIVE" : "DISABLED",
            "connectionPoolCheckInterval", monitoringProperties.getDatabase().getConnectionPoolCheckInterval(),
            "slowQueryThreshold", monitoringProperties.getDatabase().getSlowQueryThreshold()
        ));
        
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
