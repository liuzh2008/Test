package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptServiceConfig配置类单元测试
 * 
 * 测试目标：
 * 1. 配置属性正确绑定
 * 2. 默认值正确设置
 * 3. 配置验证功能
 * 4. 配置摘要方法
 */
@SpringBootTest(classes = PromptServiceConfig.class)
@EnableConfigurationProperties(PromptServiceConfig.class)
@TestPropertySource(properties = {
    "prompt.submission.interval=15000",
    "prompt.submission.page-size=15",
    "prompt.submission.max-threads=8",
    "prompt.polling.interval=8000",
    "prompt.polling.page-size=25",
    "prompt.monitoring.interval=45000",
    "prompt.monitoring.error-rate-threshold=3.5"
})
class PromptServiceConfigTest {

    @Autowired
    private PromptServiceConfig promptServiceConfig;

    @BeforeEach
    void setUp() {
        assertNotNull(promptServiceConfig, "PromptServiceConfig should be autowired");
    }

    /**
     * 测试配置类正确加载
     */
    @Test
    void testConfigLoaded() {
        assertNotNull(promptServiceConfig, "PromptServiceConfig should not be null");
        assertTrue(promptServiceConfig.isValid(), "PromptServiceConfig should be valid");
    }

    /**
     * 测试提交服务配置属性绑定
     */
    @Test
    void testSubmissionConfigProperties() {
        PromptServiceConfig.SubmissionConfig submission = promptServiceConfig.getSubmission();
        assertNotNull(submission, "Submission config should not be null");
        
        // 测试自定义属性值
        assertEquals(15000L, submission.getInterval(), "Submission interval should be 15000");
        assertEquals(15, submission.getPageSize(), "Submission page size should be 15");
        assertEquals(8, submission.getMaxThreads(), "Submission max threads should be 8");
        
        // 测试默认值
        assertTrue(submission.isEnabled(), "Submission should be enabled by default");
        assertEquals(3, submission.getMaxRetries(), "Submission max retries should be default 3");
        assertEquals(1000L, submission.getRetryInterval(), "Submission retry interval should be default 1000");
        assertEquals(300000L, submission.getConnectTimeout(), "Submission connect timeout should be default 300000");
        assertEquals(300000L, submission.getReadTimeout(), "Submission read timeout should be default 300000");
    }

    /**
     * 测试轮询服务配置属性绑定
     */
    @Test
    void testPollingConfigProperties() {
        PromptServiceConfig.PollingConfig polling = promptServiceConfig.getPolling();
        assertNotNull(polling, "Polling config should not be null");
        
        // 测试自定义属性值
        assertEquals(8000L, polling.getInterval(), "Polling interval should be 8000");
        assertEquals(25, polling.getPageSize(), "Polling page size should be 25");
        
        // 测试默认值
        assertTrue(polling.isEnabled(), "Polling should be enabled by default");
        assertEquals(5, polling.getMaxRetries(), "Polling max retries should be default 5");
        assertEquals(2000L, polling.getRetryInterval(), "Polling retry interval should be default 2000");
        assertEquals(20, polling.getBatchSize(), "Polling batch size should be 20 from application.properties");
        assertEquals(30000L, polling.getTimeout(), "Polling timeout should be default 30000");
    }

