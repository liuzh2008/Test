package com.example.medaiassistant.service.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DRGs监控服务测试
 * 按照TDD红-绿-重构流程实现指标采集功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DRGs监控服务 单元测试")
class DrgMonitoringServiceTest {

    private DrgMonitoringService drgMonitoringService;

    @BeforeEach
    void setUp() {
        // 创建新的监控服务实例，确保测试独立性
        drgMonitoringService = new DrgMonitoringService();
        // 重置监控指标
        drgMonitoringService.resetAllMetrics();
    }

    /**
     * 🔴 红阶段测试：快照生成指标采集测试
     * 测试快照生成指标记录功能
     */
    @Test
    @DisplayName("记录快照生成指标 - 成功场景")
    void recordSnapshotGeneration_ShouldRecordMetrics_WhenSuccess() {
        // Arrange
        long durationMillis = 150L;
        boolean success = true;

        // Act
        drgMonitoringService.recordSnapshotGeneration(durationMillis, success);

        // Assert
        Map<String, Object> metrics = drgMonitoringService.getSnapshotGenerationMetrics();
        assertNotNull(metrics, "快照生成指标不应为null");
        assertEquals(1L, metrics.get("total"), "总次数应为1");
        assertEquals(1L, metrics.get("success"), "成功次数应为1");
        assertEquals(0L, metrics.get("failure"), "失败次数应为0");
        assertEquals(100.0, (Double) metrics.get("successRate"), 0.01, "成功率应为100%");
        assertEquals(150.0, (Double) metrics.get("averageDuration"), 0.01, "平均耗时应为150ms");
    }

    /**
     * 🔴 红阶段测试：快照生成失败场景
     */
    @Test
    @DisplayName("记录快照生成指标 - 失败场景")
    void recordSnapshotGeneration_ShouldRecordMetrics_WhenFailure() {
        // Arrange
        long durationMillis = 200L;
        boolean success = false;

        // Act
        drgMonitoringService.recordSnapshotGeneration(durationMillis, success);

        // Assert
        Map<String, Object> metrics = drgMonitoringService.getSnapshotGenerationMetrics();
        assertEquals(1L, metrics.get("total"), "总次数应为1");
        assertEquals(0L, metrics.get("success"), "成功次数应为0");
        assertEquals(1L, metrics.get("failure"), "失败次数应为1");
        assertEquals(0.0, (Double) metrics.get("successRate"), 0.01, "成功率应为0%");
        assertEquals(200.0, (Double) metrics.get("averageDuration"), 0.01, "平均耗时应为200ms");
    }

    /**
     * 🔴 红阶段测试：Prompt保存指标采集测试
     */
    @Test
    @DisplayName("记录Prompt保存指标 - 成功场景")
    void recordPromptSave_ShouldRecordMetrics_WhenSuccess() {
        // Arrange
        long durationMillis = 100L;
        boolean success = true;

        // Act
        drgMonitoringService.recordPromptSave(durationMillis, success);

        // Assert
        Map<String, Object> metrics = drgMonitoringService.getPromptSaveMetrics();
        assertNotNull(metrics, "Prompt保存指标不应为null");
        assertEquals(1L, metrics.get("total"), "总次数应为1");
        assertEquals(1L, metrics.get("success"), "成功次数应为1");
        assertEquals(0L, metrics.get("failure"), "失败次数应为0");
        assertEquals(100.0, (Double) metrics.get("successRate"), 0.01, "成功率应为100%");
        assertEquals(100.0, (Double) metrics.get("averageDuration"), 0.01, "平均耗时应为100ms");
    }

