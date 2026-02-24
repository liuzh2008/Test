package com.example.medaiassistant.service.monitoring;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DRGs告警服务
 * 负责监控DRGs分析过程中的关键指标并触发告警
 * 
 * 主要功能：
 * 1. 告警阈值配置管理
 * 2. 告警规则检查
 * 3. 告警触发和解决
 * 4. 告警统计和查询
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@Service
public class DrgAlertService {

    private final DrgMonitoringService monitoringService;
    
    // 告警配置
    private final Map<String, Object> alertConfigurations;
    
    // 告警历史记录
    private final List<Alert> alertHistory;
    
    // 活动告警
    private final Map<String, Alert> activeAlerts;

    /**
     * 构造函数
     * 
     * @param monitoringService 监控服务
     */
    public DrgAlertService(DrgMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        this.alertConfigurations = new ConcurrentHashMap<>();
        this.alertHistory = new CopyOnWriteArrayList<>();
        this.activeAlerts = new ConcurrentHashMap<>();
    }

    /**
     * 配置快照生成失败率告警阈值
     * 
     * @param threshold 失败率阈值（百分比）
     */
    public void configureSnapshotFailureRateAlert(double threshold) {
        Map<String, Object> config = new HashMap<>();
        config.put("threshold", threshold);
        config.put("unit", "PERCENTAGE");
        alertConfigurations.put("snapshotFailureRate", config);
    }

    /**
     * 配置Prompt保存超时告警阈值
     * 
     * @param timeoutMillis 超时阈值（毫秒）
     */
    public void configurePromptSaveTimeoutAlert(long timeoutMillis) {
        Map<String, Object> config = new HashMap<>();
        config.put("threshold", timeoutMillis);
        config.put("unit", "MILLISECONDS");
        alertConfigurations.put("promptSaveTimeout", config);
    }

    /**
     * 配置用户决策响应时间告警阈值
     * 
     * @param responseTimeMillis 响应时间阈值（毫秒）
     */
    public void configureUserDecisionResponseTimeAlert(long responseTimeMillis) {
        Map<String, Object> config = new HashMap<>();
        config.put("threshold", responseTimeMillis);
        config.put("unit", "MILLISECONDS");
        alertConfigurations.put("userDecisionResponseTime", config);
    }

    /**
     * 配置盈亏计算错误率告警阈值
     * 
     * @param threshold 错误率阈值（百分比）
     */
    public void configureProfitLossErrorRateAlert(double threshold) {
        Map<String, Object> config = new HashMap<>();
        config.put("threshold", threshold);
        config.put("unit", "PERCENTAGE");
        alertConfigurations.put("profitLossErrorRate", config);
    }

    /**
     * 检查所有告警规则
     * 
     * @return 触发的告警列表
     */
    public List<Alert> checkAllAlerts() {
        List<Alert> triggeredAlerts = new ArrayList<>();
        
        // 检查快照生成失败率告警
        if (alertConfigurations.containsKey("snapshotFailureRate")) {
            Alert alert = checkSnapshotFailureRateAlert();
            if (alert != null) {
                triggeredAlerts.add(alert);
            }
        }
        
        // 检查Prompt保存超时告警
        if (alertConfigurations.containsKey("promptSaveTimeout")) {
            Alert alert = checkPromptSaveTimeoutAlert();
            if (alert != null) {
                triggeredAlerts.add(alert);
            }
        }
        
        // 检查用户决策响应时间告警
        if (alertConfigurations.containsKey("userDecisionResponseTime")) {
            Alert alert = checkUserDecisionResponseTimeAlert();
            if (alert != null) {
                triggeredAlerts.add(alert);
            }
        }
        
        // 检查盈亏计算错误率告警
        if (alertConfigurations.containsKey("profitLossErrorRate")) {
            Alert alert = checkProfitLossErrorRateAlert();
            if (alert != null) {
                triggeredAlerts.add(alert);
            }
        }
        
        // 处理自动恢复
        checkAutoResolution();
        
        return triggeredAlerts;
    }

