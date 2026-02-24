package com.example.medaiassistant.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库配置类
 * 使用HikariCP连接池优化数据库连接管理
 * 
 * @author System
 * @version 2.0
 * @since 2025-09-29
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.medaiassistant.repository",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.example\\.medaiassistant\\.repository\\.executionserver\\..*"
    )
)
public class DatabaseConfig {

    @Autowired
    private Environment environment;



    // Hikari连接池配置注入
    /**
     * 最大连接池大小，默认20个连接
     */
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    /**
     * 最小空闲连接数，默认5个连接
     */
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    /**
     * 连接超时时间，默认30秒
     */
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    /**
     * 空闲连接超时时间，默认4分钟，短于数据库空闲超时
     */
    @Value("${spring.datasource.hikari.idle-timeout:240000}")
    private long idleTimeout;

    /**
     * 连接最大生命周期，默认5分钟，防止连接被数据库提前关闭
     */
    @Value("${spring.datasource.hikari.max-lifetime:300000}")
    private long maxLifetime;

    /**
     * 连接保活时间，默认2分钟，定期心跳维持连接活跃
     */
    @Value("${spring.datasource.hikari.keepalive-time:120000}")
    private long keepaliveTime;

    /**
     * 连接泄漏检测阈值，默认60秒
     */
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * 连接验证超时时间，默认5秒
     */
    @Value("${spring.datasource.hikari.validation-timeout:5000}")
    private long validationTimeout;

    /**
     * 主数据源配置
     * 使用HikariCP连接池优化性能
     * 增强启动时连接验证
     * 仅支持Oracle数据库
     * 
     * @return 配置好的数据源
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        HikariDataSource dataSource = (HikariDataSource) createOracleDataSource();

        // 启动时连接验证（简化版本，避免启动中断）
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                logger.warn("数据库连接初始化验证失败，但允许应用继续启动");
            } else {
                logger.info("数据库连接初始化验证成功");
            }
        } catch (SQLException e) {
            logger.warn("数据库连接初始化异常: {}，但允许应用继续启动", e.getMessage());
        }

        return dataSource;
    }

    /**
     * 创建Oracle数据源
     * 配置HikariCP连接池参数
     * 支持多种属性命名方式，确保主服务器和执行服务器配置的兼容性
     * 
     * @return Oracle数据源
     * @since 2025-10-12
     * @author Cline
     * @see HikariDataSource
     * @see Environment
     * @see DataSource
     */
    private DataSource createOracleDataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // 基础连接配置 - 支持多种属性命名方式
        // 优先使用标准Spring Boot属性，回退到执行服务器特定属性
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        if (jdbcUrl == null) {
            // 如果标准属性不存在，尝试执行服务器配置的属性
            jdbcUrl = environment.getProperty("spring.datasource.oracle.url");
        }

        String username = environment.getProperty("spring.datasource.username");
        if (username == null) {
            username = environment.getProperty("spring.datasource.oracle.username");
        }

        String password = environment.getProperty("spring.datasource.password");
        if (password == null) {
            password = environment.getProperty("spring.datasource.oracle.password");
        }

        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 连接池优化配置
        configureConnectionPool(dataSource);

        return dataSource;
    }

    /**
     * 配置连接池参数
     * 优化性能，防止内存泄漏和连接耗尽
     * 使用配置注入参数，解决连接验证失败问题
     * 
     * @param dataSource Hikari数据源
     */
    private void configureConnectionPool(HikariDataSource dataSource) {
        // 连接池大小配置 - 使用注入的配置值
        dataSource.setMaximumPoolSize(maximumPoolSize); // 最大连接数
        dataSource.setMinimumIdle(minimumIdle); // 最小空闲连接数
        dataSource.setConnectionTimeout(connectionTimeout); // 连接超时时间（毫秒）
        dataSource.setIdleTimeout(idleTimeout); // 空闲连接超时时间（毫秒）
        dataSource.setMaxLifetime(maxLifetime); // 连接最大生命周期（毫秒）

        // 连接保活配置 - 防止空闲连接被数据库或网络设备关闭
        dataSource.setKeepaliveTime(keepaliveTime); // 保活时间（毫秒）

        // 连接泄漏检测
        dataSource.setLeakDetectionThreshold(leakDetectionThreshold); // 连接泄漏检测阈值（毫秒）

        // 连接验证
        dataSource.setValidationTimeout(validationTimeout); // 验证超时时间（毫秒）
        dataSource.setConnectionTestQuery("SELECT 1 FROM DUAL"); // Oracle验证查询

        // Oracle连接池优化
        dataSource.addDataSourceProperty("oracle.jdbc.ReadTimeout", 30000);
        dataSource.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", 10000);
        dataSource.addDataSourceProperty("oracle.jdbc.useThreadLocalBufferCache", "true");
        dataSource.addDataSourceProperty("oracle.net.ENABLE_EARLY_NOTIFICATION", "true");
        dataSource.addDataSourceProperty("oracle.jdbc.fanEnabled", "false");

        // 设置Oracle会话参数
        dataSource.setConnectionInitSql("ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD HH24:MI:SS'");

        // 连接池优化
        dataSource.setPoolName("MedAI-HikariPool"); // 连接池名称
        dataSource.setInitializationFailTimeout(0); // 初始化失败超时（0表示不超时）
        dataSource.setRegisterMbeans(true); // 启用JMX监控

        logger.info("HikariCP连接池配置完成 - 最大连接数: {}, 最小空闲连接: {}, 连接超时: {}ms, 空闲超时: {}ms, 最大生命周期: {}ms, 保活时间: {}ms",
                dataSource.getMaximumPoolSize(), dataSource.getMinimumIdle(),
                dataSource.getConnectionTimeout(), dataSource.getIdleTimeout(),
                dataSource.getMaxLifetime(), dataSource.getKeepaliveTime());
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.medaiassistant");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());
        return em;
    }

    /**
     * 主事务管理器配置
     * 使用@Primary注解标记为默认事务管理器，解决多事务管理器冲突问题
     * 
     * @return PlatformTransactionManager 主事务管理器实例
     * @since 2025-10-12
     * @author Cline
     * @see JpaTransactionManager
     * @see PlatformTransactionManager
     */
    @Primary
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        // 统一DDL配置为none，避免与application.properties中的配置冲突
        properties.put("hibernate.hbm2ddl.auto", "none");

        // 仅使用Oracle方言
        properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");

        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");
        // 为避免null导致NPE，提供默认值Asia/Shanghai
        properties.put("hibernate.jdbc.time_zone",
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone", "Asia/Shanghai"));

        // 性能优化配置
        properties.put("hibernate.jdbc.batch_size", "20"); // 批量操作大小
        properties.put("hibernate.order_inserts", "true"); // 优化插入顺序
        properties.put("hibernate.order_updates", "true"); // 优化更新顺序
        properties.put("hibernate.jdbc.fetch_size", "100"); // 查询获取大小
        properties.put("hibernate.cache.use_second_level_cache", "false"); // 禁用二级缓存
        properties.put("hibernate.cache.use_query_cache", "false"); // 禁用查询缓存

        return properties;
    }

    // 添加日志记录器
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseConfig.class);
}
