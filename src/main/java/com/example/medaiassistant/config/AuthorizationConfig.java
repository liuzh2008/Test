package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 授权配置类
 * 管理细粒度权限控制相关配置参数，实现最小权限原则
 * 
 * 功能特性：
 * - 支持基于角色的访问控制(RBAC)
 * - 支持基于权限的访问控制(PBAC)
 * - 支持权限审计和验证
 * - 支持环境变量映射
 * - 自动配置验证
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@ConfigurationProperties(prefix = "security.authorization")
@Validated
public class AuthorizationConfig {

    // 常量定义
    private static final String DEFAULT_ROLE_USER = "USER";
    private static final String DEFAULT_ROLE_ADMIN = "ADMIN";
    private static final String DEFAULT_ROLE_VIEWER = "VIEWER";
    
    private static final String PERMISSION_READ = "READ";
    private static final String PERMISSION_WRITE = "WRITE";
    private static final String PERMISSION_DELETE = "DELETE";
    private static final String PERMISSION_MANAGE_USERS = "MANAGE_USERS";

    private final Environment environment;

    /**
     * 是否启用授权配置
     */
    private boolean enabled = true;

    /**
     * 是否启用基于角色的访问控制
     */
    private boolean roleBasedAccessControlEnabled = true;

    /**
     * 是否启用基于权限的访问控制
     */
    private boolean permissionBasedAccessControlEnabled = false;

    /**
     * 默认角色
     */
    private String defaultRole = DEFAULT_ROLE_USER;

    /**
     * 管理员角色
     */
    private String adminRole = DEFAULT_ROLE_ADMIN;

    /**
     * 是否启用审计功能
     */
    private boolean auditEnabled = true;

    /**
     * 是否启用权限验证
     */
    private boolean permissionValidationEnabled = true;

    /**
     * 角色权限映射
     */
    private Map<String, List<String>> rolePermissions = new HashMap<>();

    /**
     * 访问控制规则
     */
    private List<String> accessControlRules = new ArrayList<>();

    /**
     * 构造函数
     */
    public AuthorizationConfig(Environment environment) {
        this.environment = environment;
        initializeDefaultRolePermissions();
        initializeDefaultAccessControlRules();
    }

    /**
     * 初始化默认角色权限
     */
    private void initializeDefaultRolePermissions() {
        // 默认角色权限配置
        rolePermissions.put(DEFAULT_ROLE_USER, Arrays.asList(PERMISSION_READ, PERMISSION_WRITE));
        rolePermissions.put(DEFAULT_ROLE_ADMIN, Arrays.asList(PERMISSION_READ, PERMISSION_WRITE, PERMISSION_DELETE, PERMISSION_MANAGE_USERS));
        rolePermissions.put(DEFAULT_ROLE_VIEWER, Arrays.asList(PERMISSION_READ));
    }

    /**
     * 初始化默认访问控制规则
     */
    private void initializeDefaultAccessControlRules() {
        // 默认访问控制规则
        accessControlRules.add(DEFAULT_ROLE_USER + " can " + PERMISSION_READ + " own data");
        accessControlRules.add(DEFAULT_ROLE_ADMIN + " can " + PERMISSION_READ + " all data");
        accessControlRules.add(DEFAULT_ROLE_ADMIN + " can " + PERMISSION_WRITE + " all data");
        accessControlRules.add(DEFAULT_ROLE_ADMIN + " can " + PERMISSION_DELETE + " all data");
        accessControlRules.add(DEFAULT_ROLE_VIEWER + " can " + PERMISSION_READ + " public data");
    }

    /**
     * 配置验证方法
     */
    @PostConstruct
    public void validateConfiguration() {
        // 验证授权配置
        if (!enabled) {
            throw new IllegalStateException("授权配置已禁用");
        }

        // 验证角色配置
        if (defaultRole == null || defaultRole.isBlank()) {
            throw new IllegalStateException("默认角色不能为空");
        }

        if (adminRole == null || adminRole.isBlank()) {
            throw new IllegalStateException("管理员角色不能为空");
        }

        // 验证角色权限配置
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            throw new IllegalStateException("角色权限配置不能为空");
        }

        // 验证访问控制规则
        if (accessControlRules == null || accessControlRules.isEmpty()) {
            throw new IllegalStateException("访问控制规则不能为空");
        }

        // 验证环境变量映射
        validateEnvironmentVariables();
    }

    /**
     * 验证环境变量映射
     */
    private void validateEnvironmentVariables() {
        // 检查环境变量是否覆盖了默认值
        String envEnabled = environment.getProperty("SECURITY_AUTHORIZATION_ENABLED");
        if (envEnabled != null && !envEnabled.isBlank()) {
            this.enabled = Boolean.parseBoolean(envEnabled);
        }

        String envDefaultRole = environment.getProperty("SECURITY_AUTHORIZATION_DEFAULT_ROLE");
        if (envDefaultRole != null && !envDefaultRole.isBlank()) {
            this.defaultRole = envDefaultRole;
        }

        String envAdminRole = environment.getProperty("SECURITY_AUTHORIZATION_ADMIN_ROLE");
        if (envAdminRole != null && !envAdminRole.isBlank()) {
            this.adminRole = envAdminRole;
        }

        String envAuditEnabled = environment.getProperty("SECURITY_AUTHORIZATION_AUDIT_ENABLED");
        if (envAuditEnabled != null && !envAuditEnabled.isBlank()) {
            this.auditEnabled = Boolean.parseBoolean(envAuditEnabled);
        }

        String envPermissionValidationEnabled = environment.getProperty("SECURITY_AUTHORIZATION_PERMISSION_VALIDATION_ENABLED");
        if (envPermissionValidationEnabled != null && !envPermissionValidationEnabled.isBlank()) {
            this.permissionValidationEnabled = Boolean.parseBoolean(envPermissionValidationEnabled);
        }
    }

    // Getter和Setter方法

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRoleBasedAccessControlEnabled() {
        return roleBasedAccessControlEnabled;
    }

    public void setRoleBasedAccessControlEnabled(boolean roleBasedAccessControlEnabled) {
        this.roleBasedAccessControlEnabled = roleBasedAccessControlEnabled;
    }

    public boolean isPermissionBasedAccessControlEnabled() {
        return permissionBasedAccessControlEnabled;
    }

    public void setPermissionBasedAccessControlEnabled(boolean permissionBasedAccessControlEnabled) {
        this.permissionBasedAccessControlEnabled = permissionBasedAccessControlEnabled;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public boolean isPermissionValidationEnabled() {
        return permissionValidationEnabled;
    }

    public void setPermissionValidationEnabled(boolean permissionValidationEnabled) {
        this.permissionValidationEnabled = permissionValidationEnabled;
    }

    public Map<String, List<String>> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(Map<String, List<String>> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public List<String> getAccessControlRules() {
        return accessControlRules;
    }

    public void setAccessControlRules(List<String> accessControlRules) {
        this.accessControlRules = accessControlRules;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
