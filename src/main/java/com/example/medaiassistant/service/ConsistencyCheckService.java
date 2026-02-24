package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一致性检查服务
 * 
 * 迭代3核心组件：状态一致性保障
 * 
 * 功能特性：
 * 1. 定期一致性检查 - 自动检测数据状态不一致问题
 * 2. 自动修复机制 - 智能修复常见的状态不一致问题
 * 3. 不一致数据报告 - 生成详细的问题分析报告
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@Service
public class ConsistencyCheckService {

    private static final Logger logger = LoggerFactory.getLogger(ConsistencyCheckService.class);

    // Prompt状态常量
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_COMPLETED = "已完成";

    private final PromptRepository promptRepository;
    private final ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository;
    private final PromptStatusManager promptStatusManager;

    // 统计计数器
    private final AtomicLong totalChecksCount = new AtomicLong(0);
    private final AtomicLong inconsistenciesFoundCount = new AtomicLong(0);
    private final AtomicLong autoFixedCount = new AtomicLong(0);

    private volatile LocalDateTime lastCheckTime;
    private volatile ConsistencyCheckResult lastCheckResult;

    public ConsistencyCheckService(PromptRepository promptRepository,
            ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository,
            PromptStatusManager promptStatusManager) {
        this.promptRepository = promptRepository;
        this.encryptedDataTempRepository = encryptedDataTempRepository;
        this.promptStatusManager = promptStatusManager;
    }

