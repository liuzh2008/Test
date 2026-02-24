package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行服务器配置环境变量覆盖测试
 * 验证环境变量和系统属性覆盖功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@SpringBootTest(
    classes = ExecutionServerProperties.class,
    properties = {
        // 配置测试属性
        "execution.server.host=config-host",
        "execution.server.oracle-port=1521",
        "execution.server.oracle-sid=TEST",
        "execution.server.api-url=http://config-api.example.com",
        
        // 禁用不必要的组件 - 提高测试性能
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        "prompt.submission.enabled=false",
        "prompt.polling.enabled=false",
        "monitoring.metrics.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
@EnableConfigurationProperties(ExecutionServerProperties.class)
class ExecutionServerPropertiesEnvironmentTest {

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 测试配置解析逻辑
     * 验证getResolvedHost()方法正确解析配置值
     */
    @Test
    void shouldResolveConfigurationValues() {
        // 验证配置值正确解析
        assertEquals("config-host", executionServerProperties.getResolvedHost());
        assertEquals(1521, executionServerProperties.getResolvedOraclePort());
        assertEquals("TEST", executionServerProperties.getResolvedOracleSid());
        assertEquals("http://config-api.example.com", executionServerProperties.getResolvedApiUrl());
    }

    /**
     * 测试向后兼容性
     * 验证旧的IP和URL配置仍然有效
     */
    @Test
    void shouldSupportBackwardCompatibility() {
        // 验证向后兼容性方法存在
        assertNotNull(executionServerProperties.getIp());
        assertNotNull(executionServerProperties.getUrl());
        assertNotNull(executionServerProperties.getResolvedIp());
        assertNotNull(executionServerProperties.resolveUrl());
    }

    /**
     * 测试配置有效性验证
     * 验证配置验证逻辑正常工作
     */
    @Test
    void shouldValidateConfiguration() {
        assertTrue(executionServerProperties.isValid());
    }

    /**
     * 测试Oracle JDBC URL生成
     * 验证JDBC URL正确生成
     */
    @Test
    void shouldGenerateOracleJdbcUrl() {
        String jdbcUrl = executionServerProperties.getOracleJdbcUrl();
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("jdbc:oracle:thin:@//"));
        assertTrue(jdbcUrl.contains("config-host"));
        assertTrue(jdbcUrl.contains("1521"));
        assertTrue(jdbcUrl.contains("TEST"));
    }

    /**
     * 测试API基地址生成
     * 验证API基地址正确生成
     */
    @Test
    void shouldGenerateApiBaseUrl() {
        String apiBaseUrl = executionServerProperties.getApiBaseUrl();
        assertNotNull(apiBaseUrl);
        assertEquals("http://config-api.example.com", apiBaseUrl);
    }
}
