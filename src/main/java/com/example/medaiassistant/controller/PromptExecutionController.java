package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import com.example.medaiassistant.util.AESEncryptionUtil;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.service.NetworkHealthService;
import com.example.medaiassistant.service.TransactionRetryService;
import com.example.medaiassistant.service.OptimisticLockRetryService;
import com.example.medaiassistant.model.ServerConfiguration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Prompt执行控制器 - 整合多线程和加密传输的提交流程
 * 
 * 该控制器提供一个接口实现两个核心功能：
 * 1. 多线程操作：并发处理多个未执行的Prompt
 * 2. 加密传输：调用未执行的Prompt，加密后传输给执行服务器
 * 
 * 主要特性：
 * - 支持批量处理未执行的Prompt任务
 * - 完整的AES加密流程
 * - 多线程并发提交
 * - 提交后立即返回，不等待处理结果
 * - 完善的错误处理和状态跟踪
 * - 重试机制和状态回滚功能
 * 
 * @author MedAI Assistant Team
 * @version 2.1.0
 * @since 2025-09-14
 */
@RestController
@RequestMapping("/api/prompt-execution")
public class PromptExecutionController {
    private static final Logger logger = LoggerFactory.getLogger(PromptExecutionController.class);

    // Prompt状态常量
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_SUBMISSION_STARTED = "SUBMISSION_STARTED"; // 新增：提交开始状态
    // private static final String STATUS_SUBMIT_FAILED = "提交失败";

    /**
     * 提交服务全局停止标志
     * 当设置为true时，停止所有新的提交操作
     * 
     * @since 2025-09-30
     */
    private volatile boolean submissionStopped = false;

    private final RestTemplate restTemplate;
    private final ServerConfigService serverConfigService;
    private final PromptRepository promptRepository;
    private final EncryptedDataTempRepository encryptedDataTempRepository;
    
    // 迭代1改进：集成网络健康监控服务
    private final NetworkHealthService networkHealthService;
    
    // 迭代2改进：集成数据库健壮性服务
    // private final DatabaseHealthService databaseHealthService; // 暂时未使用，注释掉
    private final TransactionRetryService transactionRetryService;
    private final OptimisticLockRetryService optimisticLockRetryService;

    // 线程池用于多线程处理
    private ExecutorService executorService;

    /**
     * 构造函数 - 初始化Prompt执行控制器
     * 
     * 该构造函数负责注入所有必需的依赖项，包括：
     * - RestTemplate: 用于HTTP请求调用执行服务器
     * - ServerConfigService: 用于获取服务器配置信息
     * - PromptRepository: 用于操作Prompt表数据
     * - EncryptedDataTempRepository: 用于检查ENCRYPTED_DATA_TEMP表状态
     * - NetworkHealthService: 网络健康监控服务（迭代1）
     * - DatabaseHealthService: 数据库健康监控服务（迭代2）
     * - TransactionRetryService: 事务重试服务（迭代2）
     * - OptimisticLockRetryService: 乐观锁重试服务（迭代2）
     * 
     * @param restTemplate                用于HTTP请求的RestTemplate实例
     * @param serverConfigService         服务器配置服务实例
     * @param promptRepository            Prompt表数据访问实例
     * @param encryptedDataTempRepository 加密数据临时表数据访问实例
     * @param networkHealthService        网络健康监控服务实例
     * @param databaseHealthService       数据库健康监控服务实例
     * @param transactionRetryService     事务重试服务实例
     * @param optimisticLockRetryService  乐观锁重试服务实例
     * @since 2025-09-29
     * @version 2.2.0
     */
    public PromptExecutionController(RestTemplate restTemplate,
            ServerConfigService serverConfigService,
            PromptRepository promptRepository,
            EncryptedDataTempRepository encryptedDataTempRepository,
            NetworkHealthService networkHealthService,
            TransactionRetryService transactionRetryService,
            OptimisticLockRetryService optimisticLockRetryService) {
        this.restTemplate = restTemplate;
        this.serverConfigService = serverConfigService;
        this.promptRepository = promptRepository;
        this.encryptedDataTempRepository = encryptedDataTempRepository;
        this.networkHealthService = networkHealthService;
        this.transactionRetryService = transactionRetryService;
        this.optimisticLockRetryService = optimisticLockRetryService;
    }

