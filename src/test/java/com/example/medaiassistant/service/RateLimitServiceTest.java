package com.example.medaiassistant.service;

import com.example.medaiassistant.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流服务测试类
 * 
 * 该类测试RateLimitService的各种功能，包括：
 * 1. 并发请求控制
 * 2. 超时机制
 * 3. 统计信息
 * 4. 异常处理
 * 
 * 测试说明：
 * - 测试最大并发请求数限制
 * - 测试超时机制
 * - 测试统计信息准确性
 * - 测试限流禁用功能
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private RateLimitConfig rateLimitConfig;

    /**
     * 测试前初始化
     * 
     * 该方法在每个测试方法执行前初始化限流服务和配置。
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setMaxConcurrentRequests(3); // 设置较小的并发数便于测试
        rateLimitConfig.setQueueCapacity(10);
        rateLimitConfig.setTimeoutMs(1000); // 设置较短的超时时间便于测试
        rateLimitConfig.setEnabled(true);
        
        rateLimitService = new RateLimitService(rateLimitConfig);
    }

    /**
     * 测试并发请求控制
     * 
     * 该方法测试限流服务是否正确控制并发请求数。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testConcurrentRequestControl() throws InterruptedException {
        // 测试基本并发控制
        int maxConcurrent = rateLimitConfig.getMaxConcurrentRequests();
        
        // 获取所有许可
        for (int i = 0; i < maxConcurrent; i++) {
            assertTrue(rateLimitService.tryAcquire());
        }
        
        // 尝试获取额外许可，应该失败
        boolean extraAcquired = rateLimitService.tryAcquire();
        assertFalse(extraAcquired);
        
        // 释放一个许可
        rateLimitService.release();
        
        // 现在应该能获取到许可
        assertTrue(rateLimitService.tryAcquire());
        
        // 释放所有许可
        for (int i = 0; i < maxConcurrent; i++) {
            rateLimitService.release();
        }
    }

    /**
     * 测试超时机制
     * 
     * 该方法测试限流服务的超时机制是否正常工作。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testTimeoutMechanism() throws InterruptedException {
        // 先获取所有许可
        for (int i = 0; i < rateLimitConfig.getMaxConcurrentRequests(); i++) {
            assertTrue(rateLimitService.tryAcquire());
        }

        // 尝试获取新许可，应该超时
        long startTime = System.currentTimeMillis();
        boolean acquired = rateLimitService.tryAcquire();
        long endTime = System.currentTimeMillis();
        
        assertFalse(acquired);
        assertTrue(endTime - startTime >= rateLimitConfig.getTimeoutMs() - 100); // 允许100ms误差
        
        // 释放一个许可
        rateLimitService.release();
        
        // 现在应该能获取到许可
        assertTrue(rateLimitService.tryAcquire());
    }

    /**
     * 测试统计信息
     * 
     * 该方法测试限流服务的统计信息是否正确。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testStatistics() throws InterruptedException {
        // 初始状态
        assertEquals(0, rateLimitService.getActiveRequests());
        assertEquals(rateLimitConfig.getMaxConcurrentRequests(), rateLimitService.getAvailablePermits());
        assertEquals(0, rateLimitService.getTotalRequests());
        assertEquals(0, rateLimitService.getRejectedRequests());

        // 成功获取许可
        assertTrue(rateLimitService.tryAcquire());
        assertEquals(1, rateLimitService.getActiveRequests());
        assertEquals(rateLimitConfig.getMaxConcurrentRequests() - 1, rateLimitService.getAvailablePermits());
        assertEquals(1, rateLimitService.getTotalRequests());
        assertEquals(0, rateLimitService.getRejectedRequests());

        // 释放许可
        rateLimitService.release();
        assertEquals(0, rateLimitService.getActiveRequests());
        assertEquals(rateLimitConfig.getMaxConcurrentRequests(), rateLimitService.getAvailablePermits());
        assertEquals(1, rateLimitService.getTotalRequests());
        assertEquals(0, rateLimitService.getRejectedRequests());

        // 重置统计信息
        rateLimitService.resetStatistics();
        assertEquals(0, rateLimitService.getTotalRequests());
        assertEquals(0, rateLimitService.getRejectedRequests());
    }

    /**
     * 测试限流禁用功能
     * 
     * 该方法测试限流服务禁用时的行为。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testRateLimitDisabled() throws InterruptedException {
        // 禁用限流
        rateLimitConfig.setEnabled(false);
        
        // 应该总是能获取到许可
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.tryAcquire());
        }
        
        // 释放时不会影响统计
        for (int i = 0; i < 10; i++) {
            rateLimitService.release();
        }
        
        // 统计信息应该保持不变
        assertEquals(0, rateLimitService.getActiveRequests());
        assertEquals(0, rateLimitService.getTotalRequests());
        assertEquals(0, rateLimitService.getRejectedRequests());
    }

    /**
     * 测试状态信息
     * 
     * 该方法测试限流服务的状态信息是否正确。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testStatusInfo() throws InterruptedException {
        String statusInfo = rateLimitService.getStatusInfo();
        assertNotNull(statusInfo);
        assertTrue(statusInfo.contains("RateLimitService Status"));
        assertTrue(statusInfo.contains("ActiveRequests=0"));
        assertTrue(statusInfo.contains("AvailablePermits=" + rateLimitConfig.getMaxConcurrentRequests()));
        assertTrue(statusInfo.contains("TotalRequests=0"));
        assertTrue(statusInfo.contains("RejectedRequests=0"));
        assertTrue(statusInfo.contains("Config=" + rateLimitConfig.toString()));

        // 获取许可后状态信息变化
        assertTrue(rateLimitService.tryAcquire());
        statusInfo = rateLimitService.getStatusInfo();
        assertTrue(statusInfo.contains("ActiveRequests=1"));
        assertTrue(statusInfo.contains("AvailablePermits=" + (rateLimitConfig.getMaxConcurrentRequests() - 1)));
        assertTrue(statusInfo.contains("TotalRequests=1"));
    }

    /**
     * 测试队列长度
     * 
     * 该方法测试限流服务的队列长度统计。
     * 
     * @throws InterruptedException 如果线程被中断
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    void testQueueLength() throws InterruptedException {
        // 获取所有许可
        for (int i = 0; i < rateLimitConfig.getMaxConcurrentRequests(); i++) {
            assertTrue(rateLimitService.tryAcquire());
        }

        // 启动一个线程尝试获取许可（会进入队列）
        Thread waitingThread = new Thread(() -> {
            try {
                rateLimitService.tryAcquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waitingThread.start();

        // 等待线程进入队列
        Thread.sleep(100);
        
        // 队列长度应该为1
        assertEquals(1, rateLimitService.getQueueLength());
        
        // 释放一个许可，让等待线程获取
        rateLimitService.release();
        
        // 等待线程完成
        waitingThread.join(1000);
        
        // 队列长度应该为0
        assertEquals(0, rateLimitService.getQueueLength());
    }
}
