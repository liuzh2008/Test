package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.ConnectionTestResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 健康检查服务
 * 负责定期检查医院数据库连接健康状态，记录响应时间，更新健康状态缓存
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {
    
    private final HospitalConfigService hospitalConfigService;
    private final DatabaseConnectionTester databaseConnectionTester;
    
    // 健康状态缓存
    private final Map<String, ConnectionHealth> healthCache = new ConcurrentHashMap<>();
    
    // 性能指标缓存
    private final Map<String, PerformanceMetrics> metricsCache = new ConcurrentHashMap<>();
    
    /**
     * 执行定时健康检查任务
     */
    @Scheduled(cron = "${hospital.health.check.cron:0 */5 * * * *}")
    public void performScheduledHealthCheck() {
        log.info("开始执行定时健康检查");
        long startTime = System.currentTimeMillis();
        
        try {
            checkAllConnections();
            log.info("定时健康检查完成，耗时: {}ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("定时健康检查执行异常", e);
        }
    }
    
    /**
     * 检查所有启用的医院数据库连接
     */
    public void checkAllConnections() {
        List<HospitalConfig> enabledHospitals = hospitalConfigService.getEnabledConfigs();
        log.info("检查 {} 个启用的医院数据库连接", enabledHospitals.size());
        
        for (HospitalConfig config : enabledHospitals) {
            String hospitalId = config.getHospital().getId();
            try {
                ConnectionHealth health = checkConnection(config);
                healthCache.put(hospitalId, health);
                
                if (!health.isHealthy()) {
                    log.warn("医院 {} 数据库连接异常: {}", hospitalId, health.getErrorMessage());
                } else {
                    log.debug("医院 {} 数据库连接正常，响应时间: {}ms", hospitalId, health.getResponseTime());
                }
            } catch (Exception e) {
                log.error("医院 {} 健康检查异常", hospitalId, e);
                healthCache.put(hospitalId, ConnectionHealth.error(hospitalId, e.getMessage()));
            }
        }
    }
    
    /**
     * 检查单个医院数据库连接
     */
    public ConnectionHealth checkConnection(HospitalConfig config) {
        String hospitalId = config.getHospital().getId();
        String hospitalName = config.getHospital().getName();
        
        ConnectionHealth health = new ConnectionHealth();
        health.setHospitalId(hospitalId);
        health.setHospitalName(hospitalName);
        health.setCheckTime(new Date());
        
        try {
            ConnectionTestResult result = databaseConnectionTester.testConnection(config);
            
            health.setHealthy(result.isSuccess());
            health.setResponseTime(result.getResponseTimeMs());
            health.setDatabaseUrl(result.getDatabaseUrl());
            
            if (!result.isSuccess()) {
                health.setErrorMessage(result.getErrorMessage());
            }
            
            updatePerformanceMetrics(hospitalId, result.isSuccess(), result.getResponseTimeMs());
            
        } catch (Exception e) {
            health.setHealthy(false);
            health.setErrorMessage("健康检查异常: " + e.getMessage());
            log.error("医院 {} 健康检查异常", hospitalId, e);
        }
        
        return health;
    }
    
    /**
     * 检查单个医院数据库连接（通过医院ID）
     */
    public ConnectionHealth checkConnection(String hospitalId) {
        HospitalConfig config = hospitalConfigService.getConfig(hospitalId);
        if (config == null) {
            log.warn("医院配置不存在: {}", hospitalId);
            return ConnectionHealth.error(hospitalId, "医院配置不存在: " + hospitalId);
        }
        return checkConnection(config);
    }
    
    /**
     * 获取医院健康状态
     */
    public ConnectionHealth getHealthStatus(String hospitalId) {
        ConnectionHealth health = healthCache.get(hospitalId);
        if (health == null) {
            health = checkConnection(hospitalId);
            healthCache.put(hospitalId, health);
        }
        return health;
    }
    
    /**
     * 获取所有医院健康状态
     */
    public Map<String, ConnectionHealth> getAllHealthStatus() {
        if (healthCache.isEmpty()) {
            checkAllConnections();
        }
        return new HashMap<>(healthCache);
    }
    
    /**
     * 检查服务状态
     */
    public Map<String, Object> checkServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("service", "HealthCheckService");
        status.put("status", "UP");
        status.put("timestamp", new Date());
        status.put("cacheSize", healthCache.size());
        status.put("metricsCacheSize", metricsCache.size());
        
        long totalHospitals = hospitalConfigService.getAllConfigs().size();
        long enabledHospitals = hospitalConfigService.getEnabledConfigs().size();
        long healthyCount = healthCache.values().stream().filter(ConnectionHealth::isHealthy).count();
        long unhealthyCount = healthCache.values().stream().filter(h -> !h.isHealthy()).count();
        
        status.put("totalHospitals", totalHospitals);
        status.put("enabledHospitals", enabledHospitals);
        status.put("healthyCount", healthyCount);
        status.put("unhealthyCount", unhealthyCount);
        status.put("healthCheckEnabled", true);
        status.put("healthCheckCron", "0 */5 * * * *");
        
        return status;
    }
    
    /**
     * 收集性能指标
     */
    public Map<String, Object> collectPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("timestamp", new Date());
        metrics.put("totalChecks", metricsCache.values().stream()
            .mapToLong(m -> m.getTotalChecks())
            .sum());
        metrics.put("successfulChecks", metricsCache.values().stream()
            .mapToLong(m -> m.getSuccessfulChecks())
            .sum());
        metrics.put("failedChecks", metricsCache.values().stream()
            .mapToLong(m -> m.getFailedChecks())
            .sum());
        
        double avgResponseTime = metricsCache.values().stream()
            .filter(m -> m.getTotalChecks() > 0)
            .mapToDouble(m -> m.getAverageResponseTime())
            .average()
            .orElse(0.0);
        metrics.put("averageResponseTime", avgResponseTime);
        
        long total = metricsCache.values().stream()
            .mapToLong(m -> m.getTotalChecks())
            .sum();
        long success = metricsCache.values().stream()
            .mapToLong(m -> m.getSuccessfulChecks())
            .sum();
        double successRate = total > 0 ? (success * 100.0) / total : 0.0;
        metrics.put("successRate", successRate);
        
        Map<String, Object> hospitalMetrics = new HashMap<>();
        for (Map.Entry<String, PerformanceMetrics> entry : metricsCache.entrySet()) {
            hospitalMetrics.put(entry.getKey(), entry.getValue().toMap());
        }
        metrics.put("hospitalMetrics", hospitalMetrics);
        
        return metrics;
    }
    
    /**
     * 生成健康状态报告
     */
    public Map<String, Object> generateHealthStatusReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("reportType", "HealthStatusReport");
        report.put("generatedAt", new Date());
        report.put("service", "HospitalDataSyncSystem");
        report.put("serviceStatus", checkServiceStatus());
        
        Map<String, ConnectionHealth> allHealth = getAllHealthStatus();
        report.put("totalHospitals", allHealth.size());
        
        long healthyCount = allHealth.values().stream().filter(ConnectionHealth::isHealthy).count();
        long unhealthyCount = allHealth.values().stream().filter(h -> !h.isHealthy()).count();
        report.put("healthyCount", healthyCount);
        report.put("unhealthyCount", unhealthyCount);
        
        Map<String, Object> detailedHealth = new HashMap<>();
        for (Map.Entry<String, ConnectionHealth> entry : allHealth.entrySet()) {
            detailedHealth.put(entry.getKey(), entry.getValue().toMap());
        }
        report.put("detailedHealth", detailedHealth);
        
        report.put("performanceMetrics", collectPerformanceMetrics());
        
        List<String> recommendations = new ArrayList<>();
        if (unhealthyCount > 0) {
            recommendations.add(String.format("有 %d 个医院数据库连接异常，请检查配置和网络连接", unhealthyCount));
        }
        if (healthyCount == 0 && allHealth.size() > 0) {
            recommendations.add("所有医院数据库连接都异常，请立即检查系统配置");
        }
        report.put("recommendations", recommendations);
        
        return report;
    }
    
    /**
     * 更新性能指标
     */
    private void updatePerformanceMetrics(String hospitalId, boolean success, long responseTime) {
        PerformanceMetrics metrics = metricsCache.computeIfAbsent(hospitalId, 
            k -> new PerformanceMetrics());
        metrics.recordCheck(success, responseTime);
    }
    
    /**
     * 连接健康状态类
     */
    public static class ConnectionHealth {
        private String hospitalId;
        private String hospitalName;
        private Date checkTime;
        private boolean healthy;
        private long responseTime;
        private String databaseUrl;
        private String errorMessage;
        
        public String getHospitalId() { return hospitalId; }
        public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
        
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
        
        public Date getCheckTime() { return checkTime; }
        public void setCheckTime(Date checkTime) { this.checkTime = checkTime; }
        
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        
        public String getDatabaseUrl() { return databaseUrl; }
        public void setDatabaseUrl(String databaseUrl) { this.databaseUrl = databaseUrl; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("hospitalId", hospitalId);
            map.put("hospitalName", hospitalName);
            map.put("checkTime", checkTime);
            map.put("healthy", healthy);
            map.put("responseTime", responseTime);
            map.put("databaseUrl", databaseUrl);
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            return map;
        }
        
        public static ConnectionHealth error(String hospitalId, String errorMessage) {
            ConnectionHealth health = new ConnectionHealth();
            health.setHospitalId(hospitalId);
            health.setCheckTime(new Date());
            health.setHealthy(false);
            health.setErrorMessage(errorMessage);
            return health;
        }
    }
    
    /**
     * 性能指标类
     */
    public static class PerformanceMetrics {
        private long totalChecks = 0;
        private long successfulChecks = 0;
        private long failedChecks = 0;
        private long totalResponseTime = 0;
        private long minResponseTime = Long.MAX_VALUE;
        private long maxResponseTime = 0;
        
        public void recordCheck(boolean success, long responseTime) {
            totalChecks++;
            if (success) {
                successfulChecks++;
            } else {
                failedChecks++;
            }
            
            totalResponseTime += responseTime;
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
        }
        
        public double getAverageResponseTime() {
            return totalChecks > 0 ? (double) totalResponseTime / totalChecks : 0.0;
        }
        
        public double getSuccessRate() {
            return totalChecks > 0 ? (successfulChecks * 100.0) / totalChecks : 0.0;
        }
        
        public long getTotalChecks() { return totalChecks; }
        public long getSuccessfulChecks() { return successfulChecks; }
        public long getFailedChecks() { return failedChecks; }
        public long getMinResponseTime() { return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalChecks", totalChecks);
            map.put("successfulChecks", successfulChecks);
            map.put("failedChecks", failedChecks);
            map.put("averageResponseTime", getAverageResponseTime());
            map.put("minResponseTime", getMinResponseTime());
            map.put("maxResponseTime", maxResponseTime);
            map.put("successRate", getSuccessRate());
            return map;
        }
    }
}