    /**
     * 执行未处理的Prompt任务（只提交不等待）
     * 
     * 该接口实现：
     * 1. 多线程操作：使用线程池并发提交多个Prompt
     * 2. 加密传输：加密Prompt内容并发送到执行服务器
     * 3. 立即返回：提交后立即返回，不等待处理结果
     * 4. 状态管理：自动更新Prompt状态并提供重试机制
     * 5. 重复发送保护：排除已经在ENCRYPTED_DATA_TEMP表中状态为SENT的Prompt
     * 
     * @param maxThreads 最大线程数（可选，默认5）
     * @return 提交状态响应
     */
    @PostMapping("/execute-pending-prompts")
    public ResponseEntity<Map<String, Object>> executePendingPrompts(
            @RequestParam(value = "maxThreads", defaultValue = "5") int maxThreads) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 检查提交服务是否已停止
            if (submissionStopped) {
                response.put("status", "SUBMISSION_STOPPED");
                response.put("message", "提交服务已停止，无法执行新的提交操作");
                logger.info("提交服务已停止，拒绝执行新的提交操作");
                return ResponseEntity.ok(response);
            }
            // 1. 查询未执行的Prompt
            List<Prompt> pendingPrompts = promptRepository.findByStatusName(STATUS_PENDING);

            if (pendingPrompts.isEmpty()) {
                response.put("status", "NO_PENDING_PROMPTS");
                response.put("message", "没有待处理的Prompt任务");
                return ResponseEntity.ok(response);
            }

            // 过滤掉已经在ENCRYPTED_DATA_TEMP表中状态为SENT的Prompt
            List<Prompt> filteredPrompts = filterAlreadySentPrompts(pendingPrompts);

            if (filteredPrompts.isEmpty()) {
                response.put("status", "NO_VALID_PENDING_PROMPTS");
                response.put("message", "所有待处理的Prompt任务已经在ENCRYPTED_DATA_TEMP表中处理完成");
                response.put("originalCount", pendingPrompts.size());
                response.put("filteredCount", 0);
                return ResponseEntity.ok(response);
            }

            logger.info("找到 {} 个待处理的Prompt任务，过滤后剩余 {} 个有效任务",
                    pendingPrompts.size(), filteredPrompts.size());

            // 2. 初始化线程池
            executorService = Executors.newFixedThreadPool(maxThreads);

            // 3. 多线程处理每个Prompt
            for (Prompt prompt : pendingPrompts) {
                executorService.submit(() -> {
                    try {
                        processSinglePrompt(prompt);
                    } catch (Exception e) {
                        logger.error("处理Prompt失败, ID: {}", prompt.getPromptId(), e);
                    }
                });
            }

