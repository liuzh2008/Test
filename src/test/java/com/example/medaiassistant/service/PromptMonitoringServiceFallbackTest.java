package com.example.medaiassistant.service;

import com.example.medaiassistant.config.PromptServiceConfig;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * PromptMonitoringService降级机制测试
 * 验证当执行服务器不可用时，监控服务能够优雅降级
 */
@ExtendWith(MockitoExtension.class)
class PromptMonitoringServiceFallbackTest {

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository;

    @Mock
    private PromptServiceConfig promptServiceConfig;

    @Mock
    private PromptSubmissionService promptSubmissionService;

    @Mock
    private PromptPollingService promptPollingService;

    @Mock
    private PromptServiceConfig.MonitoringConfig monitoringConfig;

    private PromptMonitoringService promptMonitoringService;

    @BeforeEach
    void setUp() {
        promptMonitoringService = new PromptMonitoringService(
                promptRepository,
                executionEncryptedDataTempRepository,
                promptServiceConfig,
                promptSubmissionService,
                promptPollingService
        );
    }

    /**
     * 配置监控配置（完整配置）
     */
    private void setupMonitoringConfig() {
        when(promptServiceConfig.getMonitoring()).thenReturn(monitoringConfig);
        when(monitoringConfig.getErrorRateThreshold()).thenReturn(10.0);
        when(monitoringConfig.getQueueLengthThreshold()).thenReturn(100);
        // 下面两个字段在当前测试中未使用，使用lenient模式避免Mockito告警
        lenient().when(monitoringConfig.getResponseTimeThreshold()).thenReturn(5000L);
        lenient().when(monitoringConfig.getInterval()).thenReturn(60000L);
    }

    /**
     * 配置监控配置（仅错误率）
     */
    private void setupErrorRateConfig() {
        when(promptServiceConfig.getMonitoring()).thenReturn(monitoringConfig);
        when(monitoringConfig.getErrorRateThreshold()).thenReturn(10.0);
    }

    /**
     * 配置监控配置（仅队列长度）
     */
    private void setupQueueLengthConfig() {
        when(promptServiceConfig.getMonitoring()).thenReturn(monitoringConfig);
        lenient().when(monitoringConfig.getErrorRateThreshold()).thenReturn(10.0);
        when(monitoringConfig.getQueueLengthThreshold()).thenReturn(100);
    }

    /**
     * 测试正常模式下的指标收集
     */
    @Test
    void testCollectSystemMetricsWithFallback_NormalMode() {
        // 准备
        when(promptRepository.count()).thenReturn(100L);
        when(promptRepository.countByStatusName("待执行")).thenReturn(10L);
        when(promptRepository.countByStatusName("已提交")).thenReturn(20L);
        when(promptRepository.countByStatusName("已完成")).thenReturn(60L);
        when(promptRepository.countByStatusName("执行失败")).thenReturn(10L);
        
        when(executionEncryptedDataTempRepository.countByStatus(any())).thenReturn(5L);

        // 执行
        Map<String, Object> metrics = promptMonitoringService.collectSystemMetricsWithFallback();

        // 验证
        assertNotNull(metrics);
        assertEquals(100L, metrics.get("totalPrompts"));
        assertEquals(10L, metrics.get("pendingPrompts"));
        assertEquals(20L, metrics.get("submittedPrompts"));
        assertEquals(60L, metrics.get("completedPrompts"));
        assertEquals(10L, metrics.get("failedPrompts"));
        assertEquals(5L, metrics.get("encryptedDataCount"));
        assertEquals(60.0, (Double) metrics.get("successRate"), 0.1);
        assertEquals(10.0, (Double) metrics.get("errorRate"), 0.1);
        assertTrue((Boolean) metrics.get("executionServerAvailable"));
    }

