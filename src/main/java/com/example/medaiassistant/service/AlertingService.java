package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警系统服务
 * 负责阈值监控、多渠道告警、告警聚合和去重
 * 
 * 主要功能：
 * 1. 阈值监控和告警触发
 * 2. 告警去重和聚合
 * 3. 多级告警（INFO、WARNING、CRITICAL）
 * 4. 告警历史记录和统计
 * 5. 告警规则管理
 */
@Service
public class AlertingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);
    
    @Autowired
    private SystemHealthMonitoringService systemHealthMonitoringService;
    
    @Autowired
    private BusinessMetricsMonitoringService businessMetricsMonitoringService;
    

    
    // 告警规则存储
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    
    // 活跃告警存储
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    
    // 告警历史队列
    private final Queue<Alert> alertHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_ALERT_HISTORY = 1000;
    
    // 告警统计
    private final AtomicLong totalAlertsGenerated = new AtomicLong(0);
    private final AtomicLong infoAlertsCount = new AtomicLong(0);
    private final AtomicLong warningAlertsCount = new AtomicLong(0);
    private final AtomicLong criticalAlertsCount = new AtomicLong(0);
    
    // 告警抑制配置
    private final Map<String, LocalDateTime> alertSuppressionMap = new ConcurrentHashMap<>();
    private long defaultSuppressionDurationMinutes = 10; // 默认抑制10分钟
    
    @PostConstruct
    public void initializeAlertingSystem() {
        logger.info("初始化告警系统...");
        
        // 初始化默认告警规则
        initializeDefaultAlertRules();
        
        logger.info("告警系统初始化完成，共{}个告警规则", alertRules.size());
    }
    
    /**
     * 初始化默认告警规则
     */
    private void initializeDefaultAlertRules() {
        // 系统资源告警规则
        addAlertRule(new AlertRule("MEMORY_HIGH_USAGE", "内存使用率过高", 
            "heap.usage.percentage", AlertRule.Operator.GREATER_THAN, 85.0, AlertLevel.WARNING));
        
        addAlertRule(new AlertRule("MEMORY_CRITICAL_USAGE", "内存使用率严重过高", 
            "heap.usage.percentage", AlertRule.Operator.GREATER_THAN, 95.0, AlertLevel.CRITICAL));
        
        addAlertRule(new AlertRule("DISK_HIGH_USAGE", "磁盘使用率过高", 
            "disk.usage.percentage", AlertRule.Operator.GREATER_THAN, 90.0, AlertLevel.WARNING));
        
        addAlertRule(new AlertRule("THREAD_COUNT_HIGH", "线程数量过多", 
            "threads.count", AlertRule.Operator.GREATER_THAN, 500.0, AlertLevel.WARNING));
        
        // 业务指标告警规则
        addAlertRule(new AlertRule("SUCCESS_RATE_LOW", "成功率过低", 
            "success.rate", AlertRule.Operator.LESS_THAN, 95.0, AlertLevel.WARNING));
        
        addAlertRule(new AlertRule("SUCCESS_RATE_CRITICAL", "成功率严重过低", 
            "success.rate", AlertRule.Operator.LESS_THAN, 80.0, AlertLevel.CRITICAL));
        
        addAlertRule(new AlertRule("RESPONSE_TIME_HIGH", "响应时间过长", 
            "average.response.time", AlertRule.Operator.GREATER_THAN, 30000.0, AlertLevel.WARNING));
        
        addAlertRule(new AlertRule("THROUGHPUT_LOW", "吞吐量过低", 
            "throughput", AlertRule.Operator.LESS_THAN, 10.0, AlertLevel.WARNING));
        
        // 数据库告警规则
        addAlertRule(new AlertRule("DATABASE_CONNECTION_FAILED", "数据库连接失败", 
            "database.connection.successful", AlertRule.Operator.EQUALS, 0.0, AlertLevel.CRITICAL));
        
        addAlertRule(new AlertRule("DATABASE_SLOW_RESPONSE", "数据库响应慢", 
            "database.response.time", AlertRule.Operator.GREATER_THAN, 5000.0, AlertLevel.WARNING));
    }
    
    /**
     * 定期检查告警（每1分钟）
     */
    @Scheduled(fixedDelay = 60000)
    public void performScheduledAlertCheck() {
        try {
            checkAllAlerts();
        } catch (Exception e) {
            logger.error("定期告警检查执行失败", e);
        }
    }
    
    /**
     * 检查所有告警规则
     */
    public void checkAllAlerts() {
        logger.debug("开始检查所有告警规则...");
        
        // 获取当前指标
        Map<String, Object> systemMetrics = systemHealthMonitoringService.getCurrentHealthMetrics();
        Map<String, Object> businessKPI = businessMetricsMonitoringService.getCurrentKPI();
        
        // 合并指标数据
        Map<String, Object> allMetrics = new ConcurrentHashMap<>();
        if (systemMetrics.get("memory") instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> memoryMetrics = (Map<String, Object>) systemMetrics.get("memory");
            allMetrics.putAll(memoryMetrics);
        }
        if (systemMetrics.get("threads") instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> threadMetrics = (Map<String, Object>) systemMetrics.get("threads");
            allMetrics.putAll(threadMetrics);
        }
        if (systemMetrics.get("disk") instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> diskMetrics = (Map<String, Object>) systemMetrics.get("disk");
            allMetrics.putAll(diskMetrics);
        }
        if (systemMetrics.get("database") instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> databaseMetrics = (Map<String, Object>) systemMetrics.get("database");
            allMetrics.putAll(databaseMetrics);
        }
        
        // 添加业务KPI
        allMetrics.put("success.rate", businessKPI.get("successRate"));
        allMetrics.put("average.response.time", businessKPI.get("averageResponseTime"));
        allMetrics.put("throughput", businessKPI.get("throughput"));
        
        // 检查每个告警规则
        for (AlertRule rule : alertRules.values()) {
            checkAlertRule(rule, allMetrics);
        }
        
        // 清理过期的抑制
        cleanupExpiredSuppressions();
        
        logger.debug("告警检查完成，当前活跃告警数量: {}", activeAlerts.size());
    }
    
    /**
     * 检查单个告警规则
     */
    private void checkAlertRule(AlertRule rule, Map<String, Object> metrics) {
        Object metricValue = metrics.get(rule.getMetricName());
        if (metricValue == null) {
            return; // 指标不存在，跳过检查
        }
        
        double value;
        if (metricValue instanceof Number) {
            value = ((Number) metricValue).doubleValue();
        } else if (metricValue instanceof Boolean) {
            value = ((Boolean) metricValue) ? 1.0 : 0.0;
        } else {
            return; // 无法转换为数值，跳过检查
        }
        
        boolean shouldAlert = evaluateCondition(value, rule.getOperator(), rule.getThreshold());
        String alertKey = rule.getRuleId();
        
        if (shouldAlert) {
            // 检查告警抑制
            if (!isAlertSuppressed(alertKey)) {
                triggerAlert(rule, value);
            }
        } else {
            // 条件不满足，解除告警
            resolveAlert(alertKey);
        }
    }
    
    /**
     * 评估告警条件
     */
    private boolean evaluateCondition(double value, AlertRule.Operator operator, double threshold) {
        switch (operator) {
            case GREATER_THAN:
                return value > threshold;
            case LESS_THAN:
                return value < threshold;
            case EQUALS:
                return Math.abs(value - threshold) < 0.001;
            case GREATER_THAN_OR_EQUAL:
                return value >= threshold;
            case LESS_THAN_OR_EQUAL:
                return value <= threshold;
            default:
                return false;
        }
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, double currentValue) {
        String alertKey = rule.getRuleId();
        Alert existingAlert = activeAlerts.get(alertKey);
        
        if (existingAlert == null) {
            // 新告警
            Alert newAlert = new Alert(
                rule.getRuleId(),
                rule.getName(),
                rule.getDescription(),
                rule.getLevel(),
                LocalDateTime.now(),
                currentValue,
                rule.getThreshold(),
                AlertStatus.ACTIVE
            );
            
            activeAlerts.put(alertKey, newAlert);
            addToHistory(newAlert);
            updateAlertStatistics(newAlert.getLevel());
            
            // 发送告警通知
            sendAlertNotification(newAlert);
            
            // 设置告警抑制
            suppressAlert(alertKey);
            
            logger.warn("触发新告警: [{}] {} - 当前值: {}, 阈值: {}", 
                rule.getLevel(), rule.getName(), currentValue, rule.getThreshold());
        } else {
            // 更新现有告警
            existingAlert.setLastOccurrence(LocalDateTime.now());
            existingAlert.setCurrentValue(currentValue);
            existingAlert.incrementOccurrenceCount();
            
            logger.debug("更新告警: [{}] {} - 发生次数: {}", 
                rule.getLevel(), rule.getName(), existingAlert.getOccurrenceCount());
        }
    }
    
    /**
     * 解除告警
     */
    private void resolveAlert(String alertKey) {
        Alert alert = activeAlerts.remove(alertKey);
        if (alert != null) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedTime(LocalDateTime.now());
            addToHistory(alert);
            
            logger.info("解除告警: [{}] {}", alert.getLevel(), alert.getName());
        }
    }
    
    /**
     * 发送告警通知
     */
    private void sendAlertNotification(Alert alert) {
        // 这里可以集成各种通知渠道：邮件、短信、钉钉、微信等
        // 目前只记录日志
        String logLevel = alert.getLevel() == AlertLevel.CRITICAL ? "ERROR" : "WARN";
        String message = String.format("告警通知: [%s] %s - %s (当前值: %.2f, 阈值: %.2f)", 
            alert.getLevel(), alert.getName(), alert.getDescription(),
            alert.getCurrentValue(), alert.getThreshold());
        
        if (logLevel.equals("ERROR")) {
            logger.error(message);
        } else {
            logger.warn(message);
        }
        
        // 实现实际的通知发送逻辑
        // - 邮件通知
        // - 短信通知  
        // - Webhook通知
        // - 消息队列通知
    }
    
    /**
     * 检查告警是否被抑制
     */
    private boolean isAlertSuppressed(String alertKey) {
        LocalDateTime suppressedUntil = alertSuppressionMap.get(alertKey);
        if (suppressedUntil != null) {
            if (LocalDateTime.now().isBefore(suppressedUntil)) {
                return true; // 仍在抑制期内
            } else {
                alertSuppressionMap.remove(alertKey); // 抑制期已过，移除
            }
        }
        return false;
    }
    
    /**
     * 抑制告警
     */
    private void suppressAlert(String alertKey) {
        LocalDateTime suppressUntil = LocalDateTime.now().plusMinutes(defaultSuppressionDurationMinutes);
        alertSuppressionMap.put(alertKey, suppressUntil);
    }
    
    /**
     * 清理过期的抑制
     */
    private void cleanupExpiredSuppressions() {
        LocalDateTime now = LocalDateTime.now();
        alertSuppressionMap.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
    
    /**
     * 添加到历史记录
     */
    private void addToHistory(Alert alert) {
        alertHistory.offer(alert.copy());
        
        // 保持最大历史记录数量
        while (alertHistory.size() > MAX_ALERT_HISTORY) {
            alertHistory.poll();
        }
    }
    
    /**
     * 更新告警统计
     */
    private void updateAlertStatistics(AlertLevel level) {
        totalAlertsGenerated.incrementAndGet();
        
        switch (level) {
            case INFO:
                infoAlertsCount.incrementAndGet();
                break;
            case WARNING:
                warningAlertsCount.incrementAndGet();
                break;
            case CRITICAL:
                criticalAlertsCount.incrementAndGet();
                break;
        }
    }
    
    /**
     * 添加告警规则
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.getRuleId(), rule);
        logger.info("添加告警规则: {} - {}", rule.getRuleId(), rule.getName());
    }
    
    /**
     * 删除告警规则
     */
    public void removeAlertRule(String ruleId) {
        AlertRule removed = alertRules.remove(ruleId);
        if (removed != null) {
            // 同时解除相关的活跃告警
            resolveAlert(ruleId);
            logger.info("删除告警规则: {} - {}", ruleId, removed.getName());
        }
    }
    
    /**
     * 获取所有活跃告警
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    /**
     * 获取指定级别的活跃告警
     */
    public List<Alert> getActiveAlertsByLevel(AlertLevel level) {
        return activeAlerts.values().stream()
            .filter(alert -> alert.getLevel() == level)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取告警历史
     */
    public List<Alert> getAlertHistory(int limit) {
        List<Alert> history = new ArrayList<>(alertHistory);
        if (limit > 0 && history.size() > limit) {
            return history.subList(Math.max(0, history.size() - limit), history.size());
        }
        return history;
    }
    
    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("totalAlertsGenerated", totalAlertsGenerated.get());
        stats.put("infoAlertsCount", infoAlertsCount.get());
        stats.put("warningAlertsCount", warningAlertsCount.get());
        stats.put("criticalAlertsCount", criticalAlertsCount.get());
        stats.put("activeAlertsCount", activeAlerts.size());
        stats.put("suppressedAlertsCount", alertSuppressionMap.size());
        
        // 按级别统计活跃告警
        Map<String, Long> activeByLevel = activeAlerts.values().stream()
            .collect(Collectors.groupingBy(
                alert -> alert.getLevel().toString(),
                Collectors.counting()
            ));
        stats.put("activeAlertsByLevel", activeByLevel);
        
        return stats;
    }
    
    /**
     * 获取所有告警规则
     */
    public List<AlertRule> getAllAlertRules() {
        return new ArrayList<>(alertRules.values());
    }
    
    /**
     * 手动触发告警检查
     */
    public void triggerManualAlertCheck() {
        logger.info("手动触发告警检查...");
        checkAllAlerts();
    }
    
    /**
     * 确认告警（停止通知但保持活跃状态）
     */
    public void acknowledgeAlert(String alertKey) {
        Alert alert = activeAlerts.get(alertKey);
        if (alert != null) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedTime(LocalDateTime.now());
            logger.info("确认告警: [{}] {}", alert.getLevel(), alert.getName());
        }
    }
    
    /**
     * 手动解除告警
     */
    public void manuallyResolveAlert(String alertKey) {
        resolveAlert(alertKey);
        logger.info("手动解除告警: {}", alertKey);
    }
    
    /**
     * 更新告警抑制时间
     */
    public void updateSuppressionDuration(long minutes) {
        this.defaultSuppressionDurationMinutes = minutes;
        logger.info("更新告警抑制时间为: {} 分钟", minutes);
    }
    
    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        INFO("信息"),
        WARNING("警告"), 
        CRITICAL("严重");
        
        private final String description;
        
        AlertLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 告警状态枚举
     */
    public enum AlertStatus {
        ACTIVE("活跃"),
        ACKNOWLEDGED("已确认"),
        RESOLVED("已解除");
        
        private final String description;
        
        AlertStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 告警规则类
     */
    public static class AlertRule {
        private final String ruleId;
        private final String name;
        private final String description;
        private final String metricName;
        private final Operator operator;
        private final double threshold;
        private final AlertLevel level;
        private boolean enabled;
        
        public AlertRule(String ruleId, String name, String metricName, 
                        Operator operator, double threshold, AlertLevel level) {
            this.ruleId = ruleId;
            this.name = name;
            this.description = name;
            this.metricName = metricName;
            this.operator = operator;
            this.threshold = threshold;
            this.level = level;
            this.enabled = true;
        }
        
        public enum Operator {
            GREATER_THAN,
            LESS_THAN,
            EQUALS,
            GREATER_THAN_OR_EQUAL,
            LESS_THAN_OR_EQUAL
        }
        
        // Getters
        public String getRuleId() { return ruleId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMetricName() { return metricName; }
        public Operator getOperator() { return operator; }
        public double getThreshold() { return threshold; }
        public AlertLevel getLevel() { return level; }
        public boolean isEnabled() { return enabled; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * 告警类
     */
    public static class Alert {
        private final String alertId;
        private final String name;
        private final String description;
        private final AlertLevel level;
        private final LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private LocalDateTime resolvedTime;
        private LocalDateTime acknowledgedTime;
        private double currentValue;
        private final double threshold;
        private AlertStatus status;
        private boolean acknowledged;
        private int occurrenceCount;
        
        public Alert(String alertId, String name, String description, AlertLevel level,
                    LocalDateTime firstOccurrence, double currentValue, double threshold, 
                    AlertStatus status) {
            this.alertId = alertId;
            this.name = name;
            this.description = description;
            this.level = level;
            this.firstOccurrence = firstOccurrence;
            this.lastOccurrence = firstOccurrence;
            this.currentValue = currentValue;
            this.threshold = threshold;
            this.status = status;
            this.acknowledged = false;
            this.occurrenceCount = 1;
        }
        
        public Alert copy() {
            Alert copy = new Alert(alertId, name, description, level, firstOccurrence, 
                                 currentValue, threshold, status);
            copy.lastOccurrence = this.lastOccurrence;
            copy.resolvedTime = this.resolvedTime;
            copy.acknowledgedTime = this.acknowledgedTime;
            copy.acknowledged = this.acknowledged;
            copy.occurrenceCount = this.occurrenceCount;
            return copy;
        }
        
        // Getters and Setters
        public String getAlertId() { return alertId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public AlertLevel getLevel() { return level; }
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
        public LocalDateTime getResolvedTime() { return resolvedTime; }
        public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }
        public LocalDateTime getAcknowledgedTime() { return acknowledgedTime; }
        public void setAcknowledgedTime(LocalDateTime acknowledgedTime) { this.acknowledgedTime = acknowledgedTime; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        public double getThreshold() { return threshold; }
        public AlertStatus getStatus() { return status; }
        public void setStatus(AlertStatus status) { this.status = status; }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public void incrementOccurrenceCount() { this.occurrenceCount++; }
    }
}