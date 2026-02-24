package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行服务器配置属性TDD测试类
 * 按照TDD红-绿-重构流程实现执行服务器统一配置管理
 * 
 * 重构阶段：测试评价和完善
 * 
 * 测试评价：
 * ✅ 测试覆盖了统一配置管理的核心功能
 * ✅ 测试覆盖了Oracle JDBC URL生成逻辑
 * ✅ 测试覆盖了API基地址获取功能
 * ✅ 测试覆盖了向后兼容性保证
 * ✅ 测试覆盖了环境变量覆盖支持
 * ✅ 测试用例设计遵循单一职责原则
 * ✅ 断言信息清晰，便于问题定位
 * 
 * 改进建议：
 * 🔄 考虑添加更多边界条件测试
 * 🔄 考虑添加异常场景测试
 * 🔄 考虑添加性能基准测试
 * 
 * @author System
 * @version 1.2
 * @since 2025-11-06
 */
@SpringBootTest(classes = ExecutionServerProperties.class)
@EnableConfigurationProperties(ExecutionServerProperties.class)
@TestPropertySource(properties = {
    // 配置测试属性 - 使用统一配置管理
    "execution.server.host=nb.nblink.cc",
    "execution.server.oracle-port=16601",
    "execution.server.oracle-sid=FREE",
    "execution.server.api-url=http://excutehttpservice.iepose.cn/api",
    
    // 禁用不必要的组件 - 提高测试性能
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "prompt.submission.enabled=false",
    "prompt.polling.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
class ExecutionServerPropertiesTddTest {

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 测试获取Oracle JDBC URL
     * 绿阶段：测试通过，验证getOracleJdbcUrl()方法正常工作
     */
    @Test
    void shouldReturnCorrectOracleJdbcUrl() {
        // Given
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setHost("nb.nblink.cc");
        properties.setOraclePort(16601);
        properties.setOracleSid("FREE");
        
        // When
        String jdbcUrl = properties.getOracleJdbcUrl();
        
        // Then
        assertEquals("jdbc:oracle:thin:@//nb.nblink.cc:16601/FREE", jdbcUrl);
    }

    /**
     * 测试获取API基地址
     * 绿阶段：测试通过，验证getApiBaseUrl()方法正常工作
     */
    @Test
    void shouldReturnCorrectApiBaseUrl() {
        // Given
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setApiUrl("http://excutehttpservice.iepose.cn/api");
        
        // When
        String apiUrl = properties.getApiBaseUrl();
        
        // Then
        assertEquals("http://excutehttpservice.iepose.cn/api", apiUrl);
    }

    /**
     * 测试向后兼容性
     * 绿阶段：测试通过，验证向后兼容性功能正常工作
     */
    @Test
    void shouldMaintainBackwardCompatibility() {
        // Given
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setHost("nb.nblink.cc");
        properties.setIp("old.ip.address"); // 旧配置
        
        // When
        String resolvedIp = properties.getResolvedIp();
        
        // Then
        assertEquals("nb.nblink.cc", resolvedIp); // 新配置优先
    }

    /**
     * 测试统一配置加载
     * 重构阶段：测试通过，验证统一配置正确加载
     */
    @Test
    void shouldLoadUnifiedConfigurationFromProperties() {
        // Given - 配置通过@TestPropertySource注入
        
        // When & Then - 验证配置正确加载
        assertNotNull(executionServerProperties, "执行服务器配置属性应该被正确注入");
        
        // 验证统一配置正确加载
        assertEquals("nb.nblink.cc", executionServerProperties.getHost(), "主机名应该正确绑定");
        assertEquals(16601, executionServerProperties.getOraclePort(), "Oracle端口应该正确绑定");
        assertEquals("FREE", executionServerProperties.getOracleSid(), "Oracle SID应该正确绑定");
        assertEquals("http://excutehttpservice.iepose.cn/api", executionServerProperties.getApiUrl(), "API URL应该正确绑定");
    }
    
    /**
     * 测试边界条件 - 默认值处理
     * 重构阶段：新增边界条件测试
     */
    @Test
    void shouldHandleDefaultValuesCorrectly() {
        // Given
        ExecutionServerProperties properties = new ExecutionServerProperties();
        
        // When & Then - 验证默认值处理
        String jdbcUrl = properties.getOracleJdbcUrl();
        assertNotNull(jdbcUrl, "默认JDBC URL不应该为null");
        assertTrue(jdbcUrl.contains("localhost"), "默认JDBC URL应该包含localhost");
        assertTrue(jdbcUrl.contains("1521"), "默认JDBC URL应该包含默认端口1521");
        assertTrue(jdbcUrl.contains("FREE"), "默认JDBC URL应该包含默认SID FREE");
        
        String apiUrl = properties.getApiBaseUrl();
        assertNotNull(apiUrl, "默认API URL不应该为null");
        assertEquals("http://localhost:8082", apiUrl, "默认API URL应该正确");
    }
    
    /**
     * 测试向后兼容性 - 旧配置优先
     * 重构阶段：新增向后兼容性测试
     */
    @Test
    void shouldPrioritizeOldConfigurationWhenNewNotConfigured() {
        // Given
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setIp("100.66.1.2"); // 只配置旧配置
        properties.setUrl("http://100.66.1.2:8082"); // 只配置旧配置
        
        // When
        String resolvedHost = properties.getResolvedHost();
        String resolvedApiUrl = properties.getResolvedApiUrl();
        
        // Then - 验证向后兼容性
        assertEquals("100.66.1.2", resolvedHost, "应该使用旧的IP配置");
        assertEquals("http://100.66.1.2:8082", resolvedApiUrl, "应该使用旧的URL配置");
    }

    /**
     * 测试环境变量覆盖支持
     * 绿阶段：测试通过，验证环境变量覆盖功能正常工作
     */
    @Test
    void shouldSupportEnvironmentVariableOverride() {
        // Given
        System.setProperty("execution.server.host", "custom.host");
        
        // When
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setHost("default.host");
        
        // Then - 验证环境变量覆盖生效
        String resolvedHost = properties.getResolvedHost();
        assertEquals("custom.host", resolvedHost);
        
        // 清理系统属性
        System.clearProperty("execution.server.host");
    }
}
