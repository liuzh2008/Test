package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.DatabaseHealthService;
import com.example.medaiassistant.service.TransactionRetryService;
import com.example.medaiassistant.service.OptimisticLockRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库健康监控控制器
 * 提供数据库健康状态、重试统计、性能指标等监控API
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DatabaseHealthController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthController.class);

    @Autowired
    private DatabaseHealthService databaseHealthService;

    @Autowired
    private TransactionRetryService transactionRetryService;

    @Autowired
    private OptimisticLockRetryService optimisticLockRetryService;

    /**
     * 获取数据库整体健康状态
     * GET /api/database/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        logger.debug("查询数据库健康状态");
        
        try {
            DatabaseHealthService.DatabaseHealthStats healthStats = databaseHealthService.getHealthStats();
            TransactionRetryService.RetryStats retryStats = transactionRetryService.getRetryStats();
            OptimisticLockRetryService.OptimisticLockStats lockStats = optimisticLockRetryService.getOptimisticLockStats();
            
            Map<String, Object> healthInfo = new HashMap<>();
            
            // 整体健康状态
            healthInfo.put("overall_healthy", healthStats.isHealthy());
            healthInfo.put("last_check_time", healthStats.getLastHealthCheck());
            healthInfo.put("check_timestamp", LocalDateTime.now());
            
            // 数据库连接统计
            Map<String, Object> connectionStats = new HashMap<>();
            connectionStats.put("success_rate", String.format("%.2f%%", healthStats.getSuccessRate() * 100));
            connectionStats.put("total_connections", healthStats.getTotalConnections());
            connectionStats.put("consecutive_failures", healthStats.getConsecutiveFailures());
            connectionStats.put("average_connection_time", healthStats.getAverageConnectionTime() + "ms");
            connectionStats.put("active_connections", healthStats.getActiveConnections());
            connectionStats.put("max_connections", healthStats.getMaxConnections());
            healthInfo.put("connection_stats", connectionStats);
            
            // 事务重试统计
            Map<String, Object> transactionStats = new HashMap<>();
            transactionStats.put("total_retries", retryStats.getTotalRetries());
            transactionStats.put("successful_retries", retryStats.getSuccessfulRetries());
            transactionStats.put("retry_success_rate", String.format("%.2f%%", retryStats.getSuccessRate() * 100));
            transactionStats.put("deadlock_retries", retryStats.getDeadlockRetries());
            transactionStats.put("timeout_retries", retryStats.getTimeoutRetries());
            transactionStats.put("average_retry_time", retryStats.getAverageRetryTime() + "ms");
            healthInfo.put("transaction_stats", transactionStats);
            
            // 乐观锁统计
            Map<String, Object> optimisticLockStats = new HashMap<>();
            optimisticLockStats.put("total_lock_retries", lockStats.getTotalRetries());
            optimisticLockStats.put("lock_retry_success_rate", String.format("%.2f%%", lockStats.getSuccessRate() * 100));
            optimisticLockStats.put("version_conflicts", lockStats.getVersionConflicts());
            optimisticLockStats.put("recommended_concurrency_level", optimisticLockRetryService.getRecommendedConcurrencyLevel());
            healthInfo.put("optimistic_lock_stats", optimisticLockStats);
            
            // 建议和警告
            Map<String, Object> recommendations = new HashMap<>();
            recommendations.put("should_reset_connection_pool", databaseHealthService.shouldResetConnectionPool());
            recommendations.put("should_reduce_concurrency", optimisticLockRetryService.shouldReduceConcurrency());
            recommendations.put("should_fallback_to_manual", transactionRetryService.shouldFallbackToManualProcessing());
            recommendations.put("recommended_retries", databaseHealthService.getRecommendedRetries());
            healthInfo.put("recommendations", recommendations);
            
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            logger.error("获取数据库健康状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取数据库健康状态失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 执行数据库健康检查
     * POST /api/database/health/check
     */
    @PostMapping("/health/check")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        logger.info("执行数据库健康检查");
        
        try {
            boolean healthy = databaseHealthService.performHealthCheck();
            
            Map<String, Object> result = new HashMap<>();
            result.put("healthy", healthy);
            result.put("check_time", LocalDateTime.now());
            
            if (healthy) {
                result.put("message", "数据库健康检查通过");
                logger.info("数据库健康检查通过");
            } else {
                result.put("message", "数据库健康检查失败");
                logger.warn("数据库健康检查失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("执行数据库健康检查失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("healthy", false);
            errorResponse.put("error", "健康检查执行失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("check_time", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取数据库连接池状态
     * GET /api/database/connection-pool
     */
    @GetMapping("/connection-pool")
    public ResponseEntity<Map<String, Object>> getConnectionPoolStatus() {
        logger.debug("查询数据库连接池状态");
        
        try {
            DatabaseHealthService.DatabaseHealthStats stats = databaseHealthService.getHealthStats();
            
            Map<String, Object> poolStatus = new HashMap<>();
            poolStatus.put("active_connections", stats.getActiveConnections());
            poolStatus.put("max_connections", stats.getMaxConnections());
            poolStatus.put("connection_utilization", 
                stats.getMaxConnections() > 0 ? 
                String.format("%.2f%%", (double) stats.getActiveConnections() / stats.getMaxConnections() * 100) : 
                "N/A");
            poolStatus.put("average_connection_time", stats.getAverageConnectionTime() + "ms");
            poolStatus.put("consecutive_failures", stats.getConsecutiveFailures());
            poolStatus.put("last_check", stats.getLastHealthCheck());
            poolStatus.put("should_reset", databaseHealthService.shouldResetConnectionPool());
            
            return ResponseEntity.ok(poolStatus);
            
        } catch (Exception e) {
            logger.error("获取连接池状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取连接池状态失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 重置数据库连接池
     * POST /api/database/connection-pool/reset
     */
    @PostMapping("/connection-pool/reset")
    public ResponseEntity<Map<String, Object>> resetConnectionPool() {
        logger.info("重置数据库连接池");
        
        try {
            boolean success = databaseHealthService.resetConnectionPool();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("reset_time", LocalDateTime.now());
            
            if (success) {
                result.put("message", "数据库连接池重置成功");
                logger.info("数据库连接池重置成功");
            } else {
                result.put("message", "数据库连接池重置失败");
                logger.warn("数据库连接池重置失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("重置数据库连接池失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "连接池重置失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("reset_time", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取事务重试统计
     * GET /api/database/transaction-retries
     */
    @GetMapping("/transaction-retries")
    public ResponseEntity<Map<String, Object>> getTransactionRetryStats() {
        logger.debug("查询事务重试统计");
        
        try {
            TransactionRetryService.RetryStats stats = transactionRetryService.getRetryStats();
            
            Map<String, Object> retryStats = new HashMap<>();
            retryStats.put("total_retries", stats.getTotalRetries());
            retryStats.put("successful_retries", stats.getSuccessfulRetries());
            retryStats.put("failed_retries", stats.getFailedRetries());
            retryStats.put("success_rate", String.format("%.2f%%", stats.getSuccessRate() * 100));
            retryStats.put("deadlock_retries", stats.getDeadlockRetries());
            retryStats.put("timeout_retries", stats.getTimeoutRetries());
            retryStats.put("average_retry_time", stats.getAverageRetryTime() + "ms");
            retryStats.put("should_fallback", transactionRetryService.shouldFallbackToManualProcessing());
            retryStats.put("query_time", LocalDateTime.now());
            
            return ResponseEntity.ok(retryStats);
            
        } catch (Exception e) {
            logger.error("获取事务重试统计失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取事务重试统计失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取乐观锁统计
     * GET /api/database/optimistic-lock
     */
    @GetMapping("/optimistic-lock")
    public ResponseEntity<Map<String, Object>> getOptimisticLockStats() {
        logger.debug("查询乐观锁统计");
        
        try {
            OptimisticLockRetryService.OptimisticLockStats stats = optimisticLockRetryService.getOptimisticLockStats();
            
            Map<String, Object> lockStats = new HashMap<>();
            lockStats.put("total_retries", stats.getTotalRetries());
            lockStats.put("successful_retries", stats.getSuccessfulRetries());
            lockStats.put("failed_retries", stats.getFailedRetries());
            lockStats.put("success_rate", String.format("%.2f%%", stats.getSuccessRate() * 100));
            lockStats.put("version_conflicts", stats.getVersionConflicts());
            lockStats.put("average_retry_time", stats.getAverageRetryTime() + "ms");
            lockStats.put("should_reduce_concurrency", optimisticLockRetryService.shouldReduceConcurrency());
            lockStats.put("recommended_concurrency_level", optimisticLockRetryService.getRecommendedConcurrencyLevel());
            lockStats.put("query_time", LocalDateTime.now());
            
            return ResponseEntity.ok(lockStats);
            
        } catch (Exception e) {
            logger.error("获取乐观锁统计失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取乐观锁统计失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 重置所有统计信息
     * POST /api/database/stats/reset
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetAllStats() {
        logger.info("重置所有数据库统计信息");
        
        try {
            // 重置各项统计
            transactionRetryService.resetRetryStats();
            optimisticLockRetryService.resetOptimisticLockStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "所有统计信息已重置");
            result.put("reset_time", LocalDateTime.now());
            
            logger.info("所有数据库统计信息重置成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("重置统计信息失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "重置统计信息失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("reset_time", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取数据库性能建议
     * GET /api/database/recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getDatabaseRecommendations() {
        logger.debug("获取数据库性能建议");
        
        try {
            Map<String, Object> recommendations = new HashMap<>();
            
            // 连接池建议
            if (databaseHealthService.shouldResetConnectionPool()) {
                recommendations.put("connection_pool", "建议重置连接池以解决连接问题");
            } else {
                recommendations.put("connection_pool", "连接池状态正常");
            }
            
            // 并发控制建议
            if (optimisticLockRetryService.shouldReduceConcurrency()) {
                recommendations.put("concurrency", "建议减少并发级别以降低乐观锁冲突");
                recommendations.put("recommended_concurrency_level", optimisticLockRetryService.getRecommendedConcurrencyLevel());
            } else {
                recommendations.put("concurrency", "并发控制状态正常");
                recommendations.put("recommended_concurrency_level", optimisticLockRetryService.getRecommendedConcurrencyLevel());
            }
            
            // 重试策略建议
            if (transactionRetryService.shouldFallbackToManualProcessing()) {
                recommendations.put("retry_strategy", "建议降级到手动处理模式");
            } else {
                recommendations.put("retry_strategy", "自动重试策略运行正常");
            }
            
            // 推荐的重试次数
            recommendations.put("recommended_retries", databaseHealthService.getRecommendedRetries());
            
            // 整体健康建议
            DatabaseHealthService.DatabaseHealthStats healthStats = databaseHealthService.getHealthStats();
            if (healthStats.getSuccessRate() > 0.95) {
                recommendations.put("overall", "数据库运行状态优秀");
            } else if (healthStats.getSuccessRate() > 0.8) {
                recommendations.put("overall", "数据库运行状态良好，建议继续监控");
            } else {
                recommendations.put("overall", "数据库运行状态需要关注，建议检查配置和网络");
            }
            
            recommendations.put("query_time", LocalDateTime.now());
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("获取数据库建议失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取数据库建议失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}