    /**
     * 🔴 红阶段测试：用户决策指标采集测试
     */
    @Test
    @DisplayName("记录用户决策指标 - 成功场景")
    void recordUserDecision_ShouldRecordMetrics_WhenSuccess() {
        // Arrange
        long durationMillis = 50L;
        boolean success = true;

        // Act
        drgMonitoringService.recordUserDecision(durationMillis, success);

        // Assert
        Map<String, Object> metrics = drgMonitoringService.getUserDecisionMetrics();
        assertNotNull(metrics, "用户决策指标不应为null");
        assertEquals(1L, metrics.get("total"), "总次数应为1");
        assertEquals(1L, metrics.get("success"), "成功次数应为1");
        assertEquals(0L, metrics.get("failure"), "失败次数应为0");
        assertEquals(100.0, (Double) metrics.get("successRate"), 0.01, "成功率应为100%");
        assertEquals(50.0, (Double) metrics.get("averageDuration"), 0.01, "平均耗时应为50ms");
    }

    /**
     * 🔴 红阶段测试：盈亏计算指标采集测试
     */
    @Test
    @DisplayName("记录盈亏计算指标 - 成功场景")
    void recordProfitLossCalculation_ShouldRecordMetrics_WhenSuccess() {
        // Arrange
        long durationMillis = 300L;
        boolean success = true;

        // Act
        drgMonitoringService.recordProfitLossCalculation(durationMillis, success);

        // Assert
        Map<String, Object> metrics = drgMonitoringService.getProfitLossCalculationMetrics();
        assertNotNull(metrics, "盈亏计算指标不应为null");
        assertEquals(1L, metrics.get("total"), "总次数应为1");
        assertEquals(1L, metrics.get("success"), "成功次数应为1");
        assertEquals(0L, metrics.get("failure"), "失败次数应为0");
        assertEquals(100.0, (Double) metrics.get("successRate"), 0.01, "成功率应为100%");
        assertEquals(300.0, (Double) metrics.get("averageDuration"), 0.01, "平均耗时应为300ms");
    }

    /**
     * 🔴 红阶段测试：多个操作指标统计测试
     */
    @Test
    @DisplayName("多个操作指标统计 - 综合场景")
    void multipleOperations_ShouldAggregateMetricsCorrectly() {
        // Arrange & Act
        // 快照生成：2次成功，1次失败
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        drgMonitoringService.recordSnapshotGeneration(150L, true);
        drgMonitoringService.recordSnapshotGeneration(200L, false);

        // Prompt保存：1次成功
        drgMonitoringService.recordPromptSave(80L, true);

        // 用户决策：3次成功
        drgMonitoringService.recordUserDecision(40L, true);
        drgMonitoringService.recordUserDecision(60L, true);
        drgMonitoringService.recordUserDecision(50L, true);

        // 盈亏计算：1次成功，1次失败
        drgMonitoringService.recordProfitLossCalculation(250L, true);
        drgMonitoringService.recordProfitLossCalculation(350L, false);

        // Assert - 快照生成指标
        Map<String, Object> snapshotMetrics = drgMonitoringService.getSnapshotGenerationMetrics();
        assertEquals(3L, snapshotMetrics.get("total"), "快照生成总次数应为3");
        assertEquals(2L, snapshotMetrics.get("success"), "快照生成成功次数应为2");
        assertEquals(1L, snapshotMetrics.get("failure"), "快照生成失败次数应为1");
        assertEquals(66.67, (Double) snapshotMetrics.get("successRate"), 0.01, "快照生成成功率应为66.67%");
        assertEquals(150.0, (Double) snapshotMetrics.get("averageDuration"), 0.01, "快照生成平均耗时应为150ms");

        // Assert - Prompt保存指标
        Map<String, Object> promptMetrics = drgMonitoringService.getPromptSaveMetrics();
        assertEquals(1L, promptMetrics.get("total"), "Prompt保存总次数应为1");
        assertEquals(1L, promptMetrics.get("success"), "Prompt保存成功次数应为1");
        assertEquals(0L, promptMetrics.get("failure"), "Prompt保存失败次数应为0");

        // Assert - 用户决策指标
        Map<String, Object> decisionMetrics = drgMonitoringService.getUserDecisionMetrics();
        assertEquals(3L, decisionMetrics.get("total"), "用户决策总次数应为3");
        assertEquals(3L, decisionMetrics.get("success"), "用户决策成功次数应为3");
        assertEquals(0L, decisionMetrics.get("failure"), "用户决策失败次数应为0");

        // Assert - 盈亏计算指标
        Map<String, Object> calculationMetrics = drgMonitoringService.getProfitLossCalculationMetrics();
        assertEquals(2L, calculationMetrics.get("total"), "盈亏计算总次数应为2");
        assertEquals(1L, calculationMetrics.get("success"), "盈亏计算成功次数应为1");
        assertEquals(1L, calculationMetrics.get("failure"), "盈亏计算失败次数应为1");
    }

