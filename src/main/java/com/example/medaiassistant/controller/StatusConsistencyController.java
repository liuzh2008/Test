package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.ConsistencyCheckService;
import com.example.medaiassistant.service.PromptStatusManager;
import com.example.medaiassistant.service.StatusTransitionHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态一致性检查API控制器
 * 
 * 迭代3核心组件：状态一致性检查API接口
 * 
 * 提供的API功能：
 * 1. 手动触发一致性检查
 * 2. 查看一致性检查结果
 * 3. 获取状态管理统计信息
 * 4. 查看状态转换历史
 * 5. 手动状态转换
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@RestController
@RequestMapping("/api/status-consistency")
public class StatusConsistencyController {

    private static final Logger logger = LoggerFactory.getLogger(StatusConsistencyController.class);

    private final ConsistencyCheckService consistencyCheckService;
    private final PromptStatusManager promptStatusManager;
    private final StatusTransitionHistory statusTransitionHistory;

    public StatusConsistencyController(ConsistencyCheckService consistencyCheckService,
            PromptStatusManager promptStatusManager,
            StatusTransitionHistory statusTransitionHistory) {
        this.consistencyCheckService = consistencyCheckService;
        this.promptStatusManager = promptStatusManager;
        this.statusTransitionHistory = statusTransitionHistory;
    }

