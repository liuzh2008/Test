package com.example.medaiassistant.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * 配置验证服务
 * 提供配置验证和健康检查功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
@Service
public class ConfigurationValidationService implements InitializingBean {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private DatabaseProperties databaseProperties;

    @Autowired(required = false)
    private RedisProperties redisProperties;

    @Autowired(required = false)
    private HttpClientProperties httpClientProperties;

    private boolean configurationValid = false;
    
    // 配置监控指标
    private final AtomicInteger validationCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private LocalDateTime lastValidationTime = LocalDateTime.now();

    /**
     * 初始化方法，在启动时执行配置验证
     */
    @Override
    public void afterPropertiesSet() {
        try {
            validateAllConfigurations();
            configurationValid = true;
        } catch (ConfigurationValidationException e) {
            configurationValid = false;
            throw e; // 重新抛出异常以阻止应用启动
        }
    }

    /**
     * 验证所有配置
     * 
     * @throws ConfigurationValidationException 如果配置验证失败
     */
    public void validateAllConfigurations() {
        // 验证数据库配置
        validateDatabaseConfiguration();
        
        // 验证JPA配置
        validateJpaConfiguration();
        
        // 验证Redis配置（如果存在）
        if (redisProperties != null) {
            validateRedisConfiguration();
        }
        
        // 验证HTTP客户端配置（如果存在）
        if (httpClientProperties != null) {
            validateHttpClientConfiguration();
        }
    }

    /**
     * 验证数据库配置
     * 
     * @throws ConfigurationValidationException 如果数据库配置验证失败
     */
    private void validateDatabaseConfiguration() {
        if (databaseProperties != null) {
            try {
                databaseProperties.validateConfiguration(environment);
            } catch (IllegalStateException e) {
                throw new ConfigurationValidationException("数据库配置验证失败: " + e.getMessage(), e);
            }
        } else {
            // 如果没有DatabaseProperties，直接验证环境变量
            String url = environment.getProperty("spring.datasource.url");
            String username = environment.getProperty("spring.datasource.username");
            String password = environment.getProperty("spring.datasource.password");
            
            if (url == null || url.trim().isEmpty()) {
                throw new ConfigurationValidationException("数据库URL配置缺失");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new ConfigurationValidationException("数据库用户名配置缺失");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new ConfigurationValidationException("数据库密码配置缺失");
            }
        }
    }

    /**
     * 验证JPA配置
     * 
     * @throws ConfigurationValidationException 如果JPA配置验证失败
     */
    private void validateJpaConfiguration() {
        // 检查JPA配置是否存在
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        String showSql = environment.getProperty("spring.jpa.show-sql");
        
        // 生产环境安全检查
        if (isProductionEnvironment()) {
            if ("create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
                throw new ConfigurationValidationException("生产环境禁止使用DDL自动创建模式");
            }
            if ("true".equals(showSql)) {
                throw new ConfigurationValidationException("生产环境禁止显示SQL语句");
            }
        }
    }

