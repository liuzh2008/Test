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
 * 数据源配置测试 - 重构阶段
 * 使用@SpringBootTest只加载数据源配置类实现轻量级测试
 * 验证执行服务器模式下数据源的正确分离
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@SpringBootTest(classes = {DatabaseConfig.class, ExecutionServerDataSourceConfig.class})
@ActiveProfiles("execution")
class DataSourceConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 🟢 绿阶段测试：验证执行服务器模式下数据源Bean存在性
     * 验证主数据源和执行服务器数据源都正确加载
     */
    @Test
    void dataSourceBeansShouldExistInExecutionProfile() {
        // 验证主数据源Bean存在
        DataSource mainDataSource = applicationContext.getBean("dataSource", DataSource.class);
        assertNotNull(mainDataSource, "主数据源Bean应该存在");
        
        // 验证执行服务器数据源Bean存在
        DataSource executionDataSource = applicationContext.getBean("executionDataSource", DataSource.class);
        assertNotNull(executionDataSource, "执行服务器数据源Bean应该存在");
    }

    /**
     * 🟢 绿阶段测试：验证执行服务器数据源连接池配置
     * 验证执行服务器数据源使用优化的连接池配置
     */
    @Test
    @SuppressWarnings("resource")
    void executionDataSourceShouldHaveOptimizedPoolConfiguration() {
        DataSource executionDataSource = applicationContext.getBean("executionDataSource", DataSource.class);
        assertNotNull(executionDataSource, "执行服务器数据源应该存在");
        
        // 验证数据源类型
        assertTrue(executionDataSource instanceof com.zaxxer.hikari.HikariDataSource, 
                   "执行服务器数据源应该是HikariDataSource类型");
        
        com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) executionDataSource;
        
        // 验证连接池配置符合执行服务器优化要求
        assertEquals(5, hikariDataSource.getMaximumPoolSize(), "执行服务器数据源最大连接数应为5");
        assertEquals(1, hikariDataSource.getMinimumIdle(), "执行服务器数据源最小空闲连接数应为1");
        assertEquals(30000, hikariDataSource.getConnectionTimeout(), "执行服务器数据源连接超时应为30秒");
    }

    /**
     * 🟢 绿阶段测试：验证数据源配置正确性
     * 验证执行服务器数据源使用正确的连接地址
     */
    @Test
    @SuppressWarnings("resource")
    void executionDataSourceShouldUseCorrectConnectionUrl() {
        DataSource executionDataSource = applicationContext.getBean("executionDataSource", DataSource.class);
        assertNotNull(executionDataSource, "执行服务器数据源应该存在");
        
        com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) executionDataSource;
        
        // 验证连接URL格式
        String jdbcUrl = hikariDataSource.getJdbcUrl();
        assertNotNull(jdbcUrl, "JDBC URL不应为空");
        assertTrue(jdbcUrl.contains("100.66.1.2"), "执行服务器数据源应连接到100.66.1.2");
        assertTrue(jdbcUrl.toUpperCase().contains("FREE"), "执行服务器数据源应使用FREE SID");
    }

    /**
     * 🔵 重构阶段测试：验证测试性能优化
     * 验证轻量级测试配置减少了不必要的组件加载
     */
    @Test
    void testShouldUseMinimalConfiguration() {
        // 验证只加载了必要的配置类
        assertNotNull(applicationContext.getBean(DatabaseConfig.class), "DatabaseConfig应该被加载");
        assertNotNull(applicationContext.getBean(ExecutionServerDataSourceConfig.class), "ExecutionServerDataSourceConfig应该被加载");
        
        // 验证没有加载Web相关组件
        String[] webBeans = applicationContext.getBeanNamesForType(org.springframework.web.reactive.function.client.WebClient.class);
        assertEquals(0, webBeans.length, "轻量级测试不应加载WebClient组件");
        
        // 验证没有加载调度相关组件
        String[] schedulingBeans = applicationContext.getBeanNamesForType(org.springframework.scheduling.TaskScheduler.class);
        assertEquals(0, schedulingBeans.length, "轻量级测试不应加载TaskScheduler组件");
    }
}
