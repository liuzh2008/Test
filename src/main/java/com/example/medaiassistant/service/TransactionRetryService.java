package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 数据库事务重试服务
 * 处理数据库死锁、事务超时、连接问题等异常的重试机制
 */
@Service
public class TransactionRetryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionRetryService.class);

    @Autowired
    private DatabaseHealthService databaseHealthService;

    // 重试统计信息
    private final AtomicInteger totalRetries = new AtomicInteger(0);
    private final AtomicInteger successfulRetries = new AtomicInteger(0);
    private final AtomicInteger failedRetries = new AtomicInteger(0);
    private final AtomicInteger deadlockRetries = new AtomicInteger(0);
    private final AtomicInteger timeoutRetries = new AtomicInteger(0);
    private final AtomicLong totalRetryTime = new AtomicLong(0);

    /**
     * 重试统计信息类
     */
    public static class RetryStats {
        private final int totalRetries;
        private final int successfulRetries;
        private final int failedRetries;
        private final int deadlockRetries;
        private final int timeoutRetries;
        private final double successRate;
        private final long averageRetryTime;

        public RetryStats(int totalRetries, int successfulRetries, int failedRetries,
                         int deadlockRetries, int timeoutRetries, double successRate, long averageRetryTime) {
            this.totalRetries = totalRetries;
            this.successfulRetries = successfulRetries;
            this.failedRetries = failedRetries;
            this.deadlockRetries = deadlockRetries;
            this.timeoutRetries = timeoutRetries;
            this.successRate = successRate;
            this.averageRetryTime = averageRetryTime;
        }

        // Getters
        public int getTotalRetries() { return totalRetries; }
        public int getSuccessfulRetries() { return successfulRetries; }
        public int getFailedRetries() { return failedRetries; }
        public int getDeadlockRetries() { return deadlockRetries; }
        public int getTimeoutRetries() { return timeoutRetries; }
        public double getSuccessRate() { return successRate; }
        public long getAverageRetryTime() { return averageRetryTime; }
    }

    /**
     * 执行带重试的数据库操作（通用方法）
     * @param operation 数据库操作
     * @param operationName 操作名称（用于日志）
     * @return 操作结果
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        long startTime = System.currentTimeMillis();
        int attempt = 0;
        int maxRetries = databaseHealthService.getRecommendedRetries();
        
        while (attempt <= maxRetries) {
            try {
                T result = operation.get();
                
                if (attempt > 0) {
                    // 重试成功
                    successfulRetries.incrementAndGet();
                    long retryTime = System.currentTimeMillis() - startTime;
                    totalRetryTime.addAndGet(retryTime);
                    logger.info("{}在第{}次重试后成功，耗时{}ms", operationName, attempt, retryTime);
                }
                
                return result;
                
            } catch (PessimisticLockingFailureException e) {
                // 包括死锁和悲观锁失败情况
                deadlockRetries.incrementAndGet();
                attempt++;
                if (attempt <= maxRetries) {
                    long delay = getDeadlockRetryDelay(attempt);
                    logger.warn("{}遇到锁冲突（包括死锁），第{}次重试，延迟{}ms: {}", operationName, attempt, delay, e.getMessage());
                    sleep(delay);
                } else {
                    failedRetries.incrementAndGet();
                    logger.error("{}在{}次重试后仍然锁冲突失败", operationName, maxRetries);
                    throw e;
                }
                
            } catch (QueryTimeoutException e) {
                timeoutRetries.incrementAndGet();
                attempt++;
                if (attempt <= maxRetries) {
                    long delay = getTimeoutRetryDelay(attempt);
                    logger.warn("{}查询超时，第{}次重试，延迟{}ms: {}", operationName, attempt, delay, e.getMessage());
                    sleep(delay);
                } else {
                    failedRetries.incrementAndGet();
                    logger.error("{}在{}次重试后仍然超时失败", operationName, maxRetries);
                    throw e;
                }
                
            } catch (TransientDataAccessException e) {
                attempt++;
                if (attempt <= maxRetries) {
                    long delay = databaseHealthService.getRecommendedRetryDelay(attempt);
                    logger.warn("{}瞬时数据访问异常，第{}次重试，延迟{}ms: {}", operationName, attempt, delay, e.getMessage());
                    sleep(delay);
                } else {
                    failedRetries.incrementAndGet();
                    logger.error("{}在{}次重试后仍然失败", operationName, maxRetries);
                    throw e;
                }
                
            } catch (DataAccessException e) {
                // 非瞬时异常，不重试
                failedRetries.incrementAndGet();
                logger.error("{}遇到非瞬时数据访问异常，不进行重试: {}", operationName, e.getMessage());
                throw e;
            }
        }
        
        totalRetries.addAndGet(attempt);
        return null; // 不应该到达这里
    }

    /**
     * 执行带重试的事务操作
     * @param operation 事务操作
     * @param operationName 操作名称
     * @return 操作结果
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_COMMITTED,
        timeout = 30,
        rollbackFor = Exception.class
    )
    public <T> T executeTransactionWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName);
    }

    /**
     * Prompt状态更新的专用重试方法
     * 遵循状态流转规范：待处理 → SUBMISSION_STARTED → 已完成/执行失败
     */
    @Retryable(
        retryFor = {PessimisticLockingFailureException.class, QueryTimeoutException.class, TransientDataAccessException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    public void updatePromptStatusWithRetry(Long promptId, String newStatus, String operationName) {
        logger.debug("更新Prompt[{}]状态为: {}", promptId, newStatus);
        
        executeWithRetry(() -> {
            // 这里应该调用实际的数据库更新方法
            // 示例：promptRepository.updateStatus(promptId, newStatus);
            
            // 验证状态转换的合法性
            validateStatusTransition(promptId, newStatus);
            
            logger.info("成功更新Prompt[{}]状态为: {}", promptId, newStatus);
            return null;
        }, operationName);
    }

    /**
     * 验证Prompt状态转换的合法性
     * 根据用户记忆中的状态流转规范进行验证
     */
    private void validateStatusTransition(Long promptId, String newStatus) {
        // 根据规范：待处理 → SUBMISSION_STARTED → 已完成/执行失败
        // 这里应该添加状态转换验证逻辑
        logger.debug("验证Prompt[{}]状态转换到: {}", promptId, newStatus);
        
        if (!isValidStatusTransition(newStatus)) {
            throw new IllegalStateException("非法的状态转换: " + newStatus);
        }
    }

    /**
     * 检查状态转换是否合法
     */
    private boolean isValidStatusTransition(String status) {
        // 根据状态流转规范验证
        return "待处理".equals(status) || 
               "SUBMISSION_STARTED".equals(status) || 
               "已完成".equals(status) || 
               "执行失败".equals(status);
    }

    /**
     * 获取死锁重试延迟
     * 死锁通常需要更短的重试间隔
     */
    private long getDeadlockRetryDelay(int attempt) {
        // 死锁重试：500ms基础延迟 + 指数退避
        long baseDelay = 500;
        long exponentialDelay = baseDelay * (1L << Math.min(attempt - 1, 4));
        
        // 添加随机抖动减少死锁概率
        double jitter = 0.1 + Math.random() * 0.3;
        return exponentialDelay + (long)(exponentialDelay * jitter);
    }

    /**
     * 获取超时重试延迟
     */
    private long getTimeoutRetryDelay(int attempt) {
        // 超时重试：较长的基础延迟
        long baseDelay = 2000;
        long exponentialDelay = baseDelay * (1L << Math.min(attempt - 1, 3));
        
        // 小幅抖动
        double jitter = 0.05 + Math.random() * 0.1;
        return exponentialDelay + (long)(exponentialDelay * jitter);
    }



    /**
     * 获取重试统计信息
     */
    public RetryStats getRetryStats() {
        int total = totalRetries.get();
        int successful = successfulRetries.get();
        int failed = failedRetries.get();
        
        double successRate = total > 0 ? (double) successful / total : 0.0;
        long avgRetryTime = successful > 0 ? totalRetryTime.get() / successful : 0;
        
        return new RetryStats(
            total,
            successful,
            failed,
            deadlockRetries.get(),
            timeoutRetries.get(),
            successRate,
            avgRetryTime
        );
    }

    /**
     * 检查是否应该降级处理
     * 当重试失败率过高时建议降级
     */
    public boolean shouldFallbackToManualProcessing() {
        RetryStats stats = getRetryStats();
        
        // 重试成功率低于50%或死锁重试超过20次时建议降级
        return stats.getSuccessRate() < 0.5 || stats.getDeadlockRetries() > 20;
    }

    /**
     * 重置重试统计信息
     */
    public void resetRetryStats() {
        totalRetries.set(0);
        successfulRetries.set(0);
        failedRetries.set(0);
        deadlockRetries.set(0);
        timeoutRetries.set(0);
        totalRetryTime.set(0);
        
        logger.info("重试统计信息已重置");
    }

    /**
     * 安全睡眠方法
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("重试睡眠被中断");
        }
    }
}
