package com.example.medaiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 数据初始化配置类 - 已禁用Oracle数据库初始化
 * 
 * 该类在Oracle数据库环境下被禁用，避免数据类型不匹配导致的启动失败
 * 
 * @author Cline
 * @since 2025-08-12
 */
@Configuration
@Profile("!oracle") // 仅在非Oracle环境下启用
public class DataInitializer {

    /**
     * 空的数据初始化方法 - 在Oracle环境下禁用
     */
    @Bean
    Object initDatabase() {
        // 在Oracle环境下禁用数据初始化
        return new Object();
    }
}
