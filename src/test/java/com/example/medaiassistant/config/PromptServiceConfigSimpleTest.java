package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptServiceConfig配置类简单单元测试
 * 不依赖Spring Boot上下文，只测试配置类的基本功能
 */
class PromptServiceConfigSimpleTest {

    private PromptServiceConfig promptServiceConfig;

    @BeforeEach
    void setUp() {
        promptServiceConfig = new PromptServiceConfig();
    }

    /**
     * 测试配置类默认值
     */
    @Test
    void testDefaultValues() {
        assertNotNull(promptServiceConfig, "PromptServiceConfig should not be null");
        assertTrue(promptServiceConfig.isValid(), "Config should be valid with default values");
        
        // 测试提交服务默认值
        PromptServiceConfig.SubmissionConfig submission = promptServiceConfig.getSubmission();
        assertNotNull(submission, "Submission config should not be null");
        assertTrue(submission.isEnabled(), "Submission should be enabled by default");
        assertEquals(10000L, submission.getInterval(), "Default submission interval should be 10000");
        assertEquals(10, submission.getPageSize(), "Default submission page size should be 10");
        assertEquals(5, submission.getMaxThreads(), "Default submission max threads should be 5");
        assertEquals(3, submission.getMaxRetries(), "Default submission max retries should be 3");
        assertEquals(1000L, submission.getRetryInterval(), "Default submission retry interval should be 1000");
        assertEquals(300000L, submission.getConnectTimeout(), "Default submission connect timeout should be 300000");
        assertEquals(300000L, submission.getReadTimeout(), "Default submission read timeout should be 300000");
        
        // 测试轮询服务默认值
        PromptServiceConfig.PollingConfig polling = promptServiceConfig.getPolling();
        assertNotNull(polling, "Polling config should not be null");
        assertTrue(polling.isEnabled(), "Polling should be enabled by default");
        assertEquals(30000L, polling.getInterval(), "Default polling interval should be 30000");
        assertEquals(20, polling.getPageSize(), "Default polling page size should be 20");
        assertEquals(5, polling.getMaxRetries(), "Default polling max retries should be 5");
        assertEquals(2000L, polling.getRetryInterval(), "Default polling retry interval should be 2000");
        assertEquals(50, polling.getBatchSize(), "Default polling batch size should be 50");
        assertEquals(30000L, polling.getTimeout(), "Default polling timeout should be 30000");
        
        // 测试监控服务默认值
        PromptServiceConfig.MonitoringConfig monitoring = promptServiceConfig.getMonitoring();
        assertNotNull(monitoring, "Monitoring config should not be null");
        assertTrue(monitoring.isEnabled(), "Monitoring should be enabled by default");
        assertEquals(60000L, monitoring.getInterval(), "Default monitoring interval should be 60000");
        assertEquals(10000L, monitoring.getHealthCheckTimeout(), "Default monitoring health check timeout should be 10000");
        assertEquals(30000L, monitoring.getMetricsInterval(), "Default monitoring metrics interval should be 30000");
        assertEquals(5.0, monitoring.getErrorRateThreshold(), 0.001, "Default monitoring error rate threshold should be 5.0");
        assertEquals(10000L, monitoring.getResponseTimeThreshold(), "Default monitoring response time threshold should be 10000");
        assertEquals(100, monitoring.getQueueLengthThreshold(), "Default monitoring queue length threshold should be 100");
    }

