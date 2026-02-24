package com.example.medaiassistant.controller;

import com.example.medaiassistant.hospital.service.SyncScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 自动同步控制器
 * 提供自动同步病人列表数据的REST API接口
 * 
 * 功能包括：
 * 1. 启动自动同步（每小时一次）
 * 2. 停止自动同步
 * 3. 获取同步状态
 * 4. 立即执行所有同步任务
 * 
 * 接口路径：/api/auto-sync
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-12
 */
@RestController
@RequestMapping("/api/auto-sync")
@Slf4j
public class AutoSyncController {

    private final SyncScheduler syncScheduler;

    @Autowired
    public AutoSyncController(SyncScheduler syncScheduler) {
        this.syncScheduler = syncScheduler;
    }

    /**
     * 启动自动同步
     * 
     * 接口：POST /api/auto-sync/start
     * 功能：启动每小时自动同步病人列表数据
     * 
     * @return 启动结果
     * 
     * @apiNote 响应示例（成功）：
     * {
     *   "success": true,
     *   "message": "自动同步已启动",
     *   "autoSyncEnabled": true,
     *   "cronExpression": "0 0 * * * ?",
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     * 
     * @apiNote 响应示例（已经是启动状态）：
     * {
     *   "success": true,
     *   "message": "自动同步已经是启动状态",
     *   "autoSyncEnabled": true,
     *   "cronExpression": "0 0 * * * ?",
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAutoSync() {
        log.info("接收到启动自动同步请求");
        
        try {
            boolean started = syncScheduler.startAutoSync();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", started ? "自动同步已启动" : "自动同步已经是启动状态",
                "autoSyncEnabled", true,
                "cronExpression", "0 0 * * * ?", // 每小时执行一次
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            log.info("自动同步启动成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("启动自动同步失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "启动自动同步失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 停止自动同步
     * 
     * 接口：POST /api/auto-sync/stop
     * 功能：停止自动同步病人列表数据
     * 
     * @return 停止结果
     * 
     * @apiNote 响应示例（成功停止）：
     * {
     *   "success": true,
     *   "message": "自动同步已停止",
     *   "autoSyncEnabled": false,
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     * 
     * @apiNote 响应示例（已经是停止状态）：
     * {
     *   "success": true,
     *   "message": "自动同步已经是停止状态",
     *   "autoSyncEnabled": false,
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopAutoSync() {
        log.info("接收到停止自动同步请求");
        
        try {
            boolean stopped = syncScheduler.stopAutoSync();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", stopped ? "自动同步已停止" : "自动同步已经是停止状态",
                "autoSyncEnabled", false,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            log.info("自动同步停止成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("停止自动同步失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "停止自动同步失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取自动同步状态
     * 
     * 接口：GET /api/auto-sync/status
     * 功能：获取自动同步的详细状态信息
     * 
     * @return 同步状态信息
     * 
     * @apiNote 响应示例：
     * {
     *   "autoSyncEnabled": true,
     *   "autoSyncCron": "0 0 * * * ?",
     *   "currentTime": "Thu Dec 12 19:30:00 CST 2025",
     *   "scheduledTasksCount": 2,
     *   "enabledTasksCount": 2,
     *   "executionStatistics": {
     *     "autoSyncStarted": 1,
     *     "lastAutoSyncStart": 1734003000,
     *     "hourlyAutoSyncExecutions": 5,
     *     "lastHourlyAutoSync": 1734003000
     *   },
     *   "recentTasks": [
     *     {
     *       "taskId": "sync-cdwyy-1734003000123",
     *       "status": "COMPLETED",
     *       "hospitalId": "cdwyy"
     *     }
     *   ],
     *   "scheduledTaskDetails": [
     *     {
     *       "hospitalId": "cdwyy",
     *       "enabled": true,
     *       "priority": "MEDIUM",
     *       "cronExpression": "0 0/30 * * * ?"
     *     },
     *     {
     *       "hospitalId": "hospital-001",
     *       "enabled": true,
     *       "priority": "MEDIUM",
     *       "cronExpression": "0 0/5 * * * ?"
     *     }
     *   ],
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAutoSyncStatus() {
        log.debug("接收到获取自动同步状态请求");
        
        try {
            Map<String, Object> status = syncScheduler.getAutoSyncStatus();
            status.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.debug("自动同步状态查询成功");
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取自动同步状态失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取自动同步状态失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 立即执行所有同步任务
     * 
     * 接口：POST /api/auto-sync/trigger-now
     * 功能：立即执行所有启用的医院同步任务，不等待定时任务
     * 
     * @return 执行结果
     * 
     * @apiNote 响应示例：
     * {
     *   "success": true,
     *   "message": "立即执行同步任务完成",
     *   "totalTasks": 2,
     *   "successCount": 2,
     *   "failureCount": 0,
     *   "executionResults": [
     *     {
     *       "hospitalId": "cdwyy",
     *       "priority": "MEDIUM",
     *       "success": true,
     *       "message": "任务触发成功"
     *     },
     *     {
     *       "hospitalId": "hospital-001",
     *       "priority": "MEDIUM",
     *       "success": true,
     *       "message": "任务触发成功"
     *     }
     *   ],
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @PostMapping("/trigger-now")
    public ResponseEntity<Map<String, Object>> triggerAllSyncNow() {
        log.info("接收到立即执行所有同步任务请求");
        
        try {
            Map<String, Object> result = syncScheduler.triggerAllEnabledSync();
            result.put("success", true);
            result.put("message", "立即执行同步任务完成");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.info("立即执行所有同步任务成功，共执行 {} 个任务", result.get("totalTasks"));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("立即执行所有同步任务失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "立即执行所有同步任务失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 健康检查端点
     * 
     * 接口：GET /api/auto-sync/health
     * 功能：检查自动同步服务的健康状态
     * 
     * @return 健康状态信息
     * 
     * @apiNote 响应示例：
     * {
     *   "status": "UP",
     *   "service": "auto-sync",
     *   "timestamp": "2025-12-12T19:30:00.123",
     *   "features": ["start-auto-sync", "stop-auto-sync", "get-status", "trigger-now"]
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("自动同步服务健康检查");
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "auto-sync",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "features", java.util.List.of("start-auto-sync", "stop-auto-sync", "get-status", "trigger-now")
        ));
    }

    /**
     * 获取自动同步配置信息
     * 
     * 接口：GET /api/auto-sync/config
     * 功能：获取自动同步的配置信息
     * 
     * @return 配置信息
     * 
     * @apiNote 响应示例：
     * {
     *   "autoSyncCron": "0 0 * * * ?",
     *   "description": "每小时执行一次自动同步",
     *   "defaultEnabled": true,
     *   "supportedHospitals": ["cdwyy", "hospital-001"],
     *   "timestamp": "2025-12-12T19:30:00.123"
     * }
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        log.debug("接收到获取自动同步配置请求");
        
        try {
            Map<String, Object> config = Map.of(
                "autoSyncCron", "0 0 * * * ?",
                "description", "每小时执行一次自动同步",
                "defaultEnabled", true,
                "supportedHospitals", java.util.List.of("cdwyy", "hospital-001"),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("获取自动同步配置失败", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取自动同步配置失败: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
