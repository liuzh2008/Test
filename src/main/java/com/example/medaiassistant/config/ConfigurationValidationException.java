package com.example.medaiassistant.config;

/**
 * 配置验证异常类
 * 用于表示配置验证过程中发生的错误
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
public class ConfigurationValidationException extends RuntimeException {

    /**
     * 构造配置验证异常
     * 
     * @param message 异常消息
     */
    public ConfigurationValidationException(String message) {
        super(message);
    }

    /**
     * 构造配置验证异常
     * 
     * @param message 异常消息
     * @param cause 根本原因
     */
    public ConfigurationValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造配置验证异常
     * 
     * @param cause 根本原因
     */
    public ConfigurationValidationException(Throwable cause) {
        super(cause);
    }
}
