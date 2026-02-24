package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自动恢复引擎
 * 负责检测系统故障并自动执行恢复策略
 */
@Component
public class AutoRecoveryEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoRecoveryEngine.class);
    
    @Autowired
    private SystemHealthMonitoringService systemHealthMonitoringService;
    
    @Autowired
    private DatabaseHealthService databaseHealthService;
    
    @Autowired
    private NetworkHealthService networkHealthService;
    
    @Autowired
    private AlertingService alertingService;
    
    @Autowired
    private SelfHealingService selfHealingService;
    
    // 恢复统计信息
    private final AtomicLong totalRecoveryAttempts = new AtomicLong(0);
    private final AtomicLong successfulRecoveries = new AtomicLong(0);
    private final AtomicLong failedRecoveries = new AtomicLong(0);
    private final AtomicInteger activeRecoveryTasks = new AtomicInteger(0);
    
    // 恢复历史记录
    private final List<RecoveryRecord> recoveryHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    // 故障类型和对应的恢复策略
    private final Map<FailureType, List<RecoveryStrategy>> recoveryStrategies = new HashMap<>();
    
    // 线程池用于并发执行恢复任务
    private ExecutorService recoveryExecutor;
    
    // 配置参数
    private int maxConcurrentRecoveries = 3;
    private int maxRetryAttempts = 3;
    
    public void initializeAutoRecovery() {
        logger.info("初始化自动恢复引擎...");
        
        // 初始化线程池
        recoveryExecutor = Executors.newFixedThreadPool(maxConcurrentRecoveries);
        
        // 初始化恢复策略
        initializeRecoveryStrategies();
        
        logger.info("自动恢复引擎初始化完成");
    }
    
    /**
     * 初始化恢复策略
     */
    private void initializeRecoveryStrategies() {
        // 数据库故障恢复策略
        recoveryStrategies.put(FailureType.DATABASE_CONNECTION_FAILED, Arrays.asList(
            RecoveryStrategy.RESET_DATABASE_CONNECTION_POOL,
            RecoveryStrategy.RESTART_DATABASE_CONNECTIONS,
            RecoveryStrategy.SWITCH_TO_BACKUP_DATABASE
        ));
        
        // 内存不足恢复策略
        recoveryStrategies.put(FailureType.MEMORY_HIGH_USAGE, Arrays.asList(
            RecoveryStrategy.TRIGGER_GARBAGE_COLLECTION,
            RecoveryStrategy.CLEAR_CACHE,
            RecoveryStrategy.RESTART_MEMORY_INTENSIVE_SERVICES
        ));
        
        // 网络故障恢复策略
        recoveryStrategies.put(FailureType.NETWORK_FAILURE, Arrays.asList(
            RecoveryStrategy.RESET_NETWORK_CONNECTIONS,
            RecoveryStrategy.SWITCH_TO_BACKUP_ENDPOINT,
            RecoveryStrategy.RESTART_NETWORK_SERVICES
        ));
        
        // 线程池耗尽恢复策略
        recoveryStrategies.put(FailureType.THREAD_POOL_EXHAUSTED, Arrays.asList(
            RecoveryStrategy.EXPAND_THREAD_POOL,
            RecoveryStrategy.CLEAR_STUCK_TASKS,
            RecoveryStrategy.RESTART_THREAD_POOL
        ));
        
        // 磁盘空间不足恢复策略
        recoveryStrategies.put(FailureType.DISK_SPACE_LOW, Arrays.asList(
            RecoveryStrategy.CLEAN_TEMPORARY_FILES,
            RecoveryStrategy.COMPRESS_LOG_FILES,
            RecoveryStrategy.ARCHIVE_OLD_DATA
        ));
        
        logger.debug("恢复策略初始化完成，共配置 {} 种故障类型", recoveryStrategies.size());
    }
    
    /**
     * 定期执行故障检测和自动恢复（每2分钟）
     */
    @Scheduled(fixedDelay = 120000)
    public void performAutomaticRecovery() {
        if (activeRecoveryTasks.get() >= maxConcurrentRecoveries) {
            logger.debug("当前恢复任务已达到最大并发数，跳过本次检查");
            return;
        }
        
        try {
            // 检测系统故障
            List<FailureDetection> failures = detectSystemFailures();
            
            if (!failures.isEmpty()) {
                logger.info("检测到 {} 个系统故障，开始执行自动恢复", failures.size());
                
                for (FailureDetection failure : failures) {
                    if (activeRecoveryTasks.get() >= maxConcurrentRecoveries) {
                        logger.warn("恢复任务队列已满，剩余故障将在下次检查时处理");
                        break;
                    }
                    
                    // 异步执行恢复
                    executeRecoveryAsync(failure);
                }
            }
            
        } catch (Exception e) {
            logger.error("自动恢复执行异常", e);
        }
    }
    
    /**
     * 检测系统故障
     */
    private List<FailureDetection> detectSystemFailures() {
        List<FailureDetection> failures = new ArrayList<>();
        
        try {
            // 1. 检查系统健康状态
            Map<String, Object> healthStatus = systemHealthMonitoringService.getCurrentHealthStatus();
            if (!(Boolean) healthStatus.getOrDefault("healthy", true)) {
                // 分析具体的健康问题
                List<SystemHealthMonitoringService.HealthAlert> alerts = 
                    systemHealthMonitoringService.getActiveAlerts();
                
                for (SystemHealthMonitoringService.HealthAlert alert : alerts) {
                    FailureType failureType = mapAlertToFailureType(alert.getType());
                    if (failureType != null) {
                        failures.add(new FailureDetection(
                            failureType,
                            alert.getMessage(),
                            alert.getSeverity(),
                            LocalDateTime.now()
                        ));
                    }
                }
            }
            
            // 2. 检查数据库健康状态
            if (!databaseHealthService.isDatabaseHealthy()) {
                failures.add(new FailureDetection(
                    FailureType.DATABASE_CONNECTION_FAILED,
                    "数据库连接健康检查失败",
                    "CRITICAL",
                    LocalDateTime.now()
                ));
            }
            
            // 3. 检查网络健康状态
            if (!networkHealthService.isNetworkHealthy()) {
                failures.add(new FailureDetection(
                    FailureType.NETWORK_FAILURE,
                    "网络连接健康检查失败",
                    "WARNING",
                    LocalDateTime.now()
                ));
            }
            
        } catch (Exception e) {
            logger.error("故障检测过程中发生异常", e);
        }
        
        return failures;
    }
    
    /**
     * 将告警类型映射到故障类型
     */
    private FailureType mapAlertToFailureType(String alertType) {
        switch (alertType) {
            case "MEMORY_HIGH_USAGE":
                return FailureType.MEMORY_HIGH_USAGE;
            case "DATABASE_CONNECTION_FAILED":
            case "DATABASE_SLOW_RESPONSE":
                return FailureType.DATABASE_CONNECTION_FAILED;
            case "THREAD_COUNT_HIGH":
                return FailureType.THREAD_POOL_EXHAUSTED;
            case "DISK_HIGH_USAGE":
                return FailureType.DISK_SPACE_LOW;
            case "SYSTEM_HIGH_LOAD":
                return FailureType.SYSTEM_OVERLOAD;
            default:
                return null;
        }
    }
    
    /**
     * 异步执行故障恢复
     */
    private void executeRecoveryAsync(FailureDetection failure) {
        activeRecoveryTasks.incrementAndGet();
        totalRecoveryAttempts.incrementAndGet();
        
        CompletableFuture.runAsync(() -> {
            try {
                executeRecovery(failure);
            } finally {
                activeRecoveryTasks.decrementAndGet();
            }
        }, recoveryExecutor);
    }
    
    /**
     * 执行故障恢复
     */
    private void executeRecovery(FailureDetection failure) {
        RecoveryRecord record = new RecoveryRecord(
            failure.getType(),
            failure.getDescription(),
            LocalDateTime.now()
        );
        
        try {
            logger.info("开始执行故障恢复: {} - {}", failure.getType(), failure.getDescription());
            
            List<RecoveryStrategy> strategies = recoveryStrategies.get(failure.getType());
            if (strategies == null || strategies.isEmpty()) {
                logger.warn("未找到故障类型 {} 的恢复策略", failure.getType());
                record.setStatus(RecoveryStatus.FAILED);
                record.setErrorMessage("未配置恢复策略");
                return;
            }
            
            boolean recovered = false;
            String lastError = null;
            
            // 尝试所有恢复策略
            for (int attempt = 0; attempt < maxRetryAttempts && !recovered; attempt++) {
                for (RecoveryStrategy strategy : strategies) {
                    try {
                        logger.info("尝试恢复策略: {} (第{}次尝试)", strategy, attempt + 1);
                        
                        boolean success = executeRecoveryStrategy(strategy, failure);
                        if (success) {
                            // 验证恢复效果
                            if (verifyRecovery(failure.getType())) {
                                recovered = true;
                                record.setStatus(RecoveryStatus.SUCCESS);
                                record.setRecoveryStrategy(strategy.toString());
                                record.setAttempts(attempt + 1);
                                successfulRecoveries.incrementAndGet();
                                
                                logger.info("故障恢复成功: {} 使用策略: {}", 
                                    failure.getType(), strategy);
                                break;
                            }
                        }
                        
                    } catch (Exception e) {
                        lastError = e.getMessage();
                        logger.warn("恢复策略 {} 执行失败: {}", strategy, e.getMessage());
                    }
                    
                    // 策略间等待
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // 尝试间等待
                if (!recovered && attempt < maxRetryAttempts - 1) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            if (!recovered) {
                record.setStatus(RecoveryStatus.FAILED);
                record.setErrorMessage(lastError != null ? lastError : "所有恢复策略都失败");
                failedRecoveries.incrementAndGet();
                
                logger.error("故障恢复失败: {} - 所有策略都已尝试", failure.getType());
                
                // 发送告警
                // 创建临时告警规则
                AlertingService.AlertRule recoveryFailedRule = new AlertingService.AlertRule(
                    "AUTO_RECOVERY_FAILED",
                    "自动恢复失败: " + failure.getDescription(),
                    "auto.recovery.failed",
                    AlertingService.AlertRule.Operator.EQUALS,
                    1.0,
                    AlertingService.AlertLevel.CRITICAL
                );
                
                alertingService.addAlertRule(recoveryFailedRule);
            }
            
        } catch (Exception e) {
            record.setStatus(RecoveryStatus.ERROR);
            record.setErrorMessage("恢复过程异常: " + e.getMessage());
            failedRecoveries.incrementAndGet();
            
            logger.error("故障恢复过程异常", e);
            
        } finally {
            record.setEndTime(LocalDateTime.now());
            record.setDuration(record.calculateDuration());
            
            // 记录恢复历史
            addRecoveryRecord(record);
        }
    }
    
    /**
     * 执行具体的恢复策略
     */
    private boolean executeRecoveryStrategy(RecoveryStrategy strategy, FailureDetection failure) {
        switch (strategy) {
            case RESET_DATABASE_CONNECTION_POOL:
                return selfHealingService.resetDatabaseConnectionPool();
                
            case TRIGGER_GARBAGE_COLLECTION:
                return selfHealingService.triggerGarbageCollection();
                
            case CLEAR_CACHE:
                return selfHealingService.clearSystemCache();
                
            case RESET_NETWORK_CONNECTIONS:
                return selfHealingService.resetNetworkConnections();
                
            case EXPAND_THREAD_POOL:
                return selfHealingService.expandThreadPool();
                
            case CLEAN_TEMPORARY_FILES:
                return selfHealingService.cleanTemporaryFiles();
                
            case RESTART_DATABASE_CONNECTIONS:
                return selfHealingService.restartDatabaseConnections();
                
            case CLEAR_STUCK_TASKS:
                return selfHealingService.clearStuckTasks();
                
            case COMPRESS_LOG_FILES:
                return selfHealingService.compressLogFiles();
                
            default:
                logger.warn("未实现的恢复策略: {}", strategy);
                return false;
        }
    }
    
    /**
     * 验证恢复效果
     */
    private boolean verifyRecovery(FailureType failureType) {
        try {
            Thread.sleep(5000); // 等待恢复生效
            
            switch (failureType) {
                case DATABASE_CONNECTION_FAILED:
                    return databaseHealthService.performHealthCheck();
                    
                case NETWORK_FAILURE:
                    return networkHealthService.performHealthCheck();
                    
                case MEMORY_HIGH_USAGE:
                case THREAD_POOL_EXHAUSTED:
                case DISK_SPACE_LOW:
                case SYSTEM_OVERLOAD:
                    Map<String, Object> healthStatus = systemHealthMonitoringService.performHealthCheck();
                    return (Boolean) healthStatus.getOrDefault("overallStatus", "HEALTHY").equals("HEALTHY");
                    
                default:
                    return true;
            }
            
        } catch (Exception e) {
            logger.error("验证恢复效果时发生异常", e);
            return false;
        }
    }
    
    /**
     * 手动触发恢复
     */
    public RecoveryResult triggerManualRecovery(FailureType failureType, String description) {
        logger.info("手动触发故障恢复: {} - {}", failureType, description);
        
        FailureDetection failure = new FailureDetection(
            failureType,
            description,
            "MANUAL",
            LocalDateTime.now()
        );
        
        try {
            executeRecovery(failure);
            return new RecoveryResult(true, "手动恢复已触发");
        } catch (Exception e) {
            logger.error("手动恢复触发失败", e);
            return new RecoveryResult(false, "手动恢复触发失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取恢复统计信息
     */
    public RecoveryStatistics getRecoveryStatistics() {
        long total = totalRecoveryAttempts.get();
        long successful = successfulRecoveries.get();
        long failed = failedRecoveries.get();
        
        double successRate = total > 0 ? (double) successful / total * 100 : 0.0;
        
        return new RecoveryStatistics(
            total,
            successful,
            failed,
            successRate,
            activeRecoveryTasks.get(),
            getAverageRecoveryTime()
        );
    }
    
    /**
     * 获取平均恢复时间
     */
    private long getAverageRecoveryTime() {
        synchronized (recoveryHistory) {
            if (recoveryHistory.isEmpty()) {
                return 0;
            }
            
            long totalTime = recoveryHistory.stream()
                .filter(r -> r.getDuration() > 0)
                .mapToLong(RecoveryRecord::getDuration)
                .sum();
            
            long count = recoveryHistory.stream()
                .filter(r -> r.getDuration() > 0)
                .count();
            
            return count > 0 ? totalTime / count : 0;
        }
    }
    
    /**
     * 获取恢复历史记录
     */
    public List<RecoveryRecord> getRecoveryHistory(int limit) {
        synchronized (recoveryHistory) {
            int size = recoveryHistory.size();
            int startIndex = Math.max(0, size - limit);
            return new ArrayList<>(recoveryHistory.subList(startIndex, size));
        }
    }
    
    /**
     * 添加恢复记录
     */
    private void addRecoveryRecord(RecoveryRecord record) {
        synchronized (recoveryHistory) {
            recoveryHistory.add(record);
            
            // 保持最大记录数
            while (recoveryHistory.size() > MAX_HISTORY_SIZE) {
                recoveryHistory.remove(0);
            }
        }
    }
    
    /**
     * 清除恢复历史
     */
    public void clearRecoveryHistory() {
        synchronized (recoveryHistory) {
            recoveryHistory.clear();
        }
        logger.info("恢复历史记录已清除");
    }
    
    /**
     * 更新配置
     */
    public void updateConfiguration(int maxConcurrentRecoveries, int maxRetryAttempts) {
        this.maxConcurrentRecoveries = maxConcurrentRecoveries;
        this.maxRetryAttempts = maxRetryAttempts;
        
        logger.info("自动恢复配置已更新: 最大并发={}, 最大重试={}", 
            maxConcurrentRecoveries, maxRetryAttempts);
    }
    
    /**
     * 故障类型枚举
     */
    public enum FailureType {
        DATABASE_CONNECTION_FAILED,
        MEMORY_HIGH_USAGE,
        NETWORK_FAILURE,
        THREAD_POOL_EXHAUSTED,
        DISK_SPACE_LOW,
        SYSTEM_OVERLOAD
    }
    
    /**
     * 恢复策略枚举
     */
    public enum RecoveryStrategy {
        RESET_DATABASE_CONNECTION_POOL,
        TRIGGER_GARBAGE_COLLECTION,
        CLEAR_CACHE,
        RESET_NETWORK_CONNECTIONS,
        EXPAND_THREAD_POOL,
        CLEAN_TEMPORARY_FILES,
        RESTART_DATABASE_CONNECTIONS,
        SWITCH_TO_BACKUP_DATABASE,
        RESTART_MEMORY_INTENSIVE_SERVICES,
        SWITCH_TO_BACKUP_ENDPOINT,
        RESTART_NETWORK_SERVICES,
        CLEAR_STUCK_TASKS,
        RESTART_THREAD_POOL,
        COMPRESS_LOG_FILES,
        ARCHIVE_OLD_DATA
    }
    
    /**
     * 恢复状态枚举
     */
    public enum RecoveryStatus {
        SUCCESS,
        FAILED,
        ERROR,
        IN_PROGRESS
    }
    
    /**
     * 故障检测类
     */
    public static class FailureDetection {
        private final FailureType type;
        private final String description;
        private final String severity;
        private final LocalDateTime detectionTime;
        
        public FailureDetection(FailureType type, String description, String severity, LocalDateTime detectionTime) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.detectionTime = detectionTime;
        }
        
        // Getters
        public FailureType getType() { return type; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public LocalDateTime getDetectionTime() { return detectionTime; }
    }
    
    /**
     * 恢复记录类
     */
    public static class RecoveryRecord {
        private final FailureType failureType;
        private final String description;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private RecoveryStatus status = RecoveryStatus.IN_PROGRESS;
        private String recoveryStrategy;
        private String errorMessage;
        private int attempts = 0;
        private long duration = 0;
        
        public RecoveryRecord(FailureType failureType, String description, LocalDateTime startTime) {
            this.failureType = failureType;
            this.description = description;
            this.startTime = startTime;
        }
        
        public long calculateDuration() {
            if (endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            }
            return 0;
        }
        
        // Getters and setters
        public FailureType getFailureType() { return failureType; }
        public String getDescription() { return description; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public RecoveryStatus getStatus() { return status; }
        public void setStatus(RecoveryStatus status) { this.status = status; }
        public String getRecoveryStrategy() { return recoveryStrategy; }
        public void setRecoveryStrategy(String recoveryStrategy) { this.recoveryStrategy = recoveryStrategy; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
    }
    
    /**
     * 恢复结果类
     */
    public static class RecoveryResult {
        private final boolean success;
        private final String message;
        
        public RecoveryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * 恢复统计类
     */
    public static class RecoveryStatistics {
        private final long totalAttempts;
        private final long successfulRecoveries;
        private final long failedRecoveries;
        private final double successRate;
        private final int activeRecoveryTasks;
        private final long averageRecoveryTime;
        
        public RecoveryStatistics(long totalAttempts, long successfulRecoveries, long failedRecoveries,
                                double successRate, int activeRecoveryTasks, long averageRecoveryTime) {
            this.totalAttempts = totalAttempts;
            this.successfulRecoveries = successfulRecoveries;
            this.failedRecoveries = failedRecoveries;
            this.successRate = successRate;
            this.activeRecoveryTasks = activeRecoveryTasks;
            this.averageRecoveryTime = averageRecoveryTime;
        }
        
        // Getters
        public long getTotalAttempts() { return totalAttempts; }
        public long getSuccessfulRecoveries() { return successfulRecoveries; }
        public long getFailedRecoveries() { return failedRecoveries; }
        public double getSuccessRate() { return successRate; }
        public int getActiveRecoveryTasks() { return activeRecoveryTasks; }
        public long getAverageRecoveryTime() { return averageRecoveryTime; }
    }
}
