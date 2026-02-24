package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.ExecutionServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行服务器配置控制器
 * 
 * 提供执行服务器统一配置的查询接口，支持获取配置信息、JDBC URL和API基地址。
 * 该控制器实现了配置的集中管理和向后兼容性，支持环境变量覆盖配置。
 * 
 * 主要功能：
 * - 获取完整的执行服务器配置信息
 * - 获取Oracle JDBC连接URL
 * - 获取API基地址
 * - 支持环境变量和系统属性覆盖配置
 * - 保持向后兼容性，支持旧配置键名
 * 
 * 重构阶段：优化代码结构和可读性
 * - 提取常量定义，提高代码可维护性
 * - 提取辅助方法，提高代码可读性
 * - 优化代码结构，遵循单一职责原则
 * 
 * @author System
 * @version 1.2
 * @since 2025-11-06
 */
@RestController
@RequestMapping("/api/execution-server")
public class ExecutionServerConfigurationController {

    // 常量定义
    private static final String HOST_KEY = "host";
    private static final String ORACLE_PORT_KEY = "oraclePort";
    private static final String ORACLE_SID_KEY = "oracleSid";
    private static final String API_URL_KEY = "apiUrl";
    private static final String RESOLVED_HOST_KEY = "resolvedHost";
    private static final String RESOLVED_API_URL_KEY = "resolvedApiUrl";
    private static final String JDBC_URL_KEY = "jdbcUrl";
    private static final String API_BASE_URL_KEY = "apiBaseUrl";

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 获取执行服务器统一配置信息
     * 
     * 返回完整的执行服务器配置信息，包括主机名、端口、Oracle配置、API地址等。
     * 该接口支持环境变量覆盖配置，保持向后兼容性。
     * 
     * @return ResponseEntity<Map<String, Object>> 包含完整配置信息的响应实体
     * @see ExecutionServerProperties
     */
    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = buildConfigurationMap();
        return ResponseEntity.ok(config);
    }

    /**
     * 获取执行服务器JDBC URL
     * 
     * 返回Oracle数据库的JDBC连接URL，格式为：
     * jdbc:oracle:thin:@//{host}:{oraclePort}/{oracleSid}
     * 
     * @return ResponseEntity<Map<String, String>> 包含JDBC URL的响应实体
     * @see ExecutionServerProperties#getOracleJdbcUrl()
     */
    @GetMapping("/jdbc-url")
    public ResponseEntity<Map<String, String>> getJdbcUrl() {
        Map<String, String> response = buildJdbcUrlResponse();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取执行服务器API基地址
     * 
     * 返回执行服务器的API基地址，用于测试脚本和外部系统调用。
     * 支持从配置的apiUrl获取，或从IP和端口生成默认地址。
     * 
     * @return ResponseEntity<Map<String, String>> 包含API基地址的响应实体
     * @see ExecutionServerProperties#getApiBaseUrl()
     */
    @GetMapping("/api-base-url")
    public ResponseEntity<Map<String, String>> getApiBaseUrl() {
        Map<String, String> response = buildApiBaseUrlResponse();
        return ResponseEntity.ok(response);
    }

    /**
     * 构建配置信息映射
     * 
     * 从ExecutionServerProperties配置类提取所有配置信息，构建完整的配置映射。
     * 包括基础配置、Oracle配置、API配置和解析后的配置值。
     * 
     * @return Map<String, Object> 包含所有配置信息的映射
     */
    private Map<String, Object> buildConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put(HOST_KEY, executionServerProperties.getHost());
        config.put(ORACLE_PORT_KEY, executionServerProperties.getOraclePort());
        config.put(ORACLE_SID_KEY, executionServerProperties.getOracleSid());
        config.put(API_URL_KEY, executionServerProperties.getApiUrl());
        config.put(RESOLVED_HOST_KEY, executionServerProperties.getResolvedHost());
        config.put(RESOLVED_API_URL_KEY, executionServerProperties.getResolvedApiUrl());
        return config;
    }

    /**
     * 构建JDBC URL响应
     * 
     * 构建包含Oracle JDBC连接URL的响应映射。
     * JDBC URL格式：jdbc:oracle:thin:@//{host}:{oraclePort}/{oracleSid}
     * 
     * @return Map<String, String> 包含JDBC URL的响应映射
     */
    private Map<String, String> buildJdbcUrlResponse() {
        Map<String, String> response = new HashMap<>();
        response.put(JDBC_URL_KEY, executionServerProperties.getOracleJdbcUrl());
        return response;
    }

    /**
     * 构建API基地址响应
     * 
     * 构建包含执行服务器API基地址的响应映射。
     * API基地址用于测试脚本和外部系统调用执行服务器API。
     * 
     * @return Map<String, String> 包含API基地址的响应映射
     */
    private Map<String, String> buildApiBaseUrlResponse() {
        Map<String, String> response = new HashMap<>();
        response.put(API_BASE_URL_KEY, executionServerProperties.getApiBaseUrl());
        return response;
    }
}
