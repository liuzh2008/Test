package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义属性配置类
 * 用于管理非标准Spring Boot属性，避免属性验证警告
 */
@Component
@ConfigurationProperties(prefix = "oracle")
public class CustomProperties {

    /**
     * Oracle服务器IP地址
     */
    private String serverIp = "100.66.1.2";

    // Getters and Setters

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
}
