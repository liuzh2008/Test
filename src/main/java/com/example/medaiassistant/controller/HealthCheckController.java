package com.example.medaiassistant.controller;

import com.example.medaiassistant.healthcheck.aggregator.HealthCheckAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查API控制器
 * 提供统一的健康检查入口
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    private final HealthCheckAggregator healthCheckAggregator;

    /**
     * 构造函数
     * @param healthCheckAggregator 健康检查聚合器实例
     */
    public HealthCheckController(HealthCheckAggregator healthCheckAggregator) {
        this.healthCheckAggregator = healthCheckAggregator;
    }

    /**
     * 获取整体健康状态
     * @return 整体健康检查结果
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOverallHealth() {
        Map<String, Object> result = healthCheckAggregator.aggregateHealthChecks();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取详细健康状态
     * @return 详细的健康检查结果
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> result = healthCheckAggregator.aggregateHealthChecks();
        result.put("detailed", true);
        return ResponseEntity.ok(result);
    }
}