package com.example.medaiassistant.service.audit;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审计日志服务
 * 负责记录DRGs分析过程中的所有操作日志
 * 
 * 主要功能：
 * 1. 记录快照生成操作
 * 2. 记录Prompt保存操作
 * 3. 记录用户决策操作
 * 4. 记录盈亏计算操作
 * 5. 审计日志查询和导出
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@Service
public class AuditLogService {

    // 审计日志存储
    private final List<AuditLogEntry> auditLogs = new CopyOnWriteArrayList<>();

    /**
     * 记录快照生成操作
     * 
     * @param userId 用户ID
     * @param patientId 患者ID
     * @param success 是否成功
     * @param details 操作详情
     */
    public void logSnapshotGeneration(String userId, String patientId, boolean success, String details) {
        AuditLogEntry logEntry = buildAuditLogEntry("SNAPSHOT_GENERATION", userId, patientId, success, details);
        auditLogs.add(logEntry);
    }

    /**
     * 记录Prompt保存操作
     * 
     * @param userId 用户ID
     * @param promptId Prompt ID
     * @param success 是否成功
     * @param details 操作详情
     */
    public void logPromptSave(String userId, String promptId, boolean success, String details) {
        AuditLogEntry logEntry = buildAuditLogEntry("PROMPT_SAVE", userId, promptId, success, details);
        auditLogs.add(logEntry);
    }

    /**
     * 记录用户决策操作
     * 
     * @param userId 用户ID
     * @param patientId 患者ID
     * @param mccType MCC类型
     * @param success 是否成功
     * @param details 操作详情
     */
    public void logUserDecision(String userId, String patientId, String mccType, boolean success, String details) {
        String fullDetails = details + " - MCC类型: " + mccType;
        AuditLogEntry logEntry = buildAuditLogEntry("USER_DECISION", userId, patientId, success, fullDetails);
        auditLogs.add(logEntry);
    }

    /**
     * 记录盈亏计算操作
     * 
     * @param userId 用户ID
     * @param patientId 患者ID
     * @param profitLossAmount 盈亏金额
     * @param success 是否成功
     * @param details 操作详情
     */
    public void logProfitLossCalculation(String userId, String patientId, double profitLossAmount, boolean success, String details) {
        String fullDetails = details + " - 盈亏金额: " + profitLossAmount;
        AuditLogEntry logEntry = buildAuditLogEntry("PROFIT_LOSS_CALCULATION", userId, patientId, success, fullDetails);
        auditLogs.add(logEntry);
    }

    /**
     * 查询审计日志
     * 
     * @param userId 用户ID（可选）
     * @param operationType 操作类型（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 审计日志列表
     */
    public List<AuditLogEntry> queryAuditLogs(String userId, String operationType, String startDate, String endDate) {
        List<AuditLogEntry> result = new ArrayList<>();
        
        for (AuditLogEntry log : auditLogs) {
            if (matchesFilter(log, userId, operationType)) {
                // 过滤日期范围（简化实现）
                // 在实际项目中，这里应该解析日期字符串并进行比较
                result.add(log);
            }
        }
        
        return result;
    }

    /**
     * 导出审计日志
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 导出的审计日志数据
     */
    public Map<String, Object> exportAuditLogs(String startDate, String endDate) {
        Map<String, Object> exportData = new ConcurrentHashMap<>();
        
        // 获取符合条件的日志
        List<AuditLogEntry> logs = queryAuditLogs(null, null, startDate, endDate);
        
        // 构建导出数据
        exportData.put("exportTime", LocalDateTime.now().toString());
        exportData.put("startDate", startDate);
        exportData.put("endDate", endDate);
        exportData.put("totalRecords", logs.size());
        exportData.put("logs", logs);
        
        return exportData;
    }

