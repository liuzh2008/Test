package com.example.medaiassistant.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控服务
 * 
 * 提供线程池状态监控、性能指标收集、动态调整等功能
 * 支持多个专用线程池的监控和管理
 * 
 * @author Cline
 * @since 2025-09-29
 */
@Service
public class ThreadPoolMonitorService {

    private final TaskExecutor promptGenerationExecutor;
    private final TaskExecutor surgeryAnalysisExecutor;
    private final TaskExecutor taskExecutor;
    private final TaskExecutor monitoringExecutor;

    /**
     * 线程池监控数据缓存
     */
    private final Map<String, ThreadPoolMetrics> metricsCache = new HashMap<>();

    /**
     * 构造函数
     * 
     * @param promptGenerationExecutor Prompt生成专用线程池
     * @param surgeryAnalysisExecutor  手术分析专用线程池
     * @param taskExecutor             通用异步线程池
     * @param monitoringExecutor       监控线程池
     */
    public ThreadPoolMonitorService(
            @Qualifier("promptGenerationExecutor") TaskExecutor promptGenerationExecutor,
            @Qualifier("surgeryAnalysisExecutor") TaskExecutor surgeryAnalysisExecutor,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor,
            @Qualifier("monitoringExecutor") TaskExecutor monitoringExecutor) {
        this.promptGenerationExecutor = promptGenerationExecutor;
        this.surgeryAnalysisExecutor = surgeryAnalysisExecutor;
        this.taskExecutor = taskExecutor;
        this.monitoringExecutor = monitoringExecutor;
    }

    /**
     * 获取所有线程池状态
     * 
     * @return 线程池状态映射
     */
    public Map<String, Object> getAllThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("promptGenerationExecutor", getThreadPoolStatus(promptGenerationExecutor, "promptGeneration"));
        status.put("surgeryAnalysisExecutor", getThreadPoolStatus(surgeryAnalysisExecutor, "surgeryAnalysis"));
        status.put("taskExecutor", getThreadPoolStatus(taskExecutor, "taskExecutor"));
        status.put("monitoringExecutor", getThreadPoolStatus(monitoringExecutor, "monitoring"));