    /**
     * 测试执行服务器不可用时的降级模式
     */
    @Test
    void testCollectSystemMetricsWithFallback_FallbackMode() {
        // 准备
        when(promptRepository.count()).thenReturn(100L);
        when(promptRepository.countByStatusName("待执行")).thenReturn(10L);
        when(promptRepository.countByStatusName("已提交")).thenReturn(20L);
        when(promptRepository.countByStatusName("已完成")).thenReturn(60L);
        when(promptRepository.countByStatusName("执行失败")).thenReturn(10L);
        
        // 模拟执行服务器异常
        when(executionEncryptedDataTempRepository.countByStatus(any()))
                .thenThrow(new RuntimeException("执行服务器连接失败"));

        // 执行
        Map<String, Object> metrics = promptMonitoringService.collectSystemMetricsWithFallback();

        // 验证
        assertNotNull(metrics);
        assertEquals(100L, metrics.get("totalPrompts"));
        assertEquals(10L, metrics.get("pendingPrompts"));
        assertEquals(20L, metrics.get("submittedPrompts"));
        assertEquals(60L, metrics.get("completedPrompts"));
        assertEquals(10L, metrics.get("failedPrompts"));
        assertEquals(-1, metrics.get("encryptedDataCount")); // 降级数据（Integer）
        assertEquals(-1, metrics.get("sentDataCount")); // 降级数据（Integer）
        assertEquals(-1, metrics.get("errorDataCount")); // 降级数据（Integer）
        assertFalse((Boolean) metrics.get("executionServerAvailable"));
        assertNotNull(metrics.get("executionServerError"));
    }

    /**
     * 测试完全降级模式（主数据源也失败）
     */
    @Test
    void testCollectSystemMetricsWithFallback_FullFallbackMode() {
        // 准备 - 模拟主数据源也失败
        when(promptRepository.count()).thenThrow(new RuntimeException("主数据库连接失败"));

        // 执行
        Map<String, Object> metrics = promptMonitoringService.collectSystemMetricsWithFallback();

        // 验证
        assertNotNull(metrics);
        assertFalse((Boolean) metrics.get("executionServerAvailable"));
        assertNotNull(metrics.get("error"));
        assertNotNull(metrics.get("timestamp"));
    }

    /**
     * 测试正常模式下的健康检查
     */
    @Test
    void testCheckSystemHealthWithFallback_NormalMode() {
        // 准备
        setupMonitoringConfig();
        Map<String, Object> metrics = Map.of(
                "errorRate", 5.0,
                "pendingPrompts", 50L,
                "totalPrompts", 100L,
                "executionServerAvailable", true
        );

        // 执行
        boolean isHealthy = promptMonitoringService.checkSystemHealthWithFallback(metrics);

        // 验证
        assertTrue(isHealthy);
    }

    /**
     * 测试降级模式下的健康检查
     */
    @Test
    void testCheckSystemHealthWithFallback_FallbackMode() {
        // 准备 - 执行服务器不可用，但主服务正常
        setupMonitoringConfig();
        Map<String, Object> metrics = Map.of(
                "errorRate", 5.0,
                "pendingPrompts", 50L,
                "totalPrompts", 100L,
                "executionServerAvailable", false
        );

        // 执行
        boolean isHealthy = promptMonitoringService.checkSystemHealthWithFallback(metrics);

        // 验证 - 在降级模式下，只要主服务正常就认为健康
        assertTrue(isHealthy);
    }

    /**
     * 测试错误率超过阈值时的健康检查
     */
    @Test
    void testCheckSystemHealthWithFallback_ErrorRateExceeded() {
        // 准备 - 错误率超过阈值
        setupErrorRateConfig();
        Map<String, Object> metrics = Map.of(
                "errorRate", 15.0, // 超过10%阈值
                "pendingPrompts", 50L,
                "totalPrompts", 100L,
                "executionServerAvailable", true
        );

        // 执行
        boolean isHealthy = promptMonitoringService.checkSystemHealthWithFallback(metrics);

        // 验证
        assertFalse(isHealthy);
    }

    /**
     * 测试队列长度超过阈值时的健康检查
     */
    @Test
    void testCheckSystemHealthWithFallback_QueueLengthExceeded() {
        // 准备 - 队列长度超过阈值
        setupQueueLengthConfig();
        Map<String, Object> metrics = Map.of(
                "errorRate", 5.0,
                "pendingPrompts", 150L, // 超过100阈值
                "totalPrompts", 100L,
                "executionServerAvailable", true
        );

        // 执行
        boolean isHealthy = promptMonitoringService.checkSystemHealthWithFallback(metrics);

        // 验证
        assertFalse(isHealthy);
    }

    /**
     * 测试执行服务器可用性检查
     */
    @Test
    void testIsExecutionServerAvailable_Normal() {
        // 准备
        when(executionEncryptedDataTempRepository.count()).thenReturn(10L);

        // 执行
        boolean isAvailable = promptMonitoringService.isExecutionServerAvailable();

        // 验证
        assertTrue(isAvailable);
    }

