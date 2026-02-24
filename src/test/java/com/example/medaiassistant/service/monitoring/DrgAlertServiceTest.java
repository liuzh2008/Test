package com.example.medaiassistant.service.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DRGs告警服务测试
 * 按照TDD红-绿-重构流程实现告警机制
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DRGs告警服务 单元测试")
class DrgAlertServiceTest {

    private DrgAlertService drgAlertService;
    private DrgMonitoringService drgMonitoringService;

    @BeforeEach
    void setUp() {
        // 创建新的监控服务实例
        drgMonitoringService = new DrgMonitoringService();
        // 创建新的告警服务实例
        drgAlertService = new DrgAlertService(drgMonitoringService);
        // 重置监控指标
        drgMonitoringService.resetAllMetrics();
        // 重置告警配置
        drgAlertService.resetAlertConfigurations();
    }

    /**
     * 🔴 红阶段测试：配置快照生成失败率告警阈值
     */
    @Test
    @DisplayName("配置快照生成失败率告警阈值 - 功能测试")
    void configureSnapshotFailureRateAlert_ShouldSetThreshold() {
        // Arrange
        double threshold = 5.0; // 5%失败率阈值

        // Act
        drgAlertService.configureSnapshotFailureRateAlert(threshold);

        // Assert
        Map<String, Object> config = drgAlertService.getAlertConfigurations();
        assertNotNull(config, "告警配置不应为null");
        assertTrue(config.containsKey("snapshotFailureRate"), "应包含快照生成失败率配置");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotConfig = (Map<String, Object>) config.get("snapshotFailureRate");
        assertEquals(threshold, snapshotConfig.get("threshold"), "阈值应匹配");
        assertEquals("PERCENTAGE", snapshotConfig.get("unit"), "单位应为百分比");
    }

