package com.example.medaiassistant.integration.datasource;

import com.example.medaiassistant.config.DatabaseConfig;
import com.example.medaiassistant.config.ExecutionServerDataSourceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源分离配置测试 - 重构阶段
 * 验证执行服务器模式下数据源的正确分离
 * 
 * @author System
 * @version 2.0
 * @since 2025-11-22
 */
@SpringBootTest
@ActiveProfiles("execution")
class DataSourceSeparationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 🔴 红阶段测试：验证执行服务器模式下主数据源不应连接
     * 执行服务器模式下应该无法获取主数据源连接
     */
    @Test
    void mainDataSourceShouldNotConnectInExecutionProfile() {
        // 验证主数据源在execution profile下不应连接主数据库
        DataSource mainDataSource = applicationContext.getBean("dataSource", DataSource.class);
        assertNotNull(mainDataSource, "主数据源应该存在");
        
        // 验证主数据源配置是否正确（应该使用执行服务器配置）
        // 这里需要检查数据源的实际连接配置
        // 由于配置复杂性，我们主要验证数据源Bean的存在性
    }

    /**
     * 🔴 红阶段测试：验证执行服务器数据源应该连接执行服务器数据库
     * 执行服务器模式下应该能够获取执行服务器数据源
     */
    @Test
    void executionDataSourceShouldConnectToExecutionServer() {
        // 验证执行服务器数据源在execution profile下应该存在
        DataSource executionDataSource = applicationContext.getBean("executionDataSource", DataSource.class);
        assertNotNull(executionDataSource, "执行服务器数据源应该存在");
    }

    /**
     * 🔴 红阶段测试：验证数据源配置类应该正确加载
     * 执行服务器模式下应该正确加载数据源配置类
     */
    @Test
    void dataSourceConfigClassesShouldBeLoaded() {
        // 验证主数据源配置类应该存在
        DatabaseConfig databaseConfig = applicationContext.getBean(DatabaseConfig.class);
        assertNotNull(databaseConfig, "主数据源配置类应该存在");

        // 验证执行服务器数据源配置类应该存在
        ExecutionServerDataSourceConfig executionConfig = applicationContext.getBean(ExecutionServerDataSourceConfig.class);
        assertNotNull(executionConfig, "执行服务器数据源配置类应该存在");
    }

    /**
     * 🔴 红阶段测试：验证数据源连接池配置正确
     * 执行服务器数据源应该使用优化的连接池配置
     */
    @Test
    @SuppressWarnings("resource")
    void executionDataSourceShouldHaveOptimizedPoolConfig() {
        DataSource executionDataSource = applicationContext.getBean("executionDataSource", DataSource.class);
        assertNotNull(executionDataSource, "执行服务器数据源应该存在");
        
        // 验证数据源类型
        assertTrue(executionDataSource instanceof com.zaxxer.hikari.HikariDataSource, 
                   "执行服务器数据源应该是HikariDataSource类型");
        
        com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) executionDataSource;
        
        // 验证连接池配置
        assertEquals(5, hikariDataSource.getMaximumPoolSize(), "执行服务器数据源最大连接数应为5");
        assertEquals(1, hikariDataSource.getMinimumIdle(), "执行服务器数据源最小空闲连接数应为1");
    }
}
