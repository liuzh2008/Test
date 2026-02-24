package com.example.medaiassistant.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查结果类
 * 封装健康检查的状态和详细信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-03
 */
public class HealthCheckResult {

    private final boolean healthy;
    private final LocalDateTime timestamp;
    private final Map<String, ComponentHealth> components;
    private final List<String> unhealthyComponents;

    /**
     * 构造健康检查结果
     * 
     * @param healthy 是否健康
     * @param components 组件健康状态
     */
    public HealthCheckResult(boolean healthy, Map<String, ComponentHealth> components) {
        this.healthy = healthy;
        this.timestamp = LocalDateTime.now();
        this.components = components != null ? components : new HashMap<>();
        this.unhealthyComponents = new ArrayList<>();
        
        // 收集不健康的组件
        this.components.forEach((name, health) -> {
            if (!health.isHealthy()) {
                this.unhealthyComponents.add(name);
            }
        });
    }

    /**
     * 获取是否健康
     * 
     * @return 是否健康
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * 获取检查时间戳
     * 
     * @return 检查时间戳
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 获取所有组件健康状态
     * 
     * @return 组件健康状态映射
     */
    public Map<String, ComponentHealth> getComponents() {
        return components;
    }

    /**
     * 获取不健康的组件列表
     * 
     * @return 不健康的组件名称列表
     */
    public List<String> getUnhealthyComponents() {
        return unhealthyComponents;
    }

    /**
     * 组件健康状态内部类
     */
    public static class ComponentHealth {
        private final String name;
        private final boolean healthy;
        private final String status;
        private final String details;

        /**
         * 构造组件健康状态
         * 
         * @param name 组件名称
         * @param healthy 是否健康
         * @param status 状态描述
         * @param details 详细信息
         */
        public ComponentHealth(String name, boolean healthy, String status, String details) {
            this.name = name;
            this.healthy = healthy;
            this.status = status;
            this.details = details;
        }

        /**
         * 获取组件名称
         * 
         * @return 组件名称
         */
        public String getName() {
            return name;
        }

        /**
         * 获取是否健康
         * 
         * @return 是否健康
         */
        public boolean isHealthy() {
            return healthy;
        }

        /**
         * 获取状态描述
         * 
         * @return 状态描述
         */
        public String getStatus() {
            return status;
        }

        /**
         * 获取详细信息
         * 
         * @return 详细信息
         */
        public String getDetails() {
            return details;
        }
    }
}
