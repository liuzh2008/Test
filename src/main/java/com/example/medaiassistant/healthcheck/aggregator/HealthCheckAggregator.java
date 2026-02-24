package com.example.medaiassistant.healthcheck.aggregator;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查结果聚合器
 * 通过HTTP调用各模块的健康检查API，聚合结果
 */
@Component
public class HealthCheckAggregator {

    private final RestTemplate restTemplate;

    /**
     * 构造函数
     * @param restTemplate RestTemplate实例
     */
    public HealthCheckAggregator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 配置的API端点
    private static final String MAIN_SERVER_HEALTH_URL = "/api/health/ping";
    private static final String EXECUTION_SERVER_HEALTH_URL = "http://localhost:8082/api/execute/health";
    private static final String DATABASE_HEALTH_URL = "/api/database/health";
    private static final String NETWORK_HEALTH_URL = "/api/network/health";
    private static final String CONFIG_HEALTH_URL = "/api/configuration/health";
    private static final String POLLING_SERVICE_HEALTH_URL = "/api/polling/detailed-status";

    /**
     * 聚合所有健康检查结果
     * @return 整体健康检查结果
     */
    public Map<String, Object> aggregateHealthChecks() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> modules = new HashMap<>();
        boolean allUp = true;
        
        try {
            // 检查主服务器健康状态
            allUp &= checkModuleHealth(MAIN_SERVER_HEALTH_URL, "mainServer", modules);
            
            // 检查执行服务器健康状态
            allUp &= checkModuleHealth(EXECUTION_SERVER_HEALTH_URL, "executionServer", modules);
            
            // 检查数据库健康状态
            allUp &= checkModuleHealth(DATABASE_HEALTH_URL, "database", modules);
            
            // 检查网络健康状态
            allUp &= checkModuleHealth(NETWORK_HEALTH_URL, "network", modules);
            
            // 检查配置健康状态
            allUp &= checkModuleHealth(CONFIG_HEALTH_URL, "configuration", modules);
            
            // 检查轮询服务健康状态
            allUp &= checkModuleHealth(POLLING_SERVICE_HEALTH_URL, "pollingService", modules);
            
            // 设置整体状态
            result.put("overallStatus", allUp ? "UP" : "DOWN");
            result.put("timestamp", System.currentTimeMillis());
            result.put("modules", modules);
        } catch (Exception e) {
            // 处理异常情况，返回DOWN状态
            result.put("overallStatus", "DOWN");
            result.put("timestamp", System.currentTimeMillis());
            result.put("errorMessage", e.getMessage());
            result.put("modules", modules);
        }
        
        return result;
    }
    
    /**
     * 检查单个模块的健康状态
     * @param url 模块健康检查API URL
     * @param moduleName 模块名称
     * @param modules 模块健康状态映射
     * @return 模块是否健康
     */
    @SuppressWarnings("unchecked")
    private boolean checkModuleHealth(String url, String moduleName, Map<String, Object> modules) {
        try {
            Map<String, Object> healthResult = restTemplate.getForObject(url, Map.class);
            if (healthResult != null) {
                modules.put(moduleName, healthResult);
                Object status = healthResult.get("status");
                return status != null && "UP".equals(status.toString());
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "DOWN");
                errorResult.put("errorMessage", "健康检查返回空结果");
                modules.put(moduleName, errorResult);
                return false;
            }
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "DOWN");
            errorResult.put("errorMessage", e.getMessage());
            modules.put(moduleName, errorResult);
            return false;
        }
    }
}
