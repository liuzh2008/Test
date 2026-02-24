package com.example.medaiassistant.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 健康检查配置属性
 * 用于配置健康检查服务的各种参数
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "hospital.health.check")
public class HealthCheckProperties {
    
    /**
     * 是否启用健康检查
     */
    private boolean enabled = true;
    
    /**
     * 定时健康检查的cron表达式
     */
    private String cron = "0 */5 * * * *";
    
    /**
     * 健康检查超时时间（毫秒）
     */
    private long timeout = 10000;
    
    /**
     * 健康状态缓存过期时间（毫秒）
     */
    private long cacheExpiration = 300000; // 5分钟
    
    /**
     * 是否启用性能指标收集
     */
    private boolean metricsEnabled = true;
    
    /**
     * 性能指标保留天数
     */
    private int metricsRetentionDays = 7;
    
    /**
     * 是否启用告警
     */
    private boolean alertsEnabled = true;
    
    /**
     * 连续失败多少次触发告警
     */
    private int consecutiveFailuresForAlert = 3;
    
    /**
     * 告警抑制时间（分钟）
     */
    private int alertSuppressionMinutes = 10;
    
    /**
     * 响应时间警告阈值（毫秒）
     */
    private long responseTimeWarningThreshold = 5000;
    
    /**
     * 响应时间错误阈值（毫秒）
     */
    private long responseTimeErrorThreshold = 10000;
    
    /**
     * 是否启用详细日志
     */
    private boolean verboseLogging = false;
}
