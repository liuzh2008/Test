package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 执行服务器配置属性类
 * 用于统一管理执行服务器配置，避免硬编码IP地址
 * 
 * 重构阶段：优化代码结构和可读性
 * - 提取常量定义
 * - 优化方法命名和注释
 * - 增强向后兼容性
 * 
 * @author System
 * @version 3.0
 * @since 2025-11-05
 */
@Component
@ConfigurationProperties(prefix = "execution.server")
public class ExecutionServerProperties {
    
    // 常量定义
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8082;
    private static final int DEFAULT_ORACLE_PORT = 1521;
    private static final String DEFAULT_ORACLE_SID = "FREE";
    private static final String DEFAULT_ORACLE_USERNAME = "system";
    private static final String DEFAULT_ORACLE_PASSWORD = "Liuzh_123";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_HEALTH_CHECK_INTERVAL = 60;
    
    /**
     * 执行服务器IP地址（旧配置，向后兼容）
     */
    private String ip;
    
    /**
     * 执行服务器URL（旧配置，向后兼容）
     */
    private String url;
    
    /**
     * 执行服务器端口号
     */
    private int port = DEFAULT_PORT;
    
    /**
     * 连接超时时间（秒）
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    
    /**
     * 重试次数
     */
    private int retryCount = DEFAULT_RETRY_COUNT;
    
    /**
     * 健康检查间隔（秒）
     */
    private int healthCheckInterval = DEFAULT_HEALTH_CHECK_INTERVAL;
    
    /**
     * 执行服务器主机名（统一配置）
     */
    private String host;
    
    /**
     * Oracle数据库端口（统一配置）
     */
    private int oraclePort = DEFAULT_ORACLE_PORT;
    
    /**
     * Oracle数据库SID（统一配置）
     */
    private String oracleSid;
    
    /**
     * Oracle数据库用户名（统一配置）
     */
    private String oracleUsername;
    
    /**
     * Oracle数据库密码（统一配置）
     */
    private String oraclePassword;
    
    /**
     * API基地址（统一配置）
     */
    private String apiUrl;
    
