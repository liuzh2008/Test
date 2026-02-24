package com.example.medaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

/**
 * 加密配置类
 * 
 * 统一管理AES加密算法参数和密钥管理配置，支持全自动化部署。
 * 与宿主机环境变量模板对齐，确保配置一致性和安全性。
 * 
 * @author TDD实施团队
 * @version 1.0
 * @since 2025-11-06
 * 
 * @Configuration 声明为Spring配置类
 * @ConfigurationProperties 绑定encryption前缀的配置属性
 * @Validated 启用配置验证
 * @Data Lombok注解，自动生成getter/setter等方法
 */
@Configuration
@ConfigurationProperties(prefix = "encryption")
@Validated
@Data
public class EncryptionConfig {
    
    /**
     * AES加密密钥
     * 
     * 用于AES加密算法的密钥，必须至少16个字符长度。
     * 支持通过ENCRYPTION_AES_KEY环境变量注入。
     * 
     * @required true
     * @minLength 16
     * @example "my-secret-key-1234567890"
     */
    private String aesKey;
    
    /**
     * AES加密盐值
     * 
     * 用于AES加密算法的盐值，必须至少8个字符长度。
     * 支持通过ENCRYPTION_AES_SALT环境变量注入。
     * 
     * @required true
     * @minLength 8
     * @example "my-salt-value-123"
     */
    private String aesSalt;
    
    /**
     * 加密算法
     * 
     * 指定使用的加密算法，默认为AES/CBC/PKCS5Padding。
     * 
     * @default "AES/CBC/PKCS5Padding"
     * @example "AES/CBC/PKCS5Padding"
     */
    private String algorithm = "AES/CBC/PKCS5Padding";
    
    /**
     * 加密参数配置
     * 
     * 包含密钥大小、迭代次数、算法类型等详细加密参数。
     * 
     * @see Config
     */
    private Config config = new Config();
    
    /**
     * 密钥管理配置
     * 
     * 包含密钥轮换机制、存储方式、审计功能等密钥管理参数。
     * 
     * @see KeyManagementConfig
     */
    private KeyManagementConfig keyManagement = new KeyManagementConfig();
    
    /**
     * 加密参数配置类
     * 
     * 包含详细的加密算法参数配置，如密钥大小、迭代次数等。
     * 
     * @author TDD实施团队
     * @version 1.0
     * @since 2025-11-06
     */
    @Data
    public static class Config {
        /**
         * 密钥大小（位）
         * 
         * 指定加密密钥的位数，默认为256位。
         * 
         * @default 256
         * @min 128
         * @max 512
         */
        @Positive
        private int keySize = 256;
        
        /**
         * 迭代次数
         * 
         * 密钥派生函数的迭代次数，默认为65536次。
         * 
         * @default 65536
         * @min 1000
         * @max 1000000
         */
        @Positive
        private int iterationCount = 65536;
        
        /**
         * 密钥算法
         * 
         * 密钥派生算法，默认为PBKDF2WithHmacSHA256。
         * 
         * @default "PBKDF2WithHmacSHA256"
         */
        private String keyAlgorithm = "PBKDF2WithHmacSHA256";
        
        /**
         * 初始化向量参数
         * 
         * 加密算法的初始化向量参数，默认为AES/CBC/PKCS5Padding。
         * 
         * @default "AES/CBC/PKCS5Padding"
         */
        private String ivParameterSpec = "AES/CBC/PKCS5Padding";
        
        /**
         * 标签长度（位）
         * 
         * 认证标签的长度，默认为128位。
         * 
         * @default 128
         * @min 64
         * @max 256
         */
        @Positive
        private int tagLength = 128;
    }
    
    /**
     * 密钥管理配置类
     * 
     * 包含密钥轮换、存储、审计等密钥管理相关配置。
     * 
     * @author TDD实施团队
     * @version 1.0
     * @since 2025-11-06
     */
    @Data
    public static class KeyManagementConfig {
        /**
         * 密钥轮换启用状态
         * 
         * 是否启用密钥轮换机制，默认为false。
         * 
         * @default false
         */
        private boolean keyRotationEnabled = false;
        
        /**
         * 轮换间隔天数
         * 
         * 密钥轮换的间隔天数，默认为90天。
         * 
         * @default 90
         * @min 7
         * @max 365
         */
        @Positive
        private int rotationIntervalDays = 90;
        
        /**
         * 密钥存储方式
         * 
         * 密钥的存储方式，支持ENVIRONMENT_VARIABLE、SECRET_MANAGER、VAULT。
         * 
         * @default "ENVIRONMENT_VARIABLE"
         * @validValues ["ENVIRONMENT_VARIABLE", "SECRET_MANAGER", "VAULT"]
         */
        private String keyStorage = "ENVIRONMENT_VARIABLE";
        
        /**
         * 审计启用状态
         * 
         * 是否启用密钥使用审计，默认为true。
         * 
         * @default true
         */
        private boolean auditEnabled = true;
    }
    
    /**
     * 配置验证方法
     * 
     * 验证加密配置的完整性和有效性，确保所有必需参数都已正确配置。
     * 检查AES密钥和盐值的非空性、长度要求等。
     * 
     * @param env Spring环境对象，用于获取环境变量信息
     * @throws IllegalStateException 当配置验证失败时抛出异常
     * 
     * @example
     * // 正确配置示例
     * EncryptionConfig config = new EncryptionConfig();
     * config.setAesKey("my-secret-key-1234567890");
     * config.setAesSalt("my-salt-value-123");
     * config.validateConfiguration(environment); // 验证通过
     * 
     * @example
     * // 错误配置示例
     * EncryptionConfig config = new EncryptionConfig();
     * config.setAesKey("short"); // 密钥长度不足
     * config.validateConfiguration(environment); // 抛出IllegalStateException
     */
    public void validateConfiguration(Environment env) {
        if (aesKey == null || aesKey.trim().isEmpty()) {
            throw new IllegalStateException("AES加密密钥不能为空，请检查ENCRYPTION_AES_KEY环境变量");
        }
        
        if (aesSalt == null || aesSalt.trim().isEmpty()) {
            throw new IllegalStateException("AES加密盐值不能为空，请检查ENCRYPTION_AES_SALT环境变量");
        }
        
        // 验证密钥长度
        if (aesKey.length() < 16) {
            throw new IllegalStateException("AES加密密钥长度不足，至少需要16个字符");
        }
        
        // 验证盐值长度
        if (aesSalt.length() < 8) {
            throw new IllegalStateException("AES加密盐值长度不足，至少需要8个字符");
        }
    }
    
    /**
     * 获取加密配置摘要
     * 
     * 生成加密配置的摘要信息，用于日志记录和调试。
     * 摘要不包含敏感信息（如密钥和盐值），确保安全性。
     * 
     * @return 配置摘要字符串，包含算法、密钥大小、轮换状态等信息
     * 
     * @example
     * EncryptionConfig config = new EncryptionConfig();
     * config.setAlgorithm("AES/CBC/PKCS5Padding");
     * config.getConfig().setKeySize(256);
     * config.getKeyManagement().setKeyRotationEnabled(false);
     * String summary = config.getConfigSummary(); 
     * // 返回: "EncryptionConfig{algorithm=AES/CBC/PKCS5Padding, keySize=256, keyRotation=false}"
     */
    public String getConfigSummary() {
        return String.format("EncryptionConfig{algorithm=%s, keySize=%d, keyRotation=%b}", 
                           algorithm, config.keySize, keyManagement.keyRotationEnabled);
    }
}