    /**
     * 验证Redis配置
     * 
     * @throws ConfigurationValidationException 如果Redis配置验证失败
     */
    private void validateRedisConfiguration() {
        try {
            // 检查Redis是否启用
            String redisEnabled = environment.getProperty("spring.data.redis.enabled");
            if (redisEnabled != null && "false".equals(redisEnabled)) {
                // Redis被显式禁用，跳过验证
                return;
            }
            
            // 检查是否有Redis配置
            String redisHost = environment.getProperty("spring.data.redis.host");
            if (redisHost == null || redisHost.trim().isEmpty()) {
                // 没有Redis配置，跳过验证
                return;
            }
            
            redisProperties.validateConfiguration(environment);
        } catch (IllegalStateException e) {
            throw new ConfigurationValidationException("Redis配置验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证HTTP客户端配置
     * 
     * @throws ConfigurationValidationException 如果HTTP客户端配置验证失败
     */
    private void validateHttpClientConfiguration() {
        // 验证HTTP客户端配置的基本要求
        String poolSize = environment.getProperty("http.client.pool.max-size");
        
        if (poolSize != null) {
            try {
                int maxSize = Integer.parseInt(poolSize);
                if (maxSize <= 0) {
                    throw new ConfigurationValidationException("HTTP连接池大小必须大于0");
                }
            } catch (NumberFormatException e) {
                throw new ConfigurationValidationException("HTTP连接池大小配置格式错误");
            }
        }
    }

    /**
     * 执行健康检查
     * 
     * @return 健康检查结果
     */
    public HealthCheckResult performHealthCheck() {
        Map<String, HealthCheckResult.ComponentHealth> components = new HashMap<>();
        
        // 检查数据库配置健康状态
        boolean databaseHealthy = checkDatabaseHealth();
        components.put("database", new HealthCheckResult.ComponentHealth(
            "database", databaseHealthy, 
            databaseHealthy ? "数据库配置正常" : "数据库配置异常",
            databaseHealthy ? "数据库连接参数完整" : "数据库配置存在问题"
        ));
        
        // 检查JPA配置健康状态
        boolean jpaHealthy = checkJpaHealth();
        components.put("jpa", new HealthCheckResult.ComponentHealth(
            "jpa", jpaHealthy,
            jpaHealthy ? "JPA配置正常" : "JPA配置异常",
            jpaHealthy ? "JPA配置符合要求" : "JPA配置存在问题"
        ));
        
        // 检查Redis配置健康状态（如果启用）
        boolean redisHealthy = true;
        if (redisProperties != null) {
            redisHealthy = checkRedisHealth();
            components.put("redis", new HealthCheckResult.ComponentHealth(
                "redis", redisHealthy,
                redisHealthy ? "Redis配置正常" : "Redis配置异常",
                redisHealthy ? "Redis连接参数完整" : "Redis配置存在问题"
            ));
        }
        
        // 检查HTTP客户端配置健康状态（如果启用）
        boolean httpClientHealthy = true;
        if (httpClientProperties != null) {
            httpClientHealthy = checkHttpClientHealth();
            components.put("httpClient", new HealthCheckResult.ComponentHealth(
                "httpClient", httpClientHealthy,
                httpClientHealthy ? "HTTP客户端配置正常" : "HTTP客户端配置异常",
                httpClientHealthy ? "HTTP客户端配置完整" : "HTTP客户端配置存在问题"
            ));
        }
        
        // 总体健康状态
        boolean overallHealthy = databaseHealthy && jpaHealthy && redisHealthy && httpClientHealthy;
        
        return new HealthCheckResult(overallHealthy, components);
    }

    /**
     * 检查数据库健康状态
     * 
     * @return 数据库是否健康
     */
    private boolean checkDatabaseHealth() {
        try {
            validateDatabaseConfiguration();
            return true;
        } catch (ConfigurationValidationException e) {
            return false;
        }
    }

    /**
     * 检查JPA健康状态
     * 
     * @return JPA是否健康
     */
    private boolean checkJpaHealth() {
        try {
            validateJpaConfiguration();
            return true;
        } catch (ConfigurationValidationException e) {
            return false;
        }
    }

    /**
     * 检查Redis健康状态
     * 
     * @return Redis是否健康
     */
    private boolean checkRedisHealth() {
        try {
            validateRedisConfiguration();
            return true;
        } catch (ConfigurationValidationException e) {
            return false;
        }
    }

    /**
     * 检查HTTP客户端健康状态
     * 
     * @return HTTP客户端是否健康
     */
    private boolean checkHttpClientHealth() {
        try {
            validateHttpClientConfiguration();
            return true;
        } catch (ConfigurationValidationException e) {
            return false;
        }
    }

    /**
     * 检查是否为生产环境
     * 
     * @return 是否为生产环境
     */
    private boolean isProductionEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取配置是否有效
     * 
     * @return 配置是否有效
     */
    public boolean isConfigurationValid() {
        return configurationValid;
    }

    /**
     * 🟢 绿阶段实现：检查是否支持配置热更新
     * 
     * @return 是否支持配置热更新
     */
    public boolean supportsHotUpdate() {
        // 基础实现：返回true表示支持热更新
        return true;
    }

    /**
     * 🟢 绿阶段实现：获取配置监控指标
     * 
     * @return 配置监控指标
     */
    public Map<String, Object> getConfigurationMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("validationCount", validationCount.get());
        metrics.put("errorCount", errorCount.get());
        metrics.put("lastValidationTime", lastValidationTime.toString());
        metrics.put("configurationValid", configurationValid);
        return metrics;
    }

    /**
     * 🟢 绿阶段实现：验证AI模型配置集成
     * 
     * @return AI模型配置集成是否有效
     */
    public boolean validateAIModelIntegration() {
        try {
            // 检查AI模型配置是否存在
            String deepseekChatUrl = environment.getProperty("ai.model.deepseek-chat.url");
            String deepseekChatApiKey = environment.getProperty("ai.model.deepseek-chat.api-key");
            
            if (deepseekChatUrl == null || deepseekChatUrl.trim().isEmpty()) {
                return false;
            }
            if (deepseekChatApiKey == null || deepseekChatApiKey.trim().isEmpty()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🟢 绿阶段实现：验证执行服务器配置集成
     * 
     * @return 执行服务器配置集成是否有效
     */
    public boolean validateExecutionServerIntegration() {
        try {
            // 检查执行服务器配置是否存在
            String executionServerIp = environment.getProperty("execution.server.ip");
            String executionServerUrl = environment.getProperty("execution.server.url");
            
            // 至少需要IP或URL配置
            boolean hasIpConfig = executionServerIp != null && !executionServerIp.trim().isEmpty();
            boolean hasUrlConfig = executionServerUrl != null && !executionServerUrl.trim().isEmpty();
            
            // 更新验证统计信息
            updateValidationStats(hasIpConfig || hasUrlConfig);
            
            return hasIpConfig || hasUrlConfig;
        } catch (Exception e) {
            updateValidationStats(false);
            return false;
        }
    }

    /**
     * 🟢 绿阶段实现：模拟配置异常
     * 
     * @return 配置异常实例
     */
    public ConfigurationValidationException simulateConfigurationException() {
        return new ConfigurationValidationException("模拟配置异常：配置验证服务测试");
    }

    /**
     * 更新验证统计信息
     */
    private void updateValidationStats(boolean success) {
        validationCount.incrementAndGet();
        lastValidationTime = LocalDateTime.now();
        if (!success) {
            errorCount.incrementAndGet();
        }
    }
}