    /**
     * 检查快照生成失败率告警
     * 
     * @return 触发的告警，如果未触发则返回null
     */
    private Alert checkSnapshotFailureRateAlert() {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) alertConfigurations.get("snapshotFailureRate");
        double threshold = (Double) config.get("threshold");
        
        Map<String, Object> metrics = monitoringService.getSnapshotGenerationMetrics();
        long totalOperations = (Long) metrics.get("total");
        double failureRate = 100.0 - (Double) metrics.get("successRate");
        
        // 只有当有足够的数据时才检查告警
        if (totalOperations > 0) {
            String alertId = "SNAPSHOT_FAILURE_RATE";
            if (failureRate > threshold) {
                if (!activeAlerts.containsKey(alertId)) {
                    String message = String.format("快照生成失败率过高：%.1f%% (阈值：%.1f%%)", failureRate, threshold);
                    Alert alert = new Alert("SNAPSHOT_FAILURE_RATE", "CRITICAL", message);
                    activeAlerts.put(alertId, alert);
                    alertHistory.add(alert);
                    return alert;
                }
            } else {
                // 条件改善，尝试自动解决告警
                if (activeAlerts.containsKey(alertId)) {
                    Alert alert = activeAlerts.get(alertId);
                    alert.resolve();
                    activeAlerts.remove(alertId);
                }
            }
        }
        
