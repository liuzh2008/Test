package com.example.medaiassistant.service;

import com.example.medaiassistant.config.PromptServiceConfig;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prompt监控服务
 * 负责系统监控、健康检查和性能指标收集
 * 
 * 主要功能：
 * 1. 系统健康状态监控
 * 2. 性能指标收集和分析
 * 3. 告警机制
 * 4. 统计信息报告
 * 
 * 数据源说明：
 * - Prompt相关指标：使用主数据源（127.0.0.1）统计
 * - 加密数据相关指标：使用执行服务器数据源（100.66.1.2）统计
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@Service
@EnableScheduling
public class PromptMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(PromptMonitoringService.class);

    /**
     * Prompt仓库，用于统计主数据源的Prompt指标
     */
    private final PromptRepository promptRepository;

    /**
     * 执行服务器加密数据临时仓库，用于统计执行服务器数据源的加密数据指标
     * 避免访问本地不存在的ENCRYPTED_DATA_TEMP表导致的ORA-00942错误
     */
    private final ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository;

    /**
     * Prompt服务配置
     */
    private final PromptServiceConfig promptServiceConfig;

    /**
     * Prompt提交服务
     */
    private final PromptSubmissionService promptSubmissionService;

    /**
     * Prompt轮询服务
     */
    private final PromptPollingService promptPollingService;

    // 性能指标计数器
    private final AtomicLong totalSubmissions = new AtomicLong(0);
    private final AtomicLong totalPollingSuccess = new AtomicLong(0);
    private final AtomicLong totalPollingErrors = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    /**
     * 构造函数，依赖注入必要的服务实例
     */
    public PromptMonitoringService(PromptRepository promptRepository,
            ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository,
            PromptServiceConfig promptServiceConfig,
            PromptSubmissionService promptSubmissionService,
            PromptPollingService promptPollingService) {
        this.promptRepository = promptRepository;
        this.executionEncryptedDataTempRepository = executionEncryptedDataTempRepository;
        this.promptServiceConfig = promptServiceConfig;
        this.promptSubmissionService = promptSubmissionService;
        this.promptPollingService = promptPollingService;
    }

    /**
     * 定时监控任务
     * 定期收集系统指标和健康状态
     * 添加优雅降级机制，避免执行服务器不可用时完全失败
     */
    @Scheduled(fixedDelayString = "${monitoring.interval:60000}")
    public void monitorSystem() {
        try {
            logger.info("开始系统监控检查...");

            // 收集系统指标（带降级处理）
            Map<String, Object> metrics = collectSystemMetricsWithFallback();

            // 检查系统健康状态（带降级处理）
            boolean isHealthy = checkSystemHealthWithFallback(metrics);

            // 记录监控结果
            logMonitoringResults(metrics, isHealthy);

            // 触发告警（如果需要）
            triggerAlerts(metrics, isHealthy);

            logger.info("系统监控检查完成");

        } catch (Exception e) {
            logger.error("系统监控任务执行失败: {}", e.getMessage(), e);
            // 即使监控失败，也不影响主服务运行
        }
    }

    /**
     * 收集系统指标
     * 
     * @return 系统指标集合
     */
    public Map<String, Object> collectSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Prompt相关指标
            long totalPrompts = promptRepository.count();
            long pendingPrompts = promptRepository.countByStatusName("待执行");
            long submittedPrompts = promptRepository.countByStatusName("已提交");
            long completedPrompts = promptRepository.countByStatusName("已完成");
            long failedPrompts = promptRepository.countByStatusName("执行失败");

            // 加密数据相关指标（使用执行服务器数据源）
            long encryptedDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED);
            long sentDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.SENT);
            long errorDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ERROR);

            // 计算成功率
            double successRate = totalPrompts > 0 ? (double) completedPrompts / totalPrompts * 100 : 0.0;

            // 计算错误率
            double errorRate = totalPrompts > 0 ? (double) failedPrompts / totalPrompts * 100 : 0.0;

            // 填充指标数据
            metrics.put("totalPrompts", totalPrompts);
            metrics.put("pendingPrompts", pendingPrompts);
            metrics.put("submittedPrompts", submittedPrompts);
            metrics.put("completedPrompts", completedPrompts);
            metrics.put("failedPrompts", failedPrompts);
            metrics.put("encryptedDataCount", encryptedDataCount);
            metrics.put("sentDataCount", sentDataCount);
            metrics.put("errorDataCount", errorDataCount);
            metrics.put("successRate", Math.round(successRate * 100.0) / 100.0);
            metrics.put("errorRate", Math.round(errorRate * 100.0) / 100.0);
            metrics.put("totalSubmissions", totalSubmissions.get());
            metrics.put("totalPollingSuccess", totalPollingSuccess.get());
            metrics.put("totalPollingErrors", totalPollingErrors.get());
            metrics.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("收集系统指标失败: {}", e.getMessage(), e);
            metrics.put("error", "收集指标失败: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * 检查系统健康状态
     * 
     * @param metrics 系统指标
     * @return true如果系统健康，false否则
     */
    public boolean checkSystemHealth(Map<String, Object> metrics) {
        try {
            // 检查错误率是否超过阈值
            double errorRate = (double) metrics.getOrDefault("errorRate", 0.0);
            double errorRateThreshold = promptServiceConfig.getMonitoring().getErrorRateThreshold();

            if (errorRate > errorRateThreshold) {
                logger.warn("系统错误率超过阈值: {}% > {}%", errorRate, errorRateThreshold);
                return false;
            }

            // 检查队列长度是否超过阈值
            long pendingPrompts = (long) metrics.getOrDefault("pendingPrompts", 0L);
            int queueLengthThreshold = promptServiceConfig.getMonitoring().getQueueLengthThreshold();

            if (pendingPrompts > queueLengthThreshold) {
                logger.warn("待处理队列长度超过阈值: {} > {}", pendingPrompts, queueLengthThreshold);
                return false;
            }

            // 检查数据库连接
            long totalPrompts = (long) metrics.getOrDefault("totalPrompts", 0L);
            if (totalPrompts < 0) {
                logger.error("数据库连接异常，无法获取Prompt总数");
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("检查系统健康状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 记录监控结果
     * 
     * @param metrics   系统指标
     * @param isHealthy 系统健康状态
     */
    private void logMonitoringResults(Map<String, Object> metrics, boolean isHealthy) {
        if (isHealthy) {
            logger.info("系统健康状态: 正常, 成功率: {}%, 待处理: {}, 已提交: {}, 已完成: {}",
                    metrics.get("successRate"),
                    metrics.get("pendingPrompts"),
                    metrics.get("submittedPrompts"),
                    metrics.get("completedPrompts"));
        } else {
            logger.warn("系统健康状态: 异常, 错误率: {}%, 待处理: {}, 失败: {}",
                    metrics.get("errorRate"),
                    metrics.get("pendingPrompts"),
                    metrics.get("failedPrompts"));
        }
    }

    /**
     * 触发告警
     * 
     * @param metrics   系统指标
     * @param isHealthy 系统健康状态
     */
    private void triggerAlerts(Map<String, Object> metrics, boolean isHealthy) {
        if (!isHealthy) {
            // 这里可以集成告警系统，如发送邮件、短信、Slack通知等
            logger.warn("系统健康状态异常，触发告警 - 错误率: {}%, 待处理队列: {}",
                    metrics.get("errorRate"), metrics.get("pendingPrompts"));
        }

        // 检查其他告警条件
        double errorRate = (double) metrics.getOrDefault("errorRate", 0.0);
        long pendingPrompts = (long) metrics.getOrDefault("pendingPrompts", 0L);

        if (errorRate > promptServiceConfig.getMonitoring().getErrorRateThreshold()) {
            logger.error("错误率告警: {}% 超过阈值 {}%",
                    errorRate, promptServiceConfig.getMonitoring().getErrorRateThreshold());
        }

        if (pendingPrompts > promptServiceConfig.getMonitoring().getQueueLengthThreshold()) {
            logger.error("队列长度告警: {} 超过阈值 {}",
                    pendingPrompts, promptServiceConfig.getMonitoring().getQueueLengthThreshold());
        }
    }

    /**
     * 获取系统健康状态报告
     * 
     * @return 健康状态报告
     */
    public String getHealthReport() {
        try {
            Map<String, Object> metrics = collectSystemMetrics();
            boolean isHealthy = checkSystemHealth(metrics);

            return String.format("系统健康报告 - 状态: %s, 成功率: %.2f%%, 错误率: %.2f%%, 待处理: %d, 已提交: %d, 已完成: %d",
                    isHealthy ? "健康" : "异常",
                    metrics.get("successRate"),
                    metrics.get("errorRate"),
                    metrics.get("pendingPrompts"),
                    metrics.get("submittedPrompts"),
                    metrics.get("completedPrompts"));
        } catch (Exception e) {
            return "系统健康报告 - 生成失败: " + e.getMessage();
        }
    }

    /**
     * 获取详细性能指标
     * 
     * @return 性能指标报告
     */
    public Map<String, Object> getPerformanceMetrics() {
        return collectSystemMetrics();
    }

    /**
     * 记录提交成功
     */
    public void recordSubmissionSuccess() {
        totalSubmissions.incrementAndGet();
    }

    /**
     * 记录轮询成功
     */
    public void recordPollingSuccess() {
        totalPollingSuccess.incrementAndGet();
    }

    /**
     * 记录轮询错误
     */
    public void recordPollingError() {
        totalPollingErrors.incrementAndGet();
    }

    /**
     * 记录处理时间
     * 
     * @param processingTime 处理时间（毫秒）
     */
    public void recordProcessingTime(long processingTime) {
        totalProcessingTime.addAndGet(processingTime);
    }

    /**
     * 获取平均处理时间
     * 
     * @return 平均处理时间（毫秒）
     */
    public double getAverageProcessingTime() {
        long totalSuccess = totalPollingSuccess.get();
        if (totalSuccess > 0) {
            return (double) totalProcessingTime.get() / totalSuccess;
        }
        return 0.0;
    }

    /**
     * 获取监控服务配置信息
     * 
     * @return 监控服务配置信息
     */
    public String getMonitoringConfigInfo() {
        return String.format("监控服务配置 - 监控间隔: %dms, 错误率阈值: %.1f%%, 队列长度阈值: %d, 响应时间阈值: %dms",
                promptServiceConfig.getMonitoring().getInterval(),
                promptServiceConfig.getMonitoring().getErrorRateThreshold(),
                promptServiceConfig.getMonitoring().getQueueLengthThreshold(),
                promptServiceConfig.getMonitoring().getResponseTimeThreshold());
    }

    /**
     * 检查监控服务健康状态（带降级处理）
     * 
     * @return 健康状态信息
     */
    public String healthCheck() {
        try {
            // 验证所有依赖服务
            String submissionHealth = promptSubmissionService.healthCheck();
            String pollingHealth = promptPollingService.healthCheck();

            // 验证数据库连接（带降级处理）
            long totalPrompts = promptRepository.count();
            
            // 检查执行服务器连接（带降级处理）
            long totalEncryptedData = 0;
            String executionServerStatus = "正常";
            
            try {
                totalEncryptedData = executionEncryptedDataTempRepository.count();
            } catch (Exception e) {
                logger.warn("执行服务器连接检查失败，使用降级模式: {}", e.getMessage());
                executionServerStatus = "不可用";
            }

            return String.format("监控服务健康状态 - 数据库连接: 正常, 总Prompt数: %d, 执行服务器: %s, 总加密数据数: %d, 提交服务: %s, 轮询服务: %s",
                    totalPrompts, executionServerStatus, totalEncryptedData,
                    submissionHealth.contains("正常") ? "正常" : "异常",
                    pollingHealth.contains("正常") ? "正常" : "异常");
        } catch (Exception e) {
            return "监控服务健康状态 - 异常: " + e.getMessage();
        }
    }

    /**
     * 重置性能计数器
     * 用于测试和调试目的
     */
    public void resetCounters() {
        totalSubmissions.set(0);
        totalPollingSuccess.set(0);
        totalPollingErrors.set(0);
        totalProcessingTime.set(0);
        logger.info("性能计数器已重置");
    }

    /**
     * 获取性能统计摘要
     * 
     * @return 性能统计摘要
     */
    public String getPerformanceSummary() {
        long totalSubmissionsCount = totalSubmissions.get();
        long totalPollingSuccessCount = totalPollingSuccess.get();
        long totalPollingErrorsCount = totalPollingErrors.get();
        double averageProcessingTime = getAverageProcessingTime();

        double pollingSuccessRate = totalSubmissionsCount > 0
                ? (double) totalPollingSuccessCount / totalSubmissionsCount * 100
                : 0.0;

        return String.format("性能统计摘要 - 总提交: %d, 轮询成功: %d, 轮询错误: %d, 成功率: %.2f%%, 平均处理时间: %.2fms",
                totalSubmissionsCount, totalPollingSuccessCount, totalPollingErrorsCount,
                pollingSuccessRate, averageProcessingTime);
    }

    /**
     * 收集系统指标（带降级处理）
     * 当执行服务器不可用时，提供降级指标数据
     * 
     * @return 系统指标集合
     */
    public Map<String, Object> collectSystemMetricsWithFallback() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // 尝试收集主数据源指标
            long totalPrompts = promptRepository.count();
            long pendingPrompts = promptRepository.countByStatusName("待执行");
            long submittedPrompts = promptRepository.countByStatusName("已提交");
            long completedPrompts = promptRepository.countByStatusName("已完成");
            long failedPrompts = promptRepository.countByStatusName("执行失败");

            // 计算成功率
            double successRate = totalPrompts > 0 ? (double) completedPrompts / totalPrompts * 100 : 0.0;
            double errorRate = totalPrompts > 0 ? (double) failedPrompts / totalPrompts * 100 : 0.0;

            // 填充主数据源指标
            metrics.put("totalPrompts", totalPrompts);
            metrics.put("pendingPrompts", pendingPrompts);
            metrics.put("submittedPrompts", submittedPrompts);
            metrics.put("completedPrompts", completedPrompts);
            metrics.put("failedPrompts", failedPrompts);
            metrics.put("successRate", Math.round(successRate * 100.0) / 100.0);
            metrics.put("errorRate", Math.round(errorRate * 100.0) / 100.0);

            // 尝试收集执行服务器指标（带降级处理）
            try {
                long encryptedDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED);
                long sentDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.SENT);
                long errorDataCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ERROR);

                metrics.put("encryptedDataCount", encryptedDataCount);
                metrics.put("sentDataCount", sentDataCount);
                metrics.put("errorDataCount", errorDataCount);
                metrics.put("executionServerAvailable", true);

            } catch (Exception e) {
                logger.warn("执行服务器指标收集失败，使用降级数据: {}", e.getMessage());
                // 提供降级数据
                metrics.put("encryptedDataCount", -1);
                metrics.put("sentDataCount", -1);
                metrics.put("errorDataCount", -1);
                metrics.put("executionServerAvailable", false);
                metrics.put("executionServerError", e.getMessage());
            }

            // 添加性能计数器数据
            metrics.put("totalSubmissions", totalSubmissions.get());
            metrics.put("totalPollingSuccess", totalPollingSuccess.get());
            metrics.put("totalPollingErrors", totalPollingErrors.get());
            metrics.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("收集系统指标失败，使用完全降级模式: {}", e.getMessage(), e);
            // 完全降级模式：提供基本指标
            metrics.put("error", "收集指标失败: " + e.getMessage());
            metrics.put("executionServerAvailable", false);
            metrics.put("timestamp", LocalDateTime.now());
        }

        return metrics;
    }

    /**
     * 检查系统健康状态（带降级处理）
     * 当执行服务器不可用时，调整健康检查标准
     * 
     * @param metrics 系统指标
     * @return true如果系统健康，false否则
     */
    public boolean checkSystemHealthWithFallback(Map<String, Object> metrics) {
        try {
            // 检查错误率是否超过阈值
            double errorRate = (double) metrics.getOrDefault("errorRate", 0.0);
            double errorRateThreshold = promptServiceConfig.getMonitoring().getErrorRateThreshold();

            if (errorRate > errorRateThreshold) {
                logger.warn("系统错误率超过阈值: {}% > {}%", errorRate, errorRateThreshold);
                return false;
            }

            // 检查队列长度是否超过阈值
            long pendingPrompts = (long) metrics.getOrDefault("pendingPrompts", 0L);
            int queueLengthThreshold = promptServiceConfig.getMonitoring().getQueueLengthThreshold();

            if (pendingPrompts > queueLengthThreshold) {
                logger.warn("待处理队列长度超过阈值: {} > {}", pendingPrompts, queueLengthThreshold);
                return false;
            }

            // 检查数据库连接
            long totalPrompts = (long) metrics.getOrDefault("totalPrompts", 0L);
            if (totalPrompts < 0) {
                logger.error("数据库连接异常，无法获取Prompt总数");
                return false;
            }

            // 检查执行服务器可用性（降级处理）
            boolean executionServerAvailable = (boolean) metrics.getOrDefault("executionServerAvailable", true);
            if (!executionServerAvailable) {
                logger.warn("执行服务器不可用，系统进入降级模式");
                // 在降级模式下，只要主服务正常就认为系统基本健康
                return true;
            }

            return true;

        } catch (Exception e) {
            logger.error("检查系统健康状态失败: {}", e.getMessage(), e);
            // 健康检查失败时，保守地返回false
            return false;
        }
    }

    /**
     * 检查执行服务器可用性
     * 
     * @return 执行服务器是否可用
     */
    public boolean isExecutionServerAvailable() {
        try {
            // 尝试执行一个简单的查询来检查执行服务器连接
            executionEncryptedDataTempRepository.count();
            return true;
        } catch (Exception e) {
            logger.warn("执行服务器不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取降级模式状态报告
     * 
     * @return 降级模式状态报告
     */
    public String getFallbackStatusReport() {
        Map<String, Object> metrics = collectSystemMetricsWithFallback();
        boolean isHealthy = checkSystemHealthWithFallback(metrics);
        boolean executionServerAvailable = (boolean) metrics.getOrDefault("executionServerAvailable", true);

        String mode = executionServerAvailable ? "正常模式" : "降级模式";
        String healthStatus = isHealthy ? "健康" : "异常";

        return String.format("系统状态报告 - 模式: %s, 状态: %s, 执行服务器: %s, 成功率: %.2f%%, 待处理: %d",
                mode, healthStatus, executionServerAvailable ? "可用" : "不可用",
                metrics.get("successRate"), metrics.get("pendingPrompts"));
    }
}
