package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * 重试策略配置类
 * 用于配置HTTP请求的重试策略，支持指数退避和状态码检查
 */
@Component
@ConfigurationProperties(prefix = "retry.policy")
public class RetryPolicyConfig {
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    /**
     * 初始重试间隔（毫秒）
     */
    private long initialIntervalMs = 1000;
    
    /**
     * 重试间隔倍数因子
     */
    private double multiplier = 2.0;
    
    /**
     * 最大重试间隔（毫秒）
     */
    private long maxIntervalMs = 30000;
    
    /**
     * 可重试的状态码集合
     */
    private Set<Integer> retryableStatusCodes = Set.of(429, 500, 502, 503, 504);

    // Getters and Setters
    
    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialIntervalMs() {
        return initialIntervalMs;
    }

    public void setInitialIntervalMs(long initialIntervalMs) {
        this.initialIntervalMs = initialIntervalMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxIntervalMs() {
        return maxIntervalMs;
    }

    public void setMaxIntervalMs(long maxIntervalMs) {
        this.maxIntervalMs = maxIntervalMs;
    }

    public Set<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) {
        this.retryableStatusCodes = retryableStatusCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryPolicyConfig that = (RetryPolicyConfig) o;
        return maxRetries == that.maxRetries &&
                initialIntervalMs == that.initialIntervalMs &&
                Double.compare(multiplier, that.multiplier) == 0 &&
                maxIntervalMs == that.maxIntervalMs &&
                Objects.equals(retryableStatusCodes, that.retryableStatusCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxRetries, initialIntervalMs, multiplier, maxIntervalMs, retryableStatusCodes);
    }

    @Override
    public String toString() {
        return "RetryPolicyConfig{" +
                "maxRetries=" + maxRetries +
                ", initialIntervalMs=" + initialIntervalMs +
                ", multiplier=" + multiplier +
                ", maxIntervalMs=" + maxIntervalMs +
                ", retryableStatusCodes=" + retryableStatusCodes +
                '}';
    }
}
