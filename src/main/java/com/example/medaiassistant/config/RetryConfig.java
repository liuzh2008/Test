package com.example.medaiassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 重试机制配置类
 * 启用Spring Retry注解支持
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-09-29
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // 配置类，启用重试注解支持
}
