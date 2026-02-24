package com.example.medaiassistant.service.monitoring;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 告警实体类
 * 表示监控系统中触发的告警
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
public class Alert {
    
    private final String id;
    private final String alertType;
    private final String severity;
    private final String message;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private boolean active;
    
    /**
     * 构造函数
     * 
     * @param alertType 告警类型
     * @param severity 告警级别
     * @param message 告警信息
     */
    public Alert(String alertType, String severity, String message) {
        this.id = UUID.randomUUID().toString();
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.resolvedAt = null;
    }
    
    /**
     * 获取告警ID
     * 
     * @return 告警ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取告警类型
     * 
     * @return 告警类型
     */
    public String getAlertType() {
        return alertType;
    }
    
    /**
     * 获取告警级别
     * 
     * @return 告警级别
     */
    public String getSeverity() {
        return severity;
    }
    
    /**
     * 获取告警信息
     * 
     * @return 告警信息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取创建时间
     * 
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 获取解决时间
     * 
     * @return 解决时间
     */
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    /**
     * 检查告警是否处于活动状态
     * 
     * @return 是否活动
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * 解决告警
     */
    public void resolve() {
        this.active = false;
        this.resolvedAt = LocalDateTime.now();
    }
    
    /**
     * 重新激活告警
     */
    public void reactivate() {
        this.active = true;
        this.resolvedAt = null;
    }
    
    @Override
    public String toString() {
        return String.format("Alert{id='%s', type='%s', severity='%s', active=%s, message='%s'}",
                id, alertType, severity, active, message);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Alert alert = (Alert) obj;
        return id.equals(alert.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
