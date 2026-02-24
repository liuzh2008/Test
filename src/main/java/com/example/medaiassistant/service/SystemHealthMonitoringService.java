package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 系统健康指标监控服务
 * 负责监控系统级指标：CPU、内存、磁盘、数据库连接等
 * 
 * 主要功能：
 * 1. JVM内存监控和告警
 * 2. 系统CPU和线程监控
 * 3. 磁盘空间监控
 * 4. 数据库连接池健康监控
 * 5. 系统性能趋势分析
 */
@Service
public class SystemHealthMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthMonitoringService.class);
    
    @Autowired
    private DataSource dataSource;
    

    
    // JVM管理接口
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    
    // 健康状态指标
    private volatile boolean systemHealthy = true;
    private volatile String healthStatus = "HEALTHY";
    private final Map<String, Object> currentHealthMetrics = new ConcurrentHashMap<>();
    private final List<HealthAlert> activeAlerts = new ArrayList<>();
    
    // 阈值配置
    private double memoryUsageThreshold = 85.0; // 内存使用率阈值 85%
    private double diskUsageThreshold = 90.0;   // 磁盘使用率阈值 90%
    private int maxThreadCountThreshold = 500;   // 最大线程数阈值
    private long maxResponseTimeThreshold = 5000; // 数据库响应时间阈值 5秒
    
    @PostConstruct
    public void initializeHealthMonitoring() {
        logger.info("初始化系统健康监控服务...");
        
        // 执行初始健康检查
        performHealthCheck();
        
        logger.info("系统健康监控服务初始化完成");
    }
    
    /**
     * 定期执行健康检查（每1分钟）
     */
    @Scheduled(fixedDelay = 60000)
    public void performScheduledHealthCheck() {
        try {
            performHealthCheck();
        } catch (Exception e) {
            logger.error("定期健康检查执行失败", e);
            updateHealthStatus("UNHEALTHY", "定期检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行全面的健康检查
     */
    public Map<String, Object> performHealthCheck() {
        logger.debug("开始执行系统健康检查...");
        
        Map<String, Object> healthReport = new ConcurrentHashMap<>();
        List<HealthAlert> newAlerts = new ArrayList<>();
        boolean overallHealthy = true;
        
        try {
            // 1. JVM内存监控
            Map<String, Object> memoryMetrics = checkMemoryHealth(newAlerts);
            healthReport.put("memory", memoryMetrics);
            
            // 2. 线程监控
            Map<String, Object> threadMetrics = checkThreadHealth(newAlerts);
            healthReport.put("threads", threadMetrics);
            
            // 3. 垃圾回收监控
            Map<String, Object> gcMetrics = checkGarbageCollectionHealth(newAlerts);
            healthReport.put("gc", gcMetrics);
            
            // 4. 磁盘空间监控
            Map<String, Object> diskMetrics = checkDiskHealth(newAlerts);
            healthReport.put("disk", diskMetrics);
            
            // 5. 数据库连接监控
            Map<String, Object> databaseMetrics = checkDatabaseHealth(newAlerts);
            healthReport.put("database", databaseMetrics);
            
            // 6. 系统负载监控
            Map<String, Object> systemMetrics = checkSystemLoad(newAlerts);
            healthReport.put("system", systemMetrics);
            
            // 更新告警状态
            overallHealthy = newAlerts.isEmpty();
            updateAlerts(newAlerts);
            
            // 更新整体健康状态
            if (overallHealthy) {
                updateHealthStatus("HEALTHY", "所有系统指标正常");
            } else {
                updateHealthStatus("WARNING", "发现 " + newAlerts.size() + " 个健康问题");
            }
            
            // 更新当前指标
            currentHealthMetrics.clear();
            currentHealthMetrics.putAll(healthReport);
            currentHealthMetrics.put("checkTime", LocalDateTime.now().toString());
            currentHealthMetrics.put("overallStatus", healthStatus);
            
        } catch (Exception e) {
            logger.error("健康检查执行异常", e);
            overallHealthy = false;
            updateHealthStatus("UNHEALTHY", "健康检查异常: " + e.getMessage());
            healthReport.put("error", e.getMessage());
        }
        
        logger.debug("系统健康检查完成，状态: {}", healthStatus);
        return healthReport;
    }
    
    /**
     * 检查内存健康状态
     */
    private Map<String, Object> checkMemoryHealth(List<HealthAlert> alerts) {
        Map<String, Object> memoryMetrics = new ConcurrentHashMap<>();
        
        // 堆内存指标
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        long heapUsed = heapMemory.getUsed();
        long heapMax = heapMemory.getMax();
        double heapUsagePercentage = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0.0;
        
        memoryMetrics.put("heap.used", heapUsed);
        memoryMetrics.put("heap.max", heapMax);
        memoryMetrics.put("heap.usage.percentage", Math.round(heapUsagePercentage * 100.0) / 100.0);
        
        // 非堆内存指标
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        memoryMetrics.put("nonheap.used", nonHeapMemory.getUsed());
        memoryMetrics.put("nonheap.max", nonHeapMemory.getMax());
        
        // 检查内存使用率告警
        if (heapUsagePercentage > memoryUsageThreshold) {
            alerts.add(new HealthAlert("MEMORY_HIGH_USAGE", 
                String.format("堆内存使用率过高: %.2f%% (阈值: %.1f%%)", heapUsagePercentage, memoryUsageThreshold),
                "CRITICAL"));
        }
        
        return memoryMetrics;
    }
    
    /**
     * 检查线程健康状态
     */
    private Map<String, Object> checkThreadHealth(List<HealthAlert> alerts) {
        Map<String, Object> threadMetrics = new ConcurrentHashMap<>();
        
        int threadCount = threadMXBean.getThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        int daemonThreadCount = threadMXBean.getDaemonThreadCount();
        
        threadMetrics.put("count", threadCount);
        threadMetrics.put("peak", peakThreadCount);
        threadMetrics.put("daemon", daemonThreadCount);
        
        // 检查线程数告警
        if (threadCount > maxThreadCountThreshold) {
            alerts.add(new HealthAlert("THREAD_COUNT_HIGH", 
                String.format("线程数量过多: %d (阈值: %d)", threadCount, maxThreadCountThreshold),
                "WARNING"));
        }
        
        return threadMetrics;
    }
    
    /**
     * 检查垃圾回收健康状态
     */
    private Map<String, Object> checkGarbageCollectionHealth(List<HealthAlert> alerts) {
        Map<String, Object> gcMetrics = new ConcurrentHashMap<>();
        
        long totalCollectionCount = 0;
        long totalCollectionTime = 0;
        
        for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
            String gcName = gcMXBean.getName();
            long collectionCount = gcMXBean.getCollectionCount();
            long collectionTime = gcMXBean.getCollectionTime();
            
            totalCollectionCount += collectionCount;
            totalCollectionTime += collectionTime;
            
            Map<String, Object> gcInfo = new ConcurrentHashMap<>();
            gcInfo.put("count", collectionCount);
            gcInfo.put("time", collectionTime);
            gcMetrics.put(gcName, gcInfo);
        }
        
        gcMetrics.put("total.count", totalCollectionCount);
        gcMetrics.put("total.time", totalCollectionTime);
        
        // 检查GC频率告警（简单示例）
        long uptime = runtimeMXBean.getUptime();
        if (uptime > 0 && totalCollectionTime > uptime * 0.1) { // GC时间超过总运行时间的10%
            alerts.add(new HealthAlert("GC_HIGH_OVERHEAD", 
                String.format("GC开销过高: %d ms / %d ms (%.2f%%)", 
                    totalCollectionTime, uptime, (double)totalCollectionTime / uptime * 100),
                "WARNING"));
        }
        
        return gcMetrics;
    }
    
    /**
     * 检查磁盘健康状态
     */
    private Map<String, Object> checkDiskHealth(List<HealthAlert> alerts) {
        Map<String, Object> diskMetrics = new ConcurrentHashMap<>();
        
        File currentDir = new File(".");
        long totalSpace = currentDir.getTotalSpace();
        long freeSpace = currentDir.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double usagePercentage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0.0;
        
        diskMetrics.put("total.space", totalSpace);
        diskMetrics.put("free.space", freeSpace);
        diskMetrics.put("used.space", usedSpace);
        diskMetrics.put("usage.percentage", Math.round(usagePercentage * 100.0) / 100.0);
        
        // 检查磁盘使用率告警
        if (usagePercentage > diskUsageThreshold) {
            alerts.add(new HealthAlert("DISK_HIGH_USAGE", 
                String.format("磁盘使用率过高: %.2f%% (阈值: %.1f%%)", usagePercentage, diskUsageThreshold),
                "CRITICAL"));
        }
        
        return diskMetrics;
    }
    
    /**
     * 检查数据库健康状态
     */
    private Map<String, Object> checkDatabaseHealth(List<HealthAlert> alerts) {
        Map<String, Object> databaseMetrics = new ConcurrentHashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            databaseMetrics.put("connection.successful", true);
            databaseMetrics.put("response.time", responseTime);
            
            // 检查响应时间告警
            if (responseTime > maxResponseTimeThreshold) {
                alerts.add(new HealthAlert("DATABASE_SLOW_RESPONSE", 
                    String.format("数据库响应时间过长: %d ms (阈值: %d ms)", responseTime, maxResponseTimeThreshold),
                    "WARNING"));
            }
            
        } catch (SQLException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            databaseMetrics.put("connection.successful", false);
            databaseMetrics.put("response.time", responseTime);
            databaseMetrics.put("error.message", e.getMessage());
            
            alerts.add(new HealthAlert("DATABASE_CONNECTION_FAILED", 
                "数据库连接失败: " + e.getMessage(),
                "CRITICAL"));
        }
        
        return databaseMetrics;
    }
    
    /**
     * 检查系统负载
     */
    private Map<String, Object> checkSystemLoad(List<HealthAlert> alerts) {
        Map<String, Object> systemMetrics = new ConcurrentHashMap<>();
        
        // 系统负载（如果支持）
        double systemLoadAverage = osMXBean.getSystemLoadAverage();
        int availableProcessors = osMXBean.getAvailableProcessors();
        
        systemMetrics.put("load.average", systemLoadAverage);
        systemMetrics.put("available.processors", availableProcessors);
        systemMetrics.put("uptime", runtimeMXBean.getUptime());
        
        // 检查系统负载告警
        if (systemLoadAverage > 0 && systemLoadAverage > availableProcessors * 0.8) {
            alerts.add(new HealthAlert("SYSTEM_HIGH_LOAD", 
                String.format("系统负载过高: %.2f (处理器数: %d)", systemLoadAverage, availableProcessors),
                "WARNING"));
        }
        
        return systemMetrics;
    }
    
    /**
     * 更新告警状态
     */
    private void updateAlerts(List<HealthAlert> newAlerts) {
        synchronized (activeAlerts) {
            activeAlerts.clear();
            activeAlerts.addAll(newAlerts);
        }
        
        if (!newAlerts.isEmpty()) {
            logger.warn("检测到 {} 个健康告警", newAlerts.size());
            for (HealthAlert alert : newAlerts) {
                logger.warn("告警: [{}] {} - {}", alert.getSeverity(), alert.getType(), alert.getMessage());
            }
        }
    }
    
    /**
     * 更新健康状态
     */
    private void updateHealthStatus(String status, String message) {
        this.systemHealthy = "HEALTHY".equals(status);
        this.healthStatus = status;
        logger.info("系统健康状态更新: {} - {}", status, message);
    }
    
    /**
     * 获取当前系统健康状态
     */
    public Map<String, Object> getCurrentHealthStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("healthy", systemHealthy);
        status.put("status", healthStatus);
        status.put("activeAlertsCount", activeAlerts.size());
        status.put("lastCheckTime", currentHealthMetrics.get("checkTime"));
        return status;
    }
    
    /**
     * 获取当前健康指标
     */
    public Map<String, Object> getCurrentHealthMetrics() {
        return new ConcurrentHashMap<>(currentHealthMetrics);
    }
    
    /**
     * 获取当前活跃告警
     */
    public List<HealthAlert> getActiveAlerts() {
        synchronized (activeAlerts) {
            return new ArrayList<>(activeAlerts);
        }
    }
    
    /**
     * 更新阈值配置
     */
    public void updateThresholds(double memoryThreshold, double diskThreshold, int threadThreshold, long responseTimeThreshold) {
        this.memoryUsageThreshold = memoryThreshold;
        this.diskUsageThreshold = diskThreshold;
        this.maxThreadCountThreshold = threadThreshold;
        this.maxResponseTimeThreshold = responseTimeThreshold;
        
        logger.info("健康监控阈值已更新: 内存={}%, 磁盘={}%, 线程={}, 响应时间={}ms", 
            memoryThreshold, diskThreshold, threadThreshold, responseTimeThreshold);
    }
    
    /**
     * 健康告警类
     */
    public static class HealthAlert {
        private final String type;
        private final String message;
        private final String severity;
        private final LocalDateTime timestamp;
        
        public HealthAlert(String type, String message, String severity) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getType() {
            return type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}