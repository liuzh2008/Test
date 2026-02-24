package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Oracle数据库配置属性类
 * 用于统一管理Oracle数据库连接配置，避免硬编码
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.oracle")
public class OracleDatabaseProperties {
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 数据库驱动类名
     */
    private String driverClassName;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库密码
     */
    private String password;
    
    /**
     * Hikari连接池配置
     */
    private Hikari hikari = new Hikari();
    
    // Getters and Setters
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getDriverClassName() {
        return driverClassName;
    }
    
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
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
        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 30000;
        
        /**
         * 初始化失败超时时间（毫秒）
         */
        private int initializationFailTimeout = 0;
        
        // Getters and Setters
        
        public int getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public int getInitializationFailTimeout() {
            return initializationFailTimeout;
        }
        
        public void setInitializationFailTimeout(int initializationFailTimeout) {
            this.initializationFailTimeout = initializationFailTimeout;
        }
    }
}
