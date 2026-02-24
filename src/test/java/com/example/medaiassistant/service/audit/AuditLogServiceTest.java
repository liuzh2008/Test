package com.example.medaiassistant.service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志服务测试
 * 按照TDD红-绿-重构流程实现审计日志记录功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审计日志服务 单元测试")
class AuditLogServiceTest {

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // 创建新的审计服务实例，确保测试独立性
        auditLogService = new AuditLogService();
        // 清空审计日志
        auditLogService.clearAllAuditLogs();
    }

    /**
     * 🔴 红阶段测试：记录快照生成操作
     */
    @Test
    @DisplayName("记录快照生成操作 - 成功场景")
    void logSnapshotGeneration_ShouldRecordAuditLog_WhenSuccess() {
        // Arrange
        String userId = "user123";
        String patientId = "patient456";
        boolean success = true;
        String details = "快照生成完成";

        // Act
        auditLogService.logSnapshotGeneration(userId, patientId, success, details);

        // Assert
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertEquals(1, logs.size(), "应记录1条审计日志");

        AuditLogService.AuditLogEntry log = logs.get(0);
        assertEquals("SNAPSHOT_GENERATION", log.getOperationType(), "操作类型应为SNAPSHOT_GENERATION");
        assertEquals(userId, log.getUserId(), "用户ID应匹配");
        assertEquals(patientId, log.getTargetId(), "患者ID应匹配");
        assertTrue(log.isSuccess(), "操作应成功");
        assertEquals(details, log.getDetails(), "操作详情应匹配");
        assertNotNull(log.getTimestamp(), "时间戳不应为null");
    }

    /**
     * 🔴 红阶段测试：记录快照生成操作 - 失败场景
     */
    @Test
    @DisplayName("记录快照生成操作 - 失败场景")
    void logSnapshotGeneration_ShouldRecordAuditLog_WhenFailure() {
        // Arrange
        String userId = "user123";
        String patientId = "patient456";
        boolean success = false;
        String details = "快照生成失败：数据格式错误";

        // Act
        auditLogService.logSnapshotGeneration(userId, patientId, success, details);

        // Assert
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertEquals(1, logs.size(), "应记录1条审计日志");

        AuditLogService.AuditLogEntry log = logs.get(0);
        assertEquals("SNAPSHOT_GENERATION", log.getOperationType(), "操作类型应为SNAPSHOT_GENERATION");
        assertEquals(userId, log.getUserId(), "用户ID应匹配");
        assertEquals(patientId, log.getTargetId(), "患者ID应匹配");
        assertFalse(log.isSuccess(), "操作应失败");
        assertEquals(details, log.getDetails(), "操作详情应匹配");
    }

    /**
     * 🔴 红阶段测试：记录Prompt保存操作
     */
    @Test
    @DisplayName("记录Prompt保存操作 - 成功场景")
    void logPromptSave_ShouldRecordAuditLog_WhenSuccess() {
        // Arrange
        String userId = "user123";
        String promptId = "prompt789";
        boolean success = true;
        String details = "Prompt保存成功";

        // Act
        auditLogService.logPromptSave(userId, promptId, success, details);

        // Assert
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertEquals(1, logs.size(), "应记录1条审计日志");

        AuditLogService.AuditLogEntry log = logs.get(0);
        assertEquals("PROMPT_SAVE", log.getOperationType(), "操作类型应为PROMPT_SAVE");
        assertEquals(userId, log.getUserId(), "用户ID应匹配");
        assertEquals(promptId, log.getTargetId(), "Prompt ID应匹配");
        assertTrue(log.isSuccess(), "操作应成功");
        assertEquals(details, log.getDetails(), "操作详情应匹配");
    }

    /**
     * 🔴 红阶段测试：记录用户决策操作
     */
    @Test
    @DisplayName("记录用户决策操作 - 成功场景")
    void logUserDecision_ShouldRecordAuditLog_WhenSuccess() {
        // Arrange
        String userId = "user123";
        String patientId = "patient456";
        String mccType = "MCC_001";
        boolean success = true;
        String details = "用户选择MCC类型";

        // Act
        auditLogService.logUserDecision(userId, patientId, mccType, success, details);

        // Assert
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertEquals(1, logs.size(), "应记录1条审计日志");

        AuditLogService.AuditLogEntry log = logs.get(0);
        assertEquals("USER_DECISION", log.getOperationType(), "操作类型应为USER_DECISION");
        assertEquals(userId, log.getUserId(), "用户ID应匹配");
        assertEquals(patientId, log.getTargetId(), "患者ID应匹配");
        assertTrue(log.isSuccess(), "操作应成功");
        assertTrue(log.getDetails().contains(mccType), "操作详情应包含MCC类型");
        assertTrue(log.getDetails().contains(details), "操作详情应包含原始详情");
    }

    /**
     * 🔴 红阶段测试：记录盈亏计算操作
     */
    @Test
    @DisplayName("记录盈亏计算操作 - 成功场景")
    void logProfitLossCalculation_ShouldRecordAuditLog_WhenSuccess() {
        // Arrange
        String userId = "user123";
        String patientId = "patient456";
        double profitLossAmount = 1250.50;
        boolean success = true;
        String details = "盈亏计算完成";

        // Act
        auditLogService.logProfitLossCalculation(userId, patientId, profitLossAmount, success, details);

        // Assert
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertEquals(1, logs.size(), "应记录1条审计日志");

        AuditLogService.AuditLogEntry log = logs.get(0);
        assertEquals("PROFIT_LOSS_CALCULATION", log.getOperationType(), "操作类型应为PROFIT_LOSS_CALCULATION");
        assertEquals(userId, log.getUserId(), "用户ID应匹配");
        assertEquals(patientId, log.getTargetId(), "患者ID应匹配");
        assertTrue(log.isSuccess(), "操作应成功");
        assertTrue(log.getDetails().contains(String.valueOf(profitLossAmount)), "操作详情应包含盈亏金额");
        assertTrue(log.getDetails().contains(details), "操作详情应包含原始详情");
    }

    /**
     * 🔴 红阶段测试：查询审计日志 - 按用户ID过滤
     */
    @Test
    @DisplayName("查询审计日志 - 按用户ID过滤")
    void queryAuditLogs_ShouldFilterByUserId() {
        // Arrange
        String user1 = "user123";
        String user2 = "user456";
        
        auditLogService.logSnapshotGeneration(user1, "patient1", true, "操作1");
        auditLogService.logPromptSave(user2, "prompt1", true, "操作2");
        auditLogService.logUserDecision(user1, "patient2", "MCC_001", true, "操作3");

        // Act
        List<AuditLogService.AuditLogEntry> user1Logs = auditLogService.queryAuditLogs(user1, null, null, null);
        List<AuditLogService.AuditLogEntry> user2Logs = auditLogService.queryAuditLogs(user2, null, null, null);

        // Assert
        assertEquals(2, user1Logs.size(), "用户1应有2条日志");
        assertEquals(1, user2Logs.size(), "用户2应有1条日志");
        
        // 验证用户1的日志都包含正确的用户ID
        for (AuditLogService.AuditLogEntry log : user1Logs) {
            assertEquals(user1, log.getUserId(), "日志用户ID应为user1");
        }
        
        // 验证用户2的日志包含正确的用户ID
        for (AuditLogService.AuditLogEntry log : user2Logs) {
            assertEquals(user2, log.getUserId(), "日志用户ID应为user2");
        }
    }

    /**
     * 🔴 红阶段测试：查询审计日志 - 按操作类型过滤
     */
    @Test
    @DisplayName("查询审计日志 - 按操作类型过滤")
    void queryAuditLogs_ShouldFilterByOperationType() {
        // Arrange
        auditLogService.logSnapshotGeneration("user1", "patient1", true, "快照生成1");
        auditLogService.logSnapshotGeneration("user2", "patient2", true, "快照生成2");
        auditLogService.logPromptSave("user1", "prompt1", true, "Prompt保存1");
        auditLogService.logUserDecision("user3", "patient3", "MCC_001", true, "用户决策1");

        // Act
        List<AuditLogService.AuditLogEntry> snapshotLogs = auditLogService.queryAuditLogs(null, "SNAPSHOT_GENERATION", null, null);
        List<AuditLogService.AuditLogEntry> promptLogs = auditLogService.queryAuditLogs(null, "PROMPT_SAVE", null, null);
        List<AuditLogService.AuditLogEntry> decisionLogs = auditLogService.queryAuditLogs(null, "USER_DECISION", null, null);

        // Assert
        assertEquals(2, snapshotLogs.size(), "快照生成操作应有2条日志");
        assertEquals(1, promptLogs.size(), "Prompt保存操作应有1条日志");
        assertEquals(1, decisionLogs.size(), "用户决策操作应有1条日志");
        
        // 验证操作类型正确性
        for (AuditLogService.AuditLogEntry log : snapshotLogs) {
            assertEquals("SNAPSHOT_GENERATION", log.getOperationType(), "操作类型应为SNAPSHOT_GENERATION");
        }
        for (AuditLogService.AuditLogEntry log : promptLogs) {
            assertEquals("PROMPT_SAVE", log.getOperationType(), "操作类型应为PROMPT_SAVE");
        }
        for (AuditLogService.AuditLogEntry log : decisionLogs) {
            assertEquals("USER_DECISION", log.getOperationType(), "操作类型应为USER_DECISION");
        }
    }

    /**
     * 🔴 红阶段测试：获取审计统计信息
     */
    @Test
    @DisplayName("获取审计统计信息 - 综合测试")
    void getAuditStatistics_ShouldReturnCorrectStatistics() {
        // Arrange
        // 快照生成：2次成功，1次失败
        auditLogService.logSnapshotGeneration("user1", "patient1", true, "成功1");
        auditLogService.logSnapshotGeneration("user2", "patient2", true, "成功2");
        auditLogService.logSnapshotGeneration("user3", "patient3", false, "失败1");
        
        // Prompt保存：1次成功
        auditLogService.logPromptSave("user1", "prompt1", true, "成功");
        
        // 用户决策：1次成功
        auditLogService.logUserDecision("user2", "patient4", "MCC_001", true, "成功");

        // Act
        Map<String, Object> statistics = auditLogService.getAuditStatistics();

        // Assert
        assertNotNull(statistics, "统计信息不应为null");
        assertEquals(5, statistics.get("totalRecords"), "总记录数应为5");
        assertNotNull(statistics.get("generatedAt"), "应包含生成时间");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> operationStats = (Map<String, Object>) statistics.get("operationStatistics");
        assertNotNull(operationStats, "操作统计不应为null");
        
        // 验证快照生成统计
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotStats = (Map<String, Object>) operationStats.get("SNAPSHOT_GENERATION");
        assertNotNull(snapshotStats, "快照生成统计不应为null");
        assertEquals(3L, snapshotStats.get("total"), "快照生成总次数应为3");
        assertEquals(2L, snapshotStats.get("success"), "快照生成成功次数应为2");
        assertEquals(1L, snapshotStats.get("failure"), "快照生成失败次数应为1");
        assertEquals(66.67, (Double) snapshotStats.get("successRate"), 0.01, "快照生成成功率应为66.67%");
        
        // 验证Prompt保存统计
        @SuppressWarnings("unchecked")
        Map<String, Object> promptStats = (Map<String, Object>) operationStats.get("PROMPT_SAVE");
        assertNotNull(promptStats, "Prompt保存统计不应为null");
        assertEquals(1L, promptStats.get("total"), "Prompt保存总次数应为1");
        assertEquals(1L, promptStats.get("success"), "Prompt保存成功次数应为1");
        assertEquals(0L, promptStats.get("failure"), "Prompt保存失败次数应为0");
        assertEquals(100.0, (Double) promptStats.get("successRate"), 0.01, "Prompt保存成功率应为100%");
    }

    /**
     * 🔴 红阶段测试：导出审计日志
     */
    @Test
    @DisplayName("导出审计日志 - 功能测试")
    void exportAuditLogs_ShouldReturnExportData() {
        // Arrange
        auditLogService.logSnapshotGeneration("user1", "patient1", true, "操作1");
        auditLogService.logPromptSave("user2", "prompt1", true, "操作2");
        
        String startDate = "2025-11-01";
        String endDate = "2025-11-13";

        // Act
        Map<String, Object> exportData = auditLogService.exportAuditLogs(startDate, endDate);

        // Assert
        assertNotNull(exportData, "导出数据不应为null");
        assertEquals(startDate, exportData.get("startDate"), "开始日期应匹配");
        assertEquals(endDate, exportData.get("endDate"), "结束日期应匹配");
        assertEquals(2, exportData.get("totalRecords"), "总记录数应为2");
        assertNotNull(exportData.get("exportTime"), "应包含导出时间");
        
        @SuppressWarnings("unchecked")
        List<AuditLogService.AuditLogEntry> logs = (List<AuditLogService.AuditLogEntry>) exportData.get("logs");
        assertNotNull(logs, "导出的日志列表不应为null");
        assertEquals(2, logs.size(), "导出的日志数量应为2");
    }

    /**
     * 🔴 红阶段测试：清空审计日志
     */
    @Test
    @DisplayName("清空审计日志 - 功能测试")
    void clearAllAuditLogs_ShouldRemoveAllLogs() {
        // Arrange
        auditLogService.logSnapshotGeneration("user1", "patient1", true, "操作1");
        auditLogService.logPromptSave("user2", "prompt1", true, "操作2");
        assertEquals(2, auditLogService.getTotalAuditLogs(), "初始应有2条日志");

        // Act
        auditLogService.clearAllAuditLogs();

        // Assert
        assertEquals(0, auditLogService.getTotalAuditLogs(), "清空后应无日志");
        List<AuditLogService.AuditLogEntry> logs = auditLogService.queryAuditLogs(null, null, null, null);
        assertTrue(logs.isEmpty(), "查询结果应为空");
    }
}
