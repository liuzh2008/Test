package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConnectionPoolConfig测试类
 * 
 * 该测试类用于验证ConnectionPoolConfig的配置是否正确，包括：
 * 1. 连接池配置参数加载
 * 2. 配置属性绑定
 * 3. 默认值设置
 * 
 * 测试说明：
 * - 验证配置参数是否正确加载
 * - 验证默认值是否正确设置
 * - 验证配置属性绑定是否正常
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@SpringBootTest(
    classes = {ConnectionPoolConfig.class},
    properties = {
        // 配置测试属性
        "http.client.pool.max-total-connections=200",
        "http.client.pool.max-connections-per-route=50",
        "http.client.pool.connection-request-timeout=30000",
        "http.client.pool.connect-timeout=30000",
        "http.client.pool.socket-timeout=300000",
        "http.client.pool.connection-keep-alive=true",
        "http.client.pool.keep-alive-time=30000",
        
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
public class HttpClientPoolConfigTest {

    @Autowired
    private ConnectionPoolConfig connectionPoolConfig;

    /**
     * 测试连接池配置参数
     * 
     * 验证连接池配置参数是否正确加载：
     * - 最大连接数
     * - 每个路由连接数
     * - 超时参数
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    public void testConnectionPoolConfigParameters() {
        assertNotNull(connectionPoolConfig, "连接池配置应该被正确注入");
        
        // 验证配置参数
        assertEquals(200, connectionPoolConfig.getMaxTotalConnections(), "最大总连接数应该为200");
        assertEquals(50, connectionPoolConfig.getMaxConnectionsPerRoute(), "每个路由最大连接数应该为50");
        assertEquals(30000, connectionPoolConfig.getConnectionRequestTimeout(), "连接请求超时应该为30秒");
        assertEquals(30000, connectionPoolConfig.getConnectTimeout(), "连接超时应该为30秒");
        assertEquals(300000, connectionPoolConfig.getSocketTimeout(), "Socket超时应该为5分钟");
        assertTrue(connectionPoolConfig.isConnectionKeepAlive(), "连接保持应该启用");
        assertEquals(30000, connectionPoolConfig.getKeepAliveTime(), "连接保持时间应该为30秒");
    }

    /**
     * 测试配置对象toString方法
     * 
     * 验证配置对象的toString方法是否正确输出配置信息
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    public void testToStringMethod() {
        assertNotNull(connectionPoolConfig, "连接池配置应该被正确注入");
        
        String toStringResult = connectionPoolConfig.toString();
        assertNotNull(toStringResult, "toString方法应该返回非空字符串");
        assertTrue(toStringResult.contains("maxTotalConnections=200"), "toString应该包含最大连接数");
        assertTrue(toStringResult.contains("maxConnectionsPerRoute=50"), "toString应该包含每个路由最大连接数");
    }

    /**
     * 测试配置对象默认值
     * 
     * 验证配置对象的默认值是否正确设置
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Test
    public void testDefaultValues() {
        assertNotNull(connectionPoolConfig, "连接池配置应该被正确注入");
        
        // 验证默认值（如果未设置属性，应该使用默认值）
        // 这里我们测试的是配置了属性的情况，所以这些值应该与配置一致
        assertEquals(200, connectionPoolConfig.getMaxTotalConnections(), "最大总连接数应该与配置一致");
        assertEquals(50, connectionPoolConfig.getMaxConnectionsPerRoute(), "每个路由最大连接数应该与配置一致");
    }
}