    /**
     * 测试配置摘要方法
     */
    @Test
    void testConfigSummaryMethods() {
        // 测试提交服务配置摘要
        String submissionSummary = promptServiceConfig.getSubmissionConfigSummary();
        assertNotNull(submissionSummary, "Submission config summary should not be null");
        assertTrue(submissionSummary.contains("Submission[enabled=true"), "Submission summary should contain enabled status");
        assertTrue(submissionSummary.contains("interval=10000"), "Submission summary should contain interval");
        assertTrue(submissionSummary.contains("pageSize=10"), "Submission summary should contain page size");
        assertTrue(submissionSummary.contains("maxThreads=5"), "Submission summary should contain max threads");

        // 测试轮询服务配置摘要
        String pollingSummary = promptServiceConfig.getPollingConfigSummary();
        assertNotNull(pollingSummary, "Polling config summary should not be null");
        assertTrue(pollingSummary.contains("Polling[enabled=true"), "Polling summary should contain enabled status");
        assertTrue(pollingSummary.contains("interval=30000"), "Polling summary should contain interval");
        assertTrue(pollingSummary.contains("pageSize=20"), "Polling summary should contain page size");

        // 测试监控服务配置摘要
        String monitoringSummary = promptServiceConfig.getMonitoringConfigSummary();
        assertNotNull(monitoringSummary, "Monitoring config summary should not be null");
        assertTrue(monitoringSummary.contains("Monitoring[enabled=true"), "Monitoring summary should contain enabled status");
        assertTrue(monitoringSummary.contains("interval=60000"), "Monitoring summary should contain interval");
        assertTrue(monitoringSummary.contains("errorRateThreshold=5.0%"), "Monitoring summary should contain error rate threshold");

        // 测试完整配置摘要
        String fullSummary = promptServiceConfig.getFullConfigSummary();
        assertNotNull(fullSummary, "Full config summary should not be null");
        assertTrue(fullSummary.contains("PromptServiceConfig"), "Full summary should contain class name");
        assertTrue(fullSummary.contains("Submission["), "Full summary should contain submission config");
        assertTrue(fullSummary.contains("Polling["), "Full summary should contain polling config");
        assertTrue(fullSummary.contains("Monitoring["), "Full summary should contain monitoring config");
    }

    /**
     * 测试配置有效性验证
     */
    @Test
    void testConfigValidation() {
        // 测试有效配置
        assertTrue(promptServiceConfig.isValid(), "Config should be valid when all sub-configs are present");
        
        // 测试无效配置情况
        PromptServiceConfig invalidConfig = new PromptServiceConfig();
        invalidConfig.setSubmission(null);
        assertFalse(invalidConfig.isValid(), "Config should be invalid when submission config is null");
        
        invalidConfig = new PromptServiceConfig();
        invalidConfig.setPolling(null);
        assertFalse(invalidConfig.isValid(), "Config should be invalid when polling config is null");
        
        invalidConfig = new PromptServiceConfig();
        invalidConfig.setMonitoring(null);
        assertFalse(invalidConfig.isValid(), "Config should be invalid when monitoring config is null");
    }

    /**
     * 测试配置属性设置
     */
    @Test
    void testConfigPropertySetting() {
        // 设置提交服务配置
        PromptServiceConfig.SubmissionConfig submission = promptServiceConfig.getSubmission();
        submission.setEnabled(false);
        submission.setInterval(20000L);
        submission.setPageSize(25);
        submission.setMaxThreads(10);
        
        assertFalse(submission.isEnabled(), "Submission should be disabled after setting");
        assertEquals(20000L, submission.getInterval(), "Submission interval should be 20000 after setting");
        assertEquals(25, submission.getPageSize(), "Submission page size should be 25 after setting");
        assertEquals(10, submission.getMaxThreads(), "Submission max threads should be 10 after setting");
        
        // 设置轮询服务配置
        PromptServiceConfig.PollingConfig polling = promptServiceConfig.getPolling();
        polling.setEnabled(false);
        polling.setInterval(10000L);
        polling.setPageSize(30);
        polling.setBatchSize(100);
        
        assertFalse(polling.isEnabled(), "Polling should be disabled after setting");
        assertEquals(10000L, polling.getInterval(), "Polling interval should be 10000 after setting");
        assertEquals(30, polling.getPageSize(), "Polling page size should be 30 after setting");
        assertEquals(100, polling.getBatchSize(), "Polling batch size should be 100 after setting");
        
        // 设置监控服务配置
        PromptServiceConfig.MonitoringConfig monitoring = promptServiceConfig.getMonitoring();
        monitoring.setEnabled(false);
        monitoring.setInterval(120000L);
        monitoring.setErrorRateThreshold(10.0);
        monitoring.setResponseTimeThreshold(30000L);
        
        assertFalse(monitoring.isEnabled(), "Monitoring should be disabled after setting");
        assertEquals(120000L, monitoring.getInterval(), "Monitoring interval should be 120000 after setting");
        assertEquals(10.0, monitoring.getErrorRateThreshold(), 0.001, "Monitoring error rate threshold should be 10.0 after setting");
        assertEquals(30000L, monitoring.getResponseTimeThreshold(), "Monitoring response time threshold should be 30000 after setting");
    }
}
