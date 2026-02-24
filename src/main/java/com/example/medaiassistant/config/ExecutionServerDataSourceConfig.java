package com.example.medaiassistant.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 执行服务器数据源配置
 * 专门用于提交和轮询服务连接到执行服务器的ENCRYPTED_DATA_TEMP表
 * 
 * 重构：统一使用 ExecutionServerProperties 配置，消除配置键名不一致问题
 * 
 * @author System
 * @version 2.0
 * @since 2025-10-12
 */
@Configuration
@ConditionalOnProperty(
    name = "execution.datasource.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableJpaRepositories(
    basePackages = "com.example.medaiassistant.repository.executionserver",
    entityManagerFactoryRef = "executionEntityManagerFactory",
    transactionManagerRef = "executionTransactionManager"
)
public class ExecutionServerDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionServerDataSourceConfig.class);
    
    private final ExecutionServerProperties executionServerProperties;
    
    /**
     * 构造函数注入 ExecutionServerProperties
     * 
     * @param executionServerProperties 执行服务器配置属性
     */
    public ExecutionServerDataSourceConfig(ExecutionServerProperties executionServerProperties) {
        this.executionServerProperties = executionServerProperties;
    }

    /**
     * 执行服务器数据源
     * 专门用于连接到执行服务器的ENCRYPTED_DATA_TEMP表
     * 
     * @return 执行服务器数据源
     */
    @Bean(name = "executionDataSource")
    public DataSource executionDataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // 统一从 ExecutionServerProperties 获取配置
        String jdbcUrl = executionServerProperties.getOracleJdbcUrl();
        String username = executionServerProperties.getResolvedOracleUsername();
        String password = executionServerProperties.getResolvedOraclePassword();
        
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 连接池优化配置 - 增强稳健性
        dataSource.setMaximumPoolSize(5); // 较小的连接池，专门用于提交和轮询
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(240000);
        dataSource.setMaxLifetime(1800000); // 调整为30分钟，避免网络设备空闲回收
        dataSource.setKeepaliveTime(120000);
        dataSource.setLeakDetectionThreshold(60000);
        dataSource.setValidationTimeout(5000);
        dataSource.setConnectionTestQuery("SELECT 1 FROM DUAL");
        dataSource.setConnectionInitSql("ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD HH24:MI:SS'");

        // Oracle驱动保活与网络优化 - 防止网络中断
        dataSource.addDataSourceProperty("oracle.jdbc.ReadTimeout", 30000);
        dataSource.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", 10000);
        dataSource.addDataSourceProperty("oracle.jdbc.useThreadLocalBufferCache", "true");
        dataSource.addDataSourceProperty("oracle.net.ENABLE_EARLY_NOTIFICATION", "true");
        dataSource.addDataSourceProperty("oracle.net.keepAlive", "true"); // 启用TCP保活
        dataSource.addDataSourceProperty("oracle.net.disableOob", "true"); // 禁用OOB，避免网络问题

        dataSource.setPoolName("ExecutionServer-HikariPool");

        // 关键修复：允许启动期间Oracle不可用时不阻塞应用启动
        dataSource.setInitializationFailTimeout(0);
        dataSource.setRegisterMbeans(true);

        logger.info("执行服务器数据源配置完成 - 连接地址: {}, 用户名: {}", jdbcUrl, username);

        return dataSource;
    }

    /**
     * 执行服务器实体管理器工厂
     * 
     * @return 执行服务器实体管理器工厂
     */
    @Bean(name = "executionEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean executionEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(executionDataSource());
        em.setPackagesToScan("com.example.medaiassistant.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(executionHibernateProperties());
        return em;
    }

    /**
     * 执行服务器事务管理器
     * 
     * @return 执行服务器事务管理器
     */
    @Bean(name = "executionTransactionManager")
    public PlatformTransactionManager executionTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(executionEntityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * 执行服务器Hibernate配置
     * 
     * @return Hibernate配置属性
     */
    private Properties executionHibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        properties.put("hibernate.jdbc.time_zone", "Asia/Shanghai");
        
        // Schema配置 - 移除默认Schema，直接访问表（开发环境及生产环境的执行服务器数据库没有schema）
        // properties.put("hibernate.default_schema", "SYSTEM");
        // 标识符引号配置 - 与主配置保持一致，Oracle不加引号时自动转大写
        properties.put("hibernate.globally_quoted_identifiers", "false");
        
        // 性能优化配置
        properties.put("hibernate.jdbc.batch_size", "10");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.fetch_size", "50");
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_query_cache", "false");

        return properties;
    }
}
