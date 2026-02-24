package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 业务指标监控服务
 * 负责监控Prompt执行统计、状态转换指标、异步处理性能等业务相关指标
 * 
 * 主要功能：
 * 1. Prompt执行性能监控
 * 2. 状态转换指标统计
 * 3. 异步处理性能监控  
 * 4. 业务流程KPI监控
 * 5. 实时性能指标分析
 */
@Service
public class BusinessMetricsMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsMonitoringService.class);
    
    @Autowired
    private MetricsCollectionService metricsCollectionService;
    
    // Prompt执行指标
    private final AtomicLong totalPromptRequests = new AtomicLong(0);
    private final AtomicLong successfulPrompts = new AtomicLong(0);
    private final AtomicLong failedPrompts = new AtomicLong(0);
    private final AtomicLong timeoutPrompts = new AtomicLong(0);
    
    // 状态转换指标
    private final Map<String, AtomicLong> statusTransitionCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> statusDurationSums = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> statusDurationCounts = new ConcurrentHashMap<>();
    
    // 异步处理指标
    private final AtomicLong asyncSubmissions = new AtomicLong(0);
    private final AtomicLong asyncCompletions = new AtomicLong(0);
    private final AtomicLong asyncTimeouts = new AtomicLong(0);
    private final AtomicLong asyncFailures = new AtomicLong(0);
    
    // 性能指标队列（保存最近的性能数据）
    private final Queue<PerformanceSnapshot> recentPerformanceData = new ConcurrentLinkedQueue<>();
    private static final int MAX_PERFORMANCE_SNAPSHOTS = 288; // 24小时，每5分钟一个快照
    
    // KPI指标
    private volatile double currentThroughput = 0.0; // 当前吞吐量（次/分钟）
    private volatile double averageResponseTime = 0.0; // 平均响应时间（毫秒）
    private volatile double successRate = 0.0; // 成功率（%）
    private volatile double availabilityRate = 0.0; // 可用性（%）
    
    // 业务阈值
    private double minThroughputThreshold = 10.0; // 最小吞吐量阈值（次/分钟）
    private double maxResponseTimeThreshold = 30000.0; // 最大响应时间阈值（毫秒）
    private double minSuccessRateThreshold = 95.0; // 最小成功率阈值（%）
    private double minAvailabilityThreshold = 99.0; // 最小可用性阈值（%）
    
    @PostConstruct
    public void initializeBusinessMetrics() {
        logger.info("初始化业务指标监控服务...");
        
        // 初始化状态转换计数器
        initializeStatusTransitionCounters();
        
        // 执行初始指标计算
        calculateCurrentMetrics();
        
        logger.info("业务指标监控服务初始化完成");
    }
    
    /**
     * 初始化状态转换计数器
     */
    private void initializeStatusTransitionCounters() {
        // 根据Prompt状态流转规范初始化计数器
        String[] statusTransitions = {
            "待处理->SUBMISSION_STARTED",
            "SUBMISSION_STARTED->已完成", 
            "SUBMISSION_STARTED->执行失败",
            "执行失败->待处理", // 重试场景
            "已完成->待处理"   // 重新执行场景
        };
        
        for (String transition : statusTransitions) {
            statusTransitionCounts.put(transition, new AtomicLong(0));
            statusDurationSums.put(transition, new AtomicLong(0));
            statusDurationCounts.put(transition, new AtomicLong(0));
        }
        
        logger.debug("状态转换计数器初始化完成，共{}个转换类型", statusTransitions.length);
    }
    
    /**
     * 定期计算和更新指标（每5分钟）
     */
    @Scheduled(fixedDelay = 300000)
    public void performScheduledMetricsCalculation() {
        try {
            calculateCurrentMetrics();
            takePerformanceSnapshot();
        } catch (Exception e) {
            logger.error("定期指标计算执行失败", e);
        }
    }
    
    /**
     * 记录Prompt请求
     */
    public void recordPromptRequest() {
        totalPromptRequests.incrementAndGet();
        metricsCollectionService.recordPromptExecutionStart();
        logger.debug("记录Prompt请求，总请求数: {}", totalPromptRequests.get());
    }
    
    /**
     * 记录Prompt执行成功
     * @param executionTimeMillis 执行耗时
     */
    public void recordPromptSuccess(long executionTimeMillis) {
        successfulPrompts.incrementAndGet();
        metricsCollectionService.recordPromptExecutionSuccess(executionTimeMillis);
        
        // 更新实时指标
        updateRealTimeMetrics();
        
        logger.debug("记录Prompt成功，耗时: {}ms，总成功数: {}", 
            executionTimeMillis, successfulPrompts.get());
    }
    
    /**
     * 记录Prompt执行失败
     * @param executionTimeMillis 执行耗时
     * @param errorType 错误类型
     */
    public void recordPromptFailure(long executionTimeMillis, String errorType) {
        failedPrompts.incrementAndGet();
        metricsCollectionService.recordPromptExecutionFailure(executionTimeMillis, errorType);
        
        // 更新实时指标
        updateRealTimeMetrics();
        
        logger.debug("记录Prompt失败，耗时: {}ms，错误类型: {}，总失败数: {}", 
            executionTimeMillis, errorType, failedPrompts.get());
    }
    
    /**
     * 记录Prompt超时
     * @param timeoutMillis 超时时间
     */
    public void recordPromptTimeout(long timeoutMillis) {
        timeoutPrompts.incrementAndGet();
        failedPrompts.incrementAndGet(); // 超时也算失败
        metricsCollectionService.recordPromptExecutionFailure(timeoutMillis, "TIMEOUT");
        
        // 更新实时指标
        updateRealTimeMetrics();
        
        logger.debug("记录Prompt超时，超时时间: {}ms，总超时数: {}", 
            timeoutMillis, timeoutPrompts.get());
    }
    
    /**
     * 记录状态转换
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @param durationMillis 转换耗时
     */
    public void recordStatusTransition(String fromStatus, String toStatus, long durationMillis) {
        String transitionKey = fromStatus + "->" + toStatus;
        
        // 更新转换次数
        statusTransitionCounts.computeIfAbsent(transitionKey, k -> new AtomicLong(0)).incrementAndGet();
        
        // 更新转换耗时统计
        statusDurationSums.computeIfAbsent(transitionKey, k -> new AtomicLong(0)).addAndGet(durationMillis);
        statusDurationCounts.computeIfAbsent(transitionKey, k -> new AtomicLong(0)).incrementAndGet();
        
        // 记录到指标收集服务
        metricsCollectionService.recordStatusTransition(fromStatus, toStatus, durationMillis);
        
        logger.debug("记录状态转换: {}，耗时: {}ms", transitionKey, durationMillis);
    }
    
    /**
     * 记录异步提交
     */
    public void recordAsyncSubmission() {
        asyncSubmissions.incrementAndGet();
        metricsCollectionService.recordAsyncSubmission();
        logger.debug("记录异步提交，总提交数: {}", asyncSubmissions.get());
    }
    
    /**
     * 记录异步完成
     * @param processingTimeMillis 处理耗时
     */
    public void recordAsyncCompletion(long processingTimeMillis) {
        asyncCompletions.incrementAndGet();
        logger.debug("记录异步完成，处理耗时: {}ms，总完成数: {}", 
            processingTimeMillis, asyncCompletions.get());
    }
    
    /**
     * 记录异步超时
     */
    public void recordAsyncTimeout() {
        asyncTimeouts.incrementAndGet();
        logger.debug("记录异步超时，总超时数: {}", asyncTimeouts.get());
    }
    
    /**
     * 记录异步失败
     * @param errorMessage 错误信息
     */
    public void recordAsyncFailure(String errorMessage) {
        asyncFailures.incrementAndGet();
        logger.debug("记录异步失败，错误: {}，总失败数: {}", errorMessage, asyncFailures.get());
    }
    
    /**
     * 计算当前业务指标
     */
    private void calculateCurrentMetrics() {
        long totalRequests = totalPromptRequests.get();
        long successful = successfulPrompts.get();
        
        // 计算成功率
        if (totalRequests > 0) {
            successRate = (double) successful / totalRequests * 100;
        } else {
            successRate = 0.0;
        }
        
        // 计算可用性（基于非超时的请求）
        long nonTimeoutRequests = totalRequests - timeoutPrompts.get();
        if (totalRequests > 0) {
            availabilityRate = (double) nonTimeoutRequests / totalRequests * 100;
        } else {
            availabilityRate = 100.0;
        }
        
        // 计算平均响应时间（从指标收集服务获取）
        Map<String, Object> businessMetrics = metricsCollectionService.getBusinessMetrics();
        Object avgDuration = businessMetrics.get("prompt.execution.avg.duration");
        if (avgDuration instanceof Number) {
            averageResponseTime = ((Number) avgDuration).doubleValue();
        }
        
        logger.debug("当前业务指标 - 成功率: {:.2f}%, 可用性: {:.2f}%, 平均响应时间: {:.2f}ms", 
            successRate, availabilityRate, averageResponseTime);
    }
    
    /**
     * 更新实时指标
     */
    private void updateRealTimeMetrics() {
        // 计算当前吞吐量（基于最近的性能快照）
        calculateCurrentThroughput();
        
        // 更新其他实时指标
        calculateCurrentMetrics();
    }
    
    /**
     * 计算当前吞吐量
     */
    private void calculateCurrentThroughput() {
        if (recentPerformanceData.size() >= 2) {
            PerformanceSnapshot[] snapshots = recentPerformanceData.toArray(new PerformanceSnapshot[0]);
            int size = snapshots.length;
            
            PerformanceSnapshot latest = snapshots[size - 1];
            PerformanceSnapshot previous = snapshots[size - 2];
            
            long timeDiffMinutes = Duration.between(previous.getTimestamp(), latest.getTimestamp()).toMinutes();
            if (timeDiffMinutes > 0) {
                long requestDiff = latest.getTotalRequests() - previous.getTotalRequests();
                currentThroughput = (double) requestDiff / timeDiffMinutes;
            }
        }
    }
    
    /**
     * 创建性能快照
     */
    private void takePerformanceSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot(
            LocalDateTime.now(),
            totalPromptRequests.get(),
            successfulPrompts.get(),
            failedPrompts.get(),
            timeoutPrompts.get(),
            asyncSubmissions.get(),
            asyncCompletions.get(),
            successRate,
            averageResponseTime,
            currentThroughput
        );
        
        recentPerformanceData.offer(snapshot);
        
        // 保持最大快照数量
        while (recentPerformanceData.size() > MAX_PERFORMANCE_SNAPSHOTS) {
            recentPerformanceData.poll();
        }
        
        logger.debug("创建性能快照，当前快照数量: {}", recentPerformanceData.size());
    }
    
    /**
     * 获取当前业务KPI
     */
    public Map<String, Object> getCurrentKPI() {
        Map<String, Object> kpi = new ConcurrentHashMap<>();
        
        kpi.put("throughput", Math.round(currentThroughput * 100.0) / 100.0);
        kpi.put("averageResponseTime", Math.round(averageResponseTime * 100.0) / 100.0);
        kpi.put("successRate", Math.round(successRate * 100.0) / 100.0);
        kpi.put("availabilityRate", Math.round(availabilityRate * 100.0) / 100.0);
        
        // KPI健康状态
        Map<String, Object> kpiHealth = new ConcurrentHashMap<>();
        kpiHealth.put("throughputHealthy", currentThroughput >= minThroughputThreshold);
        kpiHealth.put("responseTimeHealthy", averageResponseTime <= maxResponseTimeThreshold);
        kpiHealth.put("successRateHealthy", successRate >= minSuccessRateThreshold);
        kpiHealth.put("availabilityHealthy", availabilityRate >= minAvailabilityThreshold);
        
        kpi.put("health", kpiHealth);
        kpi.put("timestamp", LocalDateTime.now().toString());
        
        return kpi;
    }
    
    /**
     * 获取业务指标统计
     */
    public Map<String, Object> getBusinessMetricsStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // 基础指标
        stats.put("totalPromptRequests", totalPromptRequests.get());
        stats.put("successfulPrompts", successfulPrompts.get());
        stats.put("failedPrompts", failedPrompts.get());
        stats.put("timeoutPrompts", timeoutPrompts.get());
        
        // 异步处理指标
        Map<String, Object> asyncStats = new ConcurrentHashMap<>();
        asyncStats.put("submissions", asyncSubmissions.get());
        asyncStats.put("completions", asyncCompletions.get());
        asyncStats.put("timeouts", asyncTimeouts.get());
        asyncStats.put("failures", asyncFailures.get());
        stats.put("async", asyncStats);
        
        // 状态转换统计
        Map<String, Object> transitionStats = new ConcurrentHashMap<>();
        statusTransitionCounts.forEach((transition, count) -> {
            Map<String, Object> transitionInfo = new ConcurrentHashMap<>();
            transitionInfo.put("count", count.get());
            
            // 计算平均转换时间
            AtomicLong sumMillis = statusDurationSums.get(transition);
            AtomicLong countTransitions = statusDurationCounts.get(transition);
            if (sumMillis != null && countTransitions != null && countTransitions.get() > 0) {
                double avgDuration = (double) sumMillis.get() / countTransitions.get();
                transitionInfo.put("averageDuration", Math.round(avgDuration * 100.0) / 100.0);
            } else {
                transitionInfo.put("averageDuration", 0.0);
            }
            
            transitionStats.put(transition, transitionInfo);
        });
        stats.put("statusTransitions", transitionStats);
        
        // KPI指标
        stats.put("kpi", getCurrentKPI());
        
        return stats;
    }
    
    /**
     * 获取性能趋势数据
     * @param hours 过去多少小时的数据
     */
    public List<PerformanceSnapshot> getPerformanceTrend(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        List<PerformanceSnapshot> trendData = new ArrayList<>();
        
        for (PerformanceSnapshot snapshot : recentPerformanceData) {
            if (snapshot.getTimestamp().isAfter(cutoffTime)) {
                trendData.add(snapshot);
            }
        }
        
        return trendData;
    }
    
    /**
     * 更新业务阈值
     */
    public void updateBusinessThresholds(double minThroughput, double maxResponseTime, 
                                       double minSuccessRate, double minAvailability) {
        this.minThroughputThreshold = minThroughput;
        this.maxResponseTimeThreshold = maxResponseTime;
        this.minSuccessRateThreshold = minSuccessRate;
        this.minAvailabilityThreshold = minAvailability;
        
        logger.info("业务监控阈值已更新: 吞吐量>={}, 响应时间<={}, 成功率>={}, 可用性>={}",
            minThroughput, maxResponseTime, minSuccessRate, minAvailability);
    }
    
    /**
     * 重置业务指标
     */
    public void resetBusinessMetrics() {
        totalPromptRequests.set(0);
        successfulPrompts.set(0);
        failedPrompts.set(0);
        timeoutPrompts.set(0);
        
        asyncSubmissions.set(0);
        asyncCompletions.set(0);
        asyncTimeouts.set(0);
        asyncFailures.set(0);
        
        statusTransitionCounts.clear();
        statusDurationSums.clear();
        statusDurationCounts.clear();
        
        recentPerformanceData.clear();
        
        // 重新初始化
        initializeStatusTransitionCounters();
        
        logger.info("业务指标已重置");
    }
    
    /**
     * 性能快照类
     */
    public static class PerformanceSnapshot {
        private final LocalDateTime timestamp;
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final long timeoutRequests;
        private final long asyncSubmissions;
        private final long asyncCompletions;
        private final double successRate;
        private final double averageResponseTime;
        private final double throughput;
        
        public PerformanceSnapshot(LocalDateTime timestamp, long totalRequests, long successfulRequests,
                                 long failedRequests, long timeoutRequests, long asyncSubmissions,
                                 long asyncCompletions, double successRate, double averageResponseTime,
                                 double throughput) {
            this.timestamp = timestamp;
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.timeoutRequests = timeoutRequests;
            this.asyncSubmissions = asyncSubmissions;
            this.asyncCompletions = asyncCompletions;
            this.successRate = successRate;
            this.averageResponseTime = averageResponseTime;
            this.throughput = throughput;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getTimeoutRequests() { return timeoutRequests; }
        public long getAsyncSubmissions() { return asyncSubmissions; }
        public long getAsyncCompletions() { return asyncCompletions; }
        public double getSuccessRate() { return successRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getThroughput() { return throughput; }
    }
}