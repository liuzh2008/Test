package com.example.medaiassistant.service;

import com.example.medaiassistant.model.StatusTransitionRecord;
import com.example.medaiassistant.model.Prompt;

import com.example.medaiassistant.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prompt状态管理器
 * 
 * 迭代3核心组件：状态一致性保障
 * 
 * 功能特性：
 * 1. 严格状态转换验证 - 确保状态转换符合业务规范
 * 2. 状态变更历史记录 - 提供完整的状态变更轨迹
 * 3. 原子性状态更新 - 保证状态更新的原子性和一致性
 * 4. 竞态条件处理 - 自动处理并发访问导致的竞态条件
 * 5. 状态一致性验证 - 实时验证状态的逻辑一致性
 * 
 * 状态转换规范：
 * 待处理 → SUBMISSION_STARTED → 已提交 → 已完成/执行失败
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@Component
public class PromptStatusManager {

    private static final Logger logger = LoggerFactory.getLogger(PromptStatusManager.class);

    // Prompt状态常量定义
    public static final String STATUS_PENDING = "待处理";
    public static final String STATUS_SUBMISSION_STARTED = "SUBMISSION_STARTED";
    public static final String STATUS_SUBMITTED = "已提交";
    public static final String STATUS_COMPLETED = "已完成";
    public static final String STATUS_FAILED = "执行失败";

    // 状态转换计数器
    private final AtomicLong statusTransitionCount = new AtomicLong(0);
    private final AtomicLong invalidTransitionCount = new AtomicLong(0);
    private final AtomicLong concurrentConflictCount = new AtomicLong(0);

    private final PromptRepository promptRepository;
    private final StatusTransitionHistory statusTransitionHistory;
    private final OptimisticLockRetryService optimisticLockRetryService;
    private final TransactionRetryService transactionRetryService;

    /**
     * 允许的状态转换映射表
     * 定义了严格的状态转换规范
     */
    private final Map<String, Set<String>> allowedTransitions;

    public PromptStatusManager(PromptRepository promptRepository,
            StatusTransitionHistory statusTransitionHistory,
            OptimisticLockRetryService optimisticLockRetryService,
            TransactionRetryService transactionRetryService) {
        this.promptRepository = promptRepository;
        this.statusTransitionHistory = statusTransitionHistory;
        this.optimisticLockRetryService = optimisticLockRetryService;
        this.transactionRetryService = transactionRetryService;

        // 初始化允许的状态转换规则
        this.allowedTransitions = initializeAllowedTransitions();
    }

    /**
     * 初始化允许的状态转换规则
     * 严格定义状态机的转换规范
     */
    private Map<String, Set<String>> initializeAllowedTransitions() {
        Map<String, Set<String>> transitions = new HashMap<>();

        // 待处理 → SUBMISSION_STARTED
        transitions.put(STATUS_PENDING, new HashSet<>(Arrays.asList(STATUS_SUBMISSION_STARTED)));

        // SUBMISSION_STARTED → 已提交, 待处理(回滚)
        transitions.put(STATUS_SUBMISSION_STARTED, new HashSet<>(Arrays.asList(STATUS_SUBMITTED, STATUS_PENDING)));

        // 已提交 → 已完成, 执行失败, 待处理(回滚)
        transitions.put(STATUS_SUBMITTED,
                new HashSet<>(Arrays.asList(STATUS_COMPLETED, STATUS_FAILED, STATUS_PENDING)));

        // 已完成 → 无(最终状态)
        transitions.put(STATUS_COMPLETED, new HashSet<>());

        // 执行失败 → 待处理(重试)
        transitions.put(STATUS_FAILED, new HashSet<>(Arrays.asList(STATUS_PENDING)));

        return transitions;
    }

    /**
     * 安全的状态转换方法
     * 确保状态转换的原子性和一致性
     * 
     * @param promptId     目标Prompt ID
     * @param newStatus    新状态
     * @param reason       状态转换原因
     * @param operatorInfo 操作者信息
     * @return 状态转换结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public StatusTransitionResult transitionStatus(Integer promptId, String newStatus, String reason,
            String operatorInfo) {
        logger.debug("开始状态转换：Prompt ID: {}, 目标状态: {}, 原因: {}", promptId, newStatus, reason);

        return transactionRetryService.executeWithRetry(() -> {
            return optimisticLockRetryService.executeWithOptimisticLockRetry(
                    () -> performStatusTransition(promptId, newStatus, reason, operatorInfo),
                    () -> refreshPromptEntity(promptId),
                    "状态转换[" + promptId + ":" + newStatus + "]");
        }, "状态转换事务[" + promptId + "]");
    }

    /**
     * 执行具体的状态转换逻辑
     */
    private StatusTransitionResult performStatusTransition(Integer promptId, String newStatus, String reason,
            String operatorInfo) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取当前Prompt状态
            Optional<Prompt> promptOptional = promptRepository.findById(promptId);
            if (!promptOptional.isPresent()) {
                logger.error("Prompt不存在：ID {}", promptId);
                return StatusTransitionResult.error("Prompt不存在", null, newStatus);
            }

