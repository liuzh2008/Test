package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.PromptSubmissionService;
import com.example.medaiassistant.service.PromptPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt服务控制器
 * 提供独立的提交和轮询服务管理接口
 * 
 * 主要功能：
 * 1. 独立启用/禁用提交服务
 * 2. 独立启用/禁用轮询服务
 * 3. 获取服务状态和统计信息
 * 4. 健康检查和配置信息
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-10-02
 */
@RestController
@RequestMapping("/api/prompt-services")
public class PromptServicesController {
    private static final Logger logger = LoggerFactory.getLogger(PromptServicesController.class);

    private final PromptSubmissionService promptSubmissionService;
    private final PromptPollingService promptPollingService;

    /**
     * 构造函数，依赖注入必要的服务实例
     * 
     * @param promptSubmissionService Prompt提交服务
     * @param promptPollingService    Prompt轮询服务
     */
    public PromptServicesController(PromptSubmissionService promptSubmissionService,
            PromptPollingService promptPollingService) {
        this.promptSubmissionService = promptSubmissionService;
        this.promptPollingService = promptPollingService;
    }

    /**
     * 启用提交服务
     * 
     * @return 启用结果响应
     */
    @PostMapping("/submission/enable")
    public ResponseEntity<Map<String, Object>> enableSubmission() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptSubmissionService.enableSubmission();

            response.put("status", "SUCCESS");
            response.put("message", "提交服务已启用");
            response.put("submissionEnabled", promptSubmissionService.isSubmissionEnabled());
            response.put("stats", promptSubmissionService.getSubmissionStats());

            logger.info("提交服务已通过API启用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("启用提交服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "启用提交服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 禁用提交服务
     * 
     * @return 禁用结果响应
     */
    @PostMapping("/submission/disable")
    public ResponseEntity<Map<String, Object>> disableSubmission() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptSubmissionService.disableSubmission();

            response.put("status", "SUCCESS");
            response.put("message", "提交服务已禁用");
            response.put("submissionEnabled", promptSubmissionService.isSubmissionEnabled());
            response.put("stats", promptSubmissionService.getSubmissionStats());

            logger.info("提交服务已通过API禁用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("禁用提交服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "禁用提交服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 启用轮询服务
     * 
     * @return 启用结果响应
     */
    @PostMapping("/polling/enable")
    public ResponseEntity<Map<String, Object>> enablePolling() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptPollingService.enablePolling();

            response.put("status", "SUCCESS");
            response.put("message", "轮询服务已启用");
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());
            response.put("stats", promptPollingService.getPollingStats());

            logger.info("轮询服务已通过API启用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("启用轮询服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "启用轮询服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 禁用轮询服务
     * 
     * @return 禁用结果响应
     */
    @PostMapping("/polling/disable")
    public ResponseEntity<Map<String, Object>> disablePolling() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptPollingService.disablePolling();

            response.put("status", "SUCCESS");
            response.put("message", "轮询服务已禁用");
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());
            response.put("stats", promptPollingService.getPollingStats());

            logger.info("轮询服务已通过API禁用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("禁用轮询服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "禁用轮询服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 启用所有服务
     * 
     * @return 启用结果响应
     */
    @PostMapping("/enable-all")
    public ResponseEntity<Map<String, Object>> enableAll() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptSubmissionService.enableSubmission();
            promptPollingService.enablePolling();

            response.put("status", "SUCCESS");
            response.put("message", "所有服务已启用");
            response.put("submissionEnabled", promptSubmissionService.isSubmissionEnabled());
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());
            response.put("submissionStats", promptSubmissionService.getSubmissionStats());
            response.put("pollingStats", promptPollingService.getPollingStats());

            logger.info("所有服务已通过API启用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("启用所有服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "启用所有服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 禁用所有服务
     * 
     * @return 禁用结果响应
     */
    @PostMapping("/disable-all")
    public ResponseEntity<Map<String, Object>> disableAll() {
        Map<String, Object> response = new HashMap<>();

        try {
            promptSubmissionService.disableSubmission();
            promptPollingService.disablePolling();

            response.put("status", "SUCCESS");
            response.put("message", "所有服务已禁用");
            response.put("submissionEnabled", promptSubmissionService.isSubmissionEnabled());
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());
            response.put("submissionStats", promptSubmissionService.getSubmissionStats());
            response.put("pollingStats", promptPollingService.getPollingStats());

            logger.info("所有服务已通过API禁用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("禁用所有服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "禁用所有服务失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取服务状态
     * 
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServicesStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> submissionStatus = new HashMap<>();
            submissionStatus.put("enabled", promptSubmissionService.isSubmissionEnabled());
            submissionStatus.put("stats", promptSubmissionService.getSubmissionStats());
            submissionStatus.put("pendingCount", promptSubmissionService.getPendingSubmissionCount());
            submissionStatus.put("health", promptSubmissionService.healthCheck());
            submissionStatus.put("config", promptSubmissionService.getSubmissionConfigInfo());

            Map<String, Object> pollingStatus = new HashMap<>();
            pollingStatus.put("enabled", promptPollingService.isPollingEnabled());
            pollingStatus.put("stats", promptPollingService.getPollingStats());
            pollingStatus.put("promptStats", promptPollingService.getPromptStatusStats());
            pollingStatus.put("health", promptPollingService.healthCheck());
            pollingStatus.put("config", promptPollingService.getPollingConfigInfo());

            response.put("status", "SUCCESS");
            response.put("submission", submissionStatus);
            response.put("polling", pollingStatus);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取服务状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取服务状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取提交服务状态
     * 
     * @return 提交服务状态信息
     */
    @GetMapping("/submission/status")
    public ResponseEntity<Map<String, Object>> getSubmissionStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "SUCCESS");
            response.put("enabled", promptSubmissionService.isSubmissionEnabled());
            response.put("stats", promptSubmissionService.getSubmissionStats());
            response.put("pendingCount", promptSubmissionService.getPendingSubmissionCount());
            response.put("health", promptSubmissionService.healthCheck());
            response.put("config", promptSubmissionService.getSubmissionConfigInfo());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取提交服务状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取提交服务状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取轮询服务状态
     * 
     * @return 轮询服务状态信息
     */
    @GetMapping("/polling/status")
    public ResponseEntity<Map<String, Object>> getPollingStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "SUCCESS");
            response.put("enabled", promptPollingService.isPollingEnabled());
            response.put("stats", promptPollingService.getPollingStats());
            response.put("promptStats", promptPollingService.getPromptStatusStats());
            response.put("health", promptPollingService.healthCheck());
            response.put("config", promptPollingService.getPollingConfigInfo());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取轮询服务状态失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取轮询服务状态失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 健康检查
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "healthy");
            response.put("service", "Prompt Services Controller");
            response.put("timestamp", System.currentTimeMillis());
            response.put("submissionHealth", promptSubmissionService.healthCheck());
            response.put("pollingHealth", promptPollingService.healthCheck());
            response.put("submissionEnabled", promptSubmissionService.isSubmissionEnabled());
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
