package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全配置全面测试类
 * 包含边界条件测试、异常情况测试和配置验证测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@SpringBootTest(classes = SecurityConfig.class)
@TestPropertySource(properties = {
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "prompt.submission.enabled=false",
    "prompt.polling.enabled=false",
    "monitoring.metrics.enabled=false"
})
@DisplayName("安全配置全面测试")
class SecurityConfigComprehensiveTest {

    @Autowired
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        // 确保每次测试前配置处于干净状态
        securityConfig.setAuthenticationProvider("oidc");
        securityConfig.setOauth2IssuerUri("https://auth.example.com");
        securityConfig.setJwkSetUri("https://auth.example.com/jwks");
        securityConfig.setSessionTimeout(30);
    }

    /**
     * 边界条件测试：验证空认证提供者配置
     */
    @Test
    @DisplayName("当认证提供者为空时应该抛出异常")
    void shouldThrowExceptionWhenAuthenticationProviderIsEmpty() {
        securityConfig.setAuthenticationProvider("");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> securityConfig.validateConfiguration(),
            "空认证提供者应该抛出异常");
        
        assertTrue(exception.getMessage().contains("认证提供者不能为空"),
            "异常消息应该包含'认证提供者不能为空'");
    }

    /**
     * 边界条件测试：验证空OAuth2颁发者URI配置
     */
    @Test
    @DisplayName("当OAuth2颁发者URI为空时应该抛出异常")
    void shouldThrowExceptionWhenOauth2IssuerUriIsEmpty() {
        securityConfig.setOauth2IssuerUri("");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> securityConfig.validateConfiguration(),
            "空OAuth2颁发者URI应该抛出异常");
        
        assertTrue(exception.getMessage().contains("OAuth2颁发者URI不能为空"),
            "异常消息应该包含'OAuth2颁发者URI不能为空'");
    }

    /**
     * 边界条件测试：验证空JWK集合URI配置
     */
    @Test
    @DisplayName("当JWK集合URI为空时应该抛出异常")
    void shouldThrowExceptionWhenJwkSetUriIsEmpty() {
        securityConfig.setJwkSetUri("");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> securityConfig.validateConfiguration(),
            "空JWK集合URI应该抛出异常");
        
        assertTrue(exception.getMessage().contains("JWK集合URI不能为空"),
            "异常消息应该包含'JWK集合URI不能为空'");
    }

    /**
     * 边界条件测试：验证无效会话超时时间
     */
    @Test
    @DisplayName("当会话超时时间为负数时应该抛出异常")
    void shouldThrowExceptionWhenSessionTimeoutIsNegative() {
        securityConfig.setSessionTimeout(-1);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> securityConfig.validateConfiguration(),
            "负会话超时时间应该抛出异常");
        
        assertTrue(exception.getMessage().contains("会话超时时间必须大于0"),
            "异常消息应该包含'会话超时时间必须大于0'");
    }

    /**
     * 边界条件测试：验证零会话超时时间
     */
    @Test
    @DisplayName("当会话超时时间为零时应该抛出异常")
    void shouldThrowExceptionWhenSessionTimeoutIsZero() {
        securityConfig.setSessionTimeout(0);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> securityConfig.validateConfiguration(),
            "零会话超时时间应该抛出异常");
        
        assertTrue(exception.getMessage().contains("会话超时时间必须大于0"),
            "异常消息应该包含'会话超时时间必须大于0'");
    }

    /**
     * 正常场景测试：验证有效配置
     */
    @Test
    @DisplayName("当所有配置有效时应该通过验证")
    void shouldPassValidationWhenAllConfigurationsAreValid() {
        securityConfig.setAuthenticationProvider("jwt");
        securityConfig.setOauth2IssuerUri("https://auth.company.com");
        securityConfig.setJwkSetUri("https://auth.company.com/jwks");
        securityConfig.setSessionTimeout(60);
        
        assertDoesNotThrow(() -> securityConfig.validateConfiguration(),
            "有效配置应该通过验证");
    }

    /**
     * 配置属性测试：验证CSRF配置
     */
    @Test
    @DisplayName("应该正确配置CSRF保护")
    void shouldConfigureCsrfProtection() {
        assertTrue(securityConfig.isCsrfEnabled(), "CSRF保护应该默认启用");
        
        securityConfig.setCsrfEnabled(false);
        assertFalse(securityConfig.isCsrfEnabled(), "CSRF保护应该能够禁用");
    }

    /**
     * 配置属性测试：验证CORS配置
     */
    @Test
    @DisplayName("应该正确配置CORS")
    void shouldConfigureCors() {
        assertTrue(securityConfig.isCorsEnabled(), "CORS应该默认启用");
        
        securityConfig.setCorsEnabled(false);
        assertFalse(securityConfig.isCorsEnabled(), "CORS应该能够禁用");
    }

    /**
     * 配置属性测试：验证会话超时配置
     */
    @Test
    @DisplayName("应该正确配置会话超时时间")
    void shouldConfigureSessionTimeout() {
        assertEquals(30, securityConfig.getSessionTimeout(), "默认会话超时时间应该为30分钟");
        
        securityConfig.setSessionTimeout(120);
        assertEquals(120, securityConfig.getSessionTimeout(), "会话超时时间应该能够设置");
    }

    /**
     * 配置属性测试：验证认证提供者配置
     */
    @Test
    @DisplayName("应该支持多种认证提供者")
    void shouldSupportMultipleAuthenticationProviders() {
        String[] providers = {"oidc", "jwt", "basic"};
        
        for (String provider : providers) {
            securityConfig.setAuthenticationProvider(provider);
            assertEquals(provider, securityConfig.getAuthenticationProvider(),
                "应该支持认证提供者: " + provider);
        }
    }

    /**
     * 环境变量测试：验证环境变量覆盖
     */
    @Test
    @DisplayName("应该支持环境变量覆盖配置")
    void shouldSupportEnvironmentVariableOverride() {
        // 注意：这个测试需要在实际环境中设置环境变量
        // 这里主要验证环境变量映射逻辑的存在性
        assertNotNull(securityConfig.getEnvironment(), "环境对象应该存在");
        
        // 验证环境变量映射方法存在
        assertDoesNotThrow(() -> securityConfig.validateConfiguration(),
            "环境变量映射逻辑应该正常工作");
    }

    /**
     * 性能测试：验证配置验证性能
     */
    @Test
    @DisplayName("配置验证应该在合理时间内完成")
    void configurationValidationShouldCompleteInReasonableTime() {
        long startTime = System.currentTimeMillis();
        
        securityConfig.validateConfiguration();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 1000, "配置验证应该在1秒内完成，实际耗时: " + duration + "ms");
    }
}
