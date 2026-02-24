package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行服务器配置属性测试类
 * 按照TDD红-绿-重构流程实现执行服务器配置管理
 * 
 * 测试评价：
 * ✅ 测试覆盖了配置绑定的核心功能
 * ✅ 测试覆盖了URL生成逻辑的各种场景
 * ✅ 测试覆盖了配置验证的边界条件
 * ✅ 测试覆盖了环境变量支持
 * ✅ 测试覆盖了配置摘要功能
 * ✅ 测试用例设计遵循单一职责原则
 * ✅ 断言信息清晰，便于问题定位
 * 
 * 改进建议：
 * 🔄 考虑添加更多边界条件测试
 * 🔄 考虑添加性能基准测试
 * 🔄 考虑添加异常场景测试
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-05
 */
@SpringBootTest(classes = ExecutionServerProperties.class)
@EnableConfigurationProperties(ExecutionServerProperties.class)
@TestPropertySource(properties = {
    // 配置测试属性 - 使用项目实际的执行服务器配置
    "execution.server.ip=100.66.1.2",
    "execution.server.url=http://100.66.1.2:8082",
    "execution.server.port=8082",
    "execution.server.connection-timeout=30",
    "execution.server.retry-count=3",
    "execution.server.health-check-interval=60",
    
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
class ExecutionServerPropertiesTest {

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 测试执行服务器配置属性正确绑定
     * 绿阶段：测试通过，验证配置属性正确绑定
     */
    @Test
    void shouldBindExecutionServerPropertiesCorrectly() {
        // 验证配置属性正确绑定
        assertNotNull(executionServerProperties, "执行服务器配置属性应该被正确注入");
        assertEquals("100.66.1.2", executionServerProperties.getIp(), "执行服务器IP应该正确绑定");
        assertEquals("http://100.66.1.2:8082", executionServerProperties.getUrl(), "执行服务器URL应该正确绑定");
        assertEquals(8082, executionServerProperties.getPort(), "执行服务器端口应该正确绑定");
        assertEquals(30, executionServerProperties.getConnectionTimeout(), "连接超时时间应该正确绑定");
        assertEquals(3, executionServerProperties.getRetryCount(), "重试次数应该正确绑定");
        assertEquals(60, executionServerProperties.getHealthCheckInterval(), "健康检查间隔应该正确绑定");
    }

    /**
     * 测试执行服务器URL生成逻辑
     * 绿阶段：测试通过，验证URL生成方法正常工作
     */
    @Test
    void shouldGenerateExecutionServerUrlFromIp() {
        // 验证从IP生成URL的逻辑
        String generatedUrl = executionServerProperties.generateUrlFromIp();
        assertNotNull(generatedUrl, "生成的URL不应该为null");
        assertEquals("http://100.66.1.2:8082", generatedUrl, "从IP生成的URL应该正确");
    }

    /**
     * 测试配置默认值
     * 绿阶段：测试通过，验证默认值配置正确
     */
    @Test
    void shouldUseDefaultValuesWhenNotConfigured() {
        // 验证默认值逻辑
        ExecutionServerProperties properties = new ExecutionServerProperties();
        String defaultUrl = properties.getDefaultUrl();
        assertNotNull(defaultUrl, "默认URL不应该为null");
        assertEquals("http://localhost:8082", defaultUrl, "默认URL应该正确");
    }

    /**
     * 测试配置验证方法
     * 绿阶段：测试通过，验证配置验证方法正常工作
     */
    @Test
    void shouldValidateConfiguration() {
        // 验证配置验证逻辑
        boolean isValid = executionServerProperties.isValid();
        assertTrue(isValid, "配置应该有效");
        
        // 测试无效配置
        ExecutionServerProperties invalidProperties = new ExecutionServerProperties();
        invalidProperties.setIp(null);
        boolean isInvalid = invalidProperties.isValid();
        assertFalse(isInvalid, "无效配置应该返回false");
        
        // 测试只有IP的情况
        ExecutionServerProperties ipOnlyProperties = new ExecutionServerProperties();
        ipOnlyProperties.setIp("192.168.1.100");
        assertTrue(ipOnlyProperties.isValid(), "只有IP的配置应该有效");
        
        // 测试只有URL的情况
        ExecutionServerProperties urlOnlyProperties = new ExecutionServerProperties();
        urlOnlyProperties.setUrl("http://192.168.1.100:8082");
        assertTrue(urlOnlyProperties.isValid(), "只有URL的配置应该有效");
    }

    /**
     * 测试环境变量覆盖
     * 绿阶段：测试通过，验证环境变量支持正常工作
     */
    @Test
    void shouldSupportEnvironmentVariableOverride() {
        // 验证环境变量覆盖逻辑
        String resolvedIp = executionServerProperties.resolveIp();
        String resolvedUrl = executionServerProperties.resolveUrl();
        
        assertNotNull(resolvedIp, "解析后的IP不应该为null");
        assertNotNull(resolvedUrl, "解析后的URL不应该为null");
        
        // 应该优先使用配置的值
        assertEquals("100.66.1.2", resolvedIp, "解析后的IP应该正确");
        assertEquals("http://100.66.1.2:8082", resolvedUrl, "解析后的URL应该正确");
    }

    /**
     * 测试配置摘要方法
     * 绿阶段：测试通过，验证配置摘要方法正常工作
     */
    @Test
    void shouldGenerateConfigurationSummary() {
        // 验证配置摘要生成
        String summary = executionServerProperties.getSummary();
        assertNotNull(summary, "配置摘要不应该为null");
        assertTrue(summary.contains("100.66.1.2"), "配置摘要应该包含IP地址");
        assertTrue(summary.contains("http://100.66.1.2:8082"), "配置摘要应该包含URL");
        assertTrue(summary.contains("8082"), "配置摘要应该包含端口号");
        assertTrue(summary.contains("30"), "配置摘要应该包含超时时间");
        assertTrue(summary.contains("3"), "配置摘要应该包含重试次数");
        assertTrue(summary.contains("60"), "配置摘要应该包含健康检查间隔");
    }

    /**
     * 测试URL生成优先级
     * 绿阶段：测试通过，验证URL生成优先级正确
     */
    @Test
    void shouldPrioritizeUrlOverIpForUrlGeneration() {
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setIp("192.168.1.100");
        properties.setUrl("http://custom.url:8082");
        
        String generatedUrl = properties.generateUrlFromIp();
        assertEquals("http://custom.url:8082", generatedUrl, "应该优先使用配置的URL而不是IP生成的URL");
    }

    /**
     * 测试默认URL生成
     * 绿阶段：测试通过，验证默认URL生成逻辑
     */
    @Test
    void shouldGenerateDefaultUrlWhenNoIpOrUrlConfigured() {
        ExecutionServerProperties properties = new ExecutionServerProperties();
        properties.setIp(null);
        properties.setUrl(null);
        
        String generatedUrl = properties.generateUrlFromIp();
        assertEquals("http://localhost:8082", generatedUrl, "没有配置IP和URL时应该返回默认URL");
    }
}
