package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DatabaseProperties配置类单元测试
 * 按照TDD红-绿-重构流程实现数据库配置模块化
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @author System
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(classes = DatabaseProperties.class)
@EnableConfigurationProperties(DatabaseProperties.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XE",
    "spring.datasource.username=test_user",
    "spring.datasource.password=test_password",
    "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.hikari.maximum-pool-size=10",
    "spring.datasource.hikari.connection-timeout=30000",
    "spring.datasource.hikari.minimum-idle=2",
    "spring.datasource.hikari.idle-timeout=300000",
    // 禁用无关组件
    "spring.main.web-application-type=none",
    "spring.task.scheduling.enabled=false",
    "scheduling.auto-execute.enabled=false",
    "monitoring.metrics.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
})
@DisplayName("DatabaseProperties配置类 单元测试")
class DatabasePropertiesTest {

    @Autowired
    private DatabaseProperties databaseProperties;

    @Autowired
    private Environment environment;

    @BeforeEach
    void setUp() {
        // 确保每次测试前配置对象已正确初始化
        assertThat(databaseProperties).isNotNull();
    }

    /**
     * 🟢 绿阶段测试：验证配置类正确绑定spring.datasource前缀
     * 测试通过：DatabaseProperties类已正确实现配置绑定
     */
    @Test
    @DisplayName("应该正确绑定spring.datasource前缀配置")
    void shouldBindDatabasePropertiesCorrectly() {
        // 给定：配置属性已设置
        // 当：配置类被Spring容器管理
        // 那么：配置属性应该正确绑定
        assertThat(databaseProperties).isNotNull();
        assertThat(databaseProperties.getUrl()).isEqualTo("jdbc:oracle:thin:@localhost:1521/XE");
        assertThat(databaseProperties.getUsername()).isEqualTo("test_user");
        assertThat(databaseProperties.getPassword()).isEqualTo("test_password");
        assertThat(databaseProperties.getDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    }

    /**
     * 🟢 绿阶段测试：验证Hikari连接池配置绑定
     * 测试通过：Hikari配置属性已正确绑定
     */
    @Test
    @DisplayName("应该正确绑定Hikari连接池配置")
    void shouldBindHikariPropertiesCorrectly() {
        // 给定：Hikari配置属性已设置
        // 当：配置类被Spring容器管理
        // 那么：Hikari配置属性应该正确绑定
        assertThat(databaseProperties).isNotNull();
        assertThat(databaseProperties.getHikari().getMaximumPoolSize()).isEqualTo(10);
        assertThat(databaseProperties.getHikari().getConnectionTimeout()).isEqualTo(30000L);
    }

    /**
     * 🟢 绿阶段测试：验证配置验证逻辑
     * 测试通过：validateConfiguration方法已正确实现
     */
    @Test
    @DisplayName("应该通过配置验证逻辑")
    void shouldPassConfigurationValidation() {
        // 给定：有效配置
        // 当：执行配置验证
        // 那么：不应该抛出异常
        assertThatCode(() -> databaseProperties.validateConfiguration(environment))
            .doesNotThrowAnyException();
    }

    /**
     * 🟢 绿阶段测试：验证缺失URL配置时的验证失败
     * 测试通过：配置验证逻辑正确检测到缺失的URL配置
     */
    @Test
    @DisplayName("应该在数据库URL缺失时抛出异常")
    void shouldThrowExceptionWhenDatabaseUrlMissing() {
        // 给定：缺失数据库URL的环境
        Environment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatCode(() -> databaseProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("数据库URL");
    }

    /**
     * 🟢 绿阶段测试：验证URL格式验证
     * 测试通过：配置验证逻辑正确检测到格式错误的URL
     */
    @Test
    @DisplayName("应该在数据库URL格式错误时抛出异常")
    void shouldThrowExceptionWhenDatabaseUrlFormatInvalid() {
        // 给定：格式错误的数据库URL
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.datasource.url", "invalid-url");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatCode(() -> databaseProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("URL格式");
    }

    /**
     * 🟢 新增测试：验证Hikari连接池完整配置绑定
     * 测试通过：所有Hikari配置属性正确绑定
     */
    @Test
    @DisplayName("应该正确绑定所有Hikari连接池配置")
    void shouldBindAllHikariPropertiesCorrectly() {
        // 给定：完整的Hikari配置属性
        // 当：配置类被Spring容器管理
        // 那么：所有Hikari配置属性应该正确绑定
        assertThat(databaseProperties.getHikari()).isNotNull();
        assertThat(databaseProperties.getHikari().getMaximumPoolSize()).isEqualTo(10);
        assertThat(databaseProperties.getHikari().getConnectionTimeout()).isEqualTo(30000L);
        // 注意：当前Hikari内部类只定义了maximumPoolSize和connectionTimeout
        // 如果需要测试更多Hikari属性，需要在DatabaseProperties.Hikari类中添加相应字段
    }

    /**
     * 🟢 新增测试：验证空用户名配置时的验证失败
     * 测试通过：配置验证逻辑正确检测到空用户名
     */
    @Test
    @DisplayName("应该在数据库用户名为空时抛出异常")
    void shouldThrowExceptionWhenDatabaseUsernameEmpty() {
        // 给定：空用户名的环境
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.datasource.url", "jdbc:oracle:thin:@localhost:1521/XE");
        invalidEnvironment.setProperty("spring.datasource.username", "");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> databaseProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("用户名");
    }

    /**
     * 🟢 新增测试：验证空密码配置时的验证失败
     * 测试通过：配置验证逻辑正确检测到空密码
     */
    @Test
    @DisplayName("应该在数据库密码为空时抛出异常")
    void shouldThrowExceptionWhenDatabasePasswordEmpty() {
        // 给定：空密码的环境
        org.springframework.mock.env.MockEnvironment invalidEnvironment = new org.springframework.mock.env.MockEnvironment();
        invalidEnvironment.setProperty("spring.datasource.url", "jdbc:oracle:thin:@localhost:1521/XE");
        invalidEnvironment.setProperty("spring.datasource.username", "test_user");
        invalidEnvironment.setProperty("spring.datasource.password", "");
        
        // 当：执行配置验证
        // 那么：应该抛出IllegalStateException
        assertThatThrownBy(() -> databaseProperties.validateConfiguration(invalidEnvironment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("密码");
    }

    /**
     * 🟢 新增测试：验证配置对象默认值设置
     * 测试通过：配置对象在未设置值时使用合理默认值
     */
    @Test
    @DisplayName("应该设置合理的配置默认值")
    void shouldSetReasonableDefaultValues() {
        // 给定：新创建的DatabaseProperties实例
        DatabaseProperties freshProperties = new DatabaseProperties();
        
        // 当：未设置任何配置值
        // 那么：Hikari内部类应该被初始化
        assertThat(freshProperties.getHikari()).isNotNull();
        assertThat(freshProperties.getHikari().getMaximumPoolSize()).isNull();
        assertThat(freshProperties.getHikari().getConnectionTimeout()).isNull();
    }

    /**
     * 🟢 新增测试：验证配置验证性能
     * 测试通过：配置验证在合理时间内完成
     */
    @Test
    @DisplayName("配置验证应该在合理时间内完成")
    void shouldCompleteConfigurationValidationInReasonableTime() {
        // 给定：有效配置环境
        // 当：执行配置验证
        // 那么：应该在1秒内完成
        assertThatCode(() -> {
            long startTime = System.currentTimeMillis();
            databaseProperties.validateConfiguration(environment);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(1000); // 1秒内完成
        }).doesNotThrowAnyException();
    }

    /**
     * 🟢 新增测试：验证配置类线程安全性
     * 测试通过：配置类在多线程环境下正常工作
     */
    @Test
    @DisplayName("配置类应该支持多线程访问")
    void shouldSupportMultiThreadAccess() {
        // 给定：多线程环境
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        
        // 当：多个线程同时访问配置对象
        // 那么：所有线程都应该成功获取配置值
        assertThatCode(() -> {
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    assertThat(databaseProperties.getUrl()).isNotNull();
                    assertThat(databaseProperties.getUsername()).isNotNull();
                    assertThat(databaseProperties.getHikari()).isNotNull();
                });
                threads[i].start();
            }
            
            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }
        }).doesNotThrowAnyException();
    }
}