        return status;
    }

    /**
     * 获取指定线程池的状态
     * 
     * @param executor 线程池执行器
     * @param poolName 线程池名称
     * @return 线程池状态信息
     */
    private Map<String, Object> getThreadPoolStatus(TaskExecutor executor, String poolName) {
        Map<String, Object> status = new HashMap<>();

        if (executor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
            ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();

            if (threadPoolExecutor != null) {
                status.put("poolName", poolName);
                status.put("corePoolSize", threadPoolExecutor.getCorePoolSize());
                status.put("maxPoolSize", threadPoolExecutor.getMaximumPoolSize());
                status.put("activeCount", threadPoolExecutor.getActiveCount());
                status.put("poolSize", threadPoolExecutor.getPoolSize());
                status.put("largestPoolSize", threadPoolExecutor.getLargestPoolSize());
                status.put("queueSize", threadPoolExecutor.getQueue().size());
                status.put("queueRemainingCapacity", threadPoolExecutor.getQueue().remainingCapacity());
                status.put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount());
                status.put("taskCount", threadPoolExecutor.getTaskCount());

                // 计算队列使用率
                int queueCapacity = threadPoolExecutor.getQueue().size()
                        + threadPoolExecutor.getQueue().remainingCapacity();
                double queueUsageRate = queueCapacity > 0
                        ? (double) threadPoolExecutor.getQueue().size() / queueCapacity * 100
                        : 0;
                status.put("queueUsageRate", Math.round(queueUsageRate * 100.0) / 100.0);

                // 计算线程池使用率
                double poolUsageRate = threadPoolExecutor.getMaximumPoolSize() > 0
                        ? (double) threadPoolExecutor.getActiveCount() / threadPoolExecutor.getMaximumPoolSize() * 100
                        : 0;
                status.put("poolUsageRate", Math.round(poolUsageRate * 100.0) / 100.0);

                // 健康状态评估
                String healthStatus = evaluateHealthStatus(threadPoolExecutor);
                status.put("healthStatus", healthStatus);

                // 性能建议
                status.put("performanceSuggestions", generatePerformanceSuggestions(threadPoolExecutor, poolName));
            }
        }

        return status;
    }

    /**
     * 评估线程池健康状态
     * 
     * @param executor 线程池执行器
     * @return 健康状态
     */
    private String evaluateHealthStatus(ThreadPoolExecutor executor) {
        int activeCount = executor.getActiveCount();
        int maxPoolSize = executor.getMaximumPoolSize();
        int queueSize = executor.getQueue().size();
        int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();

        double poolUsageRate = (double) activeCount / maxPoolSize * 100;
        double queueUsageRate = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;

        if (poolUsageRate >= 90 || queueUsageRate >= 90) {
            return "CRITICAL";
        } else if (poolUsageRate >= 70 || queueUsageRate >= 70) {
            return "WARNING";
        } else if (poolUsageRate >= 50 || queueUsageRate >= 50) {
            return "HEALTHY";
        } else {
            return "IDLE";
        }
    }

    /**
     * 生成性能优化建议
     * 
     * @param executor 线程池执行器
     * @param poolName 线程池名称
     * @return 性能建议列表
     */
    private String[] generatePerformanceSuggestions(ThreadPoolExecutor executor, String poolName) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        int activeCount = executor.getActiveCount();
        int maxPoolSize = executor.getMaximumPoolSize();
        int queueSize = executor.getQueue().size();
        int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();

        double poolUsageRate = (double) activeCount / maxPoolSize * 100;
        double queueUsageRate = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;

        if (poolUsageRate >= 80) {
            suggestions.add("线程池使用率较高(" + Math.round(poolUsageRate) + "%)，建议增加最大线程数");
        }

        if (queueUsageRate >= 80) {
            suggestions.add("队列使用率较高(" + Math.round(queueUsageRate) + "%)，建议增加队列容量或优化任务处理速度");
        }

        if (poolUsageRate < 20 && queueUsageRate < 20) {
            suggestions.add("线程池使用率较低，可以考虑减少核心线程数以节省资源");
        }

        // 注意：ThreadPoolExecutor没有getRejectedExecutionCount()方法
        // 可以通过其他方式监控任务拒绝情况
        if (executor.getCompletedTaskCount() > 1000) {
            suggestions.add("任务完成数量较多，建议监控任务执行效率");
        }

        return suggestions.toArray(new String[0]);
    }

    /**
     * 定期收集线程池指标
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    @Async("monitoringExecutor")
    public void scheduledMetricsCollection() {
        try {
            Map<String, Object> status = getAllThreadPoolStatus();

            // 记录关键指标到日志
            for (Map.Entry<String, Object> entry : status.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> poolStatus = (Map<String, Object>) entry.getValue();

                String poolName = (String) poolStatus.get("poolName");
                String healthStatus = (String) poolStatus.get("healthStatus");
                double poolUsageRate = (Double) poolStatus.get("poolUsageRate");
                double queueUsageRate = (Double) poolStatus.get("queueUsageRate");

                // 缓存指标数据
                ThreadPoolMetrics metrics = new ThreadPoolMetrics(
                        poolName,
                        healthStatus,
                        poolUsageRate,
                        queueUsageRate,
                        System.currentTimeMillis());
                metricsCache.put(poolName, metrics);

                // 记录警告日志
                if ("CRITICAL".equals(healthStatus)) {
                    System.err.println("线程池监控告警 - " + poolName + ": 状态=" + healthStatus +
                            ", 线程使用率=" + poolUsageRate + "%, 队列使用率=" + queueUsageRate + "%");
                }
            }

        } catch (Exception e) {
            System.err.println("线程池指标收集失败: " + e.getMessage());
        }
    }

    /**
     * 获取线程池指标历史数据
     * 
     * @return 指标历史数据
     */
    public Map<String, ThreadPoolMetrics> getMetricsHistory() {
        return new HashMap<>(metricsCache);
    }

    /**
     * 线程池指标数据类
     */
    public static class ThreadPoolMetrics {
        private final String poolName;
        private final String healthStatus;
        private final double poolUsageRate;
        private final double queueUsageRate;
        private final long timestamp;

        public ThreadPoolMetrics(String poolName, String healthStatus, double poolUsageRate,
                double queueUsageRate, long timestamp) {
            this.poolName = poolName;
            this.healthStatus = healthStatus;
            this.poolUsageRate = poolUsageRate;
            this.queueUsageRate = queueUsageRate;
            this.timestamp = timestamp;
        }

        // Getter方法
        public String getPoolName() {
            return poolName;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public double getPoolUsageRate() {
            return poolUsageRate;
        }

        public double getQueueUsageRate() {
            return queueUsageRate;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