    // Getters and Setters
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }
    
    public void setHealthCheckInterval(int healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getOraclePort() {
        return oraclePort;
    }
    
    public void setOraclePort(int oraclePort) {
        this.oraclePort = oraclePort;
    }
    
    public String getOracleSid() {
        return oracleSid;
    }
    
    public void setOracleSid(String oracleSid) {
        this.oracleSid = oracleSid;
    }
    
    public String getOracleUsername() {
        return oracleUsername;
    }
    
    public void setOracleUsername(String oracleUsername) {
        this.oracleUsername = oracleUsername;
    }
    
    public String getOraclePassword() {
        return oraclePassword;
    }
    
    public void setOraclePassword(String oraclePassword) {
        this.oraclePassword = oraclePassword;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    /**
     * 从IP地址生成完整的URL
     * 如果已配置URL则使用配置的URL，否则根据IP和端口生成
     * 
     * @return 完整的执行服务器URL
     */
    public String generateUrlFromIp() {
        if (isUrlConfigured()) {
            return this.url;
        }
        if (isIpConfigured()) {
            return buildUrlFromIp();
        }
        return getDefaultUrl();
    }
    
    /**
     * 获取默认URL
     * 
     * @return 默认的执行服务器URL
     */
    public String getDefaultUrl() {
        return String.format("http://%s:%d", DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * 验证配置是否有效
     * 
     * @return 配置是否有效
     */
    public boolean isValid() {
        return isIpConfigured() || isUrlConfigured();
    }
    
    /**
     * 解析IP地址，支持环境变量覆盖（向后兼容）
     * 
     * @return 解析后的IP地址
     */
    public String resolveIp() {
        return getValueWithFallback("EXECUTION_SERVER_IP", "execution.server.ip", this.ip, DEFAULT_HOST);
    }
    
    /**
     * 解析URL，支持环境变量覆盖
     * 
     * @return 解析后的URL
     */
    public String resolveUrl() {
        String resolvedUrl = getValueWithFallback("EXECUTION_SERVER_URL", "execution.server.url", this.url, null);
        return resolvedUrl != null ? resolvedUrl : generateUrlFromIp();
    }
    
    /**
     * 获取Oracle JDBC URL
     * 
     * @return Oracle JDBC URL
     */
    public String getOracleJdbcUrl() {
        String resolvedHost = getResolvedHost();
        int resolvedOraclePort = getResolvedOraclePort();
        String resolvedOracleSid = getResolvedOracleSid();
        
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", 
            resolvedHost, resolvedOraclePort, resolvedOracleSid);
    }
    
    /**
     * 获取API基地址
     * 
     * @return API基地址
     */
    public String getApiBaseUrl() {
        return getResolvedApiUrl();
    }
    
    /**
     * 解析IP地址，支持向后兼容性
     * 
     * @return 解析后的IP地址
     */
    public String getResolvedIp() {
        return getResolvedHost();
    }
    
    /**
     * 解析主机名，支持环境变量覆盖和向后兼容性
     * 
     * @return 解析后的主机名
     */
    public String getResolvedHost() {
        // 优先级：新配置 > 旧配置 > 默认值
        String resolvedHost = getValueWithFallback("EXECUTION_SERVER_HOST", "execution.server.host", this.host, null);
        if (resolvedHost != null) {
            return resolvedHost;
        }
        
        // 向后兼容：使用旧的IP配置
        resolvedHost = getValueWithFallback("EXECUTION_SERVER_IP", "execution.server.ip", this.ip, null);
        if (resolvedHost != null) {
            return resolvedHost;
        }
        
        return DEFAULT_HOST;
    }
    
    /**
     * 解析Oracle端口，支持环境变量覆盖
     * 
     * @return 解析后的Oracle端口
     */
    public int getResolvedOraclePort() {
        String portStr = getValueWithFallback("EXECUTION_SERVER_ORACLE_PORT", "execution.server.oracle-port", 
            this.oraclePort > 0 ? String.valueOf(this.oraclePort) : null, null);
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        return this.oraclePort > 0 ? this.oraclePort : DEFAULT_ORACLE_PORT;
    }
    
    /**
     * 解析Oracle SID，支持环境变量覆盖
     * 
     * @return 解析后的Oracle SID
     */
    public String getResolvedOracleSid() {
        return getValueWithFallback("EXECUTION_SERVER_ORACLE_SID", "execution.server.oracle-sid", 
            this.oracleSid, DEFAULT_ORACLE_SID);
    }
    
    /**
     * 解析Oracle用户名，支持环境变量覆盖
     * 
     * @return 解析后的Oracle用户名
     */
    public String getResolvedOracleUsername() {
        return getValueWithFallback("EXECUTION_SERVER_ORACLE_USERNAME", "execution.server.oracle-username", 
            this.oracleUsername, DEFAULT_ORACLE_USERNAME);
    }
    
    /**
     * 解析Oracle密码，支持环境变量覆盖
     * 
     * @return 解析后的Oracle密码
     */
    public String getResolvedOraclePassword() {
        return getValueWithFallback("EXECUTION_SERVER_ORACLE_PASSWORD", "execution.server.oracle-password", 
            this.oraclePassword, DEFAULT_ORACLE_PASSWORD);
    }
    
    /**
     * 解析API URL，支持环境变量覆盖和向后兼容性
     * 
     * @return 解析后的API URL
     */
    public String getResolvedApiUrl() {
        // 优先级：新配置 > 旧配置 > 默认值
        String resolvedApiUrl = getValueWithFallback("EXECUTION_SERVER_API_URL", "execution.server.api-url", 
            this.apiUrl, null);
        if (resolvedApiUrl != null) {
            return resolvedApiUrl;
        }
        
        // 向后兼容：使用旧的URL配置
        resolvedApiUrl = getValueWithFallback("EXECUTION_SERVER_URL", "execution.server.url", this.url, null);
        if (resolvedApiUrl != null) {
            return resolvedApiUrl;
        }
        
        // 向后兼容：从IP生成URL
        return generateUrlFromIp();
    }
    
    /**
     * 获取配置摘要信息
     * 
     * @return 配置摘要
     */
    public String getSummary() {
        return String.format(
            "ExecutionServerProperties{ip=%s, url=%s, port=%d, timeout=%d, retry=%d, healthCheck=%d, host=%s, oraclePort=%d, oracleSid=%s, oracleUsername=%s, apiUrl=%s}",
            this.ip, this.url, this.port, this.connectionTimeout, this.retryCount, this.healthCheckInterval,
            this.host, this.oraclePort, this.oracleSid, this.oracleUsername, this.apiUrl
        );
    }
    
    // 重构：提取辅助方法以提高代码可读性和可维护性
    
    /**
     * 检查IP地址是否已配置
     * 
     * @return IP地址是否已配置
     */
    private boolean isIpConfigured() {
        return this.ip != null && !this.ip.trim().isEmpty();
    }
    
    /**
     * 检查URL是否已配置
     * 
     * @return URL是否已配置
     */
    private boolean isUrlConfigured() {
        return this.url != null && !this.url.trim().isEmpty();
    }
    
    /**
     * 根据IP和端口构建URL
     * 
     * @return 构建的URL
     */
    private String buildUrlFromIp() {
        return String.format("http://%s:%d", this.ip, this.port);
    }
    
    /**
     * 获取带回退机制的值
     * 优先级：环境变量 > 系统属性 > 配置值 > 默认值
     * 
     * @param envVar 环境变量名
     * @param sysProp 系统属性名
     * @param configValue 配置值
     * @param defaultValue 默认值
     * @return 解析后的值
     */
    private String getValueWithFallback(String envVar, String sysProp, String configValue, String defaultValue) {
        // 首先检查环境变量
        String envValue = System.getenv(envVar);
        if (isNotEmpty(envValue)) {
            return envValue;
        }
        
        // 然后检查系统属性
        String sysValue = System.getProperty(sysProp);
        if (isNotEmpty(sysValue)) {
            return sysValue;
        }
        
        // 最后使用配置的值或默认值
        return isNotEmpty(configValue) ? configValue : defaultValue;
    }
    
    /**
     * 检查字符串是否非空
     * 
     * @param value 要检查的字符串
     * @return 是否非空
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
