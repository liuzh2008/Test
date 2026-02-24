package com.example.medaiassistant.healthcheck.aggregator;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 健康检查聚合器测试类
 * 按照TDD红-绿-重构流程实现
 */
public class HealthCheckAggregatorTest {

    /**
     * 测试健康检查聚合器能正确聚合所有结果
     * 红阶段：期望测试失败，因为还没有实现HealthCheckAggregator类
     */
    @Test
    void testHealthCheckAggregatorCanAggregateResults() {
        // 模拟RestTemplate返回成功结果
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        
        // 模拟所有模块健康检查结果
        Map<String, Object> healthResult = new HashMap<>();
        healthResult.put("status", "UP");
        
        // 设置mock返回值
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(healthResult);
        
        // 创建HealthCheckAggregator实例
        HealthCheckAggregator aggregator = new HealthCheckAggregator(restTemplate);
        
        // 调用聚合方法
        Map<String, Object> result = aggregator.aggregateHealthChecks();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("UP", result.get("overallStatus"));
        assertNotNull(result.get("timestamp"));
    }

    /**
     * 测试健康检查聚合器能正确处理API调用失败情况
     * 红阶段：期望测试失败，因为还没有实现错误处理逻辑
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHealthCheckAggregatorCanHandleApiFailure() {
        // 模拟RestTemplate，让其抛出异常
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class)))
                .thenThrow(new RuntimeException("API调用失败"));
        
        // 创建HealthCheckAggregator实例
        HealthCheckAggregator aggregator = new HealthCheckAggregator(restTemplate);
        
        // 调用聚合方法
        Map<String, Object> result = aggregator.aggregateHealthChecks();
        
        // 验证结果
        assertNotNull(result);
        // 当API调用失败时，整体状态应该为DOWN
        assertEquals("DOWN", result.get("overallStatus"));
        // 验证包含时间戳
        assertNotNull(result.get("timestamp"));
        // 验证包含模块信息
        Map<String, Object> modules = (Map<String, Object>) result.get("modules");
        assertNotNull(modules);
        // 验证模块中包含错误信息
        for (Map.Entry<String, Object> entry : modules.entrySet()) {
            Map<String, Object> moduleHealth = (Map<String, Object>) entry.getValue();
            assertEquals("DOWN", moduleHealth.get("status"));
            assertNotNull(moduleHealth.get("errorMessage"));
        }
    }
    
    /**
     * 测试健康检查聚合器能正确聚合所有模块的健康状态
     * 红阶段：期望测试失败，因为当前只实现了主服务器的健康检查
     */
    @Test
    @SuppressWarnings("unchecked")
    void testHealthCheckAggregatorCanAggregateAllModules() {
        // 模拟RestTemplate返回成功结果
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        
        // 模拟各模块健康检查结果
        Map<String, Object> mainHealth = new HashMap<>();
        mainHealth.put("status", "UP");
        
        Map<String, Object> executionHealth = new HashMap<>();
        executionHealth.put("status", "UP");
        
        Map<String, Object> databaseHealth = new HashMap<>();
        databaseHealth.put("status", "UP");
        
        Map<String, Object> networkHealth = new HashMap<>();
        networkHealth.put("status", "UP");
        
        Map<String, Object> configHealth = new HashMap<>();
        configHealth.put("status", "UP");
        
        Map<String, Object> pollingHealth = new HashMap<>();
        pollingHealth.put("status", "UP");
        
        // 设置mock返回值
        Mockito.when(restTemplate.getForObject("/api/health/ping", Map.class)).thenReturn(mainHealth);
        Mockito.when(restTemplate.getForObject("http://localhost:8082/api/execute/health", Map.class)).thenReturn(executionHealth);
        Mockito.when(restTemplate.getForObject("/api/database/health", Map.class)).thenReturn(databaseHealth);
        Mockito.when(restTemplate.getForObject("/api/network/health", Map.class)).thenReturn(networkHealth);
        Mockito.when(restTemplate.getForObject("/api/configuration/health", Map.class)).thenReturn(configHealth);
        Mockito.when(restTemplate.getForObject("/api/polling/detailed-status", Map.class)).thenReturn(pollingHealth);
        
        // 创建HealthCheckAggregator实例
        HealthCheckAggregator aggregator = new HealthCheckAggregator(restTemplate);
        
        // 调用聚合方法
        Map<String, Object> result = aggregator.aggregateHealthChecks();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("UP", result.get("overallStatus"));
        
        // 验证是否包含各模块的健康状态
        Map<String, Object> modules = (Map<String, Object>) result.get("modules");
        assertNotNull(modules);
        assertEquals(6, modules.size());
        assertEquals("UP", ((Map<String, Object>) modules.get("mainServer")).get("status"));
        assertEquals("UP", ((Map<String, Object>) modules.get("executionServer")).get("status"));
        assertEquals("UP", ((Map<String, Object>) modules.get("database")).get("status"));
        assertEquals("UP", ((Map<String, Object>) modules.get("network")).get("status"));
        assertEquals("UP", ((Map<String, Object>) modules.get("configuration")).get("status"));
        assertEquals("UP", ((Map<String, Object>) modules.get("pollingService")).get("status"));
    }
}
