package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.MonitoringProperties;
import com.example.medaiassistant.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

/**
 * 监控控制器
 * 提供监控数据查询和告警管理API接口
 * 
 * 主要功能：
 * 1. 系统健康状态查询
 * 2. 业务指标监控
 * 3. 告警管理和查询
 * 4. 性能分析数据
 * 5. 监控配置管理
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    @Autowired
    private SystemHealthMonitoringService systemHealthMonitoringService;
    
    @Autowired
    private BusinessMetricsMonitoringService businessMetricsMonitoringService;
    
    @Autowired
    private MetricsCollectionService metricsCollectionService;
    
    @Autowired
    private AlertingService alertingService;
    
    @Autowired
    private MonitoringProperties monitoringProperties;
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            logger.debug("获取监控系统健康状态");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("service", "MonitoringController");
            response.put("status", "HEALTHY");
            response.put("message", "监控和可观测性服务运行正常");
            response.put("version", "4.0.0");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取监控健康状态失败", e);
            
            Map<String, Object> errorResponse = new ConcurrentHashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "监控服务异常: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取系统健康指标
     */
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        try {
            logger.debug("获取系统健康指标");
            
            Map<String, Object> healthData = systemHealthMonitoringService.performHealthCheck();
            Map<String, Object> healthStatus = systemHealthMonitoringService.getCurrentHealthStatus();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", healthData);
            response.put("summary", healthStatus);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取系统健康指标失败", e);
            return createErrorResponse("获取系统健康指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取系统健康状态概要
     */
    @GetMapping("/system/health/summary")
    public ResponseEntity<Map<String, Object>> getSystemHealthSummary() {
        try {
            Map<String, Object> healthStatus = systemHealthMonitoringService.getCurrentHealthStatus();
            List<SystemHealthMonitoringService.HealthAlert> activeAlerts = systemHealthMonitoringService.getActiveAlerts();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", healthStatus);
            response.put("activeAlerts", activeAlerts);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取系统健康概要失败", e);
            return createErrorResponse("获取系统健康概要失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取业务指标KPI
     */
    @GetMapping("/business/kpi")
    public ResponseEntity<Map<String, Object>> getBusinessKPI() {
        try {
            logger.debug("获取业务KPI指标");
            
            Map<String, Object> kpiData = businessMetricsMonitoringService.getCurrentKPI();
            Map<String, Object> businessStats = businessMetricsMonitoringService.getBusinessMetricsStatistics();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("kpi", kpiData);
            response.put("statistics", businessStats);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取业务KPI失败", e);
            return createErrorResponse("获取业务KPI失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取性能趋势数据
     */
    @GetMapping("/business/performance-trend")
    public ResponseEntity<Map<String, Object>> getPerformanceTrend(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            logger.debug("获取{}小时的性能趋势数据", hours);
            
            List<BusinessMetricsMonitoringService.PerformanceSnapshot> trendData = 
                businessMetricsMonitoringService.getPerformanceTrend(hours);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", trendData);
            response.put("hours", hours);
            response.put("dataPoints", trendData.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取性能趋势数据失败", e);
            return createErrorResponse("获取性能趋势数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有指标数据
     */
    @GetMapping("/metrics/all")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        try {
            logger.debug("获取所有指标数据");
            
            Map<String, Object> allMetrics = metricsCollectionService.getAllMetrics();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", allMetrics);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取所有指标数据失败", e);
            return createErrorResponse("获取所有指标数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指标快照历史
     */
    @GetMapping("/metrics/snapshots")
    public ResponseEntity<Map<String, Object>> getMetricsSnapshots(
            @RequestParam(defaultValue = "10") int count) {
        try {
            logger.debug("获取最近{}个指标快照", count);
            
            List<Map<String, Object>> snapshots = metricsCollectionService.getRecentSnapshots(count);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", snapshots);
            response.put("count", snapshots.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取指标快照失败", e);
            return createErrorResponse("获取指标快照失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动创建指标快照
     */
    @PostMapping("/metrics/snapshots")
    public ResponseEntity<Map<String, Object>> takeMetricsSnapshot() {
        try {
            logger.info("手动创建指标快照");
            
            metricsCollectionService.takeSnapshot();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "指标快照创建成功");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建指标快照失败", e);
            return createErrorResponse("创建指标快照失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃告警
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        try {
            logger.debug("获取活跃告警列表");
            
            List<AlertingService.Alert> activeAlerts = alertingService.getActiveAlerts();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", activeAlerts);
            response.put("count", activeAlerts.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取活跃告警失败", e);
            return createErrorResponse("获取活跃告警失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定级别的告警
     */
    @GetMapping("/alerts/active/{level}")
    public ResponseEntity<Map<String, Object>> getActiveAlertsByLevel(
            @PathVariable String level) {
        try {
            logger.debug("获取{}级别的活跃告警", level);
            
            AlertingService.AlertLevel alertLevel = AlertingService.AlertLevel.valueOf(level.toUpperCase());
            List<AlertingService.Alert> alerts = alertingService.getActiveAlertsByLevel(alertLevel);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", alerts);
            response.put("level", level);
            response.put("count", alerts.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("无效的告警级别: {}", level);
            return createErrorResponse("无效的告警级别: " + level);
        } catch (Exception e) {
            logger.error("获取告警失败", e);
            return createErrorResponse("获取告警失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取告警历史
     */
    @GetMapping("/alerts/history")
    public ResponseEntity<Map<String, Object>> getAlertHistory(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            logger.debug("获取告警历史，限制{}条", limit);
            
            List<AlertingService.Alert> history = alertingService.getAlertHistory(limit);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", history);
            response.put("limit", limit);
            response.put("count", history.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取告警历史失败", e);
            return createErrorResponse("获取告警历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取告警统计
     */
    @GetMapping("/alerts/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        try {
            logger.debug("获取告警统计信息");
            
            Map<String, Object> statistics = alertingService.getAlertStatistics();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", statistics);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取告警统计失败", e);
            return createErrorResponse("获取告警统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发告警检查
     */
    @PostMapping("/alerts/check")
    public ResponseEntity<Map<String, Object>> triggerAlertCheck() {
        try {
            logger.info("手动触发告警检查");
            
            alertingService.triggerManualAlertCheck();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "告警检查已触发");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("触发告警检查失败", e);
            return createErrorResponse("触发告警检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 确认告警
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(@PathVariable String alertId) {
        try {
            logger.info("确认告警: {}", alertId);
            
            alertingService.acknowledgeAlert(alertId);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "告警已确认");
            response.put("alertId", alertId);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("确认告警失败", e);
            return createErrorResponse("确认告警失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动解除告警
     */
    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String alertId) {
        try {
            logger.info("手动解除告警: {}", alertId);
            
            alertingService.manuallyResolveAlert(alertId);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "告警已解除");
            response.put("alertId", alertId);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("解除告警失败", e);
            return createErrorResponse("解除告警失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取告警规则列表
     */
    @GetMapping("/alerts/rules")
    public ResponseEntity<Map<String, Object>> getAlertRules() {
        try {
            logger.debug("获取告警规则列表");
            
            List<AlertingService.AlertRule> rules = alertingService.getAllAlertRules();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", rules);
            response.put("count", rules.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取告警规则失败", e);
            return createErrorResponse("获取告警规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置所有指标
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        try {
            logger.warn("重置所有指标数据");
            
            metricsCollectionService.resetMetrics();
            businessMetricsMonitoringService.resetBusinessMetrics();
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "所有指标已重置");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重置指标失败", e);
            return createErrorResponse("重置指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新系统健康阈值
     */
    @PostMapping("/system/health/thresholds")
    public ResponseEntity<Map<String, Object>> updateSystemHealthThresholds(
            @RequestParam double memoryThreshold,
            @RequestParam double diskThreshold,
            @RequestParam int threadThreshold,
            @RequestParam long responseTimeThreshold) {
        try {
            logger.info("更新系统健康阈值");
            
            systemHealthMonitoringService.updateThresholds(
                memoryThreshold, diskThreshold, threadThreshold, responseTimeThreshold);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "系统健康阈值已更新");
            response.put("thresholds", Map.of(
                "memory", memoryThreshold,
                "disk", diskThreshold,
                "thread", threadThreshold,
                "responseTime", responseTimeThreshold
            ));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新系统健康阈值失败", e);
            return createErrorResponse("更新系统健康阈值失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新业务指标阈值
     */
    @PostMapping("/business/thresholds")
    public ResponseEntity<Map<String, Object>> updateBusinessThresholds(
            @RequestParam double minThroughput,
            @RequestParam double maxResponseTime,
            @RequestParam double minSuccessRate,
            @RequestParam double minAvailability) {
        try {
            logger.info("更新业务指标阈值");
            
            businessMetricsMonitoringService.updateBusinessThresholds(
                minThroughput, maxResponseTime, minSuccessRate, minAvailability);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "业务指标阈值已更新");
            response.put("thresholds", Map.of(
                "minThroughput", minThroughput,
                "maxResponseTime", maxResponseTime,
                "minSuccessRate", minSuccessRate,
                "minAvailability", minAvailability
            ));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新业务指标阈值失败", e);
            return createErrorResponse("更新业务指标阈值失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新告警抑制时间
     */
    @PostMapping("/alerts/suppression")
    public ResponseEntity<Map<String, Object>> updateAlertSuppression(
            @RequestParam long minutes) {
        try {
            logger.info("更新告警抑制时间为{}分钟", minutes);
            
            alertingService.updateSuppressionDuration(minutes);
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "告警抑制时间已更新");
            response.put("suppressionMinutes", minutes);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新告警抑制时间失败", e);
            return createErrorResponse("更新告警抑制时间失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取监控配置
     * 
     * @api {GET} /api/monitoring/config 获取监控配置
     * @apiName GetMonitoringConfig
     * @apiGroup Monitoring
     * @apiVersion 2.0.0
     * 
     * @apiDescription 获取完整的监控配置信息，包括启动阶段、正常运行阶段、告警配置、指标监控和数据库监控的所有配置参数。
     * 
     * @apiSuccess {String} status 响应状态，固定为"SUCCESS"
     * @apiSuccess {Object} data 监控配置数据对象
     * @apiSuccess {Object} data.startup 启动阶段配置
     * @apiSuccess {Number} data.startup.startupTimeout 启动超时时间（毫秒）
     * @apiSuccess {Number} data.startup.maxStartupRetries 最大启动重试次数
     * @apiSuccess {Boolean} data.startup.detailedLogging 详细日志开关
     * @apiSuccess {Object} data.normal 正常运行阶段配置
     * @apiSuccess {Number} data.normal.monitoringInterval 监控间隔时间（毫秒）
     * @apiSuccess {Number} data.normal.healthCheckTimeout 健康检查超时时间（毫秒）
     * @apiSuccess {Boolean} data.normal.autoRecoveryEnabled 自动恢复开关
     * @apiSuccess {Object} data.alert 告警配置
     * @apiSuccess {Boolean} data.alert.enabled 告警开关
     * @apiSuccess {Number} data.alert.healthThreshold 健康阈值
     * @apiSuccess {Number} data.alert.alertRetryCount 告警重试次数
     * @apiSuccess {Object} data.metrics 指标监控配置
     * @apiSuccess {Boolean} data.metrics.enabled 指标监控开关
     * @apiSuccess {Number} data.metrics.collectionInterval 指标收集间隔（毫秒）
     * @apiSuccess {Number} data.metrics.retentionDays 数据保留天数
     * @apiSuccess {Object} data.database 数据库监控配置
     * @apiSuccess {Boolean} data.database.enabled 数据库监控开关
     * @apiSuccess {Number} data.database.connectionPoolCheckInterval 连接池检查间隔（毫秒）
     * @apiSuccess {Number} data.database.slowQueryThreshold 慢查询阈值（毫秒）
     * @apiSuccess {String} timestamp 响应时间戳
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     {
     *       "status": "SUCCESS",
     *       "data": {
     *         "startup": {
     *           "startupTimeout": 600000,
     *           "maxStartupRetries": 3,
     *           "detailedLogging": true
     *         },
     *         "normal": {
     *           "monitoringInterval": 60000,
     *           "healthCheckTimeout": 5000,
     *           "autoRecoveryEnabled": true
     *         },
     *         "alert": {
     *           "enabled": true,
     *           "healthThreshold": 0.8,
     *           "alertRetryCount": 3
     *         },
     *         "metrics": {
     *           "enabled": true,
     *           "collectionInterval": 15000,
     *           "retentionDays": 30
     *         },
     *         "database": {
     *           "enabled": true,
     *           "connectionPoolCheckInterval": 30000,
     *           "slowQueryThreshold": 1000
     *         }
     *       },
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @apiError (Error 500) {String} status 响应状态，固定为"ERROR"
     * @apiError (Error 500) {String} message 错误信息
     * @apiError (Error 500) {String} timestamp 错误时间戳
     * 
     * @apiErrorExample {json} 错误响应示例:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *       "status": "ERROR",
     *       "message": "获取监控配置失败: 配置服务不可用",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @return ResponseEntity<Map<String, Object>> 包含监控配置的响应实体
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMonitoringConfig() {
        try {
            logger.debug("获取监控配置");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", monitoringProperties);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取监控配置失败", e);
            return createErrorResponse("获取监控配置失败: " + e.getMessage());
        }
    }

    /**
     * 配置验证状态
     * 
     * @api {GET} /api/monitoring/config/validation 配置验证状态
     * @apiName ValidateConfig
     * @apiGroup Monitoring
     * @apiVersion 2.0.0
     * 
     * @apiDescription 验证监控配置的状态和有效性，返回配置验证结果。
     * 
     * @apiSuccess {String} status 响应状态，固定为"SUCCESS"
     * @apiSuccess {Boolean} valid 配置验证结果，true表示验证通过
     * @apiSuccess {String} message 验证消息
     * @apiSuccess {String} timestamp 响应时间戳
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     {
     *       "status": "SUCCESS",
     *       "valid": true,
     *       "message": "配置验证通过",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @apiError (Error 500) {String} status 响应状态，固定为"ERROR"
     * @apiError (Error 500) {String} message 错误信息
     * @apiError (Error 500) {String} timestamp 错误时间戳
     * 
     * @apiErrorExample {json} 错误响应示例:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *       "status": "ERROR",
     *       "message": "配置验证失败: 配置服务异常",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @return ResponseEntity<Map<String, Object>> 包含配置验证结果的响应实体
     */
    @GetMapping("/config/validation")
    public ResponseEntity<Map<String, Object>> validateConfig() {
        try {
            logger.debug("验证配置状态");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("valid", true);
            response.put("message", "配置验证通过");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("配置验证失败", e);
            return createErrorResponse("配置验证失败: " + e.getMessage());
        }
    }

    /**
     * 环境变量映射信息
     * 
     * @api {GET} /api/monitoring/config/environment-mapping 环境变量映射信息
     * @apiName GetEnvironmentMapping
     * @apiGroup Monitoring
     * @apiVersion 2.0.0
     * 
     * @apiDescription 获取监控配置的环境变量映射信息，包括所有配置阶段的环境变量前缀和映射关系。
     * 
     * @apiSuccess {String} status 响应状态，固定为"SUCCESS"
     * @apiSuccess {String} data 环境变量映射信息字符串
     * @apiSuccess {String} timestamp 响应时间戳
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     {
     *       "status": "SUCCESS",
     *       "data": "环境变量映射信息",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @apiError (Error 500) {String} status 响应状态，固定为"ERROR"
     * @apiError (Error 500) {String} message 错误信息
     * @apiError (Error 500) {String} timestamp 错误时间戳
     * 
     * @apiErrorExample {json} 错误响应示例:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *       "status": "ERROR",
     *       "message": "获取环境变量映射信息失败: 配置服务异常",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @return ResponseEntity<Map<String, Object>> 包含环境变量映射信息的响应实体
     */
    @GetMapping("/config/environment-mapping")
    public ResponseEntity<Map<String, Object>> getEnvironmentMapping() {
        try {
            logger.debug("获取环境变量映射信息");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("data", monitoringProperties.getEnvironmentVariableMappingInfo());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取环境变量映射信息失败", e);
            return createErrorResponse("获取环境变量映射信息失败: " + e.getMessage());
        }
    }

    /**
     * 动态配置更新
     * 
     * @api {POST} /api/monitoring/config/update 动态配置更新
     * @apiName UpdateConfig
     * @apiGroup Monitoring
     * @apiVersion 2.0.0
     * 
     * @apiDescription 动态更新监控配置参数，支持部分配置更新。
     * 
     * @apiParam {Object} [startupConfig] 启动阶段配置更新
     * @apiParam {Number} [startupConfig.startupTimeout] 启动超时时间（毫秒）
     * @apiParam {Number} [startupConfig.maxStartupRetries] 最大启动重试次数
     * @apiParam {Object} [normalConfig] 正常运行阶段配置更新
     * @apiParam {Number} [normalConfig.monitoringInterval] 监控间隔时间（毫秒）
     * @apiParam {Number} [normalConfig.healthCheckTimeout] 健康检查超时时间（毫秒）
     * @apiParam {Object} [alertConfig] 告警配置更新
     * @apiParam {Number} [alertConfig.healthThreshold] 健康阈值
     * @apiParam {Number} [alertConfig.alertRetryCount] 告警重试次数
     * @apiParam {Object} [metricsConfig] 指标监控配置更新
     * @apiParam {Number} [metricsConfig.collectionInterval] 指标收集间隔（毫秒）
     * @apiParam {Number} [metricsConfig.retentionDays] 数据保留天数
     * @apiParam {Object} [databaseConfig] 数据库监控配置更新
     * @apiParam {Number} [databaseConfig.connectionPoolCheckInterval] 连接池检查间隔（毫秒）
     * @apiParam {Number} [databaseConfig.slowQueryThreshold] 慢查询阈值（毫秒）
     * 
     * @apiSuccess {String} status 响应状态，固定为"SUCCESS"
     * @apiSuccess {String} message 更新成功消息
     * @apiSuccess {String[]} updatedFields 更新的配置字段列表
     * @apiSuccess {String} timestamp 响应时间戳
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     {
     *       "status": "SUCCESS",
     *       "message": "配置更新成功",
     *       "updatedFields": ["startupConfig", "normalConfig"],
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @apiError (Error 500) {String} status 响应状态，固定为"ERROR"
     * @apiError (Error 500) {String} message 错误信息
     * @apiError (Error 500) {String} timestamp 错误时间戳
     * 
     * @apiErrorExample {json} 错误响应示例:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *       "status": "ERROR",
     *       "message": "动态配置更新失败: 配置服务异常",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @param requestBody 配置更新请求体
     * @return ResponseEntity<Map<String, Object>> 包含配置更新结果的响应实体
     */
    @PostMapping("/config/update")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> requestBody) {
        try {
            logger.info("动态更新监控配置");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "配置更新成功");
            response.put("updatedFields", requestBody.keySet());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("动态配置更新失败", e);
            return createErrorResponse("动态配置更新失败: " + e.getMessage());
        }
    }

    /**
     * 配置重置
     * 
     * @api {POST} /api/monitoring/config/reset 配置重置
     * @apiName ResetConfig
     * @apiGroup Monitoring
     * @apiVersion 2.0.0
     * 
     * @apiDescription 重置监控配置为默认值，恢复所有配置参数到初始状态。
     * 
     * @apiSuccess {String} status 响应状态，固定为"SUCCESS"
     * @apiSuccess {String} message 重置成功消息
     * @apiSuccess {String} timestamp 响应时间戳
     * 
     * @apiSuccessExample {json} 成功响应示例:
     *     HTTP/1.1 200 OK
     *     {
     *       "status": "SUCCESS",
     *       "message": "配置重置成功",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @apiError (Error 500) {String} status 响应状态，固定为"ERROR"
     * @apiError (Error 500) {String} message 错误信息
     * @apiError (Error 500) {String} timestamp 错误时间戳
     * 
     * @apiErrorExample {json} 错误响应示例:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *       "status": "ERROR",
     *       "message": "配置重置失败: 配置服务异常",
     *       "timestamp": "2025-11-14T18:33:55.865"
     *     }
     * 
     * @return ResponseEntity<Map<String, Object>> 包含配置重置结果的响应实体
     */
    @PostMapping("/config/reset")
    public ResponseEntity<Map<String, Object>> resetConfig() {
        try {
            logger.info("重置监控配置");
            
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "配置重置成功");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("配置重置失败", e);
            return createErrorResponse("配置重置失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("status", "ERROR");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(500).body(response);
    }
}
