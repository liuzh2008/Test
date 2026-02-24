package com.example.medaiassistant.service;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 性能监控服务
 * 负责收集和报告系统性能指标，包括：
 * 1. 同步性能指标（医院数据同步）
 * 2. 数据库性能指标
 * 3. 系统资源指标（CPU、内存、线程等）
 * 
 * 该服务遵循TDD实施指南中的任务5.3要求
 */
@Service
public class PerformanceMonitor {

    // 同步类型常量
    public static final String SYNC_TYPE_FULL = "FULL";
    public static final String SYNC_TYPE_INCREMENTAL = "INCREMENTAL";
    public static final String SYNC_TYPE_MANUAL = "MANUAL";

    // 数据库操作类型常量
    public static final String DB_OPERATION_QUERY = "QUERY";
    public static final String DB_OPERATION_INSERT = "INSERT";
    public static final String DB_OPERATION_UPDATE = "UPDATE";
    public static final String DB_OPERATION_DELETE = "DELETE";

    // 同步性能指标
    private final AtomicLong totalSyncOperations = new AtomicLong(0);
    private final AtomicLong totalSyncRecords = new AtomicLong(0);
    private final AtomicLong totalSyncTime = new AtomicLong(0);
    private final Map<String, AtomicLong> syncOperationsByHospital = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> syncOperationsByType = new ConcurrentHashMap<>();

    // 数据库性能指标
    private final AtomicLong totalDatabaseOperations = new AtomicLong(0);
    private final AtomicLong successfulDatabaseOperations = new AtomicLong(0);
    private final AtomicLong failedDatabaseOperations = new AtomicLong(0);
    private final AtomicLong totalDatabaseTime = new AtomicLong(0);
    private final Map<String, AtomicLong> databaseOperationsByType = new ConcurrentHashMap<>();

    // 系统资源指标
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    // 性能快照
    private final AtomicReference<Map<String, Object>> lastSnapshot = new AtomicReference<>();

    /**
     * 记录同步性能指标
     * 
     * @param hospitalId 医院ID
     * @param syncType 同步类型（FULL/INCREMENTAL/MANUAL）
     * @param recordsProcessed 处理的记录数
     * @param executionTimeMs 执行时间（毫秒）
     */
    public void recordSyncPerformance(String hospitalId, String syncType, long recordsProcessed, long executionTimeMs) {
        totalSyncOperations.incrementAndGet();
        totalSyncRecords.addAndGet(recordsProcessed);
        totalSyncTime.addAndGet(executionTimeMs);

        // 按医院统计
        incrementCounter(syncOperationsByHospital, hospitalId);

        // 按类型统计
        incrementCounter(syncOperationsByType, syncType);
    }

    /**
     * 记录数据库性能指标
     * 
     * @param operationType 操作类型（QUERY/INSERT/UPDATE/DELETE）
     * @param executionTimeMs 执行时间（毫秒）
     * @param success 是否成功
     */
    public void recordDatabasePerformance(String operationType, long executionTimeMs, boolean success) {
        totalDatabaseOperations.incrementAndGet();
        totalDatabaseTime.addAndGet(executionTimeMs);

        if (success) {
            successfulDatabaseOperations.incrementAndGet();
        } else {
            failedDatabaseOperations.incrementAndGet();
        }

        // 按操作类型统计
        incrementCounter(databaseOperationsByType, operationType);
    }

    /**
     * 获取同步性能指标
     * 
     * @return 同步性能指标
     */
    public Map<String, Object> getSyncPerformanceMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        long totalOps = totalSyncOperations.get();
        long totalTime = totalSyncTime.get();

        metrics.put("totalSyncOperations", totalOps);
        metrics.put("totalSyncRecords", totalSyncRecords.get());
        metrics.put("totalSyncTimeMs", totalTime);
        metrics.put("averageSyncTime", calculateAverage(totalTime, totalOps));
        metrics.put("averageRecordsPerSync", calculateAverage(totalSyncRecords.get(), totalOps));

        // 按医院统计
        metrics.put("syncOperationsByHospital", convertAtomicLongMap(syncOperationsByHospital));

        // 按类型统计
        metrics.put("syncOperationsByType", convertAtomicLongMap(syncOperationsByType));

        return metrics;
    }

    /**
     * 获取数据库性能指标
     * 
     * @return 数据库性能指标
     */
    public Map<String, Object> getDatabasePerformanceMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        long totalOps = totalDatabaseOperations.get();
        long successfulOps = successfulDatabaseOperations.get();
        long totalTime = totalDatabaseTime.get();

        metrics.put("totalDatabaseOperations", totalOps);
        metrics.put("successfulDatabaseOperations", successfulOps);
        metrics.put("failedDatabaseOperations", failedDatabaseOperations.get());
        metrics.put("totalDatabaseTimeMs", totalTime);
        metrics.put("averageDatabaseTime", calculateAverage(totalTime, totalOps));
        metrics.put("successRate", totalOps > 0 ? (double) successfulOps / totalOps * 100 : 0.0);

        // 按操作类型统计
        metrics.put("databaseOperationsByType", convertAtomicLongMap(databaseOperationsByType));

        return metrics;
    }

    /**
     * 获取系统资源指标
     * 
     * @return 系统资源指标
     */
    public Map<String, Object> getSystemResourceMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // 内存使用情况
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();

        metrics.put("memoryUsage", heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0.0);
        metrics.put("heapUsedBytes", heapUsed);
        metrics.put("heapMaxBytes", heapMax);
        metrics.put("nonHeapUsedBytes", nonHeapUsed);

        // CPU使用情况（近似值）
        metrics.put("cpuUsage", osMXBean.getSystemLoadAverage());
        metrics.put("availableProcessors", osMXBean.getAvailableProcessors());

        // 线程情况
        metrics.put("threadCount", threadMXBean.getThreadCount());
        metrics.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

        // 系统运行时间
        metrics.put("systemUptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        return metrics;
    }

    /**
     * 获取所有性能指标
     * 
     * @return 所有性能指标
     */
    public Map<String, Object> getAllPerformanceMetrics() {
        Map<String, Object> allMetrics = new ConcurrentHashMap<>();

        allMetrics.put("syncPerformance", getSyncPerformanceMetrics());
        allMetrics.put("databasePerformance", getDatabasePerformanceMetrics());
        allMetrics.put("systemResources", getSystemResourceMetrics());
        allMetrics.put("timestamp", LocalDateTime.now().toString());
        allMetrics.put("serviceVersion", "1.0.0");

        return allMetrics;
    }

    /**
     * 重置性能指标
     */
    public void resetPerformanceMetrics() {
        // 重置同步指标
        totalSyncOperations.set(0);
        totalSyncRecords.set(0);
        totalSyncTime.set(0);
        syncOperationsByHospital.clear();
        syncOperationsByType.clear();

        // 重置数据库指标
        totalDatabaseOperations.set(0);
        successfulDatabaseOperations.set(0);
        failedDatabaseOperations.set(0);
        totalDatabaseTime.set(0);
        databaseOperationsByType.clear();

        // 清空快照
        lastSnapshot.set(null);
    }

    /**
     * 创建性能指标快照
     * 
     * @return 性能快照
     */
    public Map<String, Object> takePerformanceSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();

        snapshot.put("snapshotId", "snapshot-" + System.currentTimeMillis());
        snapshot.put("timestamp", LocalDateTime.now().toString());
        snapshot.put("metrics", getAllPerformanceMetrics());

        lastSnapshot.set(snapshot);
        return snapshot;
    }

    /**
     * 获取最后一次性能快照
     * 
     * @return 最后一次性能快照，如果没有则返回null
     */
    public Map<String, Object> getLastSnapshot() {
        return lastSnapshot.get();
    }

    /**
     * 辅助方法：递增计数器
     * 
     * @param map 计数器映射
     * @param key 键
     */
    private void incrementCounter(Map<String, AtomicLong> map, String key) {
        map.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 辅助方法：转换AtomicLong映射为普通Long映射
     * 
     * @param atomicMap AtomicLong映射
     * @return 普通Long映射
     */
    private Map<String, Long> convertAtomicLongMap(Map<String, AtomicLong> atomicMap) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        atomicMap.forEach((key, atomicCount) -> result.put(key, atomicCount.get()));
        return result;
    }

    /**
     * 辅助方法：计算平均值
     * 
     * @param total 总和
     * @param count 数量
     * @return 平均值，如果数量为0则返回0
     */
    private double calculateAverage(long total, long count) {
        return count > 0 ? (double) total / count : 0.0;
    }
}
