package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 限流配置类
 * 用于配置并发请求限流参数，控制最大并发数和队列管理
 */
@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitConfig {
    
    /**
     * 最大并发请求数
     */
    private int maxConcurrentRequests = 10;
    
    /**
     * 队列容量
     */
    private int queueCapacity = 100;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeoutMs = 300000;
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    // Getters and Setters
    
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimitConfig that = (RateLimitConfig) o;
        return maxConcurrentRequests == that.maxConcurrentRequests &&
                queueCapacity == that.queueCapacity &&
                timeoutMs == that.timeoutMs &&
                enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxConcurrentRequests, queueCapacity, timeoutMs, enabled);
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "maxConcurrentRequests=" + maxConcurrentRequests +
                ", queueCapacity=" + queueCapacity +
                ", timeoutMs=" + timeoutMs +
                ", enabled=" + enabled +
                '}';
    }
}
