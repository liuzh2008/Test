package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 连接池配置类
 * 用于配置HTTP连接池参数，支持连接复用和资源管理
 */
@Component
@ConfigurationProperties(prefix = "http.client.pool")
public class ConnectionPoolConfig {
    
    /**
     * 最大总连接数
     */
    private int maxTotalConnections = 200;
    
    /**
     * 每个路由最大连接数
     */
    private int maxConnectionsPerRoute = 50;
    
    /**
     * 连接请求超时时间（毫秒）
     */
    private int connectionRequestTimeout = 30000;
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;
    
    /**
     * Socket超时时间（毫秒）
     */
    private int socketTimeout = 300000;
    
    /**
     * 是否启用连接保持
     */
    private boolean connectionKeepAlive = true;
    
    /**
     * 连接保持时间（毫秒）
     */
    private int keepAliveTime = 30000;

    // Getters and Setters
    
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isConnectionKeepAlive() {
        return connectionKeepAlive;
    }

    public void setConnectionKeepAlive(boolean connectionKeepAlive) {
        this.connectionKeepAlive = connectionKeepAlive;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    @Override
    public String toString() {
        return "ConnectionPoolConfig{" +
                "maxTotalConnections=" + maxTotalConnections +
                ", maxConnectionsPerRoute=" + maxConnectionsPerRoute +
                ", connectionRequestTimeout=" + connectionRequestTimeout +
                ", connectTimeout=" + connectTimeout +
                ", socketTimeout=" + socketTimeout +
                ", connectionKeepAlive=" + connectionKeepAlive +
                ", keepAliveTime=" + keepAliveTime +
                '}';
    }
}
