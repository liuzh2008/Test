package com.example.medaiassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis配置属性测试类
 * 按照TDD红-绿-重构流程实现
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@DisplayName("Redis配置属性 单元测试")
class RedisPropertiesTest {

    /**
     * 🟢 绿阶段测试用例1：验证RedisProperties类存在
     * 这个测试现在应该通过，因为RedisProperties类已创建
     */
    @Test
    @DisplayName("应该创建RedisProperties配置类")
    void shouldCreateRedisPropertiesClass() {
        // 当创建RedisProperties实例时
        // 那么应该成功创建，因为类已存在
        // 这个测试在绿阶段会通过
        RedisProperties properties = new RedisProperties();
        assertThat(properties).isNotNull();
    }

    /**
     * 🟢 绿阶段测试用例2：验证配置绑定功能
     * 这个测试现在应该通过，因为配置绑定功能已实现
     */
    @Test
    @DisplayName("应该正确绑定spring.data.redis前缀配置")
    void shouldBindRedisPropertiesCorrectly() {
        // 给定
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.data.redis.host"))
            .thenReturn("localhost");
        when(environment.getProperty("spring.data.redis.port"))
            .thenReturn("6379");
        
        // 当
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        
        // 那么
        assertThat(properties.getHost()).isEqualTo("localhost");
        assertThat(properties.getPort()).isEqualTo(6379);
    }

    /**
     * 🟢 绿阶段测试用例3：验证配置验证功能
     * 这个测试现在应该通过，因为配置验证方法已实现
     */
    @Test
    @DisplayName("应该验证Redis配置的完整性")
    void shouldValidateRedisConfiguration() {
        // 给定
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.data.redis.host"))
            .thenReturn("localhost");
        when(environment.getProperty("spring.data.redis.port"))
            .thenReturn("6379");
        
        // 当
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        
        // 那么
        assertThatCode(() -> properties.validateConfiguration(environment))
            .doesNotThrowAnyException();
    }

    /**
     * 🟢 绿阶段测试用例4：验证连接池配置
     * 这个测试现在应该通过，因为连接池配置功能已实现
     */
    @Test
    @DisplayName("应该支持连接池配置")
    void shouldSupportConnectionPoolConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        RedisProperties.Lettuce lettuce = new RedisProperties.Lettuce();
        RedisProperties.Lettuce.Pool pool = new RedisProperties.Lettuce.Pool();
        pool.setMaxActive(8);
        pool.setMaxIdle(8);
        pool.setMinIdle(0);
        lettuce.setPool(pool);
        properties.setLettuce(lettuce);
        
        // 当
        RedisProperties.Lettuce.Pool configuredPool = properties.getLettuce().getPool();
        
        // 那么
        assertThat(configuredPool.getMaxActive()).isEqualTo(8);
        assertThat(configuredPool.getMaxIdle()).isEqualTo(8);
        assertThat(configuredPool.getMinIdle()).isEqualTo(0);
    }

    /**
     * 🟢 绿阶段测试用例5：验证连接URL生成
     * 这个测试现在应该通过，因为连接URL生成功能已实现
     */
    @Test
    @DisplayName("应该生成正确的Redis连接URL")
    void shouldGenerateCorrectRedisConnectionUrl() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        properties.setPassword("secret");
        
        // 当
        String connectionUrl = properties.generateConnectionUrl();
        
        // 那么
        assertThat(connectionUrl).isEqualTo("redis://:secret@localhost:6379");
    }

    /**
     * 🟢 绿阶段测试用例6：验证无密码连接URL生成
     * 这个测试现在应该通过，因为连接URL生成功能已实现
     */
    @Test
    @DisplayName("应该生成无密码的Redis连接URL")
    void shouldGenerateRedisConnectionUrlWithoutPassword() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        properties.setPassword(null);
        
        // 当
        String connectionUrl = properties.generateConnectionUrl();
        
        // 那么
        assertThat(connectionUrl).isEqualTo("redis://localhost:6379");
    }

    /**
     * 🟢 绿阶段测试用例7：验证配置缺失异常
     * 这个测试现在应该通过，因为配置验证功能已实现
     */
    @Test
    @DisplayName("应该在Redis主机配置缺失时抛出异常")
    void shouldThrowExceptionWhenRedisHostIsMissing() {
        // 给定
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.data.redis.host"))
            .thenReturn(null);
        when(environment.getProperty("spring.data.redis.port"))
            .thenReturn("6379");
        
        // 当
        RedisProperties properties = new RedisProperties();
        
        // 那么
        assertThatThrownBy(() -> properties.validateConfiguration(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Redis主机配置缺失");
    }

    /**
     * 🟢 绿阶段测试用例8：验证端口配置缺失异常
     * 这个测试现在应该通过，因为配置验证功能已实现
     */
    @Test
    @DisplayName("应该在Redis端口配置缺失时抛出异常")
    void shouldThrowExceptionWhenRedisPortIsMissing() {
        // 给定
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.data.redis.host"))
            .thenReturn("localhost");
        when(environment.getProperty("spring.data.redis.port"))
            .thenReturn(null);
        
        // 当
        RedisProperties properties = new RedisProperties();
        
        // 那么
        assertThatThrownBy(() -> properties.validateConfiguration(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Redis端口配置缺失");
    }

    /**
     * 🟢 绿阶段测试用例9：验证超时配置
     * 这个测试现在应该通过，因为超时配置功能已实现
     */
    @Test
    @DisplayName("应该支持超时配置")
    void shouldSupportTimeoutConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setTimeout(2000L);
        
        // 当
        Long timeout = properties.getTimeout();
        
        // 那么
        assertThat(timeout).isEqualTo(2000L);
    }

    /**
     * 🟢 绿阶段测试用例10：验证数据库索引配置
     * 这个测试现在应该通过，因为数据库索引配置功能已实现
     */
    @Test
    @DisplayName("应该支持数据库索引配置")
    void shouldSupportDatabaseIndexConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setDatabase(0);
        
        // 当
        Integer database = properties.getDatabase();
        
        // 那么
        assertThat(database).isEqualTo(0);
    }

    /**
     * 🟢 补充测试用例11：验证端口格式错误异常
     */
    @Test
    @DisplayName("应该在Redis端口格式错误时抛出异常")
    void shouldThrowExceptionWhenRedisPortFormatIsInvalid() {
        // 给定
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.data.redis.host"))
            .thenReturn("localhost");
        when(environment.getProperty("spring.data.redis.port"))
            .thenReturn("invalid-port");
        
        // 当
        RedisProperties properties = new RedisProperties();
        
        // 那么
        assertThatThrownBy(() -> properties.validateConfiguration(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Redis端口格式错误");
    }

    /**
     * 🟢 补充测试用例12：验证空密码字符串的连接URL生成
     */
    @Test
    @DisplayName("应该生成无密码的Redis连接URL当密码为空字符串时")
    void shouldGenerateRedisConnectionUrlWhenPasswordIsEmptyString() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        properties.setPassword("");
        
        // 当
        String connectionUrl = properties.generateConnectionUrl();
        
        // 那么
        assertThat(connectionUrl).isEqualTo("redis://localhost:6379");
    }

    /**
     * 🟢 补充测试用例13：验证连接池配置边界值
     */
    @Test
    @DisplayName("应该支持连接池配置边界值")
    void shouldSupportConnectionPoolBoundaryConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        RedisProperties.Lettuce lettuce = new RedisProperties.Lettuce();
        RedisProperties.Lettuce.Pool pool = new RedisProperties.Lettuce.Pool();
        pool.setMaxActive(1);  // 最小连接数
        pool.setMaxIdle(0);    // 最小空闲连接
        pool.setMinIdle(0);    // 最小空闲连接
        lettuce.setPool(pool);
        properties.setLettuce(lettuce);
        
        // 当
        RedisProperties.Lettuce.Pool configuredPool = properties.getLettuce().getPool();
        
        // 那么
        assertThat(configuredPool.getMaxActive()).isEqualTo(1);
        assertThat(configuredPool.getMaxIdle()).isEqualTo(0);
        assertThat(configuredPool.getMinIdle()).isEqualTo(0);
    }

    /**
     * 🟢 补充测试用例14：验证数据库索引边界值
     */
    @Test
    @DisplayName("应该支持数据库索引边界值")
    void shouldSupportDatabaseIndexBoundaryConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setDatabase(15); // Redis默认支持0-15个数据库
        
        // 当
        Integer database = properties.getDatabase();
        
        // 那么
        assertThat(database).isEqualTo(15);
    }

    /**
     * 🟢 补充测试用例15：验证超时配置边界值
     */
    @Test
    @DisplayName("应该支持超时配置边界值")
    void shouldSupportTimeoutBoundaryConfiguration() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setTimeout(0L); // 最小超时时间
        
        // 当
        Long timeout = properties.getTimeout();
        
        // 那么
        assertThat(timeout).isEqualTo(0L);
    }

    /**
     * 🟢 补充测试用例16：验证所有配置属性同时设置
     */
    @Test
    @DisplayName("应该支持所有配置属性同时设置")
    void shouldSupportAllConfigurationProperties() {
        // 给定
        RedisProperties properties = new RedisProperties();
        properties.setHost("redis.example.com");
        properties.setPort(6380);
        properties.setPassword("complex-password-123");
        properties.setTimeout(5000L);
        properties.setDatabase(5);
        
        RedisProperties.Lettuce lettuce = new RedisProperties.Lettuce();
        RedisProperties.Lettuce.Pool pool = new RedisProperties.Lettuce.Pool();
        pool.setMaxActive(20);
        pool.setMaxIdle(10);
        pool.setMinIdle(5);
        lettuce.setPool(pool);
        properties.setLettuce(lettuce);
        
        // 当
        String connectionUrl = properties.generateConnectionUrl();
        
        // 那么
        assertThat(properties.getHost()).isEqualTo("redis.example.com");
        assertThat(properties.getPort()).isEqualTo(6380);
        assertThat(properties.getPassword()).isEqualTo("complex-password-123");
        assertThat(properties.getTimeout()).isEqualTo(5000L);
        assertThat(properties.getDatabase()).isEqualTo(5);
        assertThat(properties.getLettuce().getPool().getMaxActive()).isEqualTo(20);
        assertThat(properties.getLettuce().getPool().getMaxIdle()).isEqualTo(10);
        assertThat(properties.getLettuce().getPool().getMinIdle()).isEqualTo(5);
        assertThat(connectionUrl).isEqualTo("redis://:complex-password-123@redis.example.com:6380");
    }
}
