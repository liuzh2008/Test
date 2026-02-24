package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.NetworkHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 网络健康监控控制器
 * 迭代1：提供网络状态查询和管理接口
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@RestController
@RequestMapping("/api/network")
public class NetworkHealthController {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHealthController.class);
    
    @Autowired
    private NetworkHealthService networkHealthService;
    
    /**
     * 获取网络健康状态
     * 
     * @return 网络健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getNetworkHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            NetworkHealthService.NetworkHealthStats stats = networkHealthService.getHealthStats();
            
            response.put("status", "SUCCESS");
            response.put("healthy", stats.isHealthy());
            response.put("totalRequests", stats.getTotalRequests());
            response.put("successfulRequests", stats.getSuccessfulRequests());
            response.put("failedRequests", stats.getFailedRequests());
            response.put("successRate", Math.round(stats.getSuccessRate() * 10000) / 100.0); // 百分比，保留2位小数
            response.put("averageLatency", stats.getAverageLatency());
            response.put("consecutiveFailures", stats.getConsecutiveFailures());
            response.put("lastSuccessTime", stats.getLastSuccessTime());
            response.put("recommendedRetries", networkHealthService.getRecommendedRetries());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取网络健康状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取网络健康状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 执行网络健康检查
     * 主动检查执行服务器连通性
     * 
     * @return 检查结果
     */
    @PostMapping("/health/check")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = networkHealthService.performHealthCheck();
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("status", "SUCCESS");
            response.put("healthy", isHealthy);
            response.put("checkDuration", duration);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", isHealthy ? "网络连接正常" : "网络连接异常");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("执行网络健康检查失败", e);
            response.put("status", "ERROR");
            response.put("healthy", false);
            response.put("message", "网络健康检查失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置网络健康统计
     * 清空所有统计数据，重新开始计算
     * 
     * @return 重置结果
     */
    @PostMapping("/health/reset")
    public ResponseEntity<Map<String, Object>> resetNetworkStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            networkHealthService.resetStats();
            
            response.put("status", "SUCCESS");
            response.put("message", "网络健康统计已重置");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("网络健康统计已重置");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重置网络健康统计失败", e);
            response.put("status", "ERROR");
            response.put("message", "重置失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取网络状态摘要
     * 简化的网络状态信息，用于监控面板
     * 
     * @return 网络状态摘要
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNetworkStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            NetworkHealthService.NetworkHealthStats stats = networkHealthService.getHealthStats();
            
            response.put("status", stats.isHealthy() ? "HEALTHY" : "UNHEALTHY");
            response.put("successRate", Math.round(stats.getSuccessRate() * 100 * 100) / 100.0);
            response.put("averageLatency", stats.getAverageLatency());
            response.put("recommendedRetries", networkHealthService.getRecommendedRetries());
            response.put("lastCheck", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取网络状态摘要失败", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取网络统计详情
     * 详细的网络传输统计信息
     * 
     * @return 详细统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNetworkStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            NetworkHealthService.NetworkHealthStats stats = networkHealthService.getHealthStats();
            
            response.put("status", "SUCCESS");
            response.put("statistics", Map.of(
                "totalRequests", stats.getTotalRequests(),
                "successfulRequests", stats.getSuccessfulRequests(),
                "failedRequests", stats.getFailedRequests(),
                "successRate", stats.getSuccessRate(),
                "averageLatency", stats.getAverageLatency(),
                "consecutiveFailures", stats.getConsecutiveFailures(),
                "lastSuccessTime", stats.getLastSuccessTime(),
                "isHealthy", stats.isHealthy()
            ));
            response.put("recommendations", Map.of(
                "retryCount", networkHealthService.getRecommendedRetries(),
                "healthStatus", stats.isHealthy() ? "正常" : "异常",
                "suggestion", generateHealthSuggestion(stats)
            ));
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取网络统计详情失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取统计详情失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 生成健康状况建议
     */
    private String generateHealthSuggestion(NetworkHealthService.NetworkHealthStats stats) {
        if (!stats.isHealthy()) {
            return "网络状态异常，建议检查网络连接或执行服务器状态";
        } else if (stats.getSuccessRate() < 0.9) {
            return "网络成功率偏低，建议增加重试次数或检查网络稳定性";
        } else if (stats.getAverageLatency() > 3000) {
            return "网络延迟较高，建议优化网络配置或增加超时时间";
        } else {
            return "网络状态良好";
        }
    }
}