    /**
     * 🔴 红阶段测试：获取所有监控指标测试
     */
    @Test
    @DisplayName("获取所有监控指标 - 综合测试")
    void getMonitoringMetrics_ShouldReturnAllMetrics() {
        // Arrange
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        drgMonitoringService.recordPromptSave(80L, true);
        drgMonitoringService.recordUserDecision(50L, true);
        drgMonitoringService.recordProfitLossCalculation(200L, true);

        // Act
        Map<String, Object> allMetrics = drgMonitoringService.getMonitoringMetrics();

        // Assert
        assertNotNull(allMetrics, "所有监控指标不应为null");
        assertTrue(allMetrics.containsKey("snapshotGeneration"), "应包含快照生成指标");
        assertTrue(allMetrics.containsKey("promptSave"), "应包含Prompt保存指标");
        assertTrue(allMetrics.containsKey("userDecision"), "应包含用户决策指标");
        assertTrue(allMetrics.containsKey("profitLossCalculation"), "应包含盈亏计算指标");

        // 验证每个指标的结构
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotMetrics = (Map<String, Object>) allMetrics.get("snapshotGeneration");
        assertNotNull(snapshotMetrics.get("total"), "快照生成指标应包含总次数");
        assertNotNull(snapshotMetrics.get("success"), "快照生成指标应包含成功次数");
        assertNotNull(snapshotMetrics.get("failure"), "快照生成指标应包含失败次数");
        assertNotNull(snapshotMetrics.get("successRate"), "快照生成指标应包含成功率");
        assertNotNull(snapshotMetrics.get("averageDuration"), "快照生成指标应包含平均耗时");
    }

    /**
     * 🔴 红阶段测试：重置所有指标测试
     */
    @Test
    @DisplayName("重置所有监控指标 - 功能测试")
    void resetAllMetrics_ShouldClearAllMetrics() {
        // Arrange - 记录一些指标
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        drgMonitoringService.recordPromptSave(80L, true);
        drgMonitoringService.recordUserDecision(50L, true);
        drgMonitoringService.recordProfitLossCalculation(200L, true);

        // Act - 重置指标
        drgMonitoringService.resetAllMetrics();

        // Assert - 验证所有指标已重置
        Map<String, Object> snapshotMetrics = drgMonitoringService.getSnapshotGenerationMetrics();
        assertEquals(0L, snapshotMetrics.get("total"), "快照生成总次数应为0");
        assertEquals(0L, snapshotMetrics.get("success"), "快照生成成功次数应为0");
        assertEquals(0L, snapshotMetrics.get("failure"), "快照生成失败次数应为0");

        Map<String, Object> promptMetrics = drgMonitoringService.getPromptSaveMetrics();
        assertEquals(0L, promptMetrics.get("total"), "Prompt保存总次数应为0");

        Map<String, Object> decisionMetrics = drgMonitoringService.getUserDecisionMetrics();
        assertEquals(0L, decisionMetrics.get("total"), "用户决策总次数应为0");

        Map<String, Object> calculationMetrics = drgMonitoringService.getProfitLossCalculationMetrics();
        assertEquals(0L, calculationMetrics.get("total"), "盈亏计算总次数应为0");
    }
}