            // 4. 立即返回，不等待任务完成
            response.put("status", "SUBMISSION_STARTED");
            response.put("submittedCount", pendingPrompts.size());
            response.put("message", "Prompt任务已开始提交处理，请通过轮询机制检查处理结果");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("执行未处理Prompt任务失败", e);
            response.put("status", "ERROR");
            response.put("message", "执行失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 处理单个Prompt任务
     * 负责加密、状态更新和异步提交，不处理回调
     * 修复状态与数据不一致问题：先发送成功再更新状态
     */
    private void processSinglePrompt(Prompt prompt) {
        try {
            // 检查提交服务是否已停止
            if (submissionStopped) {
                logger.info("提交服务已停止，跳过Prompt ID: {} 的处理", prompt.getPromptId());
                return;
            }
            logger.info("开始提交Prompt, ID: {}", prompt.getPromptId());

            // 1. 组合Prompt内容
            String combinedPrompt = combinePromptContent(prompt);
            logger.debug("组合后的Prompt内容长度: {} 字符", combinedPrompt.length());

            // 2. 加密Prompt内容
            String encryptedData = encryptPrompt(combinedPrompt);
            logger.info("Prompt加密完成, 加密后长度: {} 字符", encryptedData.length());

            // 3. 迭代2改进：使用数据库重试机制更新状态为"SUBMISSION_STARTED"
            updatePromptStatusWithDatabaseRetry(prompt, STATUS_SUBMISSION_STARTED, "开始提交Prompt");
            logger.info("Prompt状态已更新为SUBMISSION_STARTED, ID: {}", prompt.getPromptId());

            // 4. 同步发送到执行服务器（带重试），确保发送成功后再更新最终状态
            sendToExecutionServerWithStatusUpdate(encryptedData, prompt.getPromptId(), 3);

        } catch (Exception e) {
            logger.error("提交Prompt失败, ID: {}", prompt.getPromptId(), e);
            // 迭代2改进：使用数据库重试机制回滚状态
            rollbackPromptStatusWithDatabaseRetry(prompt.getPromptId(), "加密或初始状态更新失败: " + e.getMessage());
        }
    }

    /**
     * 带重试机制和状态更新的同步发送到执行服务器
     * 解决状态与数据不一致问题：只有发送成功后才更新为"已提交"状态
     * 迭代1改进：增强网络传输可靠性
     * 
     * @param encryptedData 加密的Prompt数据内容
     * @param promptId      Prompt的唯一标识符
     * @param maxRetries    最大重试次数
     */
    private void sendToExecutionServerWithStatusUpdate(String encryptedData, Integer promptId, int maxRetries) {
        boolean sendSuccess = false;
        Exception lastException = null;
        long startTime = System.currentTimeMillis();
        
        // 迭代1改进：动态重试策略
        int dynamicMaxRetries = calculateDynamicRetries(maxRetries);
        
        for (int attempt = 1; attempt <= dynamicMaxRetries; attempt++) {
            try {
                // 迭代1改进：网络状态检查
                if (!isNetworkHealthy()) {
                    logger.warn("网络状态异常，等待网络恢复...");
                    waitForNetworkRecovery(attempt);
                }
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // 迭代1改进：添加连接保持和超时控制
                headers.set("Connection", "keep-alive");
                headers.set("User-Agent", "MedAI-Assistant/1.0");

                Map<String, String> request = new HashMap<>();
                request.put("encryptedPrompt", encryptedData);
                request.put("encryptionType", "AES-256-CBC");
                request.put("timestamp", String.valueOf(System.currentTimeMillis()));
                request.put("requestId", "cdwyy" + promptId);
                request.put("retryAttempt", String.valueOf(attempt)); // 迭代1改进：添加重试标识

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

                String executionServerUrl = buildExecutionServerUrl();
                logger.info("第{}/{}次尝试发送到执行服务器: {}, Prompt ID: {}", 
                          attempt, dynamicMaxRetries, executionServerUrl, promptId);

                // 迭代1改进：记录发送开始时间
                long sendStartTime = System.currentTimeMillis();
                restTemplate.exchange(executionServerUrl, HttpMethod.POST, entity, String.class);
                long sendDuration = System.currentTimeMillis() - sendStartTime;
                
                logger.info("第{}次发送成功, Prompt ID: {}, 耗时: {}ms", attempt, promptId, sendDuration);
                sendSuccess = true;
                // 迭代1改进：记录成功指标
                recordNetworkMetrics(promptId, attempt, sendDuration, true, null);
                break; // 发送成功，退出循环
                
            } catch (ResourceAccessException e) {
                // 迭代1改进：网络传输异常处理
                lastException = e;
                logger.warn("网络传输异常，第{}/{次重试, Prompt ID: {}, 错误: {}", 
                          attempt, dynamicMaxRetries, promptId, e.getMessage());
                recordNetworkMetrics(promptId, attempt, 0, false, e.getClass().getSimpleName());
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("第{}/{次发送失败, Prompt ID: {}, 错误: {}", 
                          attempt, dynamicMaxRetries, promptId, e.getMessage());
                recordNetworkMetrics(promptId, attempt, 0, false, e.getClass().getSimpleName());
            }
            
            if (attempt < dynamicMaxRetries) {
                // 迭代1改进：智能退避策略
                long delayMs = calculateBackoffDelay(attempt, lastException);
                logger.info("等待{}ms后进行第{}次重试, Prompt ID: {}", delayMs, attempt + 1, promptId);
                safeSleep(delayMs);
            }
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("Prompt ID: {} 发送操作完成，总耗时: {}ms, 成功: {}", promptId, totalDuration, sendSuccess);
        
        if (sendSuccess) {
            // 发送成功，迭代2改进：使用数据库重试机制更新状态为"已提交"
            try {
                Optional<Prompt> promptOptional = promptRepository.findById(promptId);
                if (promptOptional.isPresent()) {
                    Prompt prompt = promptOptional.get();
                    updatePromptStatusWithDatabaseRetry(prompt, STATUS_SUBMITTED, "发送成功后更新状态");
                    logger.info("Prompt ID: {} 发送成功，状态已更新为'已提交'", promptId);
                }
            } catch (Exception e) {
                logger.error("更新Prompt状态为'已提交'失败, ID: {}", promptId, e);
            }
        } else {
            // 发送失败，迭代2改进：使用数据库重试机制回滚状态
            String errorMessage = "发送失败，达到最大重试次数: " + (lastException != null ? lastException.getMessage() : "未知错误");
            rollbackPromptStatusWithDatabaseRetry(promptId, errorMessage);
        }
    }
    
    /**
     * 迭代1改进：计算动态重试次数
     * 根据系统负载和网络状况动态调整重试次数
     */
    private int calculateDynamicRetries(int baseRetries) {
        // 使用网络健康服务的建议重试次数
        return networkHealthService.getRecommendedRetries();
    }
    
    /**
     * 迭代1改进：网络健康状态检查
     */
    private boolean isNetworkHealthy() {
        return networkHealthService.isNetworkHealthy();
    }
    
    /**
     * 迭代1改进：网络恢复等待机制
     */
    private void waitForNetworkRecovery(int attempt) {
        int maxWaitTime = Math.min(5000 * attempt, 30000); // 最大等待30秒
        int checkInterval = 1000; // 每秒检查一次
        
        for (int i = 0; i < maxWaitTime; i += checkInterval) {
            safeSleep(checkInterval);
            if (isNetworkHealthy()) {
                logger.info("网络已恢复，继续发送操作");
                return;
            }
        }
        logger.warn("网络恢复等待超时，继续尝试发送");
    }
    
    /**
     * 迭代1改进：智能退避延迟计算
     */
    private long calculateBackoffDelay(int attempt, Exception lastException) {
        long baseDelay = 1000; // 基础延迟1秒
        
        // 根据异常类型调整延迟
        if (lastException instanceof ResourceAccessException) {
            // 网络超时，使用较长延迟
            baseDelay = 2000;
        }
        
        // 指数退避，但加入随机因子避免雷群效应
        long exponentialDelay = (long) (baseDelay * Math.pow(1.5, attempt - 1));
        long jitter = (long) (exponentialDelay * 0.1 * Math.random());
        
        return Math.min(exponentialDelay + jitter, 15000); // 最大延迟15秒
    }
    
    /**
     * 迭代1改进：安全的线程休眠
     */
    private void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("线程休眠被中断");
        }
    }
    
