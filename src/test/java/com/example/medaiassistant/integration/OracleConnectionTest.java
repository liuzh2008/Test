package com.example.medaiassistant.integration;

import com.example.medaiassistant.config.ExecutionServerProperties;
import com.example.medaiassistant.config.OracleDatabaseProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle数据库连接集成测试类
 * 测试Oracle数据库连接配置是否正确指向新的服务器地址10.0.19.251
 * 并验证配置是否统一管理
 * 
 * 注意：此测试需要真实Oracle环境，不适用于单元测试阶段（H2环境）
 * 应该在集成测试阶段运行，或移至独立的集成测试套件
 */
@Disabled("此测试需要真实Oracle环境，不适用于单元测试阶段的H2数据库环境")
@SpringBootTest
@TestPropertySource(properties = {
    "app.datasource.type=oracle"
})
public class OracleConnectionTest {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private OracleDatabaseProperties oracleProperties;

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 测试Oracle数据库连接是否成功建立
     * 验证连接URL是否包含配置的服务器地址
     */
    @Test
    public void testOracleConnection() throws SQLException {
        // 获取数据库连接
        try (Connection connection = dataSource.getConnection()) {
            // 验证连接是否成功建立
            assertTrue(connection.isValid(5), "数据库连接应该有效");
            
            // 获取连接URL
            String url = connection.getMetaData().getURL();
            
            // 验证URL是否包含配置的Oracle服务器地址
            assertTrue(url.contains(executionServerProperties.getIp()), 
                "连接URL应该包含配置的Oracle服务器地址" + executionServerProperties.getIp() + "，实际URL: " + url);
            
            // 验证URL是否包含正确的端口号
            assertTrue(url.contains("1521"), 
                "连接URL应该包含正确的端口号1521，实际URL: " + url);
            
            // 验证URL是否包含正确的SID/Service Name
            assertTrue(url.contains("FREE"), 
                "连接URL应该包含正确的SID/Service Name FREE，实际URL: " + url);
            
            // 验证数据库产品名称是否为Oracle
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            assertTrue(databaseProductName.contains("Oracle"), 
                "数据库产品名称应该包含Oracle，实际名称: " + databaseProductName);
        }
    }

    /**
     * 测试Oracle数据库连接配置信息
     */
    @Test
    public void testOracleConnectionMetadata() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 验证数据库元数据
            assertNotNull(connection.getMetaData(), "数据库元数据不应该为null");
            assertNotNull(connection.getMetaData().getDatabaseProductName(), "数据库产品名称不应该为null");
            assertNotNull(connection.getMetaData().getURL(), "数据库URL不应该为null");
            
            // 验证连接未关闭
            assertFalse(connection.isClosed(), "数据库连接不应该被关闭");
        }
    }
    
    /**
     * 测试Oracle数据库配置是否统一管理
     */
    @Test
    public void testOracleConfigurationManagement() {
        // 验证Oracle配置属性已正确注入
        assertNotNull(oracleProperties, "Oracle数据库配置属性不应该为null");
        
        // 验证URL配置
        assertNotNull(oracleProperties.getUrl(), "Oracle数据库URL不应该为null");
        assertTrue(oracleProperties.getUrl().contains(executionServerProperties.getIp()), 
            "Oracle数据库URL应该包含服务器地址" + executionServerProperties.getIp() + "，实际URL: " + oracleProperties.getUrl());
        assertTrue(oracleProperties.getUrl().contains("1521"), 
            "Oracle数据库URL应该包含端口号1521，实际URL: " + oracleProperties.getUrl());
        assertTrue(oracleProperties.getUrl().contains("FREE"), 
            "Oracle数据库URL应该包含SID/Service Name FREE，实际URL: " + oracleProperties.getUrl());
        
        // 验证驱动类名配置
        assertNotNull(oracleProperties.getDriverClassName(), "Oracle数据库驱动类名不应该为null");
        assertEquals("oracle.jdbc.OracleDriver", oracleProperties.getDriverClassName(),
            "Oracle数据库驱动类名应该为oracle.jdbc.OracleDriver");
        
        // 验证用户名配置
        assertNotNull(oracleProperties.getUsername(), "Oracle数据库用户名不应该为null");
        assertEquals("system", oracleProperties.getUsername(),
            "Oracle数据库用户名应该为system");
    }
}