    /**
     * 测试执行服务器不可用性检查
     */
    @Test
    void testIsExecutionServerAvailable_Unavailable() {
        // 准备
        when(executionEncryptedDataTempRepository.count())
                .thenThrow(new RuntimeException("执行服务器连接失败"));

        // 执行
        boolean isAvailable = promptMonitoringService.isExecutionServerAvailable();

        // 验证
        assertFalse(isAvailable);
    }

    /**
     * 测试降级状态报告生成
     */
    @Test
    void testGetFallbackStatusReport() {
        // 准备 - 配置监控配置（getFallbackStatusReport需要通过checkSystemHealthWithFallback使用配置）
        setupMonitoringConfig();
        
        // 使用反射设置内部状态
        ReflectionTestUtils.setField(promptMonitoringService, "totalSubmissions", new java.util.concurrent.atomic.AtomicLong(100));
        ReflectionTestUtils.setField(promptMonitoringService, "totalPollingSuccess", new java.util.concurrent.atomic.AtomicLong(80));
        ReflectionTestUtils.setField(promptMonitoringService, "totalPollingErrors", new java.util.concurrent.atomic.AtomicLong(20));

        when(promptRepository.count()).thenReturn(100L);
        when(promptRepository.countByStatusName("待执行")).thenReturn(10L);
        when(promptRepository.countByStatusName("已提交")).thenReturn(20L);
        when(promptRepository.countByStatusName("已完成")).thenReturn(60L);
        when(promptRepository.countByStatusName("执行失败")).thenReturn(10L);
        
        when(executionEncryptedDataTempRepository.countByStatus(any())).thenReturn(5L);

        // 执行
        String report = promptMonitoringService.getFallbackStatusReport();

        // 验证
        assertNotNull(report);
        assertTrue(report.contains("系统状态报告"));
        assertTrue(report.contains("模式"));
        assertTrue(report.contains("状态"));
        assertTrue(report.contains("执行服务器"));
        assertTrue(report.contains("成功率"));
        assertTrue(report.contains("待处理"));
    }

    /**
     * 测试健康检查方法（带降级处理）
     */
    @Test
    void testHealthCheckWithFallback() {
        // 准备 - healthCheck方法不需要监控配置，只进行简单的数据库和服务状态检查
        when(promptSubmissionService.healthCheck()).thenReturn("提交服务健康状态 - 正常");
        when(promptPollingService.healthCheck()).thenReturn("轮询服务健康状态 - 正常");
        when(promptRepository.count()).thenReturn(100L);
        when(executionEncryptedDataTempRepository.count()).thenReturn(50L);

        // 执行
        String healthStatus = promptMonitoringService.healthCheck();

        // 验证
        assertNotNull(healthStatus);
        assertTrue(healthStatus.contains("监控服务健康状态"));
        assertTrue(healthStatus.contains("数据库连接: 正常"));
        assertTrue(healthStatus.contains("总Prompt数: 100"));
        assertTrue(healthStatus.contains("执行服务器: 正常"));
        assertTrue(healthStatus.contains("总加密数据数: 50"));
    }

    /**
     * 测试健康检查方法（执行服务器不可用）
     */
    @Test
    void testHealthCheckWithFallback_ExecutionServerUnavailable() {
        // 准备 - healthCheck方法不需要监控配置，只进行简单的数据库和服务状态检查
        when(promptSubmissionService.healthCheck()).thenReturn("提交服务健康状态 - 正常");
        when(promptPollingService.healthCheck()).thenReturn("轮询服务健康状态 - 正常");
        when(promptRepository.count()).thenReturn(100L);
        when(executionEncryptedDataTempRepository.count())
                .thenThrow(new RuntimeException("执行服务器连接失败"));

        // 执行
        String healthStatus = promptMonitoringService.healthCheck();

        // 验证
        assertNotNull(healthStatus);
        assertTrue(healthStatus.contains("监控服务健康状态"));
        assertTrue(healthStatus.contains("数据库连接: 正常"));
        assertTrue(healthStatus.contains("总Prompt数: 100"));
        assertTrue(healthStatus.contains("执行服务器: 不可用"));
        assertTrue(healthStatus.contains("总加密数据数: 0"));
    }

    /**
     * 测试监控任务执行异常处理
     */
    @Test
    void testMonitorSystem_ExceptionHandling() {
        // 准备 - 模拟指标收集失败
        when(promptRepository.count()).thenThrow(new RuntimeException("数据库连接失败"));

        // 执行 - 应该不会抛出异常
        assertDoesNotThrow(() -> promptMonitoringService.monitorSystem());

        // 验证 - 方法应该正常执行完成，不会影响主服务
    }
}
