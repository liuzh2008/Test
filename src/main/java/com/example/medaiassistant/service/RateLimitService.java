package com.example.medaiassistant.service;

import com.example.medaiassistant.config.RateLimitConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务类
 * 
 * 该类实现基于Semaphore的并发请求控制，提供以下功能：
 * 1. 限制最大并发请求数
 * 2. 支持队列管理
 * 3. 实现超时机制
 * 4. 提供请求统计信息
 * 
 * 配置说明：
 * - 最大并发请求数：10
 * - 队列容量：100
 * - 超时时间：30秒
 * 
 * 使用示例：
 * 
 * @Autowired
 *            private RateLimitService rateLimitService;
 * 
 *            try {
 *            if (rateLimitService.tryAcquire()) {
 *            // 执行受保护的请求
 *            String result = restTemplate.getForObject(url, String.class);
 *            } else {
 *            // 处理限流情况
 *            throw new RateLimitException("请求被限流");
 *            }
 *            } finally {
 *            rateLimitService.release();
 *            }
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@Service
public class RateLimitService {

    private final Semaphore semaphore;
    private final RateLimitConfig rateLimitConfig;
    private final AtomicInteger activeRequests;
    private final AtomicInteger totalRequests;
    private final AtomicInteger rejectedRequests;

    /**
     * 构造函数
     * 
     * 初始化限流服务，创建Semaphore实例并设置配置参数。
     * 
     * @param rateLimitConfig 限流配置
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RateLimitService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
        this.semaphore = new Semaphore(rateLimitConfig.getMaxConcurrentRequests(), true);
        this.activeRequests = new AtomicInteger(0);
        this.totalRequests = new AtomicInteger(0);
        this.rejectedRequests = new AtomicInteger(0);
    }

    /**
     * 尝试获取请求许可
     * 
     * 该方法尝试在指定超时时间内获取请求许可，如果成功获取则返回true，
     * 否则返回false。支持超时机制，避免无限等待。
     * 
     * @return 是否成功获取许可
     * @throws InterruptedException 如果线程在等待时被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public boolean tryAcquire() throws InterruptedException {
        return tryAcquire(rateLimitConfig.getTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试获取请求许可（自定义超时时间）
     * 
     * 该方法尝试在指定超时时间内获取请求许可，如果成功获取则返回true，
     * 否则返回false。支持自定义超时时间。
     * 
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否成功获取许可
     * @throws InterruptedException 如果线程在等待时被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (!rateLimitConfig.isEnabled()) {
            return true; // 限流未启用，直接返回成功
        }

        totalRequests.incrementAndGet();

        // 尝试获取许可，支持超时
        boolean acquired = semaphore.tryAcquire(timeout, unit);

        if (acquired) {
            activeRequests.incrementAndGet();
        } else {
            rejectedRequests.incrementAndGet();
        }

        return acquired;
    }

    /**
     * 释放请求许可
     * 
     * 该方法释放之前获取的请求许可，减少活跃请求计数。
     * 必须在tryAcquire成功后调用，确保资源正确释放。
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public void release() {
        if (!rateLimitConfig.isEnabled()) {
            return; // 限流未启用，无需释放
        }

        semaphore.release();
        activeRequests.decrementAndGet();
    }

    /**
     * 获取当前活跃请求数
     * 
     * 该方法返回当前正在处理的并发请求数量。
     * 
     * @return 当前活跃请求数
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public int getActiveRequests() {
        return activeRequests.get();
    }

    /**
     * 获取可用许可数
     * 
     * 该方法返回当前可用的请求许可数量。
     * 
     * @return 可用许可数
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * 获取队列长度
     * 
     * 该方法返回当前等待获取许可的请求数量。
     * 
     * @return 队列长度
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public int getQueueLength() {
        return semaphore.getQueueLength();
    }

    /**
     * 获取总请求数
     * 
     * 该方法返回自服务启动以来的总请求数量。
     * 
     * @return 总请求数
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public int getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * 获取被拒绝请求数
     * 
     * 该方法返回自服务启动以来被限流拒绝的请求数量。
     * 
     * @return 被拒绝请求数
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public int getRejectedRequests() {
        return rejectedRequests.get();
    }

    /**
     * 获取限流配置
     * 
     * 该方法返回当前使用的限流配置信息。
     * 
     * @return 限流配置
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }

    /**
     * 重置统计信息
     * 
     * 该方法重置请求统计信息，包括总请求数和被拒绝请求数。
     * 主要用于测试和监控目的。
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public void resetStatistics() {
        totalRequests.set(0);
        rejectedRequests.set(0);
    }

    /**
     * 获取限流状态信息
     * 
     * 该方法返回当前限流服务的详细状态信息，包括：
     * - 活跃请求数
     * - 可用许可数
     * - 队列长度
     * - 总请求数
     * - 被拒绝请求数
     * - 配置参数
     * 
     * @return 限流状态信息字符串
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public String getStatusInfo() {
        return String.format(
                "RateLimitService Status: " +
                        "ActiveRequests=%d, " +
                        "AvailablePermits=%d, " +
                        "QueueLength=%d, " +
                        "TotalRequests=%d, " +
                        "RejectedRequests=%d, " +
                        "Config=%s",
                getActiveRequests(),
                getAvailablePermits(),
                getQueueLength(),
                getTotalRequests(),
                getRejectedRequests(),
                rateLimitConfig.toString());
    }
}
