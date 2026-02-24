package com.example.medaiassistant.integration;

import com.example.medaiassistant.config.ConfigurationValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置验证集成测试
 * 简化版本 - 专注于基本的配置验证功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-05
 */
@SpringBootTest
class ConfigurationValidationIntegrationTest {

    @Autowired
    private ConfigurationValidationService configurationValidationService;

    /**
     * 基本配置验证测试
     * 验证配置验证服务的基本功能
     */
    @Test
    void shouldValidateBasicConfiguration() {
        // 验证配置验证服务能够正确初始化
        assertNotNull(configurationValidationService, "配置验证服务应该被正确注入");
        
        // 验证配置验证功能
        boolean isValid = configurationValidationService.isConfigurationValid();
        assertTrue(isValid, "基本配置应该有效");
    }
}
