package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.service.PromptSubmissionService;
import com.example.medaiassistant.service.PromptPollingService;
import com.example.medaiassistant.service.PromptMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 自动执行控制器
 * 提供自动执行Prompt任务的端点，支持定时轮询和处理未执行的Prompt任务
 * 
 * 主要功能：
 * 1. 启动自动执行：POST /api/prompts/auto-execute/start
 * 2. 停止自动执行：POST /api/prompts/auto-execute/stop  
 * 3. 获取执行状态：GET /api/prompts/auto-execute/status
 * 4. 健康检查：GET /api/prompts/auto-execute/health
 * 5. 系统监控：GET /api/prompts/auto-execute/monitoring
 * 
 * 特性：
 * - 配置化参数：执行间隔和最大线程数可通过配置文件调整
 * - 线程安全：使用原子变量管理执行状态，确保多线程环境安全
 * - 错误处理：完善的异常捕获和日志记录机制
 * - 资源管理：正确关闭定时任务执行器，避免资源泄漏
 * - 服务分离：使用独立的提交、轮询和监控服务
 * 
 * @author MedAI Assistant Team
 * @version 2.0.0
 * @since 2025-09-30
 */
@RestController
@RequestMapping("/api/prompts/auto-execute")
public class AutoExecuteController {
    private static final Logger logger = LoggerFactory.getLogger(AutoExecuteController.class);

    private final RestTemplate restTemplate;
    private final SchedulingProperties schedulingProperties;
    private final PromptSubmissionService promptSubmissionService;
    private final PromptPollingService promptPollingService;
    private final PromptMonitoringService promptMonitoringService;

    /**
     * 自动执行状态标志，用于控制自动执行任务的启停
     * 
     * @since 2025-09-16
     */
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    
    /**
     * 当前正在处理的Prompt ID，用于状态查询和监控
     * 
     * @since 2025-09-16
     */
    private final AtomicInteger currentPromptId = new AtomicInteger(0);
    
    /**
     * 定时任务调度器，用于执行自动执行任务
     * 
     * @since 2025-09-16
     */
    private ScheduledExecutorService scheduler;

    /**
     * 构造函数，依赖注入必要的服务实例
     * 
     * @param restTemplate HTTP客户端，用于调用内部API
     * @param schedulingProperties 定时任务配置属性
     * @param promptSubmissionService Prompt提交服务
     * @param promptPollingService Prompt轮询服务
     * @param promptMonitoringService Prompt监控服务
     * @since 2025-09-30
     */
    public AutoExecuteController(RestTemplate restTemplate, 
                                SchedulingProperties schedulingProperties,
                                PromptSubmissionService promptSubmissionService,
                                PromptPollingService promptPollingService,
                                PromptMonitoringService promptMonitoringService) {
        this.restTemplate = restTemplate;
        this.schedulingProperties = schedulingProperties;
        this.promptSubmissionService = promptSubmissionService;
        this.promptPollingService = promptPollingService;
        this.promptMonitoringService = promptMonitoringService;
    }