    /**
     * 获取审计日志统计信息
     * 
     * @return 审计日志统计信息
     */
    public Map<String, Object> getAuditStatistics() {
        Map<String, Object> statistics = new ConcurrentHashMap<>();
        
        // 按操作类型统计
        Map<String, Long> operationTypeCounts = new ConcurrentHashMap<>();
        Map<String, Long> operationTypeSuccessCounts = new ConcurrentHashMap<>();
        
        for (AuditLogEntry log : auditLogs) {
            updateOperationStatistics(log, operationTypeCounts, operationTypeSuccessCounts);
        }
        
        // 构建统计信息
        Map<String, Object> operationStats = buildOperationStatistics(operationTypeCounts, operationTypeSuccessCounts);
        
        statistics.put("totalRecords", auditLogs.size());
        statistics.put("operationStatistics", operationStats);
        statistics.put("generatedAt", LocalDateTime.now().toString());
        
        return statistics;
    }

    /**
     * 清空所有审计日志
     */
    public void clearAllAuditLogs() {
        auditLogs.clear();
    }

    /**
     * 获取审计日志总数
     * 
     * @return 审计日志总数
     */
    public int getTotalAuditLogs() {
        return auditLogs.size();
    }

    /**
     * 构建审计日志条目
     * 
     * @param operationType 操作类型
     * @param userId 用户ID
     * @param targetId 目标ID
     * @param success 是否成功
     * @param details 操作详情
     * @return 审计日志条目
     */
    private AuditLogEntry buildAuditLogEntry(String operationType, String userId, 
                                           String targetId, boolean success, String details) {
        return new AuditLogEntry(operationType, userId, targetId, success, details, LocalDateTime.now());
    }

    /**
     * 检查日志条目是否匹配过滤条件
     * 
     * @param log 日志条目
     * @param userId 用户ID过滤条件
     * @param operationType 操作类型过滤条件
     * @return 是否匹配过滤条件
     */
    private boolean matchesFilter(AuditLogEntry log, String userId, String operationType) {
        return (userId == null || userId.isEmpty() || log.getUserId().equals(userId)) &&
               (operationType == null || operationType.isEmpty() || log.getOperationType().equals(operationType));
    }

    /**
     * 更新操作统计信息
     * 
     * @param log 日志条目
     * @param operationTypeCounts 操作类型计数
     * @param operationTypeSuccessCounts 操作类型成功计数
     */
    private void updateOperationStatistics(AuditLogEntry log, 
                                         Map<String, Long> operationTypeCounts,
                                         Map<String, Long> operationTypeSuccessCounts) {
        operationTypeCounts.merge(log.getOperationType(), 1L, Long::sum);
        if (log.isSuccess()) {
            operationTypeSuccessCounts.merge(log.getOperationType(), 1L, Long::sum);
        }
    }

    /**
     * 构建操作统计信息
     * 
     * @param operationTypeCounts 操作类型计数
     * @param operationTypeSuccessCounts 操作类型成功计数
     * @return 操作统计信息
     */
    private Map<String, Object> buildOperationStatistics(Map<String, Long> operationTypeCounts,
                                                        Map<String, Long> operationTypeSuccessCounts) {
        Map<String, Object> operationStats = new ConcurrentHashMap<>();
        for (String operationType : operationTypeCounts.keySet()) {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            long total = operationTypeCounts.get(operationType);
            long success = operationTypeSuccessCounts.getOrDefault(operationType, 0L);
            
            stats.put("total", total);
            stats.put("success", success);
            stats.put("failure", total - success);
            stats.put("successRate", total > 0 ? (double) success / total * 100 : 0.0);
            
            operationStats.put(operationType, stats);
        }
        return operationStats;
    }

    /**
     * 审计日志条目类
     */
    public static class AuditLogEntry {
        private final String operationType;
        private final String userId;
        private final String targetId;
        private final boolean success;
        private final String details;
        private final LocalDateTime timestamp;

        public AuditLogEntry(String operationType, String userId, String targetId, 
                           boolean success, String details, LocalDateTime timestamp) {
            this.operationType = operationType;
            this.userId = userId;
            this.targetId = targetId;
            this.success = success;
            this.details = details;
            this.timestamp = timestamp;
        }

        // Getters
        public String getOperationType() { return operationType; }
        public String getUserId() { return userId; }
        public String getTargetId() { return targetId; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