    /**
     * 迭代1改进：网络传输指标记录
     */
    private void recordNetworkMetrics(Integer promptId, int attempt, long duration, boolean success, String errorType) {
        // 集成网络健康服务
        networkHealthService.recordNetworkResult(success, duration);
        
        // 详细日志记录
        String logMessage = String.format(
            "网络传输指标 - Prompt ID: %d, 尝试次数: %d, 耗时: %dms, 成功: %s, 错误类型: %s",
            promptId, attempt, duration, success, errorType != null ? errorType : "无"
        );
        
        if (success) {
            logger.info(logMessage);
        } else {
            logger.warn(logMessage);
        }
    }

    /**
     * 构建执行服务器URL
     * 
     * @return 执行服务器URL
     */
    private String buildExecutionServerUrl() {
        String executionServerIp = serverConfigService.getDecryptionServerIp();
        if (executionServerIp != null && !executionServerIp.isEmpty()) {
            return "http://" + executionServerIp + ":8082/api/execute/encrypted-prompt";
        } else {
            return "http://localhost:8082/api/execute/encrypted-prompt";
        }
    }

    /**
     * 组合Prompt内容
     */
    private String combinePromptContent(Prompt prompt) {
        return prompt.getObjectiveContent() + "\n" +
                prompt.getDailyRecords() + "\n" +
                prompt.getPromptTemplateContent();
    }

