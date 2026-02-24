package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 数据库配置属性类
 * 绑定spring.datasource前缀的配置属性
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public class DatabaseProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Hikari hikari = new Hikari();

    // 最小化实现：只提供必要的getter和setter
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public Hikari getHikari() {
        return hikari;
    }

    public void setHikari(Hikari hikari) {
        this.hikari = hikari;
    }

    /**
     * Hikari连接池配置内部类
     */
    public static class Hikari {
        private Integer maximumPoolSize;
        private Long connectionTimeout;

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
    }

    /**
     * 配置验证方法
     * 验证数据库连接配置的完整性和正确性
     */
    public void validateConfiguration(Environment environment) {
        // 验证数据库URL
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("数据库URL配置缺失");
        }
        
        // 验证URL格式
        if (!url.startsWith("jdbc:oracle:thin:@")) {
            throw new IllegalStateException("数据库URL格式错误，必须是Oracle JDBC URL格式");
        }
        
        // 验证用户名
        String username = environment.getProperty("spring.datasource.username");
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException("数据库用户名配置缺失");
        }
        
        // 验证密码
        String password = environment.getProperty("spring.datasource.password");
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("数据库密码配置缺失");
        }
        
        // 验证驱动类名
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            throw new IllegalStateException("数据库驱动类名配置缺失");
        }
    }
}
