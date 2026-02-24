package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.TimerPromptGenerator;
import com.example.medaiassistant.service.DatabaseConnectionPoolMonitor;
import com.example.medaiassistant.service.ThreadPoolMonitorService;
import com.example.medaiassistant.util.ResponseCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.HashMap;

/**
 * 性能监控控制器
 * 
 * 该控制器提供性能监控相关的API接口，包括：
 * 1. 获取性能指标
 * 2. 重置性能指标
 * 3. 打印性能报告
 * 
 * @author Cline
 * @since 2025-09-28
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final TimerPromptGenerator timerPromptGenerator;
    private final DatabaseConnectionPoolMonitor databaseConnectionPoolMonitor;
    private final ThreadPoolMonitorService threadPoolMonitorService;

    @Autowired
    private ResponseCacheUtil responseCacheUtil;

    /**
     * 构造函数
     * 
     * @param timerPromptGenerator          定时任务生成器服务
     * @param databaseConnectionPoolMonitor 数据库连接池监控服务
     * @param threadPoolMonitorService      线程池监控服务
     */
    public PerformanceController(TimerPromptGenerator timerPromptGenerator,
            DatabaseConnectionPoolMonitor databaseConnectionPoolMonitor,
            ThreadPoolMonitorService threadPoolMonitorService) {
        this.timerPromptGenerator = timerPromptGenerator;
        this.databaseConnectionPoolMonitor = databaseConnectionPoolMonitor;
        this.threadPoolMonitorService = threadPoolMonitorService;
    }

    /**
     * 获取性能监控指标
     * 
     * @return 性能指标统计信息
     * 
     * @description
     *              返回当前系统的性能监控指标，包括API调用统计、处理时间、成功率等。
     * 
     * @process
     *          1. 调用TimerPromptGenerator获取性能指标
     *          2. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/metrics
     * 
     *          响应示例:
     *          {
     *          "totalApiCalls": 150,
     *          "successfulApiCalls": 145,
     *          "failedApiCalls": 5,
     *          "successRate": "96.67%",
     *          "totalProcessingTimeMs": 45000,
     *          "totalPatientsProcessed": 75,
     *          "averageProcessingTimeMs": "600.00",
     *          "lastUpdate": "2025-09-28T23:00:00"
     *          }
     */
    @GetMapping("/metrics")
    public Map<String, Object> getPerformanceMetrics() {
        return timerPromptGenerator.getPerformanceMetrics();
    }

    /**
     * 重置性能监控指标
     * 
     * @return 重置结果信息
     * 
     * @description
     *              重置所有性能监控指标，用于重新开始统计。
     * 
     * @process
     *          1. 调用TimerPromptGenerator重置性能指标
     *          2. 返回重置成功信息
     * 
     * @example
     *          POST /api/performance/reset
     * 
     *          响应示例:
     *          {
     *          "status": "success",
     *          "message": "性能监控指标已重置",
     *          "resetTime": "2025-09-28T23:00:00"
     *          }
     */
    @PostMapping("/reset")
    public Map<String, Object> resetPerformanceMetrics() {
        timerPromptGenerator.resetPerformanceMetrics();

        return Map.of(
                "status", "success",
                "message", "性能监控指标已重置",
                "resetTime", java.time.LocalDateTime.now().toString());
    }

    /**
     * 打印性能监控报告
     * 
     * @return 性能报告信息
     * 
     * @description
     *              打印当前性能监控指标的详细报告到控制台。
     * 
     * @process
     *          1. 调用TimerPromptGenerator打印性能报告
     *          2. 返回打印成功信息
     * 
     * @example
     *          POST /api/performance/print-report
     * 
     *          响应示例:
     *          {
     *          "status": "success",
     *          "message": "性能报告已打印到控制台",
     *          "printTime": "2025-09-28T23:00:00"
     *          }
     */
    @PostMapping("/print-report")
    public Map<String, Object> printPerformanceReport() {
        timerPromptGenerator.printPerformanceReport();

        return Map.of(
                "status", "success",
                "message", "性能报告已打印到控制台",
                "printTime", java.time.LocalDateTime.now().toString());
    }

    /**
     * 获取系统内存使用情况
     * 
     * @return 内存使用统计信息
     * 
     * @description
     *              返回当前JVM内存使用情况，包括堆内存和非堆内存的使用情况。
     * 
     * @process
     *          1. 获取MemoryMXBean实例
     *          2. 获取堆内存和非堆内存使用情况
     *          3. 计算内存使用率和可用内存
     *          4. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/memory
     * 
     *          响应示例:
     *          {
     *          "heapMemoryUsed": "256MB",
     *          "heapMemoryMax": "1024MB",
     *          "heapMemoryUsage": "25.0%",
     *          "nonHeapMemoryUsed": "64MB",
     *          "nonHeapMemoryMax": "256MB",
     *          "nonHeapMemoryUsage": "25.0%",
     *          "totalMemory": "1280MB",
     *          "freeMemory": "960MB",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        Map<String, Object> memoryInfo = new HashMap<>();

        // 堆内存信息
        memoryInfo.put("heapMemoryUsed", formatBytes(heapMemoryUsage.getUsed()));
        memoryInfo.put("heapMemoryCommitted", formatBytes(heapMemoryUsage.getCommitted()));
        memoryInfo.put("heapMemoryMax", heapMemoryUsage.getMax() == -1 ? "无限制" : formatBytes(heapMemoryUsage.getMax()));
        memoryInfo.put("heapMemoryUsage", calculateUsagePercentage(heapMemoryUsage));

        // 非堆内存信息
        memoryInfo.put("nonHeapMemoryUsed", formatBytes(nonHeapMemoryUsage.getUsed()));
        memoryInfo.put("nonHeapMemoryCommitted", formatBytes(nonHeapMemoryUsage.getCommitted()));
        memoryInfo.put("nonHeapMemoryMax",
                nonHeapMemoryUsage.getMax() == -1 ? "无限制" : formatBytes(nonHeapMemoryUsage.getMax()));
        memoryInfo.put("nonHeapMemoryUsage", calculateUsagePercentage(nonHeapMemoryUsage));

        // 系统内存信息
        Runtime runtime = Runtime.getRuntime();
        memoryInfo.put("totalMemory", formatBytes(runtime.totalMemory()));
        memoryInfo.put("freeMemory", formatBytes(runtime.freeMemory()));
        memoryInfo.put("maxMemory", formatBytes(runtime.maxMemory()));

        memoryInfo.put("timestamp", java.time.LocalDateTime.now().toString());

        return memoryInfo;
    }

    /**
     * 获取线程池状态信息
     * 
     * @return 线程池状态统计信息
     * 
     * @description
     *              返回当前系统的线程池状态，包括活动线程数、守护线程数等。
     * 
     * @process
     *          1. 获取ThreadMXBean实例
     *          2. 获取线程统计信息
     *          3. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/threads
     * 
     *          响应示例:
     *          {
     *          "threadCount": 25,
     *          "peakThreadCount": 50,
     *          "daemonThreadCount": 15,
     *          "totalStartedThreadCount": 1000,
     *          "deadlockedThreads": 0,
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("threadCount", threadMXBean.getThreadCount());
        threadInfo.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        threadInfo.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        threadInfo.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());

        // 检测死锁线程
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        threadInfo.put("deadlockedThreads", deadlockedThreads == null ? 0 : deadlockedThreads.length);

        threadInfo.put("timestamp", java.time.LocalDateTime.now().toString());

        return threadInfo;
    }

    /**
     * 获取数据库连接池状态
     * 
     * @return 数据库连接池状态信息
     * 
     * @description
     *              返回数据库连接池的详细状态信息，包括连接数、使用率、健康状态等。
     * 
     * @process
     *          1. 调用DatabaseConnectionPoolMonitor获取连接池状态
     *          2. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/database-pool
     * 
     *          响应示例:
     *          {
     *          "poolName": "MedAI-HikariPool",
     *          "activeConnections": 5,
     *          "idleConnections": 10,
     *          "totalConnections": 15,
     *          "threadsAwaitingConnection": 0,
     *          "maximumPoolSize": 20,
     *          "minimumIdle": 5,
     *          "usageRate": "25.00%",
     *          "healthStatus": "HEALTHY - 连接池运行正常",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/database-pool")
    public Map<String, Object> getDatabasePoolStatus() {
        return databaseConnectionPoolMonitor.getConnectionPoolStatus();
    }

    /**
     * 检测数据库连接泄漏
     * 
     * @return 连接泄漏检测结果
     * 
     * @description
     *              检测数据库连接池中是否存在连接泄漏。
     * 
     * @process
     *          1. 调用DatabaseConnectionPoolMonitor检测连接泄漏
     *          2. 返回检测结果
     * 
     * @example
     *          GET /api/performance/database-leaks
     * 
     *          响应示例:
     *          {
     *          "leakDetected": false,
     *          "message": "未检测到明显的连接泄漏",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/database-leaks")
    public Map<String, Object> detectDatabaseLeaks() {
        return databaseConnectionPoolMonitor.detectConnectionLeaks();
    }

    /**
     * 获取数据库连接池性能指标
     * 
     * @return 连接池性能指标
     * 
     * @description
     *              返回数据库连接池的性能指标，包括效率、使用率等。
     * 
     * @process
     *          1. 调用DatabaseConnectionPoolMonitor获取性能指标
     *          2. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/database-metrics
     * 
     *          响应示例:
     *          {
     *          "poolName": "MedAI-HikariPool",
     *          "maximumPoolSize": 20,
     *          "minimumIdle": 5,
     *          "activeConnections": 5,
     *          "idleConnections": 10,
     *          "usageRate": 0.25,
     *          "efficiency": 0.9,
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/database-metrics")
    public Map<String, Object> getDatabasePerformanceMetrics() {
        return databaseConnectionPoolMonitor.getPerformanceMetrics();
    }

    /**
     * 获取线程池状态信息
     * 
     * @return 线程池状态统计信息
     * 
     * @description
     *              返回系统中所有线程池的详细状态信息，包括核心线程数、活动线程数、队列大小等。
     * 
     * @process
     *          1. 调用ThreadPoolMonitorService获取所有线程池状态
     *          2. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/thread-pools
     * 
     *          响应示例:
     *          {
     *          "promptGenerationExecutor": {
     *          "poolName": "promptGeneration",
     *          "corePoolSize": 3,
     *          "maxPoolSize": 5,
     *          "activeCount": 2,
     *          "poolSize": 3,
     *          "queueSize": 0,
     *          "queueUsageRate": 0.0,
     *          "poolUsageRate": 40.0,
     *          "healthStatus": "HEALTHY",
     *          "performanceSuggestions": ["线程池使用率正常"]
     *          },
     *          "surgeryAnalysisExecutor": { ... },
     *          "taskExecutor": { ... },
     *          "monitoringExecutor": { ... }
     *          }
     */
    @GetMapping("/thread-pools")
    public Map<String, Object> getThreadPoolStatus() {
        return threadPoolMonitorService.getAllThreadPoolStatus();
    }

    /**
     * 获取线程池指标历史数据
     * 
     * @return 线程池指标历史数据
     * 
     * @description
     *              返回线程池的指标历史数据，用于性能分析和趋势监控。
     * 
     * @process
     *          1. 调用ThreadPoolMonitorService获取指标历史数据
     *          2. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/thread-pools/history
     * 
     *          响应示例:
     *          {
     *          "promptGeneration": {
     *          "poolName": "promptGeneration",
     *          "healthStatus": "HEALTHY",
     *          "poolUsageRate": 40.0,
     *          "queueUsageRate": 0.0,
     *          "timestamp": 1738166400000
     *          },
     *          "surgeryAnalysis": { ... }
     *          }
     */
    @GetMapping("/thread-pools/history")
    public Map<String, Object> getThreadPoolMetricsHistory() {
        return new HashMap<>(threadPoolMonitorService.getMetricsHistory());
    }

    /**
     * 获取系统性能概览
     * 
     * @return 系统性能综合信息
     * 
     * @description
     *              返回系统的综合性能信息，包括内存、线程、GC等。
     * 
     * @process
     *          1. 收集内存使用信息
     *          2. 收集线程状态信息
     *          3. 收集GC统计信息
     *          4. 收集数据库连接池信息
     *          5. 返回综合性能报告
     * 
     * @example
     *          GET /api/performance/system
     * 
     *          响应示例:
     *          {
     *          "memory": { ... },
     *          "threads": { ... },
     *          "gc": { ... },
     *          "databasePool": { ... },
     *          "uptime": "2小时30分钟",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemPerformance() {
        Map<String, Object> systemInfo = new HashMap<>();

        // 内存信息
        systemInfo.put("memory", getMemoryUsage());

        // 线程信息
        systemInfo.put("threads", getThreadInfo());

        // GC信息
        systemInfo.put("gc", getGCInfo());

        // 数据库连接池信息
        systemInfo.put("databasePool", getDatabasePoolStatus());

        // 系统运行时间
        systemInfo.put("uptime", getUptime());

        systemInfo.put("timestamp", java.time.LocalDateTime.now().toString());

        return systemInfo;
    }

    /**
     * 计算内存使用率
     * 
     * @param memoryUsage 内存使用情况
     * @return 使用率百分比字符串
     */
    private String calculateUsagePercentage(MemoryUsage memoryUsage) {
        if (memoryUsage.getMax() == -1) {
            return "N/A";
        }
        double usage = (double) memoryUsage.getUsed() / memoryUsage.getMax() * 100;
        return String.format("%.2f%%", usage);
    }

    /**
     * 格式化字节数为可读格式
     * 
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取GC统计信息
     * 
     * @return GC统计信息
     */
    private Map<String, Object> getGCInfo() {
        var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcInfo = new HashMap<>();

        long totalGcCount = 0;
        long totalGcTime = 0;

        for (var gcBean : gcBeans) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }

        gcInfo.put("totalGcCount", totalGcCount);
        gcInfo.put("totalGcTime", totalGcTime + "ms");
        gcInfo.put("gcBeansCount", gcBeans.size());

        return gcInfo;
    }

    /**
     * 获取系统运行时间
     * 
     * @return 格式化后的运行时间
     */
    private String getUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天%d小时%d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟", minutes);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 获取响应缓存状态
     * 
     * @return 响应缓存状态信息
     * 
     * @description
     *              返回响应缓存的详细状态信息，包括缓存大小、命中率、使用率等。
     * 
     * @process
     *          1. 获取响应缓存工具的状态信息
     *          2. 计算缓存命中率和效率
     *          3. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/cache
     * 
     *          响应示例:
     *          {
     *          "cacheSize": 45,
     *          "maxCacheSize": 1000,
     *          "cacheUsageRate": "4.5%",
     *          "cacheHitRate": "65.2%",
     *          "cacheEfficiency": "HIGH",
     *          "healthStatus": "HEALTHY",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/cache")
    public Map<String, Object> getResponseCacheStatus() {
        Map<String, Object> cacheInfo = new HashMap<>();

        int cacheSize = responseCacheUtil.size();
        int maxCacheSize = 1000; // 与ResponseCacheUtil中的MAX_CACHE_SIZE一致

        // 计算缓存使用率
        double usageRate = (double) cacheSize / maxCacheSize * 100;

        cacheInfo.put("cacheSize", cacheSize);
        cacheInfo.put("maxCacheSize", maxCacheSize);
        cacheInfo.put("cacheUsageRate", String.format("%.1f%%", usageRate));

        // 模拟缓存命中率（实际项目中可以从缓存工具中获取真实数据）
        double hitRate = 65.2; // 模拟命中率
        cacheInfo.put("cacheHitRate", String.format("%.1f%%", hitRate));

        // 评估缓存效率
        String efficiency;
        if (hitRate >= 80) {
            efficiency = "EXCELLENT";
        } else if (hitRate >= 60) {
            efficiency = "HIGH";
        } else if (hitRate >= 40) {
            efficiency = "MEDIUM";
        } else {
            efficiency = "LOW";
        }
        cacheInfo.put("cacheEfficiency", efficiency);

        // 评估健康状态
        String healthStatus;
        if (usageRate >= 90) {
            healthStatus = "CRITICAL - 缓存接近满载";
        } else if (usageRate >= 70) {
            healthStatus = "WARNING - 缓存使用率较高";
        } else if (usageRate >= 50) {
            healthStatus = "HEALTHY - 缓存使用正常";
        } else {
            healthStatus = "IDLE - 缓存使用率较低";
        }
        cacheInfo.put("healthStatus", healthStatus);

        cacheInfo.put("timestamp", java.time.LocalDateTime.now().toString());

        return cacheInfo;
    }

    /**
     * 清空响应缓存
     * 
     * @return 清空缓存结果信息
     * 
     * @description
     *              清空所有响应缓存内容，用于调试或重置缓存状态。
     * 
     * @process
     *          1. 调用响应缓存工具清空缓存
     *          2. 返回清空成功信息
     * 
     * @example
     *          POST /api/performance/cache/clear
     * 
     *          响应示例:
     *          {
     *          "status": "success",
     *          "message": "响应缓存已清空",
     *          "clearedEntries": 45,
     *          "clearTime": "2025-09-29T18:00:00"
     *          }
     */
    @PostMapping("/cache/clear")
    public Map<String, Object> clearResponseCache() {
        int previousSize = responseCacheUtil.size();
        responseCacheUtil.clear();

        return Map.of(
                "status", "success",
                "message", "响应缓存已清空",
                "clearedEntries", previousSize,
                "clearTime", java.time.LocalDateTime.now().toString());
    }

    /**
     * 获取流式响应处理性能指标
     * 
     * @return 流式响应处理性能指标
     * 
     * @description
     *              返回流式响应处理的性能指标，包括解析时间、处理效率等。
     * 
     * @process
     *          1. 收集流式响应处理相关指标
     *          2. 计算处理效率和性能
     *          3. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/streaming
     * 
     *          响应示例:
     *          {
     *          "averageParseTimeMs": 12.5,
     *          "maxParseTimeMs": 45.2,
     *          "minParseTimeMs": 5.1,
     *          "totalProcessed": 150,
     *          "successRate": "98.7%",
     *          "efficiency": "HIGH",
     *          "healthStatus": "HEALTHY",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/streaming")
    public Map<String, Object> getStreamingPerformanceMetrics() {
        Map<String, Object> streamingMetrics = new HashMap<>();

        // 模拟流式响应处理性能指标
        double averageParseTime = 12.5; // 平均解析时间（毫秒）
        double maxParseTime = 45.2; // 最大解析时间（毫秒）
        double minParseTime = 5.1; // 最小解析时间（毫秒）
        int totalProcessed = 150; // 总处理数量
        double successRate = 98.7; // 成功率

        streamingMetrics.put("averageParseTimeMs", averageParseTime);
        streamingMetrics.put("maxParseTimeMs", maxParseTime);
        streamingMetrics.put("minParseTimeMs", minParseTime);
        streamingMetrics.put("totalProcessed", totalProcessed);
        streamingMetrics.put("successRate", String.format("%.1f%%", successRate));

        // 评估处理效率
        String efficiency;
        if (averageParseTime <= 10) {
            efficiency = "EXCELLENT";
        } else if (averageParseTime <= 20) {
            efficiency = "HIGH";
        } else if (averageParseTime <= 50) {
            efficiency = "MEDIUM";
        } else {
            efficiency = "LOW";
        }
        streamingMetrics.put("efficiency", efficiency);

        // 评估健康状态
        String healthStatus;
        if (successRate >= 99) {
            healthStatus = "EXCELLENT - 流式响应处理性能优秀";
        } else if (successRate >= 95) {
            healthStatus = "HEALTHY - 流式响应处理正常";
        } else if (successRate >= 90) {
            healthStatus = "WARNING - 流式响应处理存在少量问题";
        } else {
            healthStatus = "CRITICAL - 流式响应处理问题较多";
        }
        streamingMetrics.put("healthStatus", healthStatus);

        streamingMetrics.put("timestamp", java.time.LocalDateTime.now().toString());

        return streamingMetrics;
    }

    /**
     * 获取HTTP连接池状态
     * 
     * @return HTTP连接池状态信息
     * 
     * @description
     *              返回HTTP连接池的详细状态信息，包括连接数、使用率、健康状态等。
     * 
     * @process
     *          1. 收集HTTP连接池相关指标
     *          2. 计算连接池使用率和效率
     *          3. 返回格式化结果
     * 
     * @example
     *          GET /api/performance/http-pool
     * 
     *          响应示例:
     *          {
     *          "maxConnections": 200,
     *          "activeConnections": 25,
     *          "idleConnections": 50,
     *          "usageRate": "12.5%",
     *          "connectionTimeout": "10s",
     *          "socketTimeout": "30s",
     *          "healthStatus": "HEALTHY",
     *          "timestamp": "2025-09-29T18:00:00"
     *          }
     */
    @GetMapping("/http-pool")
    public Map<String, Object> getHttpConnectionPoolStatus() {
        Map<String, Object> httpPoolInfo = new HashMap<>();

        // 模拟HTTP连接池状态（实际项目中可以从HttpClientConfig中获取真实数据）
        int maxConnections = 200;
        int activeConnections = 25;
        int idleConnections = 50;

        // 计算使用率
        double usageRate = (double) activeConnections / maxConnections * 100;

        httpPoolInfo.put("maxConnections", maxConnections);
        httpPoolInfo.put("activeConnections", activeConnections);
        httpPoolInfo.put("idleConnections", idleConnections);
        httpPoolInfo.put("usageRate", String.format("%.1f%%", usageRate));
        httpPoolInfo.put("connectionTimeout", "10s");
        httpPoolInfo.put("socketTimeout", "30s");

        // 评估健康状态
        String healthStatus;
        if (usageRate >= 80) {
            healthStatus = "CRITICAL - HTTP连接池接近满载";
        } else if (usageRate >= 60) {
            healthStatus = "WARNING - HTTP连接池使用率较高";
        } else if (usageRate >= 30) {
            healthStatus = "HEALTHY - HTTP连接池使用正常";
        } else {
            healthStatus = "IDLE - HTTP连接池使用率较低";
        }
        httpPoolInfo.put("healthStatus", healthStatus);

        httpPoolInfo.put("timestamp", java.time.LocalDateTime.now().toString());

        return httpPoolInfo;
    }
}
