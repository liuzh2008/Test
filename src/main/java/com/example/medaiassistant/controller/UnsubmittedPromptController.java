package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.service.UnsubmittedPromptEnhancedProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 未提交Prompt管理控制器
 * 提供增强的未提交Prompt处理API，包括详细的错误处理和故障恢复机制
 * 
 * 主要功能：
 * 1. 获取和处理未提交的Prompt列表
 * 2. 状态一致性检查和修复
 * 3. 错误处理统计和分析
 * 4. 恢复策略管理
 * 5. 熔断器状态监控
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("/api/unsubmitted-prompts")
public class UnsubmittedPromptController {
    
    private static final Logger logger = LoggerFactory.getLogger(UnsubmittedPromptController.class);
    
    // 状态常量 - 遵循Prompt状态流转规范
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SUBMISSION_STARTED = "SUBMISSION_STARTED";
    private static final String STATUS_SUBMITTED = "已提交";
    
    @Autowired
    private PromptRepository promptRepository;
    
    @Autowired
    private UnsubmittedPromptEnhancedProcessor enhancedProcessor;
    
    /**
     * 获取未提交Prompt列表
     * 包含详细的分类和统计信息
     * 
     * @return 未提交Prompt列表和统计信息
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUnsubmittedPrompts() {
        try {
            // 查询各种状态的Prompt
            List<Prompt> pendingPrompts = promptRepository.findByStatusName(STATUS_PENDING);
            List<Prompt> submissionStartedPrompts = promptRepository.findByStatusName(STATUS_SUBMISSION_STARTED);
            List<Prompt> submittedPrompts = promptRepository.findByStatusName(STATUS_SUBMITTED);
            
            // 合并未完成的Prompt
            List<Prompt> unsubmittedPrompts = new ArrayList<>();
            unsubmittedPrompts.addAll(pendingPrompts);
            unsubmittedPrompts.addAll(submissionStartedPrompts);
            unsubmittedPrompts.addAll(submittedPrompts);
            
            // 按ID排序
            unsubmittedPrompts.sort(Comparator.comparing(Prompt::getPromptId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("totalCount", unsubmittedPrompts.size());
            response.put("pendingCount", pendingPrompts.size());
            response.put("submissionStartedCount", submissionStartedPrompts.size());
            response.put("submittedCount", submittedPrompts.size());
            
            // 构建详细列表
            List<Map<String, Object>> promptDetails = unsubmittedPrompts.stream()
                .map(this::buildPromptDetail)
                .collect(Collectors.toList());
            
            response.put("prompts", promptDetails);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("查询未提交Prompt列表完成 - 总数: {}, 待处理: {}, 提交中: {}, 已提交: {}", 
                       unsubmittedPrompts.size(), pendingPrompts.size(), 
                       submissionStartedPrompts.size(), submittedPrompts.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取未提交Prompt列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "获取列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 处理未提交的Prompt列表
     * 使用增强的错误处理和恢复机制
     * 
     * @param maxConcurrency 最大并发数（可选，默认5）
     * @return 处理结果
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processUnsubmittedPrompts(
            @RequestParam(value = "maxConcurrency", defaultValue = "5") int maxConcurrency) {
        
        try {
            // 查询未提交的Prompt
            List<Prompt> pendingPrompts = promptRepository.findByStatusName(STATUS_PENDING);
            List<Prompt> submissionStartedPrompts = promptRepository.findByStatusName(STATUS_SUBMISSION_STARTED);
            List<Prompt> submittedPrompts = promptRepository.findByStatusName(STATUS_SUBMITTED);
            
            List<Prompt> unsubmittedPrompts = new ArrayList<>();
            unsubmittedPrompts.addAll(pendingPrompts);
            unsubmittedPrompts.addAll(submissionStartedPrompts);
            unsubmittedPrompts.addAll(submittedPrompts);
            
            if (unsubmittedPrompts.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NO_UNSUBMITTED_PROMPTS");
                response.put("message", "没有需要处理的未提交Prompt");
                response.put("totalCount", 0);
                return ResponseEntity.ok(response);
            }
            
            logger.info("开始处理 {} 个未提交的Prompt", unsubmittedPrompts.size());
            
            // 使用增强处理器处理
            UnsubmittedPromptEnhancedProcessor.ProcessingResult result = 
                enhancedProcessor.processUnsubmittedPrompts(unsubmittedPrompts);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PROCESSING_COMPLETED");
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("successRate", Math.round(result.getSuccessRate() * 10000) / 100.0); // 百分比，保留2位小数
            response.put("totalTimeMs", result.getTotalTimeMs());
            response.put("averageTimeMs", result.getTotalCount() > 0 ? result.getTotalTimeMs() / result.getTotalCount() : 0);
            
            // 构建详细结果
            List<Map<String, Object>> processingDetails = result.getDetails().stream()
                .map(this::buildProcessingDetail)
                .collect(Collectors.toList());
            
            response.put("details", processingDetails);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", String.format("处理完成 - 总数: %d, 成功: %d, 失败: %d, 成功率: %.2f%%",
                                                result.getTotalCount(), result.getSuccessCount(), 
                                                result.getFailureCount(), result.getSuccessRate() * 100));
            
            logger.info("未提交Prompt处理完成 - {}", response.get("message"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("处理未提交Prompt失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "处理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取处理统计信息
     * 包括错误分类、恢复策略、熔断器状态等
     * 
     * @return 详细的处理统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProcessingStatistics() {
        try {
            UnsubmittedPromptEnhancedProcessor.ProcessingStatistics stats = 
                enhancedProcessor.getProcessingStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            
            // 基础统计
            Map<String, Object> basicStats = new HashMap<>();
            basicStats.put("totalProcessed", stats.getTotalProcessed());
            basicStats.put("successfulProcessed", stats.getSuccessfulProcessed());
            basicStats.put("failedProcessed", stats.getFailedProcessed());
            basicStats.put("successRate", Math.round(stats.getSuccessRate() * 10000) / 100.0);
            response.put("basicStatistics", basicStats);
            
            // 错误分类统计
            Map<String, Object> errorStats = new HashMap<>();
            stats.getErrorCategoryCounts().forEach((category, count) -> {
                errorStats.put(category.name(), count);
            });
            response.put("errorCategoryStatistics", errorStats);
            
            // 熔断器状态
            Map<String, Object> circuitBreakerStats = new HashMap<>();
            stats.getCircuitBreakers().forEach((name, info) -> {
                Map<String, Object> cbInfo = new HashMap<>();
                cbInfo.put("open", info.isOpen());
                cbInfo.put("consecutiveFailures", info.getConsecutiveFailures());
                cbInfo.put("successRate", Math.round(info.getSuccessRate() * 10000) / 100.0);
                circuitBreakerStats.put(name, cbInfo);
            });
            response.put("circuitBreakerStatistics", circuitBreakerStats);
            
            // 最近处理历史（最近10条）
            List<Map<String, Object>> recentHistory = stats.getRecentHistory().stream()
                .limit(10)
                .map(this::buildProcessingRecord)
                .collect(Collectors.toList());
            response.put("recentHistory", recentHistory);
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取处理统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 重置处理统计信息
     * 
     * @return 重置结果
     */
    @PostMapping("/statistics/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        try {
            enhancedProcessor.resetStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "处理统计信息已重置");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("未提交Prompt处理统计信息已重置");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("重置处理统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "重置统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 健康检查接口
     * 检查处理器的整体健康状况
     * 
     * @return 健康状况信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            UnsubmittedPromptEnhancedProcessor.ProcessingStatistics stats = 
                enhancedProcessor.getProcessingStatistics();
            
            // 计算健康评分
            double successRate = stats.getSuccessRate();
            boolean hasOpenCircuitBreakers = stats.getCircuitBreakers().values().stream()
                .anyMatch(info -> info.isOpen());
            
            boolean healthy = successRate >= 0.8 && !hasOpenCircuitBreakers;
            String healthStatus = healthy ? "HEALTHY" : "UNHEALTHY";
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", healthStatus);
            response.put("healthy", healthy);
            response.put("successRate", Math.round(successRate * 10000) / 100.0);
            response.put("hasOpenCircuitBreakers", hasOpenCircuitBreakers);
            response.put("totalProcessed", stats.getTotalProcessed());
            
            // 健康建议
            List<String> recommendations = new ArrayList<>();
            if (successRate < 0.8) {
                recommendations.add("成功率偏低，建议检查错误原因并优化处理逻辑");
            }
            if (hasOpenCircuitBreakers) {
                recommendations.add("存在开启的熔断器，建议检查相关服务状态");
            }
            if (stats.getTotalProcessed() == 0) {
                recommendations.add("尚未处理任何Prompt，系统处于待机状态");
            }
            if (recommendations.isEmpty()) {
                recommendations.add("系统运行正常");
            }
            
            response.put("recommendations", recommendations);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("healthy", false);
            errorResponse.put("message", "健康检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 构建Prompt详细信息
     */
    private Map<String, Object> buildPromptDetail(Prompt prompt) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("promptId", prompt.getPromptId());
        detail.put("statusName", prompt.getStatusName());
        detail.put("submissionTime", prompt.getSubmissionTime());
        detail.put("executionTime", prompt.getExecutionTime());
        
        // 添加内容摘要（截取前100字符）
        String objectiveContent = prompt.getObjectiveContent();
        if (objectiveContent != null && objectiveContent.length() > 100) {
            detail.put("contentSummary", objectiveContent.substring(0, 100) + "...");
        } else {
            detail.put("contentSummary", objectiveContent);
        }
        
        return detail;
    }
    
    /**
     * 构建处理详情
     */
    private Map<String, Object> buildProcessingDetail(UnsubmittedPromptEnhancedProcessor.ProcessingDetail detail) {
        Map<String, Object> result = new HashMap<>();
        result.put("promptId", detail.getPromptId());
        result.put("successful", detail.isSuccessful());
        result.put("processingTimeMs", detail.getProcessingTimeMs());
        result.put("message", detail.getMessage());
        
        if (detail.getErrorCategory() != null) {
            result.put("errorCategory", detail.getErrorCategory().name());
        }
        if (detail.getStrategy() != null) {
            result.put("recoveryStrategy", detail.getStrategy().name());
        }
        
        return result;
    }
    
    /**
     * 构建处理记录
     */
    private Map<String, Object> buildProcessingRecord(UnsubmittedPromptEnhancedProcessor.ProcessingRecord record) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", record.getTimestamp());
        result.put("promptId", record.getPromptId());
        result.put("originalStatus", record.getOriginalStatus());
        result.put("recoverySuccessful", record.isRecoverySuccessful());
        result.put("processingTimeMs", record.getProcessingTimeMs());
        result.put("attemptCount", record.getAttemptCount());
        result.put("errorDetails", record.getErrorDetails());
        
        if (record.getErrorCategory() != null) {
            result.put("errorCategory", record.getErrorCategory().name());
        }
        if (record.getStrategy() != null) {
            result.put("recoveryStrategy", record.getStrategy().name());
        }
        
        return result;
    }
}