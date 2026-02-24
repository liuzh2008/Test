package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 配置验证服务扩展测试
 * 测试迭代3新增的配置验证与集成功能
 * 
 * ✅ P2修订：使用MockitoExtension，无需Spring上下文，已最优
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-07
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("配置验证服务扩展测试 - 迭代3功能")
class ConfigurationValidationServiceExtendedTest {

    @Mock
    private Environment environment;

    private ConfigurationValidationService configurationValidationService;

    @BeforeEach
    void setUp() {
        configurationValidationService = new ConfigurationValidationService();
        
        // 使用反射设置environment字段
        try {
            var environmentField = ConfigurationValidationService.class.getDeclaredField("environment");
            environmentField.setAccessible(true);
            environmentField.set(configurationValidationService, environment);
        } catch (Exception e) {
            fail("设置environment字段失败: " + e.getMessage());
        }
    }

    /**
     * ✅ 绿阶段测试通过：验证配置热更新支持
     */
    @Test
    @DisplayName("应该支持配置热更新验证")
    void shouldSupportConfigurationHotUpdateValidation() {
        // 当
        boolean hotUpdateSupported = configurationValidationService.supportsHotUpdate();
        
        // 那么
        assertTrue(hotUpdateSupported, "配置验证服务应该支持热更新验证");
    }

    /**
     * ✅ 绿阶段测试通过：验证配置监控指标
     */
    @Test
    @DisplayName("应该提供配置监控指标")
    void shouldProvideConfigurationMonitoringMetrics() {
        // 当
        Map<String, Object> metrics = configurationValidationService.getConfigurationMetrics();
        
        // 那么
        assertNotNull(metrics, "应该返回配置监控指标");
        assertTrue(metrics.containsKey("validationCount"), "应该包含验证次数指标");
        assertTrue(metrics.containsKey("lastValidationTime"), "应该包含最后验证时间指标");
        assertTrue(metrics.containsKey("errorCount"), "应该包含错误次数指标");
        assertTrue(metrics.containsKey("configurationValid"), "应该包含配置有效性指标");
        
        // 验证指标值的类型
        assertTrue(metrics.get("validationCount") instanceof Integer, "验证次数应该是整数");
        assertTrue(metrics.get("errorCount") instanceof Integer, "错误次数应该是整数");
        assertTrue(metrics.get("lastValidationTime") instanceof String, "最后验证时间应该是字符串");
        assertTrue(metrics.get("configurationValid") instanceof Boolean, "配置有效性应该是布尔值");
    }

    /**
     * ✅ 绿阶段测试通过：验证AI模型配置集成
     */
    @Test
    @DisplayName("应该验证AI模型配置集成 - 配置完整")
    void shouldValidateAIModelConfigurationIntegration_WhenConfigComplete() {
        // 给定
        when(environment.getProperty("ai.model.deepseek-chat.url")).thenReturn("https://api.deepseek.com/chat");
        when(environment.getProperty("ai.model.deepseek-chat.api-key")).thenReturn("test-key");
        
        // 当
        boolean aiModelIntegrationValid = configurationValidationService.validateAIModelIntegration();
        
        // 那么
        assertTrue(aiModelIntegrationValid, "AI模型配置集成应该有效");
    }

    /**
     * ✅ 绿阶段测试通过：验证AI模型配置集成失败场景
     */
    @Test
    @DisplayName("应该验证AI模型配置集成 - 配置缺失")
    void shouldValidateAIModelConfigurationIntegration_WhenConfigMissing() {
        // 给定
        when(environment.getProperty("ai.model.deepseek-chat.url")).thenReturn(null);
        when(environment.getProperty("ai.model.deepseek-chat.api-key")).thenReturn(null);
        
        // 当
        boolean aiModelIntegrationValid = configurationValidationService.validateAIModelIntegration();
        
        // 那么
        assertFalse(aiModelIntegrationValid, "AI模型配置缺失时集成验证应该失败");
    }

    /**
     * ✅ 绿阶段测试通过：验证执行服务器配置集成
     */
    @Test
    @DisplayName("应该验证执行服务器配置集成 - 使用IP配置")
    void shouldValidateExecutionServerConfigurationIntegration_WithIpConfig() {
        // 给定
        when(environment.getProperty("execution.server.ip")).thenReturn("100.66.1.2");
        
        // 当
        boolean executionServerIntegrationValid = configurationValidationService.validateExecutionServerIntegration();
        
        // 那么
        assertTrue(executionServerIntegrationValid, "执行服务器配置集成应该有效");
    }

    /**
     * ✅ 绿阶段测试通过：验证执行服务器配置集成 - 使用URL配置
     */
    @Test
    @DisplayName("应该验证执行服务器配置集成 - 使用URL配置")
    void shouldValidateExecutionServerConfigurationIntegration_WithUrlConfig() {
        // 给定
        when(environment.getProperty("execution.server.ip")).thenReturn(null);
        when(environment.getProperty("execution.server.url")).thenReturn("http://100.66.1.2:8082");
        
        // 当
        boolean executionServerIntegrationValid = configurationValidationService.validateExecutionServerIntegration();
        
        // 那么
        assertTrue(executionServerIntegrationValid, "执行服务器配置集成应该有效");
    }

    /**
     * ✅ 绿阶段测试通过：验证执行服务器配置集成失败场景
     */
    @Test
    @DisplayName("应该验证执行服务器配置集成 - 配置缺失")
    void shouldValidateExecutionServerConfigurationIntegration_WhenConfigMissing() {
        // 给定
        when(environment.getProperty("execution.server.ip")).thenReturn(null);
        when(environment.getProperty("execution.server.url")).thenReturn(null);
        
        // 当
        boolean executionServerIntegrationValid = configurationValidationService.validateExecutionServerIntegration();
        
        // 那么
        assertFalse(executionServerIntegrationValid, "执行服务器配置缺失时集成验证应该失败");
    }

    /**
     * ✅ 绿阶段测试通过：验证配置异常模拟
     */
    @Test
    @DisplayName("应该正确处理配置异常")
    void shouldHandleConfigurationExceptionsProperly() {
        // 当
        ConfigurationValidationException exception = configurationValidationService.simulateConfigurationException();
        
        // 那么
        assertNotNull(exception, "应该能够模拟配置异常");
        assertTrue(exception.getMessage().contains("配置异常"), "异常消息应该包含配置异常信息");
        assertTrue(exception.getMessage().contains("模拟"), "异常消息应该包含模拟标识");
    }

    /**
     * 验证配置监控指标的初始值
     */
    @Test
    @DisplayName("配置监控指标应该有合理的初始值")
    void configurationMetricsShouldHaveReasonableInitialValues() {
        // 当
        Map<String, Object> metrics = configurationValidationService.getConfigurationMetrics();
        
        // 那么
        assertEquals(0, metrics.get("validationCount"), "初始验证次数应该为0");
        assertEquals(0, metrics.get("errorCount"), "初始错误次数应该为0");
        assertNotNull(metrics.get("lastValidationTime"), "应该有最后验证时间");
    }
}
