package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回调配置类
 * 配置异步回调相关的参数
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "callback")
public class CallbackConfig {

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private long retryInterval = 5000;

    /**
     * 回调超时时间（毫秒）
     */
    private long timeout = 30000;

    /**
     * 是否启用回调
     */
    private boolean enabled = true;

    /**
     * 默认回调URL
     */
    private String defaultCallbackUrl = "http://localhost:8081/api/callback/receive";

    // Getters and setters
    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultCallbackUrl() {
        return defaultCallbackUrl;
    }

    public void setDefaultCallbackUrl(String defaultCallbackUrl) {
        this.defaultCallbackUrl = defaultCallbackUrl;
    }

    @Override
    public String toString() {
        return "CallbackConfig{" +
                "maxRetries=" + maxRetries +
                ", retryInterval=" + retryInterval +
                ", timeout=" + timeout +
                ", enabled=" + enabled +
                ", defaultCallbackUrl='" + defaultCallbackUrl + '\'' +
                '}';
    }
}