        return null;
    }

    /**
     * 检查Prompt保存超时告警
     * 
     * @return 触发的告警，如果未触发则返回null
     */
    private Alert checkPromptSaveTimeoutAlert() {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) alertConfigurations.get("promptSaveTimeout");
        long threshold = (Long) config.get("threshold");
        
        Map<String, Object> metrics = monitoringService.getPromptSaveMetrics();
        double averageDuration = (Double) metrics.get("averageDuration");
        
        if (averageDuration > threshold && (Long) metrics.get("total") > 0) {
            String alertId = "PROMPT_SAVE_TIMEOUT_" + metrics.get("total");
            if (!activeAlerts.containsKey(alertId)) {
                String message = String.format("Prompt保存平均耗时过长：%.1fms (阈值：%dms)", averageDuration, threshold);
                Alert alert = new Alert("PROMPT_SAVE_TIMEOUT", "WARNING", message);
                activeAlerts.put(alertId, alert);
                alertHistory.add(alert);
                return alert;
            }
        } else {
            // 条件改善，尝试自动解决告警
            String alertId = "PROMPT_SAVE_TIMEOUT_" + metrics.get("total");
            if (activeAlerts.containsKey(alertId)) {
                Alert alert = activeAlerts.get(alertId);
                alert.resolve();
                activeAlerts.remove(alertId);
            }
        }
        
        return null;
    }

    /**
     * 检查用户决策响应时间告警
     * 
     * @return 触发的告警，如果未触发则返回null
     */
    private Alert checkUserDecisionResponseTimeAlert() {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) alertConfigurations.get("userDecisionResponseTime");
        long threshold = (Long) config.get("threshold");
        
        Map<String, Object> metrics = monitoringService.getUserDecisionMetrics();
        double averageDuration = (Double) metrics.get("averageDuration");
        
        if (averageDuration > threshold && (Long) metrics.get("total") > 0) {
            String alertId = "USER_DECISION_RESPONSE_TIME_" + metrics.get("total");
            if (!activeAlerts.containsKey(alertId)) {
                String message = String.format("用户决策响应时间过长：%.1fms (阈值：%dms)", averageDuration, threshold);
                Alert alert = new Alert("USER_DECISION_RESPONSE_TIME", "WARNING", message);
                activeAlerts.put(alertId, alert);
                alertHistory.add(alert);
                return alert;
            }
        } else {
            // 条件改善，尝试自动解决告警
            String alertId = "USER_DECISION_RESPONSE_TIME_" + metrics.get("total");
            if (activeAlerts.containsKey(alertId)) {
                Alert alert = activeAlerts.get(alertId);
                alert.resolve();
                activeAlerts.remove(alertId);
            }
        }
        
        return null;
    }

    /**
     * 检查盈亏计算错误率告警
     * 
     * @return 触发的告警，如果未触发则返回null
     */
    private Alert checkProfitLossErrorRateAlert() {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) alertConfigurations.get("profitLossErrorRate");
        double threshold = (Double) config.get("threshold");
        
        Map<String, Object> metrics = monitoringService.getProfitLossCalculationMetrics();
        double errorRate = 100.0 - (Double) metrics.get("successRate");
        
        if (errorRate > threshold && (Long) metrics.get("total") > 0) {
            String alertId = "PROFIT_LOSS_ERROR_RATE_" + metrics.get("total");
            if (!activeAlerts.containsKey(alertId)) {
                String message = String.format("盈亏计算错误率过高：%.1f%% (阈值：%.1f%%)", errorRate, threshold);
                Alert alert = new Alert("PROFIT_LOSS_ERROR_RATE", "CRITICAL", message);
                activeAlerts.put(alertId, alert);
                alertHistory.add(alert);
                return alert;
            }
        } else {
            // 条件改善，尝试自动解决告警
            String alertId = "PROFIT_LOSS_ERROR_RATE_" + metrics.get("total");
            if (activeAlerts.containsKey(alertId)) {
                Alert alert = activeAlerts.get(alertId);
                alert.resolve();
                activeAlerts.remove(alertId);
            }
        }
        
        return null;
    }

    /**
     * 检查自动解决条件
     */
    private void checkAutoResolution() {
        // 这里可以添加更复杂的自动解决逻辑
        // 例如：如果告警持续一段时间后条件改善，则自动解决
    }

    /**
     * 获取活动告警列表
     * 
     * @return 活动告警列表
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * 获取所有告警历史记录
     * 
     * @return 所有告警历史记录
     */
    public List<Alert> getAllAlerts() {
        return new ArrayList<>(alertHistory);
    }

    /**
     * 解决指定告警
     * 
     * @param alertId 告警ID
     */
    public void resolveAlert(String alertId) {
        Alert alert = activeAlerts.values().stream()
                .filter(a -> a.getId().equals(alertId))
                .findFirst()
                .orElse(null);
        
        if (alert != null) {
            alert.resolve();
            activeAlerts.values().removeIf(a -> a.getId().equals(alertId));
        }
    }

    /**
     * 获取告警配置
     * 
     * @return 告警配置
     */
    public Map<String, Object> getAlertConfigurations() {
        return new HashMap<>(alertConfigurations);
    }

    /**
     * 重置告警配置
     */
    public void resetAlertConfigurations() {
        alertConfigurations.clear();
    }

    /**
     * 获取告警统计信息
     * 
     * @return 告警统计信息
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalAlerts = alertHistory.size();
        long activeAlertsCount = activeAlerts.size();
        long resolvedAlerts = totalAlerts - activeAlertsCount;
        
        statistics.put("totalAlerts", (int) totalAlerts);
        statistics.put("activeAlerts", (int) activeAlertsCount);
        statistics.put("resolvedAlerts", (int) resolvedAlerts);
        statistics.put("generatedAt", LocalDateTime.now());
        
        // 按告警类型统计
        Map<String, Map<String, Long>> alertTypeStatistics = new HashMap<>();
        
        for (Alert alert : alertHistory) {
            String alertType = alert.getAlertType();
            alertTypeStatistics.putIfAbsent(alertType, new HashMap<>());
            Map<String, Long> typeStats = alertTypeStatistics.get(alertType);
            
            typeStats.put("total", typeStats.getOrDefault("total", 0L) + 1);
            if (alert.isActive()) {
                typeStats.put("active", typeStats.getOrDefault("active", 0L) + 1);
            } else {
                typeStats.put("resolved", typeStats.getOrDefault("resolved", 0L) + 1);
            }
        }
        
        // 确保所有告警类型都有完整的统计字段
        for (Map<String, Long> typeStats : alertTypeStatistics.values()) {
            typeStats.putIfAbsent("total", 0L);
            typeStats.putIfAbsent("active", 0L);
            typeStats.putIfAbsent("resolved", 0L);
        }
        
        statistics.put("alertTypeStatistics", alertTypeStatistics);
        
        return statistics;
    }
}
