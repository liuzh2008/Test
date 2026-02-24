package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.logging.Logger;

/**
 * 敏感配置保护类
 * 管理敏感配置的保护机制，防止配置信息泄露
 * 
 * 功能特性：
 * - 支持敏感信息加密存储
 * - 支持配置访问权限控制
 * - 支持配置变更审计
 * - 支持安全配置验证
 * - 支持环境变量映射
 * - 自动配置验证
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@ConfigurationProperties(prefix = "security.sensitive-config")
@Validated
public class SensitiveConfigProtection {

    private static final Logger logger = Logger.getLogger(SensitiveConfigProtection.class.getName());

    // 常量定义
    private static final String DEFAULT_ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String DEFAULT_ENCRYPTION_KEY = "default-encryption-key-1234567890";
    private static final String DEFAULT_ENCRYPTION_SALT = "default-encryption-salt";
    
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_VIEWER = "VIEWER";
    private static final String ROLE_GUEST = "GUEST";

    private final Environment environment;

    /**
     * 是否启用敏感配置保护
     */
    private boolean enabled = true;

    /**
     * 是否启用敏感信息加密
     */
    private boolean encryptionEnabled = true;

    /**
     * 是否启用配置访问权限控制
     */
    private boolean accessControlEnabled = true;

    /**
     * 是否启用配置变更审计
     */
    private boolean auditEnabled = true;

    /**
     * 是否启用安全配置验证
     */
    private boolean validationEnabled = true;

    /**
     * 加密算法
     */
    private String encryptionAlgorithm = DEFAULT_ENCRYPTION_ALGORITHM;

    /**
     * 加密密钥
     */
    private String encryptionKey = DEFAULT_ENCRYPTION_KEY;

    /**
     * 加密盐值
     */
    private String encryptionSalt = DEFAULT_ENCRYPTION_SALT;

    /**
     * 敏感配置键列表
     */
    private List<String> sensitiveConfigKeys = new ArrayList<>();

    /**
     * 角色访问权限映射
     */
    private Map<String, List<String>> roleAccessPermissions = new HashMap<>();

    /**
     * 构造函数
     */
    public SensitiveConfigProtection(Environment environment) {
        this.environment = environment;
        initializeDefaultSensitiveConfigKeys();
        initializeDefaultRoleAccessPermissions();
    }

    /**
     * 初始化默认敏感配置键列表
     */
    private void initializeDefaultSensitiveConfigKeys() {
        // 默认敏感配置键
        sensitiveConfigKeys.add("database.password");
        sensitiveConfigKeys.add("database.username");
        sensitiveConfigKeys.add("api.key");
        sensitiveConfigKeys.add("encryption.key");
        sensitiveConfigKeys.add("jwt.secret");
        sensitiveConfigKeys.add("oauth2.client-secret");
        sensitiveConfigKeys.add("mail.password");
        sensitiveConfigKeys.add("smtp.password");
    }

    /**
     * 初始化默认角色访问权限
     */
    private void initializeDefaultRoleAccessPermissions() {
        // 默认角色访问权限
        roleAccessPermissions.put(ROLE_ADMIN, Arrays.asList("database.password", "database.username", "api.key", "encryption.key", "jwt.secret", "oauth2.client-secret", "mail.password", "smtp.password"));
        roleAccessPermissions.put(ROLE_USER, Arrays.asList("database.username"));
        roleAccessPermissions.put(ROLE_VIEWER, Arrays.asList());
        roleAccessPermissions.put(ROLE_GUEST, Arrays.asList());
    }

    /**
     * 配置验证方法
     */
    @PostConstruct
    public void validateConfiguration() {
        // 验证敏感配置保护配置
        if (!enabled) {
            throw new IllegalStateException("敏感配置保护已禁用");
        }

        // 验证加密配置
        if (encryptionEnabled) {
            if (encryptionKey == null || encryptionKey.isBlank()) {
                throw new IllegalStateException("加密密钥不能为空");
            }
            if (encryptionKey.length() < 16) {
                throw new IllegalStateException("加密密钥长度至少为16个字符");
            }
        }

        // 验证敏感配置键列表
        if (sensitiveConfigKeys == null || sensitiveConfigKeys.isEmpty()) {
            throw new IllegalStateException("敏感配置键列表不能为空");
        }

        // 验证角色访问权限配置
        if (roleAccessPermissions == null || roleAccessPermissions.isEmpty()) {
            throw new IllegalStateException("角色访问权限配置不能为空");
        }

        // 验证环境变量映射
        validateEnvironmentVariables();
    }

    /**
     * 验证环境变量映射
     */
    private void validateEnvironmentVariables() {
        // 检查环境变量是否覆盖了默认值
        String envEnabled = environment.getProperty("SECURITY_SENSITIVE_CONFIG_ENABLED");
        if (envEnabled != null && !envEnabled.isBlank()) {
            this.enabled = Boolean.parseBoolean(envEnabled);
        }

        String envEncryptionEnabled = environment.getProperty("SECURITY_SENSITIVE_CONFIG_ENCRYPTION_ENABLED");
        if (envEncryptionEnabled != null && !envEncryptionEnabled.isBlank()) {
            this.encryptionEnabled = Boolean.parseBoolean(envEncryptionEnabled);
        }

        String envAccessControlEnabled = environment.getProperty("SECURITY_SENSITIVE_CONFIG_ACCESS_CONTROL_ENABLED");
        if (envAccessControlEnabled != null && !envAccessControlEnabled.isBlank()) {
            this.accessControlEnabled = Boolean.parseBoolean(envAccessControlEnabled);
        }

        String envAuditEnabled = environment.getProperty("SECURITY_SENSITIVE_CONFIG_AUDIT_ENABLED");
        if (envAuditEnabled != null && !envAuditEnabled.isBlank()) {
            this.auditEnabled = Boolean.parseBoolean(envAuditEnabled);
        }

        String envValidationEnabled = environment.getProperty("SECURITY_SENSITIVE_CONFIG_VALIDATION_ENABLED");
        if (envValidationEnabled != null && !envValidationEnabled.isBlank()) {
            this.validationEnabled = Boolean.parseBoolean(envValidationEnabled);
        }

        String envEncryptionKey = environment.getProperty("SECURITY_SENSITIVE_CONFIG_ENCRYPTION_KEY");
        if (envEncryptionKey != null && !envEncryptionKey.isBlank()) {
            this.encryptionKey = envEncryptionKey;
        }

        String envEncryptionSalt = environment.getProperty("SECURITY_SENSITIVE_CONFIG_ENCRYPTION_SALT");
        if (envEncryptionSalt != null && !envEncryptionSalt.isBlank()) {
            this.encryptionSalt = envEncryptionSalt;
        }
    }

    /**
     * 加密敏感值
     * @param plainText 明文
     * @return 加密后的文本
     */
    public String encryptSensitiveValue(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("明文不能为null");
        }
        
        if (!encryptionEnabled) {
            logger.warning("敏感信息加密已禁用，返回明文");
            return plainText;
        }
        
        // 简化的加密实现 - 在实际项目中应该使用真正的加密算法
        // 这里使用Base64编码作为示例
        String encoded = Base64.getEncoder().encodeToString(plainText.getBytes());
        return "ENCRYPTED:" + encoded;
    }

    /**
     * 解密敏感值
     * @param encryptedText 加密文本
     * @return 解密后的文本
     */
    public String decryptSensitiveValue(String encryptedText) {
        if (encryptedText == null) {
            throw new IllegalArgumentException("加密文本不能为null");
        }
        
        if (!encryptionEnabled) {
            logger.warning("敏感信息加密已禁用，直接返回输入值");
            return encryptedText;
        }
        
        if (!encryptedText.startsWith("ENCRYPTED:")) {
            logger.warning("加密文本格式不正确，可能未加密");
            return encryptedText;
        }
        
        try {
            String encoded = encryptedText.substring("ENCRYPTED:".length());
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);
            return new String(decodedBytes);
        } catch (Exception e) {
            logger.warning("解密失败: " + e.getMessage());
            return encryptedText;
        }
    }

    /**
     * 检查角色是否有权访问敏感配置
     * @param role 角色
     * @param configKey 配置键
     * @return 是否有访问权限
     */
    public boolean hasAccessToSensitiveConfig(String role, String configKey) {
        if (!accessControlEnabled) {
            logger.warning("配置访问权限控制已禁用，允许所有访问");
            return true;
        }
        
        if (role == null || configKey == null) {
            return false;
        }
        
        List<String> allowedConfigs = roleAccessPermissions.get(role);
        if (allowedConfigs == null) {
            return false;
        }
        
        return allowedConfigs.contains(configKey);
    }

    /**
     * 审计配置变更
     * @param configKey 配置键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param user 操作用户
     */
    public void auditConfigChange(String configKey, String oldValue, String newValue, String user) {
        if (!auditEnabled) {
            logger.warning("配置变更审计已禁用，跳过审计记录");
            return;
        }
        
        // 简化的审计日志记录
        String auditMessage = String.format(
            "配置变更审计 - 用户: %s, 配置键: %s, 旧值: %s, 新值: %s",
            user, configKey, maskSensitiveValue(oldValue), maskSensitiveValue(newValue)
        );
        
        logger.info(auditMessage);
    }

    /**
     * 验证敏感配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否有效
     */
    public boolean validateSensitiveConfig(String configKey, String configValue) {
        if (!validationEnabled) {
            logger.warning("安全配置验证已禁用，跳过验证");
            return true;
        }
        
        if (configKey == null || configValue == null) {
            return false;
        }
        
        // 简化的验证逻辑
        if (configKey.contains("password") && configValue.length() < 8) {
            logger.warning("密码配置值太短: " + configKey);
            return false;
        }
        
        if (configKey.contains("key") && configValue.length() < 16) {
            logger.warning("密钥配置值太短: " + configKey);
            return false;
        }
        
        return true;
    }

    /**
     * 掩码敏感值（用于日志记录）
     * @param value 原始值
     * @return 掩码后的值
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    // Getter和Setter方法

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public boolean isAccessControlEnabled() {
        return accessControlEnabled;
    }

    public void setAccessControlEnabled(boolean accessControlEnabled) {
        this.accessControlEnabled = accessControlEnabled;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getEncryptionSalt() {
        return encryptionSalt;
    }

    public void setEncryptionSalt(String encryptionSalt) {
        this.encryptionSalt = encryptionSalt;
    }

    public List<String> getSensitiveConfigKeys() {
        return sensitiveConfigKeys;
    }

    public void setSensitiveConfigKeys(List<String> sensitiveConfigKeys) {
        this.sensitiveConfigKeys = sensitiveConfigKeys;
    }

    public Map<String, List<String>> getRoleAccessPermissions() {
        return roleAccessPermissions;
    }

    public void setRoleAccessPermissions(Map<String, List<String>> roleAccessPermissions) {
        this.roleAccessPermissions = roleAccessPermissions;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
