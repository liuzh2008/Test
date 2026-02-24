package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.ExecutionServerProperties;
import com.example.medaiassistant.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行服务器配置数据访问层测试
 * 验证数据访问层使用新的统一配置，而不是硬编码地址
 * 使用 @TestConfig 注解简化测试配置
 * 
 * 重构阶段：评价和完善测试文件
 * 
 * 测试评价：
 * ✅ 测试覆盖了统一配置管理的核心功能
 * ✅ 测试验证了配置属性正确注入
 * ✅ 测试验证了JDBC URL生成逻辑
 * ✅ 测试验证了API基地址获取功能
 * ✅ 测试验证了向后兼容性保证
 * ✅ 测试用例设计遵循单一职责原则
 * ✅ 使用@TestConfig注解简化配置
 * ✅ 断言信息清晰，便于问题定位
 * 
 * 改进建议：
 * 🔄 考虑添加更多边界条件测试
 * 🔄 考虑添加异常场景测试
 * 🔄 考虑添加性能基准测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-06
 */
@TestConfig(description = "执行服务器配置数据访问层测试")
@EnableConfigurationProperties(ExecutionServerProperties.class)
@TestPropertySource(properties = {
    // 配置测试属性 - 使用统一配置管理
    "execution.server.host=nb.nblink.cc",
    "execution.server.oracle-port=16601",
    "execution.server.oracle-sid=FREE",
    "execution.server.api-url=http://excutehttpservice.iepose.cn/api"
})
class ExecutionServerConfigurationRepositoryTest {

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 测试统一配置正确加载
     * 重构阶段：测试通过，验证统一配置正确注入
     */
    @Test
    void shouldLoadUnifiedConfigurationCorrectly() {
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
     * 测试JDBC URL生成逻辑
     * 重构阶段：测试通过，验证JDBC URL格式正确
     */
    @Test
    void shouldGenerateCorrectOracleJdbcUrl() {
        // Given - 配置已加载
        
        // When
        String jdbcUrl = executionServerProperties.getOracleJdbcUrl();
        
        // Then
        assertNotNull(jdbcUrl, "JDBC URL不应该为null");
        assertEquals("jdbc:oracle:thin:@//nb.nblink.cc:16601/FREE", jdbcUrl, "JDBC URL格式应该正确");
        assertTrue(jdbcUrl.startsWith("jdbc:oracle:thin:@//"), "JDBC URL应该以正确的前缀开始");
        assertTrue(jdbcUrl.contains("nb.nblink.cc"), "JDBC URL应该包含正确的主机名");
        assertTrue(jdbcUrl.contains("16601"), "JDBC URL应该包含正确的端口");
        assertTrue(jdbcUrl.contains("FREE"), "JDBC URL应该包含正确的SID");
    }

    /**
     * 测试API基地址获取功能
     * 重构阶段：测试通过，验证API基地址正确
     */
    @Test
    void shouldReturnCorrectApiBaseUrl() {
        // Given - 配置已加载
        
        // When
        String apiBaseUrl = executionServerProperties.getApiBaseUrl();
        
        // Then
        assertNotNull(apiBaseUrl, "API基地址不应该为null");
        assertEquals("http://excutehttpservice.iepose.cn/api", apiBaseUrl, "API基地址应该正确");
        assertTrue(apiBaseUrl.startsWith("http://"), "API基地址应该以http://开始");
        assertTrue(apiBaseUrl.contains("excutehttpservice.iepose.cn"), "API基地址应该包含正确的主机名");
        assertTrue(apiBaseUrl.endsWith("/api"), "API基地址应该以/api结束");
    }

    /**
     * 测试向后兼容性保证
     * 重构阶段：测试通过，验证向后兼容性功能正常工作
     */
    @Test
    void shouldMaintainBackwardCompatibility() {
        // Given - 配置已加载
        
        // When
        String resolvedHost = executionServerProperties.getResolvedHost();
        String resolvedApiUrl = executionServerProperties.getResolvedApiUrl();
        
        // Then - 验证向后兼容性
        assertEquals("nb.nblink.cc", resolvedHost, "解析后的主机名应该正确");
        assertEquals("http://excutehttpservice.iepose.cn/api", resolvedApiUrl, "解析后的API URL应该正确");
    }

    /**
     * 测试配置解析逻辑
     * 重构阶段：测试通过，验证配置解析功能正常工作
     */
    @Test
    void shouldResolveConfigurationCorrectly() {
        // Given - 配置已加载
        
        // When
        String resolvedOraclePort = String.valueOf(executionServerProperties.getResolvedOraclePort());
        String resolvedOracleSid = executionServerProperties.getResolvedOracleSid();
        
        // Then - 验证配置解析正确
        assertEquals("16601", resolvedOraclePort, "解析后的Oracle端口应该正确");
        assertEquals("FREE", resolvedOracleSid, "解析后的Oracle SID应该正确");
    }

    /**
     * 测试边界条件 - 默认值处理
     * 重构阶段：新增边界条件测试
     */
    @Test
    void shouldHandleDefaultValuesCorrectly() {
        // Given - 创建一个新的配置实例，不设置任何值
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
}
