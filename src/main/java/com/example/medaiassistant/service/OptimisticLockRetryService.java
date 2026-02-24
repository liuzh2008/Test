package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 乐观锁重试服务
 * 处理乐观锁冲突、版本控制和并发更新问题
 */
@Service
public class OptimisticLockRetryService {

    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockRetryService.class);



    // 乐观锁统计信息
    private final AtomicInteger totalOptimisticLockRetries = new AtomicInteger(0);
    private final AtomicInteger successfulOptimisticLockRetries = new AtomicInteger(0);
    private final AtomicInteger failedOptimisticLockRetries = new AtomicInteger(0);
    private final AtomicInteger versionConflicts = new AtomicInteger(0);
    private final AtomicLong totalOptimisticLockRetryTime = new AtomicLong(0);

    /**
     * 乐观锁重试统计信息
     */
    public static class OptimisticLockStats {
        private final int totalRetries;
        private final int successfulRetries;
        private final int failedRetries;
        private final int versionConflicts;
        private final double successRate;
        private final long averageRetryTime;

        public OptimisticLockStats(int totalRetries, int successfulRetries, int failedRetries,
                                 int versionConflicts, double successRate, long averageRetryTime) {
            this.totalRetries = totalRetries;
            this.successfulRetries = successfulRetries;
            this.failedRetries = failedRetries;
            this.versionConflicts = versionConflicts;
            this.successRate = successRate;
            this.averageRetryTime = averageRetryTime;
        }

        // Getters
        public int getTotalRetries() { return totalRetries; }
        public int getSuccessfulRetries() { return successfulRetries; }
        public int getFailedRetries() { return failedRetries; }
        public int getVersionConflicts() { return versionConflicts; }
        public double getSuccessRate() { return successRate; }
        public long getAverageRetryTime() { return averageRetryTime; }
    }

    /**
     * 执行带乐观锁重试的操作
     * @param operation 需要执行的操作
     * @param entityRefresher 实体刷新函数，用于获取最新版本
     * @param operationName 操作名称
     * @return 操作结果
     */
    public <T> T executeWithOptimisticLockRetry(
            Supplier<T> operation, 
            Supplier<Object> entityRefresher, 
            String operationName) {
        
        long startTime = System.currentTimeMillis();
        int attempt = 0;
        int maxRetries = getOptimisticLockMaxRetries();
        
        while (attempt <= maxRetries) {
            try {
                T result = operation.get();
                
                if (attempt > 0) {
                    // 重试成功
                    successfulOptimisticLockRetries.incrementAndGet();
                    long retryTime = System.currentTimeMillis() - startTime;
                    totalOptimisticLockRetryTime.addAndGet(retryTime);
                    logger.info("{}在第{}次乐观锁重试后成功，耗时{}ms", operationName, attempt, retryTime);
                }
                
                return result;
                
            } catch (OptimisticLockingFailureException e) {
                versionConflicts.incrementAndGet();
                attempt++;
                
                if (attempt <= maxRetries) {
                    // 刷新实体获取最新版本
                    if (entityRefresher != null) {
                        try {
                            entityRefresher.get();
                            logger.debug("刷新实体版本成功，准备第{}次重试", attempt);
                        } catch (Exception refreshException) {
                            logger.warn("刷新实体版本失败: {}", refreshException.getMessage());
                        }
                    }
                    
                    long delay = getOptimisticLockRetryDelay(attempt);
                    logger.warn("{}遇到乐观锁冲突，第{}次重试，延迟{}ms: {}", 
                              operationName, attempt, delay, e.getMessage());
                    sleep(delay);
                } else {
                    failedOptimisticLockRetries.incrementAndGet();
                    logger.error("{}在{}次乐观锁重试后仍然失败", operationName, maxRetries);
                    throw e;
                }
            }
        }
        
        totalOptimisticLockRetries.addAndGet(attempt);
        return null; // 不应该到达这里
    }

    /**
     * Prompt状态更新的乐观锁重试
     * 专门用于Prompt状态的并发更新控制
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    public void updatePromptStatusWithOptimisticLock(
            Long promptId, 
            String newStatus, 
            Long expectedVersion,
            Function<Long, Object> promptRefresher) {
        
        executeWithOptimisticLockRetry(
            () -> {
                // 执行状态更新操作
                return updatePromptStatusInternal(promptId, newStatus, expectedVersion);
            },
            () -> promptRefresher != null ? promptRefresher.apply(promptId) : null,
            "更新Prompt状态[" + promptId + ":" + newStatus + "]"
        );
    }

    /**
     * 内部状态更新方法
     * 这里应该包含实际的数据库更新逻辑
     */
    private Object updatePromptStatusInternal(Long promptId, String newStatus, Long expectedVersion) {
        // 验证状态转换合法性（遵循状态流转规范）
        if (!isValidStatusTransition(newStatus)) {
            throw new IllegalStateException("非法的状态转换: " + newStatus);
        }
        
        // 这里应该调用实际的DAO或Repository方法
        // 示例：promptRepository.updateStatusWithVersion(promptId, newStatus, expectedVersion);
        
        logger.debug("执行Prompt[{}]状态更新为: {}, 期望版本: {}", promptId, newStatus, expectedVersion);
        
        // 模拟版本检查
        if (Math.random() < 0.1) { // 10%概率模拟版本冲突
            throw new OptimisticLockingFailureException("模拟版本冲突");
        }
        
        return null;
    }

    /**
     * 检查状态转换是否合法
     * 根据用户记忆中的状态流转规范：待处理 → SUBMISSION_STARTED → 已完成/执行失败
     */
    private boolean isValidStatusTransition(String status) {
        return "待处理".equals(status) || 
               "SUBMISSION_STARTED".equals(status) || 
               "已完成".equals(status) || 
               "执行失败".equals(status);
    }

    /**
     * 批量状态更新的乐观锁处理
     * 用于轮询状态统计时的批量更新
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    public void batchUpdatePromptStatusWithOptimisticLock(
            java.util.List<Long> promptIds, 
            String newStatus) {
        
        executeWithOptimisticLockRetry(
            () -> {
                // 批量更新逻辑
                promptIds.forEach(promptId -> {
                    updatePromptStatusInternal(promptId, newStatus, null);
                });
                return null;
            },
            null, // 批量操作不刷新单个实体
            "批量更新Prompt状态[" + promptIds.size() + "个记录:" + newStatus + "]"
        );
    }

    /**
     * 并发安全的状态查询和更新
     * 确保查询和更新的原子性
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_COMMITTED,
        readOnly = false
    )
    public <T> T queryAndUpdateWithOptimisticLock(
            Supplier<T> queryOperation,
            Supplier<Object> updateOperation,
            String operationName) {
        
        return executeWithOptimisticLockRetry(
            () -> {
                // 先查询
                T queryResult = queryOperation.get();
                
                // 再更新
                updateOperation.get();
                
                return queryResult;
            },
            null,
            operationName
        );
    }

    /**
     * 获取乐观锁最大重试次数
     * 根据系统负载动态调整
     */
    private int getOptimisticLockMaxRetries() {
        // 乐观锁冲突通常需要更多重试次数
        OptimisticLockStats stats = getOptimisticLockStats();
        
        if (stats.getSuccessRate() < 0.6) {
            return 8; // 成功率低时增加重试次数
        } else if (stats.getVersionConflicts() > 50) {
            return 6; // 版本冲突较多时适度增加
        } else {
            return 4; // 默认重试次数
        }
    }

    /**
     * 获取乐观锁重试延迟
     * 使用较短的延迟时间，因为乐观锁冲突通常很快解决
     */
    private long getOptimisticLockRetryDelay(int attempt) {
        // 乐观锁重试：较短的基础延迟
        long baseDelay = 200; // 200ms基础延迟
        
        // 线性增长而不是指数增长，因为乐观锁冲突通常快速解决
        long linearDelay = baseDelay * attempt;
        
        // 添加随机抖动减少冲突概率
        double jitter = 0.2 + Math.random() * 0.4; // 20%-60%的抖动
        long jitterDelay = (long)(linearDelay * jitter);
        
        return Math.min(linearDelay + jitterDelay, 2000); // 最大延迟2秒
    }

    /**
     * 获取乐观锁重试统计信息
     */
    public OptimisticLockStats getOptimisticLockStats() {
        int total = totalOptimisticLockRetries.get();
        int successful = successfulOptimisticLockRetries.get();
        int failed = failedOptimisticLockRetries.get();
        
        double successRate = total > 0 ? (double) successful / total : 0.0;
        long avgRetryTime = successful > 0 ? totalOptimisticLockRetryTime.get() / successful : 0;
        
        return new OptimisticLockStats(
            total,
            successful,
            failed,
            versionConflicts.get(),
            successRate,
            avgRetryTime
        );
    }

    /**
     * 检查是否应该增加延迟或减少并发
     * 当乐观锁冲突过多时建议调整策略
     */
    public boolean shouldReduceConcurrency() {
        OptimisticLockStats stats = getOptimisticLockStats();
        
        // 版本冲突超过100次或成功率低于40%时建议减少并发
        return stats.getVersionConflicts() > 100 || stats.getSuccessRate() < 0.4;
    }

    /**
     * 重置乐观锁统计信息
     */
    public void resetOptimisticLockStats() {
        totalOptimisticLockRetries.set(0);
        successfulOptimisticLockRetries.set(0);
        failedOptimisticLockRetries.set(0);
        versionConflicts.set(0);
        totalOptimisticLockRetryTime.set(0);
        
        logger.info("乐观锁重试统计信息已重置");
    }

    /**
     * 获取当前推荐的并发级别
     * 根据乐观锁冲突情况动态调整
     */
    public int getRecommendedConcurrencyLevel() {
        OptimisticLockStats stats = getOptimisticLockStats();
        
        if (stats.getVersionConflicts() > 200) {
            return 1; // 高冲突时序列化执行
        } else if (stats.getVersionConflicts() > 100) {
            return 2; // 中等冲突时低并发
        } else if (stats.getSuccessRate() > 0.8) {
            return 5; // 成功率高时可以提高并发
        } else {
            return 3; // 默认并发级别
        }
    }

    /**
     * 安全睡眠方法
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("乐观锁重试睡眠被中断");
        }
    }
}
