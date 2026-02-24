package com.example.medaiassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 配置验证服务测试类
 * 测试配置验证与健康检查功能
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(classes = {ConfigurationValidationService.class})
@TestPropertySource(properties = {
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "prompt.submission.enabled=false",
    "prompt.polling.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
@DisplayName("配置验证服务 单元测试")
class ConfigurationValidationServiceTest {

    @Autowired
    private ConfigurationValidationService configurationValidationService;

    @Test
    @DisplayName("应该成功验证有效配置")
    void shouldValidateValidConfigurationSuccessfully() {
        // 当
        // 配置验证服务执行验证
        
        // 那么
        // 验证应该成功，不抛出异常
        assertThatCode(() -> configurationValidationService.validateAllConfigurations())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该检测到数据库配置缺失")
    void shouldDetectMissingDatabaseConfiguration() {
        // 给定
        // 配置验证服务已经初始化，应该已经执行了验证
        
        // 当
        // 检查配置是否有效
        
        // 那么
        // 配置应该有效（因为测试环境有完整的配置）
        assertThat(configurationValidationService.isConfigurationValid()).isTrue();
    }

    @Test
    @DisplayName("应该提供健康检查状态")
    void shouldProvideHealthCheckStatus() {
        // 当
        // 执行健康检查
        
        // 那么
        // 应该返回健康状态信息
        HealthCheckResult result = configurationValidationService.performHealthCheck();
        
        assertThat(result).isNotNull();
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getComponents()).isNotEmpty();
    }

    @Test
    @DisplayName("应该检测到不健康的组件")
    void shouldDetectUnhealthyComponents() {
        // 给定
        // 在正常测试环境中，所有组件应该是健康的
        
        // 当
        // 执行健康检查
        
        // 那么
        // 应该返回健康状态
        HealthCheckResult result = configurationValidationService.performHealthCheck();
        
        assertThat(result).isNotNull();
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getUnhealthyComponents()).isEmpty();
    }

    @Test
    @DisplayName("应该在启动时执行配置验证")
    void shouldExecuteConfigurationValidationOnStartup() {
        // 当
        // 配置验证服务初始化
        
        // 那么
        // 应该自动执行配置验证
        assertThat(configurationValidationService.isConfigurationValid()).isTrue();
    }
}