            Prompt prompt = promptOptional.get();
            String currentStatus = prompt.getStatusName();

            // 2. 验证状态转换合法性
            ValidationResult validation = validateStatusTransition(currentStatus, newStatus);
            if (!validation.isValid()) {
                invalidTransitionCount.incrementAndGet();
                logger.warn("非法状态转换：Prompt ID: {}, {} → {}, 原因: {}",
                        promptId, currentStatus, newStatus, validation.getErrorMessage());
                return StatusTransitionResult.error(validation.getErrorMessage(), currentStatus, newStatus);
            }

            // 3. 检查并发冲突
            if (detectConcurrentConflict(prompt, currentStatus)) {
                concurrentConflictCount.incrementAndGet();
                logger.warn("检测到并发冲突：Prompt ID: {}, 期望状态: {}, 实际状态: {}",
                        promptId, currentStatus, prompt.getStatusName());
                throw new org.springframework.orm.ObjectOptimisticLockingFailureException("并发冲突",
                        new Exception("状态已被其他操作修改"));
            }

            // 4. 执行状态更新
            String previousStatus = prompt.getStatusName();
            prompt.setStatusName(newStatus);

            // 5. 更新时间戳
            updateTimestamps(prompt, newStatus);

            // 6. 保存到数据库
            promptRepository.save(prompt);

            // 7. 记录状态变更历史
            recordTransitionHistory(promptId, previousStatus, newStatus, reason, operatorInfo, startTime);

            // 8. 更新统计
            statusTransitionCount.incrementAndGet();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("状态转换成功：Prompt ID: {}, {} → {}, 耗时: {}ms",
                    promptId, previousStatus, newStatus, duration);

