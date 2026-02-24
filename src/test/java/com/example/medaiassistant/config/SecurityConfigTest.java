package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全配置测试类
 * 按照TDD红-绿-重构流程实现故事2：认证配置管理
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
    "monitoring.metrics.enabled=false",
    "security.authentication-provider=oidc",
    "security.oauth2-issuer-uri=https://auth.example.com",
    "security.jwk-set-uri=https://auth.example.com/jwks"
})
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    /**
     * 绿阶段测试用例1：验证SecurityConfig类存在
     * 这个测试现在应该通过，因为SecurityConfig类已存在
     */
    @Test
    void securityConfigShouldBeAutowired() {
        // 这个测试现在应该通过，因为SecurityConfig类已存在
        assertNotNull(securityConfig, "SecurityConfig应该被自动装配");
    }

    /**
     * 绿阶段测试用例2：验证认证配置参数绑定
     * 这个测试现在应该通过，因为SecurityConfig类已存在
     */
    @Test
    void authenticationConfigShouldBindProperties() {
        // 这个测试现在应该通过，因为SecurityConfig类已存在
        assertNotNull(securityConfig.getAuthenticationProvider(), "认证提供者配置应该存在");
        assertNotNull(securityConfig.getOauth2IssuerUri(), "OAuth2颁发者URI配置应该存在");
        assertNotNull(securityConfig.getJwkSetUri(), "JWK集合URI配置应该存在");
    }

    /**
     * 绿阶段测试用例3：验证配置验证功能
     * 这个测试现在应该通过，因为SecurityConfig类已存在
     */
    @Test
    void securityConfigShouldValidateConfiguration() {
        // 这个测试现在应该通过，因为SecurityConfig类已存在
        assertDoesNotThrow(() -> securityConfig.validateConfiguration(), 
            "安全配置验证应该通过");
    }

    /**
     * 绿阶段测试用例4：验证环境变量映射
     * 这个测试现在应该通过，因为SecurityConfig类已存在
     */
    @Test
    void securityConfigShouldMapEnvironmentVariables() {
        // 这个测试现在应该通过，因为SecurityConfig类已存在
        assertEquals("oidc", securityConfig.getAuthenticationProvider(), 
            "认证提供者应该映射环境变量");
        assertEquals("https://auth.example.com", securityConfig.getOauth2IssuerUri(), 
            "OAuth2颁发者URI应该映射环境变量");
        assertEquals("https://auth.example.com/jwks", securityConfig.getJwkSetUri(), 
            "JWK集合URI应该映射环境变量");
    }
}
