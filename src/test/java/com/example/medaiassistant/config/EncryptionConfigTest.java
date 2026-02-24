package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptionConfig 单元测试
 * 按照TDD红-绿-重构流程实现加密配置管理
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(classes = EncryptionConfig.class)
@EnableConfigurationProperties(EncryptionConfig.class)
@TestPropertySource(properties = {
    "encryption.aes-key=test-encryption-key-1234567890",
    "encryption.aes-salt=test-salt-12345678",
    "encryption.algorithm=AES/CBC/PKCS5Padding",
    "encryption.config.keySize=256",
    "encryption.config.iterationCount=65536",
    "encryption.config.keyAlgorithm=PBKDF2WithHmacSHA256",
    "encryption.config.ivParameterSpec=AES/CBC/PKCS5Padding",
    "encryption.config.tagLength=128",
    "encryption.key-management.key-rotation-enabled=false",
    "encryption.key-management.rotation-interval-days=90",
    "encryption.key-management.key-storage=ENVIRONMENT_VARIABLE",
    "encryption.key-management.audit-enabled=true",
    // 禁用无关组件
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
class EncryptionConfigTest {

    @Autowired
    private EncryptionConfig encryptionConfig;

    @Autowired
    private Environment environment;

    /**
     * 🟢 绿阶段测试：验证配置绑定功能
     * 这个测试现在应该通过，因为EncryptionConfig类已实现
     */
    @Test
    void testEncryptionConfigBinding() {
        // 验证配置类能够正确绑定属性
        assertNotNull(encryptionConfig, "EncryptionConfig应该被正确注入");
        
        // 验证配置类字段不为空
        assertNotNull(encryptionConfig.getAesKey(), "AES密钥应该被正确绑定");
        assertNotNull(encryptionConfig.getAesSalt(), "AES盐值应该被正确绑定");
    }

    /**
     * 🟢 绿阶段测试：验证配置验证逻辑
     * 这个测试现在应该通过，因为validateConfiguration方法已实现
     */
    @Test
    void testEncryptionConfigValidation() {
        // 验证配置验证逻辑
        assertDoesNotThrow(() -> encryptionConfig.validateConfiguration(environment),
            "配置验证应该通过");
    }

    /**
     * 🟢 绿阶段测试：验证默认值设置
     * 这个测试现在应该通过，因为默认值配置已实现
     */
    @Test
    void testEncryptionConfigDefaultValues() {
        // 验证默认算法设置
        assertEquals("AES/CBC/PKCS5Padding", encryptionConfig.getAlgorithm(),
            "默认加密算法应该为AES/CBC/PKCS5Padding");
        
        // 验证配置子对象默认值
        assertNotNull(encryptionConfig.getConfig(), "Config子对象应该被初始化");
        assertEquals(256, encryptionConfig.getConfig().getKeySize(),
            "默认密钥大小应该为256");
        assertEquals(65536, encryptionConfig.getConfig().getIterationCount(),
            "默认迭代次数应该为65536");
        
        // 验证密钥管理配置默认值
        assertNotNull(encryptionConfig.getKeyManagement(), "KeyManagement子对象应该被初始化");
        assertFalse(encryptionConfig.getKeyManagement().isKeyRotationEnabled(),
            "默认密钥轮换应该禁用");
    }

    /**
     * 🟢 绿阶段测试：验证配置摘要生成
     * 这个测试现在应该通过，因为getConfigSummary方法已实现
     */
    @Test
    void testEncryptionConfigSummary() {
        // 验证配置摘要生成
        String summary = encryptionConfig.getConfigSummary();
        assertNotNull(summary, "配置摘要不应该为空");
        assertTrue(summary.contains("algorithm"), "配置摘要应该包含算法信息");
        assertTrue(summary.contains("keySize"), "配置摘要应该包含密钥大小信息");
    }

    /**
     * 🔵 重构阶段：验证配置验证逻辑的边界条件
     * 测试短密钥和短盐值的验证失败情况
     */
    @Test
    void testEncryptionConfigValidation_ShortKey() {
        EncryptionConfig configWithShortKey = new EncryptionConfig();
        configWithShortKey.setAesKey("short");
        configWithShortKey.setAesSalt("test-salt-12345678");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> configWithShortKey.validateConfiguration(environment),
            "短密钥应该抛出异常");
        
        assertTrue(exception.getMessage().contains("AES加密密钥长度不足"),
            "错误信息应该包含密钥长度不足的提示");
    }

    /**
     * 🔵 重构阶段：验证配置验证逻辑的边界条件
     * 测试短盐值的验证失败情况
     */
    @Test
    void testEncryptionConfigValidation_ShortSalt() {
        EncryptionConfig configWithShortSalt = new EncryptionConfig();
        configWithShortSalt.setAesKey("test-encryption-key-1234567890");
        configWithShortSalt.setAesSalt("short");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> configWithShortSalt.validateConfiguration(environment),
            "短盐值应该抛出异常");
        
        assertTrue(exception.getMessage().contains("AES加密盐值长度不足"),
            "错误信息应该包含盐值长度不足的提示");
    }

    /**
     * 🔵 重构阶段：验证空密钥的验证失败情况
     */
    @Test
    void testEncryptionConfigValidation_EmptyKey() {
        EncryptionConfig configWithEmptyKey = new EncryptionConfig();
        configWithEmptyKey.setAesKey("");
        configWithEmptyKey.setAesSalt("test-salt-12345678");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> configWithEmptyKey.validateConfiguration(environment),
            "空密钥应该抛出异常");
        
        assertTrue(exception.getMessage().contains("AES加密密钥不能为空"),
            "错误信息应该包含密钥为空的提示");
    }

    /**
     * 🔵 重构阶段：验证空盐值的验证失败情况
     */
    @Test
    void testEncryptionConfigValidation_EmptySalt() {
        EncryptionConfig configWithEmptySalt = new EncryptionConfig();
        configWithEmptySalt.setAesKey("test-encryption-key-1234567890");
        configWithEmptySalt.setAesSalt("");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> configWithEmptySalt.validateConfiguration(environment),
            "空盐值应该抛出异常");
        
        assertTrue(exception.getMessage().contains("AES加密盐值不能为空"),
            "错误信息应该包含盐值为空的提示");
    }
}