    /**
     * 加密Prompt内容
     */
    private String encryptPrompt(String plainPrompt) throws Exception {
        ServerConfiguration encryptionKeyConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_KEY");
        ServerConfiguration encryptionSaltConfig = serverConfigService.getConfigByName("AES_ENCRYPTION_SALT");

        if (encryptionKeyConfig == null || encryptionSaltConfig == null) {
            throw new RuntimeException("AES加密配置未找到");
        }

        String encryptionKey = encryptionKeyConfig.getConfigData();
        String encryptionSalt = encryptionSaltConfig.getConfigData();

        return AESEncryptionUtil.encrypt(plainPrompt, encryptionKey, encryptionSalt);
    }

    /**
     * 获取当前待处理的Prompt数量
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Object>> getPendingPromptCount() {
        Map<String, Object> response = new HashMap<>();
        long count = promptRepository.countByStatusName(STATUS_PENDING);

        response.put("pendingCount", count);
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * 诊断和修复状态不一致问题
     * 查找状态为"已提交"但ENCRYPTED_DATA_TEMP表中无记录的Prompt
     */
    @GetMapping("/diagnose-inconsistent-status")
    public ResponseEntity<Map<String, Object>> diagnoseInconsistentStatus() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> inconsistentPrompts = new ArrayList<>();
        int fixedCount = 0;
        
