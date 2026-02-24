package com.example.medaiassistant.controller;

import com.example.medaiassistant.util.DuplicateDataCleanupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据维护控制器
 * 提供数据清理和维护相关的API接口
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@RestController
@RequestMapping("/api/maintenance")
public class DataMaintenanceController {

    private static final Logger logger = LoggerFactory.getLogger(DataMaintenanceController.class);

    private final DuplicateDataCleanupUtil duplicateDataCleanupUtil;

    public DataMaintenanceController(DuplicateDataCleanupUtil duplicateDataCleanupUtil) {
        this.duplicateDataCleanupUtil = duplicateDataCleanupUtil;
    }

    /**
     * 检查重复数据
     * GET /api/maintenance/check-duplicates
     * 
     * @return 重复数据检查结果
     */
    @GetMapping("/check-duplicates")
    public ResponseEntity<Map<String, Object>> checkDuplicates() {
        logger.info("收到检查重复数据请求");

        try {
            int duplicateCount = duplicateDataCleanupUtil.checkDuplicateRequestIds();
            String report = duplicateDataCleanupUtil.getDuplicateCleanupReport();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("duplicateGroups", duplicateCount);
            response.put("report", report);
            response.put("message", duplicateCount > 0 ? "发现 " + duplicateCount + " 个重复的REQUEST_ID" : "未发现重复数据");

            logger.info("重复数据检查完成，发现 {} 个重复组", duplicateCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("检查重复数据时发生异常: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "检查重复数据失败");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 清理所有重复数据
     * POST /api/maintenance/cleanup-duplicates
     * 
     * @return 清理结果
     */
    @PostMapping("/cleanup-duplicates")
    public ResponseEntity<Map<String, Object>> cleanupDuplicates() {
        logger.info("收到清理重复数据请求");

        try {
            int cleanedCount = duplicateDataCleanupUtil.cleanupAllDuplicateRequestIds();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cleanedRecords", cleanedCount);
            response.put("message", cleanedCount > 0 ? "成功清理 " + cleanedCount + " 条重复记录" : "没有发现需要清理的重复记录");

            logger.info("重复数据清理完成，清理了 {} 条记录", cleanedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("清理重复数据时发生异常: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "清理重复数据失败");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 清理指定REQUEST_ID的重复数据
     * POST /api/maintenance/cleanup-request-id/{requestId}
     * 
     * @param requestId 要清理的REQUEST_ID
     * @return 清理结果
     */
    @PostMapping("/cleanup-request-id/{requestId}")
    public ResponseEntity<Map<String, Object>> cleanupSpecificRequestId(@PathVariable String requestId) {
        logger.info("收到清理指定REQUEST_ID的重复数据请求: {}", requestId);

        try {
            int cleanedCount = duplicateDataCleanupUtil.cleanupSpecificRequestId(requestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requestId", requestId);
            response.put("cleanedRecords", cleanedCount);
            response.put("message", cleanedCount > 0 ? "成功清理REQUEST_ID " + requestId + " 的 " + cleanedCount + " 条重复记录"
                    : "REQUEST_ID " + requestId + " 没有重复记录");

            logger.info("REQUEST_ID {} 的重复数据清理完成，清理了 {} 条记录", requestId, cleanedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("清理REQUEST_ID {} 的重复数据时发生异常: {}", requestId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("requestId", requestId);
            response.put("error", e.getMessage());
            response.put("message", "清理指定REQUEST_ID的重复数据失败");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取数据维护统计信息
     * GET /api/maintenance/stats
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMaintenanceStats() {
        logger.info("收到获取数据维护统计信息请求");

        try {
            String report = duplicateDataCleanupUtil.getDuplicateCleanupReport();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("report", report);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取数据维护统计信息时发生异常: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "获取统计信息失败");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