    /**
     * 启动自动执行
     * 
     * @param maxThreads 最大线程数（可选，默认使用配置值）
     * @return 启动结果响应
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAutoExecute(
            @RequestParam(value = "maxThreads", required = false) Integer maxThreads) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (isExecuting.get()) {
            response.put("status", "ALREADY_RUNNING");
            response.put("message", "自动执行已经在运行中");
            return ResponseEntity.ok(response);
        }

        try {
            // 使用配置的线程数或请求参数
            int threads = maxThreads != null ? maxThreads : schedulingProperties.getAutoExecute().getMaxThreads();
            
            // 启动提交服务
            startSubmissionService();
            
            // 启动轮询服务
            promptPollingService.enablePolling();
            
            // 启动定时任务
            startScheduledExecution(threads);
            
            response.put("status", "STARTED");
            response.put("message", "自动执行已启动，提交和轮询已启动，线程数: " + threads);
            response.put("maxThreads", threads);
            response.put("interval", schedulingProperties.getAutoExecute().getInterval());
            response.put("submissionConfig", promptSubmissionService.getSubmissionConfigInfo());
            response.put("pollingConfig", promptPollingService.getPollingConfigInfo());
            response.put("monitoringConfig", promptMonitoringService.getMonitoringConfigInfo());
            
            logger.info("自动执行已启动，提交和轮询已启动，线程数: {}, 间隔: {}ms", threads, schedulingProperties.getAutoExecute().getInterval());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("启动自动执行失败", e);
            response.put("status", "ERROR");
            response.put("message", "启动失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 停止自动执行
     * 
     * @return 停止结果响应
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopAutoExecute() {
        Map<String, Object> response = new HashMap<>();
        
        if (!isExecuting.get()) {
            response.put("status", "NOT_RUNNING");
            response.put("message", "自动执行未在运行中");
            return ResponseEntity.ok(response);
        }

        try {
            // 停止轮询服务
            promptPollingService.disablePolling();
            
            // 停止提交服务
            stopSubmissionService();
            
            // 停止定时任务
            stopScheduledExecution();
            
            response.put("status", "STOPPED");
            response.put("message", "自动执行已停止，提交和轮询已禁止");
            response.put("performanceSummary", promptMonitoringService.getPerformanceSummary());
            
            logger.info("自动执行已停止，提交和轮询已禁止");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("停止自动执行失败", e);
            response.put("status", "ERROR");
            response.put("message", "停止失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取自动执行状态
     * 
     * @return 执行状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAutoExecuteStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("isExecuting", isExecuting.get());
        response.put("currentPromptId", currentPromptId.get());
        response.put("interval", schedulingProperties.getAutoExecute().getInterval());
        response.put("maxThreads", schedulingProperties.getAutoExecute().getMaxThreads());
        response.put("pollingEnabled", promptPollingService.isPollingEnabled());
        response.put("submissionStats", promptSubmissionService.getSubmissionStats());
        response.put("pollingStats", promptPollingService.getPollingStats());
        response.put("status", "SUCCESS");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取系统监控信息
     * 
     * @return 系统监控信息
     */
    @GetMapping("/monitoring")
    public ResponseEntity<Map<String, Object>> getMonitoringInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("healthReport", promptMonitoringService.getHealthReport());
            response.put("performanceMetrics", promptMonitoringService.getPerformanceMetrics());
            response.put("submissionHealth", promptSubmissionService.healthCheck());
            response.put("pollingHealth", promptPollingService.healthCheck());
            response.put("monitoringHealth", promptMonitoringService.healthCheck());
            response.put("status", "SUCCESS");
        } catch (Exception e) {
            logger.error("获取监控信息失败", e);
            response.put("status", "ERROR");
            response.put("message", "获取监控信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发轮询
     * 
     * @return 触发结果响应
     */
    @PostMapping("/trigger-polling")
    public ResponseEntity<Map<String, Object>> triggerPolling() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            promptPollingService.triggerPolling();
            response.put("status", "SUCCESS");
            response.put("message", "手动轮询已触发");
            logger.info("手动轮询已触发");
        } catch (Exception e) {
            logger.error("手动触发轮询失败", e);
            response.put("status", "ERROR");
            response.put("message", "触发失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 重置性能计数器
     * 
     * @return 重置结果响应
     */
    @PostMapping("/reset-counters")
    public ResponseEntity<Map<String, Object>> resetCounters() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            promptMonitoringService.resetCounters();
            response.put("status", "SUCCESS");
            response.put("message", "性能计数器已重置");
            logger.info("性能计数器已重置");
        } catch (Exception e) {
            logger.error("重置性能计数器失败", e);
            response.put("status", "ERROR");
            response.put("message", "重置失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 启动定时执行任务
     * 创建定时调度器，定期执行待处理的Prompt任务
     * 
     * @param maxThreads 最大线程数，用于并发处理
     * @since 1.0.0
     */
    private void startScheduledExecution(int maxThreads) {
        isExecuting.set(true);
        currentPromptId.set(0);
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 使用配置的间隔时间
        long interval = schedulingProperties.getAutoExecute().getInterval();
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!isExecuting.get()) {
                return;
            }
            
            try {
                // 检查服务状态，避免在关闭过程中执行任务
                if (scheduler.isShutdown() || scheduler.isTerminated()) {
                    logger.debug("调度器已关闭，跳过当前任务执行");
                    return;
                }
                
                // 调用现有的execute-pending-prompts端点
                String url = "http://localhost:8081/api/prompt-execution/execute-pending-prompts?maxThreads=" + maxThreads;
                
                try {
                    ResponseEntity<String> result = restTemplate.postForEntity(url, null, String.class);
                    
                    if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
                        String responseBody = result.getBody();
                        logger.debug("自动执行结果: {}", responseBody);
                        
                        // 记录提交成功
                        promptMonitoringService.recordSubmissionSuccess();
                        
                        // 解析执行结果，获取实际处理的Prompt ID
                        // 如果没有处理任何Prompt，则设置为0
                        try {
                            // 解析JSON响应获取处理的Prompt ID
                            ObjectMapper objectMapper = new ObjectMapper();
                            Map<String, Object> resultMap = objectMapper.readValue(responseBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            
                            // 检查是否有处理的Prompt
                            Integer processedCount = (Integer) resultMap.get("processedCount");
                            if (processedCount != null && processedCount > 0) {
                                // 如果有处理Prompt，尝试从响应中获取实际的Prompt ID
                                // 这里需要根据实际响应格式调整，假设响应中包含处理的Prompt ID信息
                                if (resultMap.containsKey("promptIds") && resultMap.get("promptIds") instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Integer> promptIds = (List<Integer>) resultMap.get("promptIds");
                                    if (!promptIds.isEmpty()) {
                                        // 返回第一个处理的Prompt ID
                                        currentPromptId.set(promptIds.get(0));
                                    } else {
                                        currentPromptId.set((int) System.currentTimeMillis());
                                    }
                                } else {
                                    // 如果没有明确的Prompt ID信息，使用时间戳作为标识
                                    currentPromptId.set((int) System.currentTimeMillis());
                                }
                            } else {
                                currentPromptId.set(0); // 没有正在执行的Prompt
                            }
                        } catch (Exception e) {
                            logger.warn("解析执行结果失败，使用默认逻辑", e);
                            currentPromptId.set(0); // 解析失败时设置为0
                        }
                    } else {
                        // 执行失败或没有响应体时，设置为0
                        currentPromptId.set(0);
                    }
                } catch (org.springframework.web.client.ResourceAccessException e) {
                    // 网络访问异常，可能是连接超时或连接被拒绝
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("Connection pool shut down")) {
                        logger.warn("检测到连接池已关闭，尝试重新初始化连接池...");
                        if (handleConnectionPoolRecovery()) {
                            logger.info("连接池重新初始化成功，继续执行任务");
                        } else {
                            logger.error("连接池重新初始化失败，停止自动执行任务");
                            isExecuting.set(false);
                            currentPromptId.set(0);
                        }
                        return;
                    } else {
                        logger.warn("自动执行任务网络访问异常: {}", errorMessage);
                        currentPromptId.set(0);
                    }
                } catch (org.springframework.web.client.RestClientException e) {
                    // REST客户端异常
                    logger.warn("自动执行任务REST客户端异常: {}", e.getMessage());
                    currentPromptId.set(0);
                }
                
            } catch (IllegalStateException e) {
                // 连接池关闭的专项处理
                if (e.getMessage() != null && e.getMessage().contains("Connection pool shut down")) {
                    logger.warn("检测到连接池状态异常，尝试重新初始化连接池...");
                    if (handleConnectionPoolRecovery()) {
                        logger.info("连接池重新初始化成功，继续执行任务");
                    } else {
                        logger.error("连接池重新初始化失败，停止自动执行任务");
                        isExecuting.set(false);
                        currentPromptId.set(0);
                    }
                } else {
                    logger.error("自动执行任务发生状态异常", e);
                }
            } catch (Exception e) {
                // 检查是否为连接池相关错误
                String errorMessage = e.getMessage();
                if (errorMessage != null && 
                    (errorMessage.contains("Connection pool shut down") || 
                     errorMessage.contains("Pool is closed") ||
                     errorMessage.contains("HikariPool") && errorMessage.contains("shutdown"))) {
                    logger.warn("检测到连接池相关错误，尝试重新初始化: {}", errorMessage);
                    if (handleConnectionPoolRecovery()) {
                        logger.info("连接池重新初始化成功，继续执行任务");
                    } else {
                        logger.error("连接池重新初始化失败，停止自动执行任务");
                        isExecuting.set(false);
                        currentPromptId.set(0);
                    }
                } else {
                    logger.error("自动执行任务失败", e);
                }
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止定时执行任务
     * 优雅关闭调度器，避免连接池关闭错误
     */
    private void stopScheduledExecution() {
        // 首先设置执行状态为false，阻止新任务执行
        isExecuting.set(false);
        currentPromptId.set(0);
        
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                logger.info("正在停止定时执行任务...");
                
                // 先停止接受新任务
                scheduler.shutdown();
                
                // 等待正在执行的任务完成（最多等待10秒）
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("定时任务未在指定时间内完成，强制关闭");
                    // 强制关闭
                    scheduler.shutdownNow();
                    
                    // 再等待5秒
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("定时任务强制关闭失败");
                    }
                }
                
                logger.info("定时执行任务已停止");
                
            } catch (InterruptedException e) {
                logger.warn("等待任务关闭被中断，强制关闭调度器");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("停止定时任务时发生异常", e);
                // 在异常情况下也要尝试关闭
                if (!scheduler.isShutdown()) {
                    scheduler.shutdownNow();
                }
            } finally {
                scheduler = null;
            }
        }
    }

    /**
     * 停止提交服务
     * 调用提交服务的停止接口
     * 
     * @since 2025-09-30
     */
    private void stopSubmissionService() {
        try {
            String url = "http://localhost:8081/api/prompt-execution/stop-submission";
            ResponseEntity<String> result = restTemplate.postForEntity(url, null, String.class);
            
            if (result.getStatusCode().is2xxSuccessful()) {
                logger.info("提交服务停止成功");
            } else {
                logger.warn("提交服务停止失败，状态码: {}", result.getStatusCode());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 网络访问异常，可能是连接池问题
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Connection pool shut down")) {
                logger.warn("连接池已关闭，尝试恢复后重试停止提交服务");
                if (handleConnectionPoolRecovery()) {
                    // 恢复成功后重试
                    try {
                        String url = "http://localhost:8081/api/prompt-execution/stop-submission";
                        ResponseEntity<String> retryResult = restTemplate.postForEntity(url, null, String.class);
                        if (retryResult.getStatusCode().is2xxSuccessful()) {
                            logger.info("恢复后提交服务停止成功");
                        }
                    } catch (Exception retryException) {
                        logger.warn("恢复后重试停止提交服务失败: {}", retryException.getMessage());
                    }
                } else {
                    logger.warn("连接池恢复失败，跳过提交服务停止操作");
                }
            } else {
                logger.error("调用提交服务停止接口网络异常: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("调用提交服务停止接口失败", e);
        }
    }

    /**
     * 启动提交服务
     * 调用提交服务的启动接口
     * 
     * @since 2025-09-30
     */
    private void startSubmissionService() {
        try {
            String url = "http://localhost:8081/api/prompt-execution/start-submission";
            ResponseEntity<String> result = restTemplate.postForEntity(url, null, String.class);
            
            if (result.getStatusCode().is2xxSuccessful()) {
                logger.info("提交服务启动成功");
            } else {
                logger.warn("提交服务启动失败，状态码: {}", result.getStatusCode());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 网络访问异常，可能是连接池问题
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Connection pool shut down")) {
                logger.warn("连接池已关闭，尝试恢复后重试启动提交服务");
                if (handleConnectionPoolRecovery()) {
                    // 恢复成功后重试
                    try {
                        String url = "http://localhost:8081/api/prompt-execution/start-submission";
                        ResponseEntity<String> retryResult = restTemplate.postForEntity(url, null, String.class);
                        if (retryResult.getStatusCode().is2xxSuccessful()) {
                            logger.info("恢复后提交服务启动成功");
                        }
                    } catch (Exception retryException) {
                        logger.warn("恢复后重试启动提交服务失败: {}", retryException.getMessage());
                    }
                } else {
                    logger.warn("连接池恢复失败，跳过提交服务启动操作");
                }
            } else {
                logger.error("调用提交服务启动接口网络异常: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error("调用提交服务启动接口失败", e);
        }
    }

    /**
     * 处理连接池恢复逻辑
     * 当检测到连接池关闭但自动执行仍在进行时，尝试重新创建连接池
     * 
     * @return true 如果恢复成功，false 如果恢复失败
     */
    private boolean handleConnectionPoolRecovery() {
        try {
            logger.info("开始尝试恢复连接池...");
            
            // 等待一段时间，给连接池一些时间恢复
            Thread.sleep(2000);
            
            // 尝试发送一个简单的测试请求来检查连接是否恢复
            String testUrl = "http://localhost:8081/api/prompts/auto-execute/health";
            ResponseEntity<String> testResult = restTemplate.getForEntity(testUrl, String.class);
            
            if (testResult.getStatusCode().is2xxSuccessful()) {
                logger.info("连接池恢复成功，可以继续执行任务");
                return true;
            } else {
                logger.warn("连接测试失败，状态码: {}", testResult.getStatusCode());
                return false;
            }
            
        } catch (InterruptedException e) {
            logger.warn("连接池恢复等待被中断");
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.warn("连接池恢复尝试失败: {}", e.getMessage());
            
            // 如果是连接池相关错误，再次尝试
            String errorMessage = e.getMessage();
            if (errorMessage != null && 
                (errorMessage.contains("Connection pool shut down") || 
                 errorMessage.contains("Pool is closed") ||
                 errorMessage.contains("HikariPool"))) {
                
                logger.info("仍然是连接池错误，等待系统自动恢复...");
                
                try {
                    // 再等待5秒，给系统更多时间来恢复
                    Thread.sleep(5000);
                    
                    // 再次尝试测试连接
                    String testUrl = "http://localhost:8081/api/prompts/auto-execute/health";
                    ResponseEntity<String> retryResult = restTemplate.getForEntity(testUrl, String.class);
                    
                    if (retryResult.getStatusCode().is2xxSuccessful()) {
                        logger.info("第二次尝试成功，连接池已恢复");
                        return true;
                    } else {
                        logger.error("第二次尝试失败，放弃恢复");
                        return false;
                    }
                    
                } catch (Exception retryException) {
                    logger.error("第二次恢复尝试失败: {}", retryException.getMessage());
                    return false;
                }
            } else {
                // 非连接池错误，直接返回失败
                return false;
            }
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "healthy");
            response.put("service", "Auto Execute Controller");
            response.put("timestamp", System.currentTimeMillis());
            response.put("isExecuting", isExecuting.get());
            response.put("pollingEnabled", promptPollingService.isPollingEnabled());
            response.put("submissionHealth", promptSubmissionService.healthCheck());
            response.put("pollingHealth", promptPollingService.healthCheck());
            response.put("monitoringHealth", promptMonitoringService.healthCheck());
        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
