package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 指标收集服务
 * 负责收集系统性能指标、业务指标，支持监控和告警
 * 
 * 主要功能：
 * 1. 系统级指标收集（CPU、内存、线程等）
 * 2. 业务指标收集（Prompt执行、状态转换等）
 * 3. 自定义指标注册和管理
 * 4. 指标数据聚合和统计
 */
@Service
public class MetricsCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionService.class);
    
    // JVM 管理接口
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    // 业务指标计数器
    private final AtomicLong promptExecutionCounter = new AtomicLong(0);
    private final AtomicLong promptSuccessCounter = new AtomicLong(0);
    private final AtomicLong promptFailureCounter = new AtomicLong(0);
    private final AtomicLong statusTransitionCounter = new AtomicLong(0);
    private final AtomicLong asyncSubmissionCounter = new AtomicLong(0);
    private final AtomicLong networkRetryCounter = new AtomicLong(0);
    private final AtomicLong databaseRetryCounter = new AtomicLong(0);
    
    // 业务指标计时器(使用AtomicLong进行平均值计算)
    private final AtomicLong promptExecutionTimeSum = new AtomicLong(0);
    private final AtomicLong promptExecutionTimeCount = new AtomicLong(0);
    private final AtomicLong statusTransitionTimeSum = new AtomicLong(0);
    private final AtomicLong statusTransitionTimeCount = new AtomicLong(0);
    private final AtomicLong networkRequestTimeSum = new AtomicLong(0);
    private final AtomicLong networkRequestTimeCount = new AtomicLong(0);
    private final AtomicLong databaseOperationTimeSum = new AtomicLong(0);
    private final AtomicLong databaseOperationTimeCount = new AtomicLong(0);
    
    // 错误类型统计
    private final Map<String, AtomicLong> errorTypeCounters = new ConcurrentHashMap<>();
    
    // 状态转换统计
    private final Map<String, AtomicLong> statusTransitionCounters = new ConcurrentHashMap<>();
    
    // 网络重试统计
    private final Map<Integer, AtomicLong> networkRetryCounters = new ConcurrentHashMap<>();
    
    // 数据库重试统计
    private final Map<Integer, AtomicLong> databaseRetryCounters = new ConcurrentHashMap<>();
    
    // 性能历史记录（保存最近100条记录）
    private final List<Map<String, Object>> recentSnapshots = new ArrayList<>();
    private static final int MAX_SNAPSHOTS = 100;
    
    @PostConstruct
    public void initializeMetrics() {
        logger.info("初始化指标收集服务...");
        
        // 初始化基础指标
        initializeBasicMetrics();
        
        logger.info("指标收集服务初始化完成");
    }
    
    /**
     * 初始化基础指标
     */
    private void initializeBasicMetrics() {
        // 初始化错误类型计数器
        errorTypeCounters.put("NETWORK_ERROR", new AtomicLong(0));
        errorTypeCounters.put("DATABASE_ERROR", new AtomicLong(0));
        errorTypeCounters.put("TIMEOUT_ERROR", new AtomicLong(0));
        errorTypeCounters.put("VALIDATION_ERROR", new AtomicLong(0));
        errorTypeCounters.put("UNKNOWN_ERROR", new AtomicLong(0));
        
        logger.debug("基础指标初始化完成");
    }
    
    /**
     * 记录Prompt执行开始
     */
    public void recordPromptExecutionStart() {
        promptExecutionCounter.incrementAndGet();
        logger.debug("记录Prompt执行开始，总执行次数: {}", promptExecutionCounter.get());
    }
    
    /**
     * 记录Prompt执行成功
     * @param executionTimeMillis 执行耗时（毫秒）
     */
    public void recordPromptExecutionSuccess(long executionTimeMillis) {
        promptSuccessCounter.incrementAndGet();
        promptExecutionTimeSum.addAndGet(executionTimeMillis);
        promptExecutionTimeCount.incrementAndGet();
        
        logger.debug("记录Prompt执行成功，耗时: {}ms，总成功次数: {}", 
            executionTimeMillis, promptSuccessCounter.get());
    }
    
    /**
     * 记录Prompt执行失败
     * @param executionTimeMillis 执行耗时（毫秒）
     * @param errorType 错误类型
     */
    public void recordPromptExecutionFailure(long executionTimeMillis, String errorType) {
        promptFailureCounter.incrementAndGet();
        promptExecutionTimeSum.addAndGet(executionTimeMillis);
        promptExecutionTimeCount.incrementAndGet();
        
        // 记录错误类型统计
        errorTypeCounters.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("记录Prompt执行失败，耗时: {}ms，错误类型: {}，总失败次数: {}", 
            executionTimeMillis, errorType, promptFailureCounter.get());
    }
    
    /**
     * 记录状态转换
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @param transitionTimeMillis 转换耗时（毫秒）
     */
    public void recordStatusTransition(String fromStatus, String toStatus, long transitionTimeMillis) {
        statusTransitionCounter.incrementAndGet();
        statusTransitionTimeSum.addAndGet(transitionTimeMillis);
        statusTransitionTimeCount.incrementAndGet();
        
        // 记录具体的状态转换统计
        String transitionKey = fromStatus + "->" + toStatus;
        statusTransitionCounters.computeIfAbsent(transitionKey, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("记录状态转换: {} -> {}，耗时: {}ms，总转换次数: {}", 
            fromStatus, toStatus, transitionTimeMillis, statusTransitionCounter.get());
    }
    
    /**
     * 记录异步提交
     */
    public void recordAsyncSubmission() {
        asyncSubmissionCounter.incrementAndGet();
        logger.debug("记录异步提交，总提交次数: {}", asyncSubmissionCounter.get());
    }
    
    /**
     * 记录网络重试
     * @param retryCount 重试次数
     * @param requestTimeMillis 请求耗时（毫秒）
     */
    public void recordNetworkRetry(int retryCount, long requestTimeMillis) {
        networkRetryCounter.incrementAndGet();
        networkRequestTimeSum.addAndGet(requestTimeMillis);
        networkRequestTimeCount.incrementAndGet();
        
        // 记录重试次数统计
        networkRetryCounters.computeIfAbsent(retryCount, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("记录网络重试，重试次数: {}，耗时: {}ms，总重试次数: {}", 
            retryCount, requestTimeMillis, networkRetryCounter.get());
    }
    
    /**
     * 记录数据库重试
     * @param retryCount 重试次数
     * @param operationTimeMillis 操作耗时（毫秒）
     */
    public void recordDatabaseRetry(int retryCount, long operationTimeMillis) {
        databaseRetryCounter.incrementAndGet();
        databaseOperationTimeSum.addAndGet(operationTimeMillis);
        databaseOperationTimeCount.incrementAndGet();
        
        // 记录重试次数统计
        databaseRetryCounters.computeIfAbsent(retryCount, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("记录数据库重试，重试次数: {}，耗时: {}ms，总重试次数: {}", 
            retryCount, operationTimeMillis, databaseRetryCounter.get());
    }
    
    /**
     * 获取系统指标
     * @return 系统指标信息
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new ConcurrentHashMap<>();
        
        // JVM内存指标
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        systemMetrics.put("jvm.memory.heap.used", heapUsed);
        systemMetrics.put("jvm.memory.heap.max", heapMax);
        systemMetrics.put("jvm.memory.heap.usage", heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0.0);
        systemMetrics.put("jvm.memory.nonheap.used", nonHeapUsed);
        
        // JVM线程指标
        systemMetrics.put("jvm.threads.count", threadMXBean.getThreadCount());
        systemMetrics.put("jvm.threads.peak", threadMXBean.getPeakThreadCount());
        systemMetrics.put("jvm.threads.daemon", threadMXBean.getDaemonThreadCount());
        
        // 系统运行时间
        systemMetrics.put("system.uptime.seconds", runtimeMXBean.getUptime() / 1000.0);
        systemMetrics.put("system.start.time", runtimeMXBean.getStartTime());
        
        return systemMetrics;
    }
    
    /**
     * 获取业务指标
     * @return 业务指标信息
     */
    public Map<String, Object> getBusinessMetrics() {
        Map<String, Object> businessMetrics = new ConcurrentHashMap<>();
        
        // 基础计数指标
        businessMetrics.put("prompt.execution.total", promptExecutionCounter.get());
        businessMetrics.put("prompt.execution.success", promptSuccessCounter.get());
        businessMetrics.put("prompt.execution.failure", promptFailureCounter.get());
        businessMetrics.put("status.transition.total", statusTransitionCounter.get());
        businessMetrics.put("async.submission.total", asyncSubmissionCounter.get());
        businessMetrics.put("network.retry.total", networkRetryCounter.get());
        businessMetrics.put("database.retry.total", databaseRetryCounter.get());
        
        // 成功率统计
        long totalExecutions = promptExecutionCounter.get();
        if (totalExecutions > 0) {
            double successRate = (double) promptSuccessCounter.get() / totalExecutions * 100;
            double failureRate = (double) promptFailureCounter.get() / totalExecutions * 100;
            businessMetrics.put("prompt.success.rate", Math.round(successRate * 100.0) / 100.0);
            businessMetrics.put("prompt.failure.rate", Math.round(failureRate * 100.0) / 100.0);
        } else {
            businessMetrics.put("prompt.success.rate", 0.0);
            businessMetrics.put("prompt.failure.rate", 0.0);
        }
        
        // 平均耗时统计
        if (promptExecutionTimeCount.get() > 0) {
            businessMetrics.put("prompt.execution.avg.duration", 
                (double) promptExecutionTimeSum.get() / promptExecutionTimeCount.get());
        } else {
            businessMetrics.put("prompt.execution.avg.duration", 0.0);
        }
        
        if (statusTransitionTimeCount.get() > 0) {
            businessMetrics.put("status.transition.avg.duration", 
                (double) statusTransitionTimeSum.get() / statusTransitionTimeCount.get());
        } else {
            businessMetrics.put("status.transition.avg.duration", 0.0);
        }
        
        if (networkRequestTimeCount.get() > 0) {
            businessMetrics.put("network.request.avg.duration", 
                (double) networkRequestTimeSum.get() / networkRequestTimeCount.get());
        } else {
            businessMetrics.put("network.request.avg.duration", 0.0);
        }
        
        if (databaseOperationTimeCount.get() > 0) {
            businessMetrics.put("database.operation.avg.duration", 
                (double) databaseOperationTimeSum.get() / databaseOperationTimeCount.get());
        } else {
            businessMetrics.put("database.operation.avg.duration", 0.0);
        }
        
        return businessMetrics;
    }
    
    /**
     * 获取详细统计信息
     * @return 详细统计信息
     */
    public Map<String, Object> getDetailedStatistics() {
        Map<String, Object> detailedStats = new ConcurrentHashMap<>();
        
        // 错误类型统计
        Map<String, Long> errorStats = new ConcurrentHashMap<>();
        errorTypeCounters.forEach((errorType, counter) -> 
            errorStats.put(errorType, counter.get()));
        detailedStats.put("error.types", errorStats);
        
        // 状态转换统计
        Map<String, Long> transitionStats = new ConcurrentHashMap<>();
        statusTransitionCounters.forEach((transition, counter) -> 
            transitionStats.put(transition, counter.get()));
        detailedStats.put("status.transitions", transitionStats);
        
        // 网络重试统计
        Map<String, Long> networkRetryStats = new ConcurrentHashMap<>();
        networkRetryCounters.forEach((retryCount, counter) -> 
            networkRetryStats.put("retry_" + retryCount, counter.get()));
        detailedStats.put("network.retries", networkRetryStats);
        
        // 数据库重试统计
        Map<String, Long> databaseRetryStats = new ConcurrentHashMap<>();
        databaseRetryCounters.forEach((retryCount, counter) -> 
            databaseRetryStats.put("retry_" + retryCount, counter.get()));
        detailedStats.put("database.retries", databaseRetryStats);
        
        return detailedStats;
    }
    
    /**
     * 获取完整的指标统计信息
     * @return 完整指标统计
     */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> allMetrics = new ConcurrentHashMap<>();
        
        // 系统指标
        allMetrics.put("system", getSystemMetrics());
        
        // 业务指标
        allMetrics.put("business", getBusinessMetrics());
        
        // 详细统计
        allMetrics.put("detailed", getDetailedStatistics());
        
        // 元数据
        allMetrics.put("timestamp", LocalDateTime.now().toString());
        allMetrics.put("collection.service.version", "1.0.0");
        
        return allMetrics;
    }
    
    /**
     * 创建指标快照
     */
    public void takeSnapshot() {
        Map<String, Object> snapshot = getAllMetrics();
        
        synchronized (recentSnapshots) {
            recentSnapshots.add(snapshot);
            
            // 保持最多MAX_SNAPSHOTS个快照
            while (recentSnapshots.size() > MAX_SNAPSHOTS) {
                recentSnapshots.remove(0);
            }
        }
        
        logger.debug("创建指标快照，当前快照数量: {}", recentSnapshots.size());
    }
    
    /**
     * 获取最近的指标快照
     * @param count 快照数量
     * @return 最近的快照列表
     */
    public List<Map<String, Object>> getRecentSnapshots(int count) {
        synchronized (recentSnapshots) {
            int size = recentSnapshots.size();
            int startIndex = Math.max(0, size - count);
            return new ArrayList<>(recentSnapshots.subList(startIndex, size));
        }
    }
    
    /**
     * 重置所有指标
     */
    public void resetMetrics() {
        promptExecutionCounter.set(0);
        promptSuccessCounter.set(0);
        promptFailureCounter.set(0);
        statusTransitionCounter.set(0);
        asyncSubmissionCounter.set(0);
        networkRetryCounter.set(0);
        databaseRetryCounter.set(0);
        
        promptExecutionTimeSum.set(0);
        promptExecutionTimeCount.set(0);
        statusTransitionTimeSum.set(0);
        statusTransitionTimeCount.set(0);
        networkRequestTimeSum.set(0);
        networkRequestTimeCount.set(0);
        databaseOperationTimeSum.set(0);
        databaseOperationTimeCount.set(0);
        
        errorTypeCounters.clear();
        statusTransitionCounters.clear();
        networkRetryCounters.clear();
        databaseRetryCounters.clear();
        
        synchronized (recentSnapshots) {
            recentSnapshots.clear();
        }
        
        // 重新初始化基础指标
        initializeBasicMetrics();
        
        logger.info("所有指标已重置");
    }
}