            return StatusTransitionResult.success(previousStatus, newStatus, null);

        } catch (Exception e) {
            logger.error("状态转换失败：Prompt ID: {}, 目标状态: {}, 错误: {}",
                    promptId, newStatus, e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }

    /**
     * 验证状态转换的合法性
     */
    private ValidationResult validateStatusTransition(String currentStatus, String newStatus) {
        // 1. 检查状态是否为null或空
        if (currentStatus == null || currentStatus.trim().isEmpty()) {
            return ValidationResult.invalid("当前状态为空或null");
        }

        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ValidationResult.invalid("目标状态为空或null");
        }

        // 2. 检查状态是否相同（无需转换）
        if (currentStatus.equals(newStatus)) {
            return ValidationResult.invalid("目标状态与当前状态相同，无需转换");
        }

        // 3. 检查是否为已知状态
        if (!allowedTransitions.containsKey(currentStatus)) {
            return ValidationResult.invalid("未知的当前状态: " + currentStatus);
        }

        // 4. 检查转换是否被允许
        Set<String> allowedTargets = allowedTransitions.get(currentStatus);
        if (!allowedTargets.contains(newStatus)) {
            return ValidationResult.invalid(
                    String.format("不允许的状态转换: %s → %s, 允许的转换: %s",
                            currentStatus, newStatus, allowedTargets));
        }

        return ValidationResult.valid();
    }

    /**
     * 检测并发冲突
     */
    private boolean detectConcurrentConflict(Prompt prompt, String expectedStatus) {
        return !expectedStatus.equals(prompt.getStatusName());
    }

    /**
     * 更新时间戳字段
     */
    private void updateTimestamps(Prompt prompt, String newStatus) {
        LocalDateTime now = LocalDateTime.now();

        switch (newStatus) {
            case STATUS_SUBMISSION_STARTED:
                prompt.setProcessingStartTime(now);
                break;
            case STATUS_SUBMITTED:
                prompt.setSubmissionTime(now);
                break;
            case STATUS_COMPLETED:
            case STATUS_FAILED:
                prompt.setExecutionTime(now);
                prompt.setProcessingEndTime(now);
                break;
        }
    }

    /**
     * 记录状态变更历史
     */
    private void recordTransitionHistory(Integer promptId, String fromStatus, String toStatus,
            String reason, String operatorInfo, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            statusTransitionHistory.recordTransition(promptId, fromStatus, toStatus, reason, operatorInfo, duration);
        } catch (Exception e) {
            logger.error("记录状态变更历史失败：Prompt ID: {}, {} → {}, 错误: {}",
                    promptId, fromStatus, toStatus, e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 刷新Prompt实体（用于乐观锁重试）
     */
    private Object refreshPromptEntity(Integer promptId) {
        try {
            return promptRepository.findById(promptId).orElse(null);
        } catch (Exception e) {
            logger.warn("刷新Prompt实体失败：ID {}, 错误: {}", promptId, e.getMessage());
            return null;
        }
    }

    /**
     * 批量状态转换
     * 适用于批量操作场景
     * 
     * @param transitions 状态转换请求列表
     * @return 批量转换结果
     */
    public BatchTransitionResult batchTransitionStatus(List<StatusTransitionRequest> transitions) {
        logger.info("开始批量状态转换：数量 {}", transitions.size());

        BatchTransitionResult result = new BatchTransitionResult();

        for (StatusTransitionRequest request : transitions) {
            try {
                StatusTransitionResult singleResult = transitionStatus(
                        request.getPromptId(),
                        request.getNewStatus(),
                        request.getReason(),
                        request.getOperatorInfo());

                if (singleResult.isSuccess()) {
                    result.addSuccess(request.getPromptId());
                } else {
                    result.addFailure(request.getPromptId(), singleResult.getErrorMessage());
                }

            } catch (Exception e) {
                logger.error("批量状态转换失败：Prompt ID: {}, 错误: {}",
                        request.getPromptId(), e.getMessage(), e);
                result.addFailure(request.getPromptId(), e.getMessage());
            }
        }

        logger.info("批量状态转换完成：成功 {}, 失败 {}", result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    /**
     * 获取Prompt当前状态
     * 
     * @param promptId Prompt ID
     * @return 当前状态信息
     */
    public PromptStatusInfo getPromptStatus(Integer promptId) {
        Optional<Prompt> promptOptional = promptRepository.findById(promptId);
        if (!promptOptional.isPresent()) {
            return null;
        }

        Prompt prompt = promptOptional.get();
        List<StatusTransitionRecord> history = statusTransitionHistory.getTransitionHistory(promptId);

        return new PromptStatusInfo(
                promptId,
                prompt.getStatusName(),
                null,
                prompt.getSubmissionTime(),
                prompt.getExecutionTime(),
                history);
    }

    /**
     * 获取状态管理器统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStatusTransitions", statusTransitionCount.get());
        stats.put("invalidTransitions", invalidTransitionCount.get());
        stats.put("concurrentConflicts", concurrentConflictCount.get());
        stats.put("allowedTransitionsConfig", allowedTransitions);
        return stats;
    }

    /**
     * 验证结果内部类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 状态转换结果类
     */
    public static class StatusTransitionResult {
        private final boolean success;
        private final String errorMessage;
        private final String fromStatus;
        private final String toStatus;
        private final Integer version;

        private StatusTransitionResult(boolean success, String errorMessage, String fromStatus, String toStatus,
                Integer version) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.version = version;
        }

        public static StatusTransitionResult success(String fromStatus, String toStatus, Integer version) {
            return new StatusTransitionResult(true, null, fromStatus, toStatus, version);
        }

        public static StatusTransitionResult error(String errorMessage, String fromStatus, String toStatus) {
            return new StatusTransitionResult(false, errorMessage, fromStatus, toStatus, null);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }

        public Integer getVersion() {
            return version;
        }
    }

    /**
     * 状态转换请求类
     */
    public static class StatusTransitionRequest {
        private Integer promptId;
        private String newStatus;
        private String reason;
        private String operatorInfo;

        // 构造函数和getter/setter方法
        public StatusTransitionRequest(Integer promptId, String newStatus, String reason, String operatorInfo) {
            this.promptId = promptId;
            this.newStatus = newStatus;
            this.reason = reason;
            this.operatorInfo = operatorInfo;
        }

        public Integer getPromptId() {
            return promptId;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public String getReason() {
            return reason;
        }

        public String getOperatorInfo() {
            return operatorInfo;
        }
    }

    /**
     * 批量转换结果类
     */
    public static class BatchTransitionResult {
        private final Map<Integer, String> successResults = new HashMap<>();
        private final Map<Integer, String> failureResults = new HashMap<>();

        public void addSuccess(Integer promptId) {
            successResults.put(promptId, "SUCCESS");
        }

        public void addFailure(Integer promptId, String errorMessage) {
            failureResults.put(promptId, errorMessage);
        }

        public int getSuccessCount() {
            return successResults.size();
        }

        public int getFailureCount() {
            return failureResults.size();
        }

        public Map<Integer, String> getSuccessResults() {
            return successResults;
        }

        public Map<Integer, String> getFailureResults() {
            return failureResults;
        }
    }

    /**
     * Prompt状态信息类
     */
    public static class PromptStatusInfo {
        private final Integer promptId;
        private final String currentStatus;
        private final Integer version;
        private final LocalDateTime submissionTime;
        private final LocalDateTime executionTime;
        private final List<StatusTransitionRecord> transitionHistory;

        public PromptStatusInfo(Integer promptId, String currentStatus, Integer version,
                LocalDateTime submissionTime, LocalDateTime executionTime,
                List<StatusTransitionRecord> transitionHistory) {
            this.promptId = promptId;
            this.currentStatus = currentStatus;
            this.version = version;
            this.submissionTime = submissionTime;
            this.executionTime = executionTime;
            this.transitionHistory = transitionHistory;
        }

        // Getters
        public Integer getPromptId() {
            return promptId;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public Integer getVersion() {
            return version;
        }

        public LocalDateTime getSubmissionTime() {
            return submissionTime;
        }

        public LocalDateTime getExecutionTime() {
            return executionTime;
        }

        public List<StatusTransitionRecord> getTransitionHistory() {
            return transitionHistory;
        }
    }
}
