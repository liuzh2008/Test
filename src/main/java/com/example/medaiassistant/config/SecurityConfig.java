package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;

/**
 * 安全配置类
 * 管理认证和授权相关配置参数
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityConfig {

    private final Environment environment;

    /**
     * 认证提供者类型
     * 支持: oidc, jwt, basic
     */
    private String authenticationProvider = "oidc";

    /**
     * OAuth2颁发者URI
     */
    private String oauth2IssuerUri = "https://auth.example.com";

    /**
     * JWK集合URI
     */
    private String jwkSetUri = "https://auth.example.com/jwks";

    /**
     * 会话超时时间（分钟）
     */
    private int sessionTimeout = 30;

    /**
     * 是否启用CSRF保护
     */
    private boolean csrfEnabled = true;

    /**
     * 是否启用CORS
     */
    private boolean corsEnabled = true;

    /**
     * 构造函数
     */
    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 配置验证方法
     */
    @PostConstruct
    public void validateConfiguration() {
        // 验证认证提供者配置
        if (authenticationProvider == null || authenticationProvider.isBlank()) {
            throw new IllegalStateException("认证提供者不能为空");
        }

        // 验证OAuth2配置
        if (oauth2IssuerUri == null || oauth2IssuerUri.isBlank()) {
            throw new IllegalStateException("OAuth2颁发者URI不能为空");
        }

        // 验证JWK集合URI配置
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new IllegalStateException("JWK集合URI不能为空");
        }

        // 验证会话超时时间
        if (sessionTimeout <= 0) {
            throw new IllegalStateException("会话超时时间必须大于0");
        }

        // 验证环境变量映射
        validateEnvironmentVariables();
    }

    /**
     * 验证环境变量映射
     */
    private void validateEnvironmentVariables() {
        // 检查环境变量是否覆盖了默认值
        String envAuthProvider = environment.getProperty("SECURITY_AUTHENTICATION_PROVIDER");
        if (envAuthProvider != null && !envAuthProvider.isBlank()) {
            this.authenticationProvider = envAuthProvider;
        }

        String envIssuerUri = environment.getProperty("SECURITY_OAUTH2_ISSUER_URI");
        if (envIssuerUri != null && !envIssuerUri.isBlank()) {
            this.oauth2IssuerUri = envIssuerUri;
        }

        String envJwkSetUri = environment.getProperty("SECURITY_JWK_SET_URI");
        if (envJwkSetUri != null && !envJwkSetUri.isBlank()) {
            this.jwkSetUri = envJwkSetUri;
        }
    }

    // Getter和Setter方法

    public String getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(String authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public String getOauth2IssuerUri() {
        return oauth2IssuerUri;
    }

    public void setOauth2IssuerUri(String oauth2IssuerUri) {
        this.oauth2IssuerUri = oauth2IssuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    public boolean isCorsEnabled() {
        return corsEnabled;
    }

    public void setCorsEnabled(boolean corsEnabled) {
        this.corsEnabled = corsEnabled;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
