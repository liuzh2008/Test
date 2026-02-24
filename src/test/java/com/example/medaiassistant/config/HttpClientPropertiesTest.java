package com.example.medaiassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * HTTP客户端配置属性测试类
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@SpringBootTest(
    classes = HttpClientProperties.class,
    properties = {
        // 禁用不必要的组件
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
@EnableConfigurationProperties(HttpClientProperties.class)
@TestPropertySource(properties = {
    // HTTP客户端配置测试属性
    "http.client.pool.max-total=100",
    "http.client.pool.default-max-per-route=20",
    "http.client.timeout.connection-timeout=5000",
    "http.client.timeout.socket-timeout=10000",
    "http.client.timeout.request-timeout=15000",
    "http.client.retry.max-retries=3",
    "http.client.retry.backoff-period=1000",
    "http.client.keep-alive.enabled=true",
    "http.client.keep-alive.timeout=30000"
})
@DisplayName("HTTP客户端配置属性 单元测试")
class HttpClientPropertiesTest {

    @Autowired
    private HttpClientProperties httpClientProperties;

    @Autowired
    private Environment environment;

    /**
     * 测试HTTP客户端配置属性正确绑定
     */
    @Test
    @DisplayName("应该正确绑定HTTP客户端配置属性")
    void shouldBindHttpClientPropertiesCorrectly() {
        // 验证连接池配置
        assertThat(httpClientProperties.getPool().getMaxTotal()).isEqualTo(100);
        assertThat(httpClientProperties.getPool().getDefaultMaxPerRoute()).isEqualTo(20);
        
        // 验证超时配置
        assertThat(httpClientProperties.getTimeout().getConnectionTimeout()).isEqualTo(5000);
        assertThat(httpClientProperties.getTimeout().getSocketTimeout()).isEqualTo(10000);
        assertThat(httpClientProperties.getTimeout().getRequestTimeout()).isEqualTo(15000);
        
        // 验证重试配置
        assertThat(httpClientProperties.getRetry().getMaxRetries()).isEqualTo(3);
        assertThat(httpClientProperties.getRetry().getBackoffPeriod()).isEqualTo(1000);
        
        // 验证Keep-Alive配置
        assertThat(httpClientProperties.getKeepAlive().isEnabled()).isTrue();
        assertThat(httpClientProperties.getKeepAlive().getTimeout()).isEqualTo(30000);
    }

    /**
     * 测试配置验证逻辑
     */
    @Test
    @DisplayName("应该通过配置验证")
    void shouldPassConfigurationValidation() {
        // 验证配置验证逻辑不抛出异常
        assertThatCode(() -> httpClientProperties.validateConfiguration(environment))
            .doesNotThrowAnyException();
    }

    /**
     * 测试默认值设置
     */
    @Test
    @DisplayName("应该设置合理的默认值")
    void shouldSetReasonableDefaultValues() {
        // 创建新的配置实例验证默认值
        HttpClientProperties properties = new HttpClientProperties();
        
        // 验证连接池默认值
        assertThat(properties.getPool().getMaxTotal()).isEqualTo(50);
        assertThat(properties.getPool().getDefaultMaxPerRoute()).isEqualTo(10);
        
        // 验证超时默认值
        assertThat(properties.getTimeout().getConnectionTimeout()).isEqualTo(3000);
        assertThat(properties.getTimeout().getSocketTimeout()).isEqualTo(5000);
        assertThat(properties.getTimeout().getRequestTimeout()).isEqualTo(10000);
        
        // 验证重试默认值
        assertThat(properties.getRetry().getMaxRetries()).isEqualTo(2);
        assertThat(properties.getRetry().getBackoffPeriod()).isEqualTo(500);
        
        // 验证Keep-Alive默认值
        assertThat(properties.getKeepAlive().isEnabled()).isTrue();
        assertThat(properties.getKeepAlive().getTimeout()).isEqualTo(20000);
    }

    /**
     * 测试边界条件 - 最小连接池大小
     */
    @Test
    @DisplayName("应该验证最小连接池大小")
    void shouldValidateMinimumPoolSize() {
        // 创建新的配置实例并设置无效值
        HttpClientProperties properties = new HttpClientProperties();
        properties.getPool().setMaxTotal(0); // 设置无效值
        
        // 验证配置验证会检测到无效值
        assertThatCode(() -> properties.validateConfiguration(environment))
            .hasMessageContaining("连接池最大连接数必须大于0");
    }

    /**
     * 测试边界条件 - 超时时间范围
     */
    @Test
    @DisplayName("应该验证超时时间范围")
    void shouldValidateTimeoutRange() {
        // 创建新的配置实例并设置无效值
        HttpClientProperties properties = new HttpClientProperties();
        properties.getTimeout().setConnectionTimeout(-1); // 设置无效值
        
        // 验证配置验证会检测到无效值
        assertThatCode(() -> properties.validateConfiguration(environment))
            .hasMessageContaining("连接超时必须大于0");
    }
}
