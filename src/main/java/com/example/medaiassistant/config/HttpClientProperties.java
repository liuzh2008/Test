package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * HTTP客户端配置属性类
 * 绑定http.client前缀的配置属性，用于统一管理HTTP客户端的连接池、超时、重试等配置
 * 
 * <p>配置示例：
 * <pre>
 * http.client.pool.max-total=100
 * http.client.pool.default-max-per-route=20
 * http.client.timeout.connection-timeout=5000
 * http.client.timeout.socket-timeout=10000
 * http.client.timeout.request-timeout=15000
 * http.client.retry.max-retries=3
 * http.client.retry.backoff-period=1000
 * http.client.keep-alive.enabled=true
 * http.client.keep-alive.timeout=30000
 * </pre>
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@ConfigurationProperties(prefix = "http.client")
@Validated
public class HttpClientProperties {

    private final Pool pool = new Pool();
    private final Timeout timeout = new Timeout();
    private final Retry retry = new Retry();
    private final KeepAlive keepAlive = new KeepAlive();

    /**
     * 连接池配置
     */
    public static class Pool {
        private int maxTotal = 50;
        private int defaultMaxPerRoute = 10;

        public int getMaxTotal() {
            return maxTotal;
        }

        public void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
        }

        public int getDefaultMaxPerRoute() {
            return defaultMaxPerRoute;
        }

        public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
            this.defaultMaxPerRoute = defaultMaxPerRoute;
        }
    }

    /**
     * 超时配置
     */
    public static class Timeout {
        private int connectionTimeout = 3000;
        private int socketTimeout = 5000;
        private int requestTimeout = 10000;

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    /**
     * 重试配置
     */
    public static class Retry {
        private int maxRetries = 2;
        private int backoffPeriod = 500;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getBackoffPeriod() {
            return backoffPeriod;
        }

        public void setBackoffPeriod(int backoffPeriod) {
            this.backoffPeriod = backoffPeriod;
        }
    }

    /**
     * Keep-Alive配置
     */
    public static class KeepAlive {
        private boolean enabled = true;
        private int timeout = 20000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public Pool getPool() {
        return pool;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public KeepAlive getKeepAlive() {
        return keepAlive;
    }

    /**
     * 验证配置参数
     * 
     * @param environment Spring环境
     * @throws IllegalStateException 如果配置验证失败
     */
    public void validateConfiguration(Environment environment) {
        // 验证连接池配置
        if (pool.getMaxTotal() <= 0) {
            throw new IllegalStateException("连接池最大连接数必须大于0");
        }
        if (pool.getDefaultMaxPerRoute() <= 0) {
            throw new IllegalStateException("连接池默认每路由最大连接数必须大于0");
        }

        // 验证超时配置
        if (timeout.getConnectionTimeout() <= 0) {
            throw new IllegalStateException("连接超时必须大于0");
        }
        if (timeout.getSocketTimeout() <= 0) {
            throw new IllegalStateException("Socket超时必须大于0");
        }
        if (timeout.getRequestTimeout() <= 0) {
            throw new IllegalStateException("请求超时必须大于0");
        }

        // 验证重试配置
        if (retry.getMaxRetries() < 0) {
            throw new IllegalStateException("最大重试次数不能为负数");
        }
        if (retry.getBackoffPeriod() < 0) {
            throw new IllegalStateException("退避周期不能为负数");
        }

        // 验证Keep-Alive配置
        if (keepAlive.getTimeout() < 0) {
            throw new IllegalStateException("Keep-Alive超时不能为负数");
        }
    }
}
