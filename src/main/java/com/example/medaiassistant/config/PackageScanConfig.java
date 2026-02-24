package com.example.medaiassistant.config;

import org.springframework.context.annotation.Configuration;

/**
 * 包结构扫描配置类
 * 使用@Profile注解控制组件加载，简化配置
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@Configuration
public class PackageScanConfig {
    // 重构：使用@Profile注解控制组件加载，不再需要复杂的包扫描配置
    // 组件通过@Profile("main")或@Profile("execution")注解控制加载
}