        try {
            // 查找所有状态为"已提交"的Prompt
            List<Prompt> submittedPrompts = promptRepository.findByStatusName(STATUS_SUBMITTED);
            logger.info("找到 {} 个状态为'已提交'的Prompt", submittedPrompts.size());
            
            for (Prompt prompt : submittedPrompts) {
                Integer promptId = prompt.getPromptId();
                String requestId = "cdwyy" + promptId;
                
                // 检查ENCRYPTED_DATA_TEMP表中是否存在对应记录
                Optional<EncryptedDataTemp> encryptedDataOptional = encryptedDataTempRepository.findByRequestId(requestId);
                
                if (!encryptedDataOptional.isPresent()) {
                    // 数据不一致：状态为"已提交"但无ENCRYPTED_DATA_TEMP记录
                    Map<String, Object> inconsistentPrompt = new HashMap<>();
                    inconsistentPrompt.put("promptId", promptId);
                    inconsistentPrompt.put("statusName", prompt.getStatusName());
                    inconsistentPrompt.put("submissionTime", prompt.getSubmissionTime());
                    inconsistentPrompt.put("problem", "状态为'已提交'但ENCRYPTED_DATA_TEMP表中无记录");
                    
                    // 自动修复：回滚到待处理状态
                    try {
                        prompt.setStatusName(STATUS_PENDING);
                        promptRepository.save(prompt);
                        inconsistentPrompt.put("fixed", true);
                        inconsistentPrompt.put("action", "已回滚到待处理状态");
                        fixedCount++;
                        logger.info("已修复Prompt ID: {} 的状态不一致问题", promptId);
                    } catch (Exception e) {
                        inconsistentPrompt.put("fixed", false);
                        inconsistentPrompt.put("error", e.getMessage());
                        logger.error("修复Prompt ID: {} 失败: {}", promptId, e.getMessage());
                    }
                    
                    inconsistentPrompts.add(inconsistentPrompt);
                }
            }
            
            response.put("status", "SUCCESS");
            response.put("totalSubmittedPrompts", submittedPrompts.size());
            response.put("inconsistentCount", inconsistentPrompts.size());
            response.put("fixedCount", fixedCount);
            response.put("inconsistentPrompts", inconsistentPrompts);
            response.put("message", String.format("检查完成：找到 %d 个不一致的Prompt，已修复 %d 个", 
                    inconsistentPrompts.size(), fixedCount));
            
            logger.info("状态不一致诊断完成 - 总数: {}, 不一致: {}, 已修复: {}", 
                    submittedPrompts.size(), inconsistentPrompts.size(), fixedCount);
            
        } catch (Exception e) {
            logger.error("诊断状态不一致问题时发生异常", e);
            response.put("status", "ERROR");
            response.put("message", "诊断失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 处理已经在ENCRYPTED_DATA_TEMP表中状态为SENT的Prompt
     * 根据用户要求：如果记录已经在ENCRYPTED_DATA_TEMP表中存在且状态为SENT，则取回该记录
     * 
     * 修复数据不一致问题：
     * 1. 检查所有可能的状态，而不仅仅是SENT状态
     * 2. 处理ENCRYPTED状态的数据，避免数据丢失
     * 3. 增强错误处理和日志记录
     * 
     * @param pendingPrompts 待处理的Prompt列表
     * @return 处理后的Prompt列表（包含需要发送的记录）
     */
    private List<Prompt> filterAlreadySentPrompts(List<Prompt> pendingPrompts) {
        List<Prompt> processedPrompts = new java.util.ArrayList<>();
        int alreadySentCount = 0;
        int encryptedCount = 0;
        int recoveredCount = 0;
        int errorCount = 0;

        for (Prompt prompt : pendingPrompts) {
            Integer promptId = prompt.getPromptId();
            String requestId = "cdwyy" + promptId;

            // 查询ENCRYPTED_DATA_TEMP表中对应的记录
            Optional<EncryptedDataTemp> encryptedDataOptional = encryptedDataTempRepository.findByRequestId(requestId);

            if (encryptedDataOptional.isPresent()) {
                EncryptedDataTemp encryptedData = encryptedDataOptional.get();
                DataStatus currentStatus = encryptedData.getStatus();

                // 如果状态为SENT，说明已经处理完成，需要取回该记录
                if (currentStatus == DataStatus.SENT) {
                    logger.info("Prompt ID: {} 已经在ENCRYPTED_DATA_TEMP表中处理完成，状态为SENT，取回该记录", promptId);
                    alreadySentCount++;

                    // 取回记录：更新Prompt状态为"已完成"
                    try {
                        prompt.setStatusName("已完成");
                        promptRepository.save(prompt);
                        recoveredCount++;
                        logger.info("Prompt ID: {} 状态已更新为'已完成'", promptId);
                    } catch (Exception e) {
                        logger.error("更新Prompt ID: {} 状态失败: {}", promptId, e.getMessage());
                        errorCount++;
                    }
                    continue;
                }
                
                // 如果状态为ENCRYPTED，说明执行服务器已经处理完成，但轮询服务尚未处理
                if (currentStatus == DataStatus.ENCRYPTED) {
                    logger.info("Prompt ID: {} 在ENCRYPTED_DATA_TEMP表中状态为ENCRYPTED，等待轮询服务处理", promptId);
                    encryptedCount++;
                    // 保留该记录，让轮询服务继续处理
                    processedPrompts.add(prompt);
                    continue;
                }
                
                // 如果状态为ERROR，记录错误信息
                if (currentStatus == DataStatus.ERROR) {
                    logger.warn("Prompt ID: {} 在ENCRYPTED_DATA_TEMP表中状态为ERROR，需要人工干预", promptId);
                    // 保留该记录，可能需要重新发送
                    processedPrompts.add(prompt);
                    continue;
                }
                
                // 其他状态（RECEIVED, DECRYPTED, PROCESSING, PROCESSED）表示数据正在处理中
                logger.info("Prompt ID: {} 在ENCRYPTED_DATA_TEMP表中状态为{}，数据正在处理中", promptId, currentStatus);
                // 保留该记录，继续等待处理
                processedPrompts.add(prompt);
                continue;
            }

            // 如果记录不存在，则保留该记录用于发送
            processedPrompts.add(prompt);
        }

        if (alreadySentCount > 0 || encryptedCount > 0 || errorCount > 0) {
            logger.info("处理完成：总共 {} 个待处理Prompt，其中 {} 个已发送（已取回 {} 个），{} 个已加密，{} 个错误，剩余 {} 个需要发送",
                    pendingPrompts.size(), alreadySentCount, recoveredCount, encryptedCount, errorCount, processedPrompts.size());
        }

        return processedPrompts;
    }

    /**
     * 停止提交服务
     * 禁止所有新的提交操作，已经在处理中的任务将继续完成
     * 
     * @return 停止结果响应
     * @since 2025-09-30
     */
    @PostMapping("/stop-submission")
    public ResponseEntity<Map<String, Object>> stopSubmission() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            submissionStopped = true;
            
            // 停止线程池（如果存在）
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                        logger.warn("强制终止提交服务线程池");
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                    logger.error("终止提交服务线程池被中断", e);
                }
            }
            
