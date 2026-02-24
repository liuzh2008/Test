package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络健康监控服务
 * 迭代1：提供网络状态监控和统计功能
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@Service
public class NetworkHealthService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHealthService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ServerConfigService serverConfigService;
    
    // 网络状态统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    
    private volatile boolean networkHealthy = true;
    private volatile long lastSuccessTime = System.currentTimeMillis();
    
    /**
     * 检查网络健康状态
     * @return true if network is healthy
     */
    public boolean isNetworkHealthy() {
        return networkHealthy && consecutiveFailures.get() < 3;
    }
    
    /**
     * 记录网络请求结果
     */
    public void recordNetworkResult(boolean success, long latency) {
        totalRequests.incrementAndGet();
        totalLatency.addAndGet(latency);
        
        if (success) {
            successfulRequests.incrementAndGet();
            consecutiveFailures.set(0);
            lastSuccessTime = System.currentTimeMillis();
            networkHealthy = true;
        } else {
            failedRequests.incrementAndGet();
            consecutiveFailures.incrementAndGet();
            
            // 连续失败超过3次标记为不健康
            if (consecutiveFailures.get() >= 3) {
                networkHealthy = false;
                logger.warn("网络状态标记为不健康，连续失败次数: {}", consecutiveFailures.get());
            }
        }
    }
    
    /**
     * 获取网络健康统计信息
     */
    public NetworkHealthStats getHealthStats() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();
        long avgLatency = total > 0 ? totalLatency.get() / total : 0;
        double successRate = total > 0 ? (double) successful / total : 0.0;
        
        return new NetworkHealthStats(
            networkHealthy,
            total,
            successful,
            failed,
            successRate,
            avgLatency,
            consecutiveFailures.get(),
            lastSuccessTime
        );
    }
    
    /**
     * 重置网络健康统计
     */
    public void resetStats() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalLatency.set(0);
        consecutiveFailures.set(0);
        networkHealthy = true;
        lastSuccessTime = System.currentTimeMillis();
        logger.info("网络健康统计已重置");
    }
    
    /**
     * 获取建议的重试次数
     */
    public int getRecommendedRetries() {
        double successRate = getHealthStats().getSuccessRate();
        long avgLatency = getHealthStats().getAverageLatency();
        
        // 基于成功率和延迟动态调整重试次数
        if (successRate < 0.5) {
            return 8; // 成功率低，多重试几次
        } else if (successRate < 0.8) {
            return 5;
        } else if (avgLatency > 5000) {
            return 6; // 高延迟，适当增加重试
        } else {
            return 3; // 默认重试次数
        }
    }
    
    /**
     * 执行网络健康检查
     */
    public boolean performHealthCheck() {
        try {
            String executionServerIp = serverConfigService.getDecryptionServerIp();
            String healthUrl = "http://" + (executionServerIp != null ? executionServerIp : "localhost") + ":8082/health";
            
            long startTime = System.currentTimeMillis();
            String response = restTemplate.getForObject(healthUrl, String.class);
            long latency = System.currentTimeMillis() - startTime;
            
            boolean success = response != null && !response.isEmpty();
            recordNetworkResult(success, latency);
            
            return success;
        } catch (Exception e) {
            recordNetworkResult(false, 0);
            logger.debug("网络健康检查失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 网络健康统计数据类
     */
    public static class NetworkHealthStats {
        private final boolean healthy;
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final double successRate;
        private final long averageLatency;
        private final int consecutiveFailures;
        private final long lastSuccessTime;
        
        public NetworkHealthStats(boolean healthy, long totalRequests, long successfulRequests, 
                                long failedRequests, double successRate, long averageLatency,
                                int consecutiveFailures, long lastSuccessTime) {
            this.healthy = healthy;
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.successRate = successRate;
            this.averageLatency = averageLatency;
            this.consecutiveFailures = consecutiveFailures;
            this.lastSuccessTime = lastSuccessTime;
        }
        
        // Getters
        public boolean isHealthy() { return healthy; }
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getSuccessRate() { return successRate; }
        public long getAverageLatency() { return averageLatency; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public long getLastSuccessTime() { return lastSuccessTime; }
        
        @Override
        public String toString() {
            return String.format(
                "NetworkHealth{healthy=%s, total=%d, success=%d, failed=%d, successRate=%.2f%%, avgLatency=%dms, consecutiveFailures=%d}",
                healthy, totalRequests, successfulRequests, failedRequests, successRate * 100, averageLatency, consecutiveFailures
            );
        }
    }
}