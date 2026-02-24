package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Redis配置属性类
 * 绑定spring.data.redis前缀的配置属性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@ConfigurationProperties(prefix = "spring.data.redis")
@Validated
public class RedisProperties {

    private String host;
    private Integer port;
    private String password;
    private Long timeout;
    private Integer database;
    private Lettuce lettuce = new Lettuce();

    // 最小化实现：只提供必要的getter和setter
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public Lettuce getLettuce() {
        return lettuce;
    }

    public void setLettuce(Lettuce lettuce) {
        this.lettuce = lettuce;
    }

    /**
     * Lettuce连接池配置内部类
     */
    public static class Lettuce {
        private Pool pool = new Pool();

        public Pool getPool() {
            return pool;
        }

        public void setPool(Pool pool) {
            this.pool = pool;
        }

        /**
         * 连接池配置内部类
         */
        public static class Pool {
            private Integer maxActive;
            private Integer maxIdle;
            private Integer minIdle;

            public Integer getMaxActive() {
                return maxActive;
            }

            public void setMaxActive(Integer maxActive) {
                this.maxActive = maxActive;
            }

            public Integer getMaxIdle() {
                return maxIdle;
            }

            public void setMaxIdle(Integer maxIdle) {
                this.maxIdle = maxIdle;
            }

            public Integer getMinIdle() {
                return minIdle;
            }

            public void setMinIdle(Integer minIdle) {
                this.minIdle = minIdle;
            }
        }
    }

    /**
     * 配置验证方法
     * 验证Redis配置的完整性和正确性
     */
    public void validateConfiguration(Environment environment) {
        validateHostConfiguration(environment);
        validatePortConfiguration(environment);
    }

    /**
     * 验证Redis主机配置
     */
    private void validateHostConfiguration(Environment environment) {
        String host = environment.getProperty("spring.data.redis.host");
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalStateException("Redis主机配置缺失");
        }
    }

    /**
     * 验证Redis端口配置
     */
    private void validatePortConfiguration(Environment environment) {
        String port = environment.getProperty("spring.data.redis.port");
        if (port == null || port.trim().isEmpty()) {
            throw new IllegalStateException("Redis端口配置缺失");
        }
        
        // 验证端口格式
        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Redis端口格式错误，必须是有效的端口号");
        }
    }

    /**
     * 生成Redis连接URL
     * 根据配置生成标准的Redis连接URL
     */
    public String generateConnectionUrl() {
        if (password != null && !password.trim().isEmpty()) {
            return String.format("redis://:%s@%s:%d", password, host, port);
        } else {
            return String.format("redis://%s:%d", host, port);
        }
    }
}
