package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.AutoRecoveryEngine;
import com.example.medaiassistant.service.SelfHealingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动恢复API控制器
 * 提供自动恢复功能的HTTP接口
 */
@RestController
@RequestMapping("/api/auto-recovery")
public class AutoRecoveryController {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoRecoveryController.class);
    
    @Autowired
    private AutoRecoveryEngine autoRecoveryEngine;
    
    @Autowired
    private SelfHealingService selfHealingService;
    
    /**
     * 获取自动恢复状态概览
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAutoRecoveryStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 获取恢复统计信息
            AutoRecoveryEngine.RecoveryStatistics recoveryStats = autoRecoveryEngine.getRecoveryStatistics();
            status.put("recoveryStatistics", Map.of(
                "totalAttempts", recoveryStats.getTotalAttempts(),
                "successfulRecoveries", recoveryStats.getSuccessfulRecoveries(),
                "failedRecoveries", recoveryStats.getFailedRecoveries(),
                "successRate", recoveryStats.getSuccessRate(),
                "activeRecoveryTasks", recoveryStats.getActiveRecoveryTasks(),
                "averageRecoveryTime", recoveryStats.getAverageRecoveryTime()
            ));
            
            // 获取自愈操作统计
            SelfHealingService.HealingStatistics healingStats = selfHealingService.getHealingStatistics();
            status.put("healingStatistics", Map.of(
                "totalOperations", healingStats.getTotalOperations(),
                "successfulOperations", healingStats.getSuccessfulOperations(),
                "failedOperations", healingStats.getFailedOperations(),
                "successRate", healingStats.getSuccessRate()
            ));
            
            status.put("timestamp", System.currentTimeMillis());
            status.put("serviceAvailable", true);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", status,
                "message", "自动恢复状态获取成功"
            ));
            
        } catch (Exception e) {
            logger.error("获取自动恢复状态失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "获取自动恢复状态失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 手动触发故障恢复
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerManualRecovery(
            @RequestParam("failureType") String failureType,
            @RequestParam(value = "description", defaultValue = "手动触发的恢复") String description) {
        
        try {
            logger.info("手动触发故障恢复: {} - {}", failureType, description);
            
            // 解析故障类型
            AutoRecoveryEngine.FailureType type;
            try {
                type = AutoRecoveryEngine.FailureType.valueOf(failureType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "无效的故障类型: " + failureType,
                    "availableTypes", AutoRecoveryEngine.FailureType.values()
                ));
            }
            
            // 触发恢复
            AutoRecoveryEngine.RecoveryResult result = autoRecoveryEngine.triggerManualRecovery(type, description);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                        "recoveryTriggered", true,
                        "failureType", failureType,
                        "description", description
                    ),
                    "message", result.getMessage()
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", result.getMessage()
                ));
            }
            
        } catch (Exception e) {
            logger.error("手动触发恢复失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "手动触发恢复失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取恢复历史记录
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getRecoveryHistory(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        try {
            List<AutoRecoveryEngine.RecoveryRecord> history = autoRecoveryEngine.getRecoveryHistory(limit);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                    "recoveryHistory", history,
                    "totalRecords", history.size(),
                    "limit", limit
                ),
                "message", "恢复历史记录获取成功"
            ));
            
        } catch (Exception e) {
            logger.error("获取恢复历史记录失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "获取恢复历史记录失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 清除恢复历史记录
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearRecoveryHistory() {
        try {
            autoRecoveryEngine.clearRecoveryHistory();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "恢复历史记录已清除"
            ));
            
        } catch (Exception e) {
            logger.error("清除恢复历史记录失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "清除恢复历史记录失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 更新自动恢复配置
     */
    @PostMapping("/configuration")
    public ResponseEntity<Map<String, Object>> updateConfiguration(
            @RequestParam(value = "maxConcurrentRecoveries", defaultValue = "3") int maxConcurrentRecoveries,
            @RequestParam(value = "recoveryTimeoutMs", defaultValue = "300000") long recoveryTimeoutMs,
            @RequestParam(value = "maxRetryAttempts", defaultValue = "3") int maxRetryAttempts) {
        
        try {
            // 验证参数
            if (maxConcurrentRecoveries < 1 || maxConcurrentRecoveries > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "最大并发恢复数必须在1-10之间"
                ));
            }
            
            if (recoveryTimeoutMs < 30000 || recoveryTimeoutMs > 600000) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "恢复超时时间必须在30秒-10分钟之间"
                ));
            }
            
            if (maxRetryAttempts < 1 || maxRetryAttempts > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "最大重试次数必须在1-10之间"
                ));
            }
            
            // 更新配置
            autoRecoveryEngine.updateConfiguration(maxConcurrentRecoveries, maxRetryAttempts);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                    "maxConcurrentRecoveries", maxConcurrentRecoveries,
                    "maxRetryAttempts", maxRetryAttempts
                ),
                "message", "自动恢复配置已更新"
            ));
            
        } catch (Exception e) {
            logger.error("更新自动恢复配置失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "更新自动恢复配置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取支持的故障类型
     */
    @GetMapping("/failure-types")
    public ResponseEntity<Map<String, Object>> getSupportedFailureTypes() {
        try {
            AutoRecoveryEngine.FailureType[] failureTypes = AutoRecoveryEngine.FailureType.values();
            
            Map<String, String> typeDescriptions = new HashMap<>();
            typeDescriptions.put("DATABASE_CONNECTION_FAILED", "数据库连接失败");
            typeDescriptions.put("MEMORY_HIGH_USAGE", "内存使用率过高");
            typeDescriptions.put("NETWORK_FAILURE", "网络连接失败");
            typeDescriptions.put("THREAD_POOL_EXHAUSTED", "线程池耗尽");
            typeDescriptions.put("DISK_SPACE_LOW", "磁盘空间不足");
            typeDescriptions.put("SYSTEM_OVERLOAD", "系统负载过高");
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                    "failureTypes", failureTypes,
                    "descriptions", typeDescriptions
                ),
                "message", "支持的故障类型获取成功"
            ));
            
        } catch (Exception e) {
            logger.error("获取支持的故障类型失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "获取支持的故障类型失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 执行自愈操作
     */
    @PostMapping("/self-healing/{operation}")
    public ResponseEntity<Map<String, Object>> performSelfHealing(@PathVariable String operation) {
        try {
            boolean result;
            String operationName;
            
            switch (operation.toLowerCase()) {
                case "reset-database-pool":
                    result = selfHealingService.resetDatabaseConnectionPool();
                    operationName = "重置数据库连接池";
                    break;
                case "trigger-gc":
                    result = selfHealingService.triggerGarbageCollection();
                    operationName = "触发垃圾回收";
                    break;
                case "clear-cache":
                    result = selfHealingService.clearSystemCache();
                    operationName = "清理系统缓存";
                    break;
                case "reset-network":
                    result = selfHealingService.resetNetworkConnections();
                    operationName = "重置网络连接";
                    break;
                case "expand-thread-pool":
                    result = selfHealingService.expandThreadPool();
                    operationName = "扩展线程池";
                    break;
                case "clean-temp-files":
                    result = selfHealingService.cleanTemporaryFiles();
                    operationName = "清理临时文件";
                    break;
                case "restart-database":
                    result = selfHealingService.restartDatabaseConnections();
                    operationName = "重启数据库连接";
                    break;
                case "clear-stuck-tasks":
                    result = selfHealingService.clearStuckTasks();
                    operationName = "清理卡住的任务";
                    break;
                case "compress-logs":
                    result = selfHealingService.compressLogFiles();
                    operationName = "压缩日志文件";
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "不支持的自愈操作: " + operation,
                        "supportedOperations", List.of(
                            "reset-database-pool", "trigger-gc", "clear-cache", 
                            "reset-network", "expand-thread-pool", "clean-temp-files",
                            "restart-database", "clear-stuck-tasks", "compress-logs"
                        )
                    ));
            }
            
            if (result) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                        "operation", operation,
                        "operationName", operationName,
                        "result", "success"
                    ),
                    "message", operationName + "执行成功"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", operationName + "执行失败"
                ));
            }
            
        } catch (Exception e) {
            logger.error("执行自愈操作失败: {}", operation, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "执行自愈操作失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取自愈操作统计
     */
    @GetMapping("/self-healing/statistics")
    public ResponseEntity<Map<String, Object>> getSelfHealingStatistics() {
        try {
            SelfHealingService.HealingStatistics stats = selfHealingService.getHealingStatistics();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                    "totalOperations", stats.getTotalOperations(),
                    "successfulOperations", stats.getSuccessfulOperations(),
                    "failedOperations", stats.getFailedOperations(),
                    "successRate", stats.getSuccessRate(),
                    "timestamp", System.currentTimeMillis()
                ),
                "message", "自愈操作统计获取成功"
            ));
            
        } catch (Exception e) {
            logger.error("获取自愈操作统计失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "获取自愈操作统计失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 重置自愈操作统计
     */
    @PostMapping("/self-healing/statistics/reset")
    public ResponseEntity<Map<String, Object>> resetSelfHealingStatistics() {
        try {
            selfHealingService.resetStatistics();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "自愈操作统计已重置"
            ));
            
        } catch (Exception e) {
            logger.error("重置自愈操作统计失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "重置自愈操作统计失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // 检查自动恢复引擎状态
            AutoRecoveryEngine.RecoveryStatistics stats = autoRecoveryEngine.getRecoveryStatistics();
            boolean healthy = stats.getActiveRecoveryTasks() < 5; // 活跃任务数少于5认为健康
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                    "healthy", healthy,
                    "activeRecoveryTasks", stats.getActiveRecoveryTasks(),
                    "uptime", System.currentTimeMillis(),
                    "version", "1.0.0"
                ),
                "message", healthy ? "自动恢复服务健康" : "自动恢复服务繁忙"
            ));
            
        } catch (Exception e) {
            logger.error("自动恢复健康检查失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "自动恢复健康检查失败: " + e.getMessage()
            ));
        }
    }
}
