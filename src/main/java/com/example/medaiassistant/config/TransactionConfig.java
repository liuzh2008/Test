package com.example.medaiassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务配置类
 * 启用事务管理并配置事务隔离级别
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-09-29
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // 配置类，启用事务管理
    // 默认使用READ_COMMITTED隔离级别，避免脏读同时保证性能
}