    /**
     * 🔴 红阶段测试：检查快照生成失败率告警 - 触发告警场景
     */
    @Test
    @DisplayName("检查快照生成失败率告警 - 触发告警场景")
    void checkSnapshotFailureRateAlert_ShouldTriggerAlert_WhenThresholdExceeded() {
        // Arrange
        double threshold = 5.0; // 5%失败率阈值
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 模拟失败率超过阈值：10次操作，1次成功，9次失败（90%失败率）
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, false);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, true);

        // Act
        List<Alert> alerts = drgAlertService.checkAllAlerts();

        // Assert
        assertFalse(alerts.isEmpty(), "应触发告警");
        assertEquals(1, alerts.size(), "应触发1个告警");
        
        Alert alert = alerts.get(0);
        assertEquals("SNAPSHOT_FAILURE_RATE", alert.getAlertType(), "告警类型应为快照生成失败率");
        assertEquals("CRITICAL", alert.getSeverity(), "告警级别应为严重");
        assertTrue(alert.getMessage().contains("90.0"), "告警信息应包含失败率");
        assertTrue(alert.isActive(), "告警应处于活动状态");
    }

    /**
     * 🔴 红阶段测试：检查快照生成失败率告警 - 未触发告警场景
     */
    @Test
    @DisplayName("检查快照生成失败率告警 - 未触发告警场景")
    void checkSnapshotFailureRateAlert_ShouldNotTriggerAlert_WhenThresholdNotExceeded() {
        // Arrange
        double threshold = 15.0; // 15%失败率阈值
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 模拟失败率未超过阈值：10次操作，9次成功，1次失败（10%失败率）
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, true);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, false);

        // Act
        List<Alert> alerts = drgAlertService.checkAllAlerts();

        // Assert
        assertTrue(alerts.isEmpty(), "不应触发告警");
    }

    /**
     * 🔴 红阶段测试：配置Prompt保存超时告警阈值
     */
    @Test
    @DisplayName("配置Prompt保存超时告警阈值 - 功能测试")
    void configurePromptSaveTimeoutAlert_ShouldSetThreshold() {
        // Arrange
        long timeoutMillis = 30000L; // 30秒超时阈值

        // Act
        drgAlertService.configurePromptSaveTimeoutAlert(timeoutMillis);

        // Assert
        Map<String, Object> config = drgAlertService.getAlertConfigurations();
        assertNotNull(config, "告警配置不应为null");
        assertTrue(config.containsKey("promptSaveTimeout"), "应包含Prompt保存超时配置");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> promptConfig = (Map<String, Object>) config.get("promptSaveTimeout");
        assertEquals(timeoutMillis, promptConfig.get("threshold"), "阈值应匹配");
        assertEquals("MILLISECONDS", promptConfig.get("unit"), "单位应为毫秒");
    }

    /**
     * 🔴 红阶段测试：配置用户决策响应时间告警阈值
     */
    @Test
    @DisplayName("配置用户决策响应时间告警阈值 - 功能测试")
    void configureUserDecisionResponseTimeAlert_ShouldSetThreshold() {
        // Arrange
        long responseTimeMillis = 5000L; // 5秒响应时间阈值

        // Act
        drgAlertService.configureUserDecisionResponseTimeAlert(responseTimeMillis);

        // Assert
        Map<String, Object> config = drgAlertService.getAlertConfigurations();
        assertNotNull(config, "告警配置不应为null");
        assertTrue(config.containsKey("userDecisionResponseTime"), "应包含用户决策响应时间配置");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> decisionConfig = (Map<String, Object>) config.get("userDecisionResponseTime");
        assertEquals(responseTimeMillis, decisionConfig.get("threshold"), "阈值应匹配");
        assertEquals("MILLISECONDS", decisionConfig.get("unit"), "单位应为毫秒");
    }

    /**
     * 🔴 红阶段测试：配置盈亏计算错误率告警阈值
     */
    @Test
    @DisplayName("配置盈亏计算错误率告警阈值 - 功能测试")
    void configureProfitLossErrorRateAlert_ShouldSetThreshold() {
        // Arrange
        double threshold = 2.0; // 2%错误率阈值

        // Act
        drgAlertService.configureProfitLossErrorRateAlert(threshold);

        // Assert
        Map<String, Object> config = drgAlertService.getAlertConfigurations();
        assertNotNull(config, "告警配置不应为null");
        assertTrue(config.containsKey("profitLossErrorRate"), "应包含盈亏计算错误率配置");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> calculationConfig = (Map<String, Object>) config.get("profitLossErrorRate");
        assertEquals(threshold, calculationConfig.get("threshold"), "阈值应匹配");
        assertEquals("PERCENTAGE", calculationConfig.get("unit"), "单位应为百分比");
    }

    /**
     * 🔴 红阶段测试：获取活动告警列表
     */
    @Test
    @DisplayName("获取活动告警列表 - 功能测试")
    void getActiveAlerts_ShouldReturnActiveAlerts() {
        // Arrange
        double threshold = 5.0;
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 模拟触发告警
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, false);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        
        // 触发告警检查
        drgAlertService.checkAllAlerts();

        // Act
        List<Alert> activeAlerts = drgAlertService.getActiveAlerts();

        // Assert
        assertFalse(activeAlerts.isEmpty(), "应有活动告警");
        assertEquals(1, activeAlerts.size(), "应有1个活动告警");
        
        Alert alert = activeAlerts.get(0);
        assertTrue(alert.isActive(), "告警应处于活动状态");
        assertEquals("SNAPSHOT_FAILURE_RATE", alert.getAlertType(), "告警类型应匹配");
    }

    /**
     * 🔴 红阶段测试：解决告警
     */
    @Test
    @DisplayName("解决告警 - 功能测试")
    void resolveAlert_ShouldDeactivateAlert() {
        // Arrange
        double threshold = 5.0;
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 模拟触发告警
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, false);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        
        List<Alert> alerts = drgAlertService.checkAllAlerts();
        assertEquals(1, alerts.size(), "应触发1个告警");
        Alert alert = alerts.get(0);

        // Act
        drgAlertService.resolveAlert(alert.getId());

        // Assert
        List<Alert> activeAlerts = drgAlertService.getActiveAlerts();
        assertTrue(activeAlerts.isEmpty(), "解决后应无活动告警");
        
        List<Alert> allAlerts = drgAlertService.getAllAlerts();
        Alert resolvedAlert = allAlerts.stream()
            .filter(a -> a.getId().equals(alert.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(resolvedAlert, "告警应存在于历史记录中");
        assertFalse(resolvedAlert.isActive(), "告警应处于非活动状态");
        assertNotNull(resolvedAlert.getResolvedAt(), "应包含解决时间");
    }

    /**
     * 🔴 红阶段测试：告警自动恢复 - 条件改善后自动解决
     */
    @Test
    @DisplayName("告警自动恢复 - 条件改善后自动解决")
    void checkAllAlerts_ShouldAutoResolve_WhenConditionsImprove() {
        // Arrange
        double threshold = 5.0;
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 第一阶段：触发告警（高失败率）
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, false);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        
        List<Alert> initialAlerts = drgAlertService.checkAllAlerts();
        assertEquals(1, initialAlerts.size(), "应触发1个告警");

        // 第二阶段：重置指标，模拟条件改善
        drgMonitoringService.resetAllMetrics();
        
        // 添加成功的操作（低失败率）
        for (int i = 0; i < 10; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, true);
        }

        // Act
        List<Alert> updatedAlerts = drgAlertService.checkAllAlerts();

        // Assert
        assertTrue(updatedAlerts.isEmpty(), "条件改善后应无活动告警");
        
        List<Alert> allAlerts = drgAlertService.getAllAlerts();
        Alert resolvedAlert = allAlerts.stream()
            .filter(a -> a.getAlertType().equals("SNAPSHOT_FAILURE_RATE"))
            .findFirst()
            .orElse(null);
        assertNotNull(resolvedAlert, "告警应存在于历史记录中");
        assertFalse(resolvedAlert.isActive(), "告警应处于非活动状态");
        assertNotNull(resolvedAlert.getResolvedAt(), "应包含解决时间");
    }

    /**
     * 🔴 红阶段测试：获取告警统计信息
     */
    @Test
    @DisplayName("获取告警统计信息 - 功能测试")
    void getAlertStatistics_ShouldReturnCorrectStatistics() {
        // Arrange
        double threshold = 5.0;
        drgAlertService.configureSnapshotFailureRateAlert(threshold);
        
        // 触发多个告警
        for (int i = 0; i < 9; i++) {
            drgMonitoringService.recordSnapshotGeneration(100L, false);
        }
        drgMonitoringService.recordSnapshotGeneration(100L, true);
        
        drgAlertService.checkAllAlerts();
        
        // 解决一个告警
        List<Alert> alerts = drgAlertService.getActiveAlerts();
        drgAlertService.resolveAlert(alerts.get(0).getId());

        // Act
        Map<String, Object> statistics = drgAlertService.getAlertStatistics();

        // Assert
        assertNotNull(statistics, "统计信息不应为null");
        assertEquals(1, statistics.get("totalAlerts"), "总告警数应为1");
        assertEquals(0, statistics.get("activeAlerts"), "活动告警数应为0");
        assertEquals(1, statistics.get("resolvedAlerts"), "已解决告警数应为1");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> typeStats = (Map<String, Object>) statistics.get("alertTypeStatistics");
        assertNotNull(typeStats, "告警类型统计不应为null");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotStats = (Map<String, Object>) typeStats.get("SNAPSHOT_FAILURE_RATE");
        assertNotNull(snapshotStats, "快照生成失败率统计不应为null");
        assertEquals(1L, snapshotStats.get("total"), "快照生成失败率告警总数应为1");
        assertEquals(0L, snapshotStats.get("active"), "活动告警数应为0");
        assertEquals(1L, snapshotStats.get("resolved"), "已解决告警数应为1");
    }

    /**
     * 🔴 红阶段测试：重置告警配置
     */
    @Test
    @DisplayName("重置告警配置 - 功能测试")
    void resetAlertConfigurations_ShouldClearAllConfigurations() {
        // Arrange
        drgAlertService.configureSnapshotFailureRateAlert(5.0);
        drgAlertService.configurePromptSaveTimeoutAlert(30000L);
        drgAlertService.configureUserDecisionResponseTimeAlert(5000L);
        drgAlertService.configureProfitLossErrorRateAlert(2.0);
        
        Map<String, Object> initialConfig = drgAlertService.getAlertConfigurations();
        assertFalse(initialConfig.isEmpty(), "初始配置不应为空");

        // Act
        drgAlertService.resetAlertConfigurations();

        // Assert
        Map<String, Object> resetConfig = drgAlertService.getAlertConfigurations();
        assertTrue(resetConfig.isEmpty(), "重置后配置应为空");
    }
}