    /**
     * 定期执行一致性检查
     * 每5分钟检查一次
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void performScheduledConsistencyCheck() {
        logger.info("开始定期一致性检查...");

        try {
            ConsistencyCheckResult result = performConsistencyCheck(true);
            this.lastCheckTime = LocalDateTime.now();
            this.lastCheckResult = result;

            logger.info("定期一致性检查完成 - 检查: {}, 问题: {}, 修复: {}",
                    result.getTotalChecked(), result.getInconsistenciesFound(), result.getAutoFixed());

        } catch (Exception e) {
            logger.error("定期一致性检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行完整的一致性检查
     */
    @Transactional
    public ConsistencyCheckResult performConsistencyCheck(boolean autoFix) {
        long startTime = System.currentTimeMillis();
        totalChecksCount.incrementAndGet();

        ConsistencyCheckResult result = new ConsistencyCheckResult();
        result.setCheckTime(LocalDateTime.now());
        result.setAutoFixEnabled(autoFix);

        try {
            // 1. 检查状态为"已提交"但EncryptedDataTemp表中无记录的Prompt
            checkSubmittedPromptsWithoutEncryptedData(result, autoFix);

            // 2. 检查EncryptedDataTemp状态为SENT但Prompt状态不是"已完成"
            checkSentDataWithIncompletePrompts(result, autoFix);

            // 更新统计信息
            inconsistenciesFoundCount.addAndGet(result.getInconsistenciesFound());
            autoFixedCount.addAndGet(result.getAutoFixed());

            long duration = System.currentTimeMillis() - startTime;
            result.setCheckDurationMs(duration);

        } catch (Exception e) {
            logger.error("一致性检查异常: {}", e.getMessage(), e);
            result.addError("检查过程异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查状态为"已提交"但EncryptedDataTemp表中无记录的Prompt
     */
    private void checkSubmittedPromptsWithoutEncryptedData(ConsistencyCheckResult result, boolean autoFix) {
        try {
            List<Prompt> submittedPrompts = promptRepository.findByStatusName(STATUS_SUBMITTED);
            result.incrementTotalChecked(submittedPrompts.size());

            for (Prompt prompt : submittedPrompts) {
                String requestId = "cdwyy" + prompt.getPromptId();
                Optional<EncryptedDataTemp> encryptedDataOptional = encryptedDataTempRepository
                        .findByRequestId(requestId);

                if (!encryptedDataOptional.isPresent()) {
                    InconsistencyIssue issue = new InconsistencyIssue(
                            "MISSING_ENCRYPTED_DATA",
                            "状态为'已提交'但ENCRYPTED_DATA_TEMP表中无记录",
                            prompt.getPromptId(),
                            prompt.getStatusName());

                    result.addInconsistency(issue);

                    if (autoFix) {
                        try {
                            promptStatusManager.transitionStatus(
                                    prompt.getPromptId(),
                                    STATUS_PENDING,
                                    "一致性检查自动修复",
                                    "ConsistencyCheckService");
                            issue.setFixed(true);
                            result.incrementAutoFixed();
                            logger.info("自动修复成功：Prompt ID {} 状态已回滚", prompt.getPromptId());
                        } catch (Exception e) {
                            logger.error("自动修复失败：Prompt ID {}, 错误: {}", prompt.getPromptId(), e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("检查已提交Prompt异常: {}", e.getMessage());
            result.addError("已提交Prompt检查异常: " + e.getMessage());
        }
    }

    /**
     * 检查EncryptedDataTemp状态为SENT但Prompt状态不是"已完成"
     */
    private void checkSentDataWithIncompletePrompts(ConsistencyCheckResult result, boolean autoFix) {
        try {
            List<EncryptedDataTemp> sentData = encryptedDataTempRepository.findByStatus(DataStatus.SENT);

            for (EncryptedDataTemp data : sentData) {
                String requestId = data.getRequestId();
                if (requestId != null && requestId.startsWith("cdwyy")) {
                    Integer promptId = Integer.parseInt(requestId.substring(5));
                    Optional<Prompt> promptOptional = promptRepository.findById(promptId);

                    if (promptOptional.isPresent()) {
                        Prompt prompt = promptOptional.get();
                        if (!STATUS_COMPLETED.equals(prompt.getStatusName())) {
                            InconsistencyIssue issue = new InconsistencyIssue(
                                    "SENT_DATA_INCOMPLETE_PROMPT",
                                    "EncryptedDataTemp状态为SENT但Prompt状态不是已完成",
                                    promptId,
                                    prompt.getStatusName());

                            result.addInconsistency(issue);

                            if (autoFix) {
                                try {
                                    promptStatusManager.transitionStatus(
                                            promptId,
                                            STATUS_COMPLETED,
                                            "一致性检查：同步SENT状态",
                                            "ConsistencyCheckService");
                                    issue.setFixed(true);
                                    result.incrementAutoFixed();
                                    logger.info("自动修复成功：Prompt ID {} 状态已更新为已完成", promptId);
                                } catch (Exception e) {
                                    logger.error("自动修复失败：Prompt ID {}, 错误: {}", promptId, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            result.incrementTotalChecked(sentData.size());
        } catch (Exception e) {
            logger.error("检查SENT数据异常: {}", e.getMessage());
            result.addError("SENT数据检查异常: " + e.getMessage());
        }
    }

    /**
     * 获取服务统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", totalChecksCount.get());
        stats.put("inconsistenciesFound", inconsistenciesFoundCount.get());
        stats.put("autoFixed", autoFixedCount.get());
        stats.put("lastCheckTime", lastCheckTime);

        if (lastCheckResult != null) {
            stats.put("lastCheckDuration", lastCheckResult.getCheckDurationMs());
            stats.put("lastCheckInconsistencies", lastCheckResult.getInconsistenciesFound());
        }

        return stats;
    }

    /**
     * 获取最后一次检查结果
     */
    public ConsistencyCheckResult getLastCheckResult() {
        return lastCheckResult;
    }

    /**
     * 一致性检查结果类
     */
    public static class ConsistencyCheckResult {
        private LocalDateTime checkTime;
        private long checkDurationMs;
        private boolean autoFixEnabled;
        private int totalChecked = 0;
        private int inconsistenciesFound = 0;
        private int autoFixed = 0;
        private List<InconsistencyIssue> issues = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        // Getters and setters
        public LocalDateTime getCheckTime() {
            return checkTime;
        }

        public void setCheckTime(LocalDateTime checkTime) {
            this.checkTime = checkTime;
        }

        public long getCheckDurationMs() {
            return checkDurationMs;
        }

        public void setCheckDurationMs(long checkDurationMs) {
            this.checkDurationMs = checkDurationMs;
        }

        public boolean isAutoFixEnabled() {
            return autoFixEnabled;
        }

        public void setAutoFixEnabled(boolean autoFixEnabled) {
            this.autoFixEnabled = autoFixEnabled;
        }

        public int getTotalChecked() {
            return totalChecked;
        }

        public void incrementTotalChecked(int count) {
            this.totalChecked += count;
        }

        public int getInconsistenciesFound() {
            return inconsistenciesFound;
        }

        public int getAutoFixed() {
            return autoFixed;
        }

        public void incrementAutoFixed() {
            this.autoFixed++;
        }

        public List<InconsistencyIssue> getIssues() {
            return issues;
        }

        public void addInconsistency(InconsistencyIssue issue) {
            this.issues.add(issue);
            this.inconsistenciesFound++;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            this.errors.add(error);
        }
    }

    /**
     * 不一致问题类
     */
    public static class InconsistencyIssue {
        private String issueType;
        private String description;
        private Integer promptId;
        private String promptStatus;
        private boolean fixed = false;
        private LocalDateTime detectedTime;

        public InconsistencyIssue(String issueType, String description, Integer promptId, String promptStatus) {
            this.issueType = issueType;
            this.description = description;
            this.promptId = promptId;
            this.promptStatus = promptStatus;
            this.detectedTime = LocalDateTime.now();
        }

        // Getters and setters
        public String getIssueType() {
            return issueType;
        }

        public String getDescription() {
            return description;
        }

        public Integer getPromptId() {
            return promptId;
        }

        public String getPromptStatus() {
            return promptStatus;
        }

        public boolean isFixed() {
            return fixed;
        }

        public void setFixed(boolean fixed) {
            this.fixed = fixed;
        }

        public LocalDateTime getDetectedTime() {
            return detectedTime;
        }
    }
}