    /**
     * 手动触发一致性检查
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> performConsistencyCheck(
            @RequestParam(value = "autoFix", defaultValue = "false") boolean autoFix) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("手动触发一致性检查，自动修复: {}", autoFix);

            ConsistencyCheckService.ConsistencyCheckResult result = consistencyCheckService
                    .performConsistencyCheck(autoFix);

            response.put("status", "SUCCESS");
            response.put("checkTime", result.getCheckTime());
            response.put("checkDurationMs", result.getCheckDurationMs());
            response.put("autoFixEnabled", result.isAutoFixEnabled());
            response.put("totalChecked", result.getTotalChecked());
            response.put("inconsistenciesFound", result.getInconsistenciesFound());
            response.put("autoFixed", result.getAutoFixed());
            response.put("issues", result.getIssues());
            response.put("errors", result.getErrors());

            if (result.getInconsistenciesFound() > 0) {
                response.put("message", String.format("发现 %d 个不一致问题，已自动修复 %d 个",
                        result.getInconsistenciesFound(), result.getAutoFixed()));
            } else {
                response.put("message", "未发现一致性问题，系统状态正常");
            }

            logger.info("一致性检查完成 - 问题: {}, 修复: {}", result.getInconsistenciesFound(), result.getAutoFixed());

        } catch (Exception e) {
            logger.error("一致性检查失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "一致性检查失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取最后一次检查结果
     */
    @GetMapping("/last-check-result")
    public ResponseEntity<Map<String, Object>> getLastCheckResult() {
        Map<String, Object> response = new HashMap<>();

        try {
            ConsistencyCheckService.ConsistencyCheckResult lastResult = consistencyCheckService.getLastCheckResult();

            if (lastResult == null) {
                response.put("status", "NO_DATA");
                response.put("message", "尚未执行过一致性检查");
            } else {
                response.put("status", "SUCCESS");
                response.put("checkTime", lastResult.getCheckTime());
                response.put("checkDurationMs", lastResult.getCheckDurationMs());
                response.put("autoFixEnabled", lastResult.isAutoFixEnabled());
                response.put("totalChecked", lastResult.getTotalChecked());
                response.put("inconsistenciesFound", lastResult.getInconsistenciesFound());
                response.put("autoFixed", lastResult.getAutoFixed());
                response.put("issues", lastResult.getIssues());
                response.put("errors", lastResult.getErrors());
            }

        } catch (Exception e) {
            logger.error("获取最后检查结果失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "获取检查结果失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取一致性检查统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getConsistencyStatistics() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> consistencyStats = consistencyCheckService.getStatistics();
            Map<String, Object> statusManagerStats = promptStatusManager.getStatistics();

            response.put("status", "SUCCESS");
            response.put("consistencyCheck", consistencyStats);
            response.put("statusManager", statusManagerStats);
            response.put("message", "统计信息获取成功");

        } catch (Exception e) {
            logger.error("获取统计信息失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 查看指定Prompt的状态信息
     */
    @GetMapping("/prompt/{promptId}/status")
    public ResponseEntity<Map<String, Object>> getPromptStatus(@PathVariable Integer promptId) {
        Map<String, Object> response = new HashMap<>();

        try {
            PromptStatusManager.PromptStatusInfo statusInfo = promptStatusManager.getPromptStatus(promptId);

            if (statusInfo == null) {
                response.put("status", "NOT_FOUND");
                response.put("message", "Prompt不存在: " + promptId);
                return ResponseEntity.notFound().build();
            }

            response.put("status", "SUCCESS");
            response.put("promptId", statusInfo.getPromptId());
            response.put("currentStatus", statusInfo.getCurrentStatus());
            response.put("version", statusInfo.getVersion());
            response.put("submissionTime", statusInfo.getSubmissionTime());
            response.put("executionTime", statusInfo.getExecutionTime());
            response.put("transitionHistory", statusInfo.getTransitionHistory());

        } catch (Exception e) {
            logger.error("获取Prompt状态失败: Prompt ID {}, 错误: {}", promptId, e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "获取Prompt状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 手动执行状态转换
     */
    @PostMapping("/prompt/{promptId}/transition")
    public ResponseEntity<Map<String, Object>> transitionPromptStatus(
            @PathVariable Integer promptId,
            @RequestParam String newStatus,
            @RequestParam(required = false, defaultValue = "手动状态转换") String reason) {

        Map<String, Object> response = new HashMap<>();

        try {
            PromptStatusManager.StatusTransitionResult result = promptStatusManager.transitionStatus(
                    promptId, newStatus, reason, "StatusConsistencyController");

            if (result.isSuccess()) {
                response.put("status", "SUCCESS");
                response.put("message", String.format("状态转换成功：%s → %s", result.getFromStatus(), result.getToStatus()));
                response.put("fromStatus", result.getFromStatus());
                response.put("toStatus", result.getToStatus());
                response.put("version", result.getVersion());
            } else {
                response.put("status", "FAILED");
                response.put("message", "状态转换失败: " + result.getErrorMessage());
                response.put("errorMessage", result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("手动状态转换失败: Prompt ID {}, 目标状态: {}, 错误: {}",
                    promptId, newStatus, e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "状态转换失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取转换历史统计
     */
    @GetMapping("/transition-statistics")
    public ResponseEntity<Map<String, Object>> getTransitionStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Map<String, Object> response = new HashMap<>();

        try {
            java.time.LocalDateTime start = startTime != null ? java.time.LocalDateTime.parse(startTime)
                    : java.time.LocalDateTime.now().minusDays(7);

            java.time.LocalDateTime end = endTime != null ? java.time.LocalDateTime.parse(endTime)
                    : java.time.LocalDateTime.now();

            Map<String, Object> statistics = statusTransitionHistory.getTransitionStatistics(start, end);
            Map<String, Object> commonPaths = statusTransitionHistory.getCommonTransitionPaths();

            response.put("status", "SUCCESS");
            response.put("timeRange", Map.of("start", start, "end", end));
            response.put("statistics", statistics);
            response.put("commonTransitionPaths", commonPaths);

        } catch (Exception e) {
            logger.error("获取转换统计失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", "获取转换统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "HEALTHY");
            response.put("service", "StatusConsistencyController");
            response.put("version", "3.0.0");
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("message", "状态一致性服务运行正常");

        } catch (Exception e) {
            response.put("status", "UNHEALTHY");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}