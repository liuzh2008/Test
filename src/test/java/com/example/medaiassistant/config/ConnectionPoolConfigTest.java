package com.example.medaiassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 连接池配置测试类
 * 验证ConnectionPoolConfig类的配置加载和默认值设置
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(classes = ConnectionPoolConfig.class)
@EnableConfigurationProperties(ConnectionPoolConfig.class)
@TestPropertySource(properties = {
    "http.client.pool.max-total=200",
    "http.client.pool.max-per-route=50",
    "http.client.pool.request-timeout=30000",
    "http.client.pool.connect-timeout=30000",
    "http.client.pool.socket-timeout=300000",
    "http.client.pool.connection-keep-alive=true",
    "http.client.pool.keep-alive-time=30000",
    // 禁用无关组件
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
class ConnectionPoolConfigTest {

    @Autowired
    private ConnectionPoolConfig connectionPoolConfig;

    @Test
    void testConnectionPoolConfigLoaded() {
        assertNotNull(connectionPoolConfig, "ConnectionPoolConfig should be loaded");
    }

    @Test
    void testDefaultValues() {
        assertEquals(200, connectionPoolConfig.getMaxTotalConnections(), "Default maxTotalConnections should be 200");
        assertEquals(50, connectionPoolConfig.getMaxConnectionsPerRoute(), "Default maxConnectionsPerRoute should be 50");
        assertEquals(30000, connectionPoolConfig.getConnectionRequestTimeout(), "Default connectionRequestTimeout should be 30000");
        assertEquals(30000, connectionPoolConfig.getConnectTimeout(), "Default connectTimeout should be 30000");
        assertEquals(300000, connectionPoolConfig.getSocketTimeout(), "Default socketTimeout should be 300000");
        assertTrue(connectionPoolConfig.isConnectionKeepAlive(), "Default connectionKeepAlive should be true");
        assertEquals(30000, connectionPoolConfig.getKeepAliveTime(), "Default keepAliveTime should be 30000");
    }

    @Test
    void testToString() {
        String toString = connectionPoolConfig.toString();
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("maxTotalConnections=200"), "toString should contain maxTotalConnections");
        assertTrue(toString.contains("maxConnectionsPerRoute=50"), "toString should contain maxConnectionsPerRoute");
    }
}
