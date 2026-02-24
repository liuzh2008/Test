package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.PollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 轮询控制器
 * 提供轮询服务的REST接口，用于测试和监控轮询功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/polling")
public class PollingController {

    private static final Logger logger = LoggerFactory.getLogger(PollingController.class);

    @Autowired
    private PollingService pollingService;

    /**
     * 获取轮询状态统计
     * 返回ENCRYPTED_DATA_TEMP表中各种状态的数据数量统计
     * 
     * @return 轮询状态统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPollingStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            String stats = pollingService.getPollingStats();
            response.put("status", "SUCCESS");
            response.put("stats", stats);
            response.put("message", "轮询状态统计获取成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取轮询状态统计失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取轮询状态统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 手动触发轮询
     * 用于测试和调试目的，立即执行一次轮询操作
     * 
     * @return 轮询执行结果
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerPolling() {
        Map<String, Object> response = new HashMap<>();
        try {
            pollingService.triggerPolling();
            response.put("status", "SUCCESS");
            response.put("message", "手动轮询触发成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("手动触发轮询失败", e);
            response.put("status", "ERROR");
            response.put("message", "手动触发轮询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 启用轮询服务
     * 
     * @return 操作结果
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enablePolling() {
        Map<String, Object> response = new HashMap<>();
        try {
            pollingService.enablePolling();
            response.put("status", "SUCCESS");
            response.put("message", "轮询服务已启用");
            response.put("enabled", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("启用轮询服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "启用轮询服务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 禁用轮询服务
     * 
     * @return 操作结果
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disablePolling() {
        Map<String, Object> response = new HashMap<>();
        try {
            pollingService.disablePolling();
            response.put("status", "SUCCESS");
            response.put("message", "轮询服务已禁用");
            response.put("enabled", false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("禁用轮询服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "禁用轮询服务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取轮询服务状态
     * 
     * @return 轮询服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPollingStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean enabled = pollingService.isPollingEnabled();
            response.put("status", "SUCCESS");
            response.put("enabled", enabled);
            response.put("message", enabled ? "轮询服务已启用" : "轮询服务已禁用");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取轮询服务状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取轮询服务状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 健康检查接口
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "Polling Controller");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取轮询服务详细状态
     * 包含是否启用、最后一次轮询时间、下一次轮询时间、轮询间隔等
     * 
     * @return 轮询服务的详细状态信息
     */
    @GetMapping("/detailed-status")
    public ResponseEntity<Map<String, Object>> getDetailedPollingStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> status = pollingService.getDetailedStatus();
            response.put("status", "SUCCESS");
            response.putAll(status);
            response.put("message", "轮询服务详细状态获取成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取轮询服务详细状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取轮询服务详细状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
