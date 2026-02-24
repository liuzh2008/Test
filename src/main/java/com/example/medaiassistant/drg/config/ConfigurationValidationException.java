package com.example.medaiassistant.drg.config;

/**
 * 配置验证异常
 * 当配置结构或内容不符合要求时抛出
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
public class ConfigurationValidationException extends RuntimeException {
    
    /**
     * 构造配置验证异常
     * @param message 异常消息
     */
    public ConfigurationValidationException(String message) {
        super(message);
    }
    
    /**
     * 构造配置验证异常
     * @param message 异常消息
     * @param cause 根本原因
     */
    public ConfigurationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