            response.put("status", "STOPPED");
            response.put("message", "提交服务已停止");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("提交服务已停止");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("停止提交服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "停止失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 启动提交服务
     * 重新启用提交服务，允许执行新的提交操作
     * 
     * @return 启动结果响应
     * @since 2025-09-30
     */
    @PostMapping("/start-submission")
    public ResponseEntity<Map<String, Object>> startSubmission() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            submissionStopped = false;
            
            response.put("status", "STARTED");
            response.put("message", "提交服务已启动");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("提交服务已重新启动");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("启动提交服务失败", e);
            response.put("status", "ERROR");
            response.put("message", "启动失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取提交服务状态
     * 
     * @return 提交服务状态信息
     * @since 2025-09-30
     */
    @GetMapping("/submission-status")
    public ResponseEntity<Map<String, Object>> getSubmissionStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("submissionStopped", submissionStopped);
        response.put("status", submissionStopped ? "STOPPED" : "RUNNING");
        response.put("message", submissionStopped ? "提交服务已停止" : "提交服务正在运行");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // =========================== 迭代2：数据库健壮性增强相关方法 ===========================
    
    /**
     * 使用数据库重试机制更新Prompt状态
     * 迭代2改进：集成事务重试和乐观锁机制
     * 
     * @param prompt 需要更新的Prompt对象
     * @param newStatus 新状态
     * @param operationName 操作名称（用于日志）
     */
    private void updatePromptStatusWithDatabaseRetry(Prompt prompt, String newStatus, String operationName) {
        try {
            // 使用事务重试服务进行状态更新
            transactionRetryService.executeWithRetry(() -> {
                // 使用乐观锁重试服务确保并发安全
                    optimisticLockRetryService.executeWithOptimisticLockRetry(
                        () -> {
                            prompt.setStatusName(newStatus);
                            promptRepository.save(prompt);
                            return null;
                        },
                        () -> {
                            // 刷新实体获取最新版本
                            Optional<Prompt> refreshedPrompt = promptRepository.findById(prompt.getPromptId());
                            if (refreshedPrompt.isPresent()) {
                                // 获取最新实体，但不使用变量
                                refreshedPrompt.get();
                            }
                            return null;
                        },
                        operationName + "[更新Prompt状态]"
                    );
                return null;
            }, operationName);
            
            logger.info("{}成功，Prompt ID: {}, 新状态: {}", operationName, prompt.getPromptId(), newStatus);
            
        } catch (Exception e) {
            logger.error("{}失败，Prompt ID: {}, 目标状态: {}, 错误: {}", 
                        operationName, prompt.getPromptId(), newStatus, e.getMessage());
            throw new RuntimeException(operationName + "失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用数据库重试机制回滚状态
     * 迭代2改进：增强错误恢复能力
     * 
     * @param promptId Prompt ID
     * @param errorMessage 错误信息
     */
    private void rollbackPromptStatusWithDatabaseRetry(Integer promptId, String errorMessage) {
        try {
            transactionRetryService.executeWithRetry(() -> {
                Optional<Prompt> promptOptional = promptRepository.findById(promptId);
                if (promptOptional.isPresent()) {
                    Prompt prompt = promptOptional.get();
                    
                    // 使用乐观锁重试确保并发安全
                    optimisticLockRetryService.executeWithOptimisticLockRetry(
                        () -> {
                            prompt.setStatusName(STATUS_PENDING); // 回滚到待处理状态
                            promptRepository.save(prompt);
                            return null;
                        },
                        () -> {
                            // 刷新实体获取最新版本
                            Optional<Prompt> refreshed = promptRepository.findById(promptId);
                            if (refreshed.isPresent()) {
                                // 获取最新实体，但不使用变量
                                refreshed.get();
                            }
                            return null;
                        },
                        "回滚 Prompt状态[" + promptId + "]"
                    );
                }
                return null;
            }, "回滚 Prompt状态");
            
            logger.warn("Prompt ID: {} 状态已回滚到'待处理', 原因: {}", promptId, errorMessage);
            
        } catch (Exception e) {
            logger.error("回滚 Prompt状态失败, ID: {}, 错误: {}", promptId, e.getMessage());
        }
    }
    
}
