package com.example.medaiassistant.service;

import com.example.medaiassistant.model.StatusTransitionRecord;
import com.example.medaiassistant.repository.StatusTransitionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 状态转换历史服务
 * 
 * 迭代3核心组件：状态变更历史追踪
 * 
 * 功能特性：
 * 1. 完整的状态变更历史记录
 * 2. 高性能的历史查询
 * 3. 转换性能统计分析
 * 4. 历史数据清理和维护
 * 5. 审计跟踪支持
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@Service
public class StatusTransitionHistory {

    private static final Logger logger = LoggerFactory.getLogger(StatusTransitionHistory.class);

    private final StatusTransitionRecordRepository repository;

    // 性能统计计数器
    private final AtomicLong totalRecordsCount = new AtomicLong(0);
    private final AtomicLong successfulRecordsCount = new AtomicLong(0);
    private final AtomicLong failedRecordsCount = new AtomicLong(0);

    public StatusTransitionHistory(StatusTransitionRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录状态转换
     * 异步记录以不影响主业务流程性能
     * 
     * @param promptId     Prompt ID
     * @param fromStatus   源状态
     * @param toStatus     目标状态
     * @param reason       转换原因
     * @param operatorInfo 操作者信息
     * @param durationMs   操作耗时
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransition(Integer promptId, String fromStatus, String toStatus,
            String reason, String operatorInfo, Long durationMs) {
        try {
            StatusTransitionRecord record = new StatusTransitionRecord();
            record.setPromptId(promptId);
            record.setFromStatus(fromStatus);
            record.setToStatus(toStatus);
            record.setReason(reason);
            record.setOperatorInfo(operatorInfo);
            record.setDurationMs(durationMs);
            record.setSuccess(true);
            record.setTransitionTime(LocalDateTime.now());

            repository.save(record);
            totalRecordsCount.incrementAndGet();
            successfulRecordsCount.incrementAndGet();

            logger.debug("状态转换记录已保存：Prompt ID: {}, {} → {}, 耗时: {}ms",
                    promptId, fromStatus, toStatus, durationMs);

        } catch (Exception e) {
            failedRecordsCount.incrementAndGet();
            logger.error("记录状态转换失败：Prompt ID: {}, {} → {}, 错误: {}",
                    promptId, fromStatus, toStatus, e.getMessage(), e);

            // 尝试记录失败的转换
            try {
                recordFailedTransition(promptId, fromStatus, toStatus, reason, operatorInfo, e.getMessage());
            } catch (Exception ex) {
                logger.error("记录失败转换也失败了：{}", ex.getMessage());
            }
        }
    }

    /**
     * 记录失败的状态转换
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedTransition(Integer promptId, String fromStatus, String toStatus,
            String reason, String operatorInfo, String errorMessage) {
        try {
            StatusTransitionRecord record = new StatusTransitionRecord();
            record.setPromptId(promptId);
            record.setFromStatus(fromStatus);
            record.setToStatus(toStatus);
            record.setReason(reason);
            record.setOperatorInfo(operatorInfo);
            record.setSuccess(false);
            record.setErrorMessage(errorMessage);
            record.setTransitionTime(LocalDateTime.now());

            repository.save(record);
            totalRecordsCount.incrementAndGet();

            logger.debug("失败转换记录已保存：Prompt ID: {}, {} → {}, 错误: {}",
                    promptId, fromStatus, toStatus, errorMessage);

        } catch (Exception e) {
            logger.error("记录失败转换异常：Prompt ID: {}, 错误: {}", promptId, e.getMessage());
        }
    }

    /**
     * 获取Prompt的转换历史
     * 
     * @param promptId Prompt ID
     * @return 转换历史记录列表
     */
    public List<StatusTransitionRecord> getTransitionHistory(Integer promptId) {
        try {
            return repository.findByPromptIdOrderByTransitionTimeDesc(promptId);
        } catch (Exception e) {
            logger.error("查询转换历史失败：Prompt ID: {}, 错误: {}", promptId, e.getMessage());
            return List.of(); // 返回空列表而不是null
        }
    }

    /**
     * 获取指定时间范围内的转换历史
     * 
     * @param promptId  Prompt ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 转换历史记录列表
     */
    public List<StatusTransitionRecord> getTransitionHistory(Integer promptId, LocalDateTime startTime,
            LocalDateTime endTime) {
        try {
            return repository.findByPromptIdAndTransitionTimeBetweenOrderByTransitionTimeDesc(promptId, startTime,
                    endTime);
        } catch (Exception e) {
            logger.error("查询时间范围转换历史失败：Prompt ID: {}, 错误: {}", promptId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取最新的转换记录
     * 
     * @param promptId Prompt ID
     * @return 最新的转换记录，如果没有则返回null
     */
    public StatusTransitionRecord getLatestTransition(Integer promptId) {
        try {
            List<StatusTransitionRecord> records = repository.findLatestByPromptId(promptId);
            return records.isEmpty() ? null : records.get(0);
        } catch (Exception e) {
            logger.error("查询最新转换记录失败：Prompt ID: {}, 错误: {}", promptId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取状态转换统计信息
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计信息Map
     */
    public Map<String, Object> getTransitionStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 总转换次数
            Long totalTransitions = repository.countTransitionsBetween(startTime, endTime);
            statistics.put("totalTransitions", totalTransitions);

            // 成功转换次数
            Long successfulTransitions = repository.countTransitionsBySuccessAndTimeBetween(true, startTime, endTime);
            statistics.put("successfulTransitions", successfulTransitions);

            // 失败转换次数
            Long failedTransitions = repository.countTransitionsBySuccessAndTimeBetween(false, startTime, endTime);
            statistics.put("failedTransitions", failedTransitions);

            // 成功率
            double successRate = totalTransitions > 0 ? (double) successfulTransitions / totalTransitions * 100 : 0.0;
            statistics.put("successRate", Math.round(successRate * 100.0) / 100.0);

            // 运行时统计
            statistics.put("totalRecordsInMemory", totalRecordsCount.get());
            statistics.put("successfulRecordsInMemory", successfulRecordsCount.get());
            statistics.put("failedRecordsInMemory", failedRecordsCount.get());

        } catch (Exception e) {
            logger.error("获取转换统计信息失败：{}", e.getMessage());
            statistics.put("error", "获取统计信息失败: " + e.getMessage());
        }

        return statistics;
    }

    /**
     * 获取平均转换耗时
     * 
     * @param fromStatus 源状态
     * @param toStatus   目标状态
     * @return 平均耗时（毫秒），如果没有数据则返回null
     */
    public Double getAverageTransitionDuration(String fromStatus, String toStatus) {
        try {
            return repository.getAverageTransitionDuration(fromStatus, toStatus);
        } catch (Exception e) {
            logger.error("获取平均转换耗时失败：{} → {}, 错误: {}", fromStatus, toStatus, e.getMessage());
            return null;
        }
    }

    /**
     * 获取常见的状态转换路径
     * 
     * @return 转换路径统计Map
     */
    public Map<String, Object> getCommonTransitionPaths() {
        Map<String, Object> paths = new HashMap<>();

        try {
            // 这里可以实现更复杂的统计逻辑
            // 目前返回基本的转换模式
            paths.put("待处理 → SUBMISSION_STARTED", getTransitionCount("待处理", "SUBMISSION_STARTED"));
            paths.put("SUBMISSION_STARTED → 已提交", getTransitionCount("SUBMISSION_STARTED", "已提交"));
            paths.put("已提交 → 已完成", getTransitionCount("已提交", "已完成"));
            paths.put("已提交 → 执行失败", getTransitionCount("已提交", "执行失败"));
            paths.put("执行失败 → 待处理", getTransitionCount("执行失败", "待处理"));

        } catch (Exception e) {
            logger.error("获取常见转换路径失败：{}", e.getMessage());
            paths.put("error", "获取转换路径失败: " + e.getMessage());
        }

        return paths;
    }

    /**
     * 获取特定转换的次数
     */
    private Long getTransitionCount(String fromStatus, String toStatus) {
        try {
            List<StatusTransitionRecord> records = repository
                    .findByFromStatusAndToStatusOrderByTransitionTimeDesc(fromStatus, toStatus);
            return (long) records.size();
        } catch (Exception e) {
            logger.error("获取转换次数失败：{} → {}, 错误: {}", fromStatus, toStatus, e.getMessage());
            return 0L;
        }
    }

    /**
     * 清理过期的历史记录
     * 删除指定时间之前的记录以节省存储空间
     * 
     * @param beforeTime 时间界限
     * @return 删除的记录数
     */
    @Transactional
    public Long cleanupHistoryBefore(LocalDateTime beforeTime) {
        try {
            Long deletedCount = repository.deleteByTransitionTimeBefore(beforeTime);
            logger.info("清理历史记录完成：删除了 {} 条记录，时间界限: {}", deletedCount, beforeTime);
            return deletedCount;
        } catch (Exception e) {
            logger.error("清理历史记录失败：{}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取失败的转换记录
     * 用于故障排查和分析
     * 
     * @return 失败的转换记录列表
     */
    public List<StatusTransitionRecord> getFailedTransitions() {
        try {
            return repository.findBySuccessOrderByTransitionTimeDesc(false);
        } catch (Exception e) {
            logger.error("获取失败转换记录失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取指定操作者的转换记录
     * 用于操作审计
     * 
     * @param operatorInfo 操作者信息
     * @return 转换记录列表
     */
    public List<StatusTransitionRecord> getTransitionsByOperator(String operatorInfo) {
        try {
            return repository.findByOperatorInfoOrderByTransitionTimeDesc(operatorInfo);
        } catch (Exception e) {
            logger.error("获取操作者转换记录失败：操作者: {}, 错误: {}", operatorInfo, e.getMessage());
            return List.of();
        }
    }

    /**
     * 重置内存统计计数器
     * 用于测试或管理目的
     */
    public void resetCounters() {
        totalRecordsCount.set(0);
        successfulRecordsCount.set(0);
        failedRecordsCount.set(0);
        logger.info("内存统计计数器已重置");
    }
}
