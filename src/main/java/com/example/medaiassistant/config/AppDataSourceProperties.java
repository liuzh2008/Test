package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用数据源配置属性类
 * 用于统一管理应用级别的数据源配置
 */
@Component
@ConfigurationProperties(prefix = "app.datasource")
public class AppDataSourceProperties {

    /**
     * 数据库类型配置: oracle
     */
    private String type = "oracle";

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