    /**
     * 测试监控服务配置属性绑定
     */
    @Test
    void testMonitoringConfigProperties() {
        PromptServiceConfig.MonitoringConfig monitoring = promptServiceConfig.getMonitoring();
        assertNotNull(monitoring, "Monitoring config should not be null");
        
        // 测试自定义属性值
        assertEquals(45000L, monitoring.getInterval(), "Monitoring interval should be 45000");
        assertEquals(3.5, monitoring.getErrorRateThreshold(), 0.001, "Monitoring error rate threshold should be 3.5");
        
        // 测试默认值
        assertTrue(monitoring.isEnabled(), "Monitoring should be enabled by default");
        assertEquals(10000L, monitoring.getHealthCheckTimeout(), "Monitoring health check timeout should be default 10000");
        assertEquals(30000L, monitoring.getMetricsInterval(), "Monitoring metrics interval should be default 30000");
        assertEquals(10000L, monitoring.getResponseTimeThreshold(), "Monitoring response time threshold should be default 10000");
        assertEquals(100, monitoring.getQueueLengthThreshold(), "Monitoring queue length threshold should be default 100");
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
        assertTrue(submissionSummary.contains("interval=15000"), "Submission summary should contain interval");
        assertTrue(submissionSummary.contains("pageSize=15"), "Submission summary should contain page size");
        assertTrue(submissionSummary.contains("maxThreads=8"), "Submission summary should contain max threads");

        // 测试轮询服务配置摘要
        String pollingSummary = promptServiceConfig.getPollingConfigSummary();
        assertNotNull(pollingSummary, "Polling config summary should not be null");
        assertTrue(pollingSummary.contains("Polling[enabled=true"), "Polling summary should contain enabled status");
        assertTrue(pollingSummary.contains("interval=8000"), "Polling summary should contain interval");
        assertTrue(pollingSummary.contains("pageSize=25"), "Polling summary should contain page size");

        // 测试监控服务配置摘要
        String monitoringSummary = promptServiceConfig.getMonitoringConfigSummary();
        assertNotNull(monitoringSummary, "Monitoring config summary should not be null");
        assertTrue(monitoringSummary.contains("Monitoring[enabled=true"), "Monitoring summary should contain enabled status");
        assertTrue(monitoringSummary.contains("interval=45000"), "Monitoring summary should contain interval");
        assertTrue(monitoringSummary.contains("errorRateThreshold=3.5%"), "Monitoring summary should contain error rate threshold");

        // 测试完整配置摘要
        String fullSummary = promptServiceConfig.getFullConfigSummary();
        assertNotNull(fullSummary, "Full config summary should not be null");
        assertTrue(fullSummary.contains("PromptServiceConfig"), "Full summary should contain class name");
        assertTrue(fullSummary.contains("Submission["), "Full summary should contain submission config");
        assertTrue(fullSummary.contains("Polling["), "Full summary should contain polling config");
        assertTrue(fullSummary.contains("Monitoring["), "Full summary should contain monitoring config");
    }

    /**
     * 测试默认配置值
     */
    @Test
    void testDefaultConfigValues() {
        // 创建新的配置实例测试默认值
        PromptServiceConfig defaultConfig = new PromptServiceConfig();
        
        // 测试提交服务默认值
        assertEquals(10000L, defaultConfig.getSubmission().getInterval(), "Default submission interval should be 10000");
        assertEquals(10, defaultConfig.getSubmission().getPageSize(), "Default submission page size should be 10");
        assertEquals(5, defaultConfig.getSubmission().getMaxThreads(), "Default submission max threads should be 5");
        
        // 测试轮询服务默认值
        assertEquals(30000L, defaultConfig.getPolling().getInterval(), "Default polling interval should be 30000");
        assertEquals(20, defaultConfig.getPolling().getPageSize(), "Default polling page size should be 20");
        assertEquals(50, defaultConfig.getPolling().getBatchSize(), "Default polling batch size should be 50 (Java class default)");
        
        // 测试监控服务默认值
        assertEquals(60000L, defaultConfig.getMonitoring().getInterval(), "Default monitoring interval should be 60000");
        assertEquals(5.0, defaultConfig.getMonitoring().getErrorRateThreshold(), 0.001, "Default monitoring error rate threshold should be 5.0");
        assertEquals(10000L, defaultConfig.getMonitoring().getResponseTimeThreshold(), "Default monitoring response time threshold should be 10000");
    }

    /**
     * 测试配置有效性验证
     */
    @Test
    void testConfigValidation() {
        assertTrue(promptServiceConfig.isValid(), "Config should be valid when all sub-configs are present");
        
        // 测试无效配置情况
        PromptServiceConfig invalidConfig = new PromptServiceConfig();
        invalidConfig.setSubmission(null);
        assertFalse(invalidConfig.isValid(), "Config should be invalid when submission config is null");
    }
}
