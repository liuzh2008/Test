package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 未提交Prompt增强处理服务
 * 提供详细的乐观锁、网络延迟、数据库异常等错误处理和故障恢复逻辑
 * 
 * 主要功能：
 * 1. 智能错误分类和处理策略选择
 * 2. 乐观锁冲突的精确重试机制
 * 3. 网络延迟和超时的自适应处理
 * 4. 数据库异常的自动恢复
 * 5. 状态一致性保障和自动修复
 * 6. 熔断器模式防止系统过载
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-10-01
 */
@Service
public class UnsubmittedPromptEnhancedProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(UnsubmittedPromptEnhancedProcessor.class);
    
    // 状态常量 - 遵循Prompt状态流转规范
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SUBMISSION_STARTED = "SUBMISSION_STARTED";
    private static final String STATUS_SUBMITTED = "已提交";
    private static final String STATUS_COMPLETED = "已完成";
    private static final String STATUS_FAILED = "执行失败";
    
    @Autowired
    private PromptRepository promptRepository;
    
    @Autowired
    private EncryptedDataTempRepository encryptedDataTempRepository;
    
    @Autowired
    private OptimisticLockRetryService optimisticLockRetryService;
    
    @Autowired
    private TransactionRetryService transactionRetryService;
    
    @Autowired
    private NetworkHealthService networkHealthService;
    
    @Autowired
    private DatabaseHealthService databaseHealthService;
    
    // 处理统计
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong successfulProcessed = new AtomicLong(0);
    private final AtomicLong failedProcessed = new AtomicLong(0);
    private final Map<ErrorCategory, AtomicInteger> errorCategoryCounters = new ConcurrentHashMap<>();
    
    // 处理历史记录
    private final List<ProcessingRecord> processingHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 500;
    
    // 熔断器管理
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();
    
    /**
     * 错误分类枚举
     */
    public enum ErrorCategory {
        OPTIMISTIC_LOCK_CONFLICT,     // 乐观锁冲突
        NETWORK_TIMEOUT,              // 网络超时
        NETWORK_CONNECTION_FAILED,    // 网络连接失败
        DATABASE_DEADLOCK,            // 数据库死锁
        DATABASE_TIMEOUT,             // 数据库超时
        DATABASE_CONNECTION_POOL,     // 连接池问题
        SERVER_OVERLOAD,              // 服务器过载
        VALIDATION_ERROR,             // 数据验证错误
        STATE_INCONSISTENCY,          // 状态不一致
        RESOURCE_EXHAUSTION,          // 资源耗尽
        CIRCUIT_BREAKER_OPEN,         // 熔断器开启
        UNKNOWN_ERROR                 // 未知错误
    }
    
    /**
     * 恢复策略枚举
     */
    public enum RecoveryStrategy {
        OPTIMISTIC_LOCK_RETRY,        // 乐观锁重试
        EXPONENTIAL_BACKOFF,          // 指数退避
        LINEAR_BACKOFF,               // 线性退避
        IMMEDIATE_RETRY,              // 立即重试
        DELAYED_RETRY,                // 延迟重试
        CIRCUIT_BREAKER_FALLBACK,     // 熔断器降级
        NETWORK_RECOVERY,             // 网络恢复
        DATABASE_RECOVERY,            // 数据库恢复
        STATE_RECONCILIATION,         // 状态协调
        MANUAL_INTERVENTION           // 人工干预
    }
    
    /**
     * 处理记录
     */
    public static class ProcessingRecord {
        private final LocalDateTime timestamp;
        private final Integer promptId;
        private final String originalStatus;
        private final ErrorCategory errorCategory;
        private final RecoveryStrategy strategy;
        private final boolean recoverySuccessful;
        private final long processingTimeMs;
        private final int attemptCount;
        private final String errorDetails;
        
        public ProcessingRecord(Integer promptId, String originalStatus, ErrorCategory errorCategory,
                              RecoveryStrategy strategy, boolean recoverySuccessful, 
                              long processingTimeMs, int attemptCount, String errorDetails) {
            this.timestamp = LocalDateTime.now();
            this.promptId = promptId;
            this.originalStatus = originalStatus;
            this.errorCategory = errorCategory;
            this.strategy = strategy;
            this.recoverySuccessful = recoverySuccessful;
            this.processingTimeMs = processingTimeMs;
            this.attemptCount = attemptCount;
            this.errorDetails = errorDetails;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public Integer getPromptId() { return promptId; }
        public String getOriginalStatus() { return originalStatus; }
        public ErrorCategory getErrorCategory() { return errorCategory; }
        public RecoveryStrategy getStrategy() { return strategy; }
        public boolean isRecoverySuccessful() { return recoverySuccessful; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public int getAttemptCount() { return attemptCount; }
        public String getErrorDetails() { return errorDetails; }
    }
    
    /**
     * 熔断器状态
     */
    public static class CircuitBreakerState {
        private volatile boolean open = false;
        private volatile long lastFailureTime = 0;
        private volatile int consecutiveFailures = 0;
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger successfulRequests = new AtomicInteger(0);
        private final int failureThreshold;
        private final long timeoutMs;
        
        public CircuitBreakerState(int failureThreshold, long timeoutMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
        }
        
        public boolean shouldAllowRequest() {
            if (!open) return true;
            return System.currentTimeMillis() - lastFailureTime > timeoutMs;
        }
        
        public void recordSuccess() {
            consecutiveFailures = 0;
            open = false;
            successfulRequests.incrementAndGet();
            totalRequests.incrementAndGet();
        }
        
        public void recordFailure() {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            totalRequests.incrementAndGet();
            
            if (consecutiveFailures >= failureThreshold) {
                open = true;
            }
        }
        
        // Getters
        public boolean isOpen() { return open; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public double getSuccessRate() {
            int total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total : 0.0;
        }
    }
    
    /**
     * 处理未提交的Prompt列表
     * 主入口方法，提供完整的错误处理和恢复机制
     * 
     * @param unsubmittedPrompts 未提交的Prompt列表
     * @return 处理结果统计
     */
    public ProcessingResult processUnsubmittedPrompts(List<Prompt> unsubmittedPrompts) {
        totalProcessed.addAndGet(unsubmittedPrompts.size());
        long startTime = System.currentTimeMillis();
        
        logger.info("开始处理 {} 个未提交的Prompt", unsubmittedPrompts.size());
        
        List<ProcessingDetail> details = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (Prompt prompt : unsubmittedPrompts) {
            try {
                ProcessingDetail detail = processWithEnhancedRecovery(prompt);
                details.add(detail);
                
                if (detail.isSuccessful()) {
                    successCount++;
                    successfulProcessed.incrementAndGet();
                } else {
                    failureCount++;
                    failedProcessed.incrementAndGet();
                }
                
            } catch (Exception e) {
                failureCount++;
                failedProcessed.incrementAndGet();
                logger.error("处理Prompt ID: {} 时发生未捕获异常", prompt.getPromptId(), e);
                
                ProcessingDetail errorDetail = new ProcessingDetail(
                    prompt.getPromptId(), false, ErrorCategory.UNKNOWN_ERROR,
                    RecoveryStrategy.MANUAL_INTERVENTION, 0, e.getMessage()
                );
                details.add(errorDetail);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        logger.info("未提交Prompt处理完成 - 总数: {}, 成功: {}, 失败: {}, 耗时: {}ms",
                   unsubmittedPrompts.size(), successCount, failureCount, totalTime);
        
        return new ProcessingResult(unsubmittedPrompts.size(), successCount, failureCount, totalTime, details);
    }
    
    /**
     * 使用增强恢复机制处理单个Prompt
     */
    private ProcessingDetail processWithEnhancedRecovery(Prompt prompt) {
        long startTime = System.currentTimeMillis();
        String originalStatus = prompt.getStatusName();
        Integer promptId = prompt.getPromptId();
        
        // 检查熔断器状态
        CircuitBreakerState circuitBreaker = getOrCreateCircuitBreaker("prompt_processing");
        if (!circuitBreaker.shouldAllowRequest()) {
            logger.warn("熔断器开启，跳过处理Prompt ID: {}", promptId);
            return new ProcessingDetail(promptId, false, ErrorCategory.CIRCUIT_BREAKER_OPEN,
                                      RecoveryStrategy.CIRCUIT_BREAKER_FALLBACK, 0, "熔断器开启");
        }
        
        try {
            // 执行状态检查和处理
            boolean result = executePromptProcessing(prompt);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (result) {
                circuitBreaker.recordSuccess();
                recordProcessingHistory(promptId, originalStatus, null, null, true, processingTime, 1, "处理成功");
                return new ProcessingDetail(promptId, true, null, null, processingTime, "处理成功");
            } else {
                circuitBreaker.recordFailure();
                recordProcessingHistory(promptId, originalStatus, ErrorCategory.UNKNOWN_ERROR, 
                                      RecoveryStrategy.MANUAL_INTERVENTION, false, processingTime, 1, "处理失败");
                return new ProcessingDetail(promptId, false, ErrorCategory.UNKNOWN_ERROR,
                                          RecoveryStrategy.MANUAL_INTERVENTION, processingTime, "处理失败");
            }
            
        } catch (Exception e) {
            ErrorCategory errorCategory = classifyError(e);
            errorCategoryCounters.computeIfAbsent(errorCategory, k -> new AtomicInteger(0)).incrementAndGet();
            
            RecoveryStrategy strategy = selectRecoveryStrategy(errorCategory, prompt);
            
            // 执行恢复策略
            ProcessingDetail result = executeRecoveryStrategy(prompt, originalStatus, errorCategory, strategy, e, startTime);
            
            if (result.isSuccessful()) {
                circuitBreaker.recordSuccess();
            } else {
                circuitBreaker.recordFailure();
            }
            
            return result;
        }
    }
    
    /**
     * 执行Prompt处理逻辑
     */
    private boolean executePromptProcessing(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        String currentStatus = prompt.getStatusName();
        
        logger.debug("开始处理Prompt ID: {}, 当前状态: {}", promptId, currentStatus);
        
        // 1. 状态一致性检查
        if (!isStatusConsistent(prompt)) {
            logger.warn("检测到状态不一致，Prompt ID: {}", promptId);
            reconcilePromptState(prompt);
            return true; // 状态协调成功
        }
        
        // 2. 根据当前状态执行相应处理
        switch (currentStatus) {
            case STATUS_PENDING:
                return handlePendingPrompt(prompt);
                
            case STATUS_SUBMISSION_STARTED:
                return handleSubmissionStartedPrompt(prompt);
                
            case STATUS_SUBMITTED:
                return handleSubmittedPrompt(prompt);
                
            default:
                logger.warn("未知的Prompt状态: {}, ID: {}", currentStatus, promptId);
                return false;
        }
    }
    
    /**
     * 处理待处理状态的Prompt
     */
    private boolean handlePendingPrompt(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        logger.debug("处理待处理状态的Prompt ID: {}", promptId);
        
        // 检查是否已存在加密数据
        String requestId = "cdwyy" + promptId;
        Optional<EncryptedDataTemp> existingData = encryptedDataTempRepository.findByRequestId(requestId);
        
        if (existingData.isPresent()) {
            EncryptedDataTemp encryptedData = existingData.get();
            DataStatus dataStatus = encryptedData.getStatus();
            
            logger.info("Prompt ID: {} 已存在加密数据，状态: {}", promptId, dataStatus);
            
            // 根据加密数据状态决定处理方式
            switch (dataStatus) {
                case SENT:
                    // 已发送完成，更新Prompt状态
                    return updatePromptStatusWithRetry(prompt, STATUS_COMPLETED, "数据已发送完成");
                    
                case ENCRYPTED:
                case PROCESSED:
                    // 等待轮询服务处理
                    logger.info("Prompt ID: {} 等待轮询服务处理，加密数据状态: {}", promptId, dataStatus);
                    return true;
                    
                case ERROR:
                    // 处理错误，需要重新提交
                    logger.warn("Prompt ID: {} 加密数据状态为ERROR，需要重新处理", promptId);
                    return false; // 需要重新处理
                    
                default:
                    // 其他状态继续等待
                    return true;
            }
        }
        
        // 没有加密数据，状态正常，等待提交服务处理
        return true;
    }
    
    /**
     * 处理提交开始状态的Prompt
     */
    private boolean handleSubmissionStartedPrompt(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        logger.debug("处理提交开始状态的Prompt ID: {}", promptId);
        
        // 检查提交超时
        if (isSubmissionTimeout(prompt)) {
            logger.warn("Prompt ID: {} 提交超时，回滚到待处理状态", promptId);
            return updatePromptStatusWithRetry(prompt, STATUS_PENDING, "提交超时回滚");
        }
        
        // 检查加密数据状态
        String requestId = "cdwyy" + promptId;
        Optional<EncryptedDataTemp> encryptedData = encryptedDataTempRepository.findByRequestId(requestId);
        
        if (encryptedData.isPresent()) {
            DataStatus dataStatus = encryptedData.get().getStatus();
            
            switch (dataStatus) {
                case SENT:
                    // 提交成功，更新状态
                    return updatePromptStatusWithRetry(prompt, STATUS_COMPLETED, "提交成功");
                    
                case RECEIVED:
                case DECRYPTED:
                case PROCESSING:
                case PROCESSED:
                case ENCRYPTED:
                    // 正在处理中，更新为已提交状态
                    return updatePromptStatusWithRetry(prompt, STATUS_SUBMITTED, "提交成功，正在处理");
                    
                case ERROR:
                    // 提交失败，回滚状态
                    return updatePromptStatusWithRetry(prompt, STATUS_PENDING, "提交失败回滚");
                    
                default:
                    // 继续等待
                    return true;
            }
        } else {
            // 没有加密数据，可能提交失败，检查是否超时
            if (isSubmissionTimeout(prompt)) {
                return updatePromptStatusWithRetry(prompt, STATUS_PENDING, "提交超时无数据");
            }
        }
        
        return true;
    }
    
    /**
     * 处理已提交状态的Prompt
     */
    private boolean handleSubmittedPrompt(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        logger.debug("处理已提交状态的Prompt ID: {}", promptId);
        
        // 检查加密数据状态
        String requestId = "cdwyy" + promptId;
        Optional<EncryptedDataTemp> encryptedData = encryptedDataTempRepository.findByRequestId(requestId);
        
        if (encryptedData.isPresent()) {
            DataStatus dataStatus = encryptedData.get().getStatus();
            
            if (dataStatus == DataStatus.SENT) {
                // 处理完成
                return updatePromptStatusWithRetry(prompt, STATUS_COMPLETED, "处理完成");
            } else if (dataStatus == DataStatus.ERROR) {
                // 处理失败
                return updatePromptStatusWithRetry(prompt, STATUS_FAILED, "执行失败");
            }
            // 其他状态继续等待
            return true;
        } else {
            // 状态不一致：已提交但无加密数据
            logger.warn("状态不一致：Prompt ID: {} 状态为已提交但无加密数据", promptId);
            return updatePromptStatusWithRetry(prompt, STATUS_PENDING, "状态不一致修复");
        }
    }
    
    /**
     * 检查状态一致性
     */
    private boolean isStatusConsistent(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        String status = prompt.getStatusName();
        String requestId = "cdwyy" + promptId;
        
        Optional<EncryptedDataTemp> encryptedData = encryptedDataTempRepository.findByRequestId(requestId);
        
        // 状态一致性规则
        if (status.equals(STATUS_SUBMITTED) && !encryptedData.isPresent()) {
            return false; // 已提交但无数据
        }
        
        if (status.equals(STATUS_PENDING) && encryptedData.isPresent()) {
            DataStatus dataStatus = encryptedData.get().getStatus();
            if (dataStatus == DataStatus.SENT || dataStatus == DataStatus.ENCRYPTED) {
                return false; // 待处理但数据已处理
            }
        }
        
        return true;
    }
    
    /**
     * 协调Prompt状态
     */
    private void reconcilePromptState(Prompt prompt) {
        Integer promptId = prompt.getPromptId();
        String currentStatus = prompt.getStatusName();
        String requestId = "cdwyy" + promptId;
        
        Optional<EncryptedDataTemp> encryptedData = encryptedDataTempRepository.findByRequestId(requestId);
        
        if (encryptedData.isPresent()) {
            DataStatus dataStatus = encryptedData.get().getStatus();
            
            // 根据数据状态调整Prompt状态
            switch (dataStatus) {
                case SENT:
                    if (!currentStatus.equals(STATUS_COMPLETED)) {
                        updatePromptStatusWithRetry(prompt, STATUS_COMPLETED, "状态协调：数据已发送");
                    }
                    break;
                    
                case RECEIVED:
                case DECRYPTED:
                case PROCESSING:
                case PROCESSED:
                case ENCRYPTED:
                    if (currentStatus.equals(STATUS_PENDING)) {
                        updatePromptStatusWithRetry(prompt, STATUS_SUBMITTED, "状态协调：数据正在处理");
                    }
                    break;
                    
                case ERROR:
                    if (!currentStatus.equals(STATUS_FAILED)) {
                        updatePromptStatusWithRetry(prompt, STATUS_FAILED, "状态协调：处理失败");
                    }
                    break;
            }
        } else {
            // 无加密数据
            if (currentStatus.equals(STATUS_SUBMITTED)) {
                updatePromptStatusWithRetry(prompt, STATUS_PENDING, "状态协调：无数据回滚");
            }
        }
    }
    
    /**
     * 检查提交是否超时
     */
    private boolean isSubmissionTimeout(Prompt prompt) {
        if (prompt.getSubmissionTime() == null) {
            return false;
        }
        
        // 30分钟超时
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
        return prompt.getSubmissionTime().isBefore(timeoutThreshold);
    }
    
    /**
     * 错误分类
     */
    private ErrorCategory classifyError(Exception e) {
        if (e instanceof OptimisticLockingFailureException) {
            return ErrorCategory.OPTIMISTIC_LOCK_CONFLICT;
        } else if (e instanceof SocketTimeoutException || 
                   (e instanceof ResourceAccessException && e.getCause() instanceof SocketTimeoutException)) {
            return ErrorCategory.NETWORK_TIMEOUT;
        } else if (e instanceof ConnectException || 
                   (e instanceof ResourceAccessException && e.getCause() instanceof ConnectException)) {
            return ErrorCategory.NETWORK_CONNECTION_FAILED;
        } else if (e instanceof DataAccessException && e.getMessage().contains("deadlock")) {
            return ErrorCategory.DATABASE_DEADLOCK;
        } else if (e instanceof QueryTimeoutException) {
            return ErrorCategory.DATABASE_TIMEOUT;
        } else if (e instanceof HttpServerErrorException) {
            HttpServerErrorException httpError = (HttpServerErrorException) e;
            if (httpError.getStatusCode().value() == 503) {
                return ErrorCategory.SERVER_OVERLOAD;
            }
        }
        
        return ErrorCategory.UNKNOWN_ERROR;
    }
    
    /**
     * 选择恢复策略
     */
    private RecoveryStrategy selectRecoveryStrategy(ErrorCategory errorCategory, Prompt prompt) {
        switch (errorCategory) {
            case OPTIMISTIC_LOCK_CONFLICT:
                return RecoveryStrategy.OPTIMISTIC_LOCK_RETRY;
                
            case NETWORK_TIMEOUT:
            case NETWORK_CONNECTION_FAILED:
                return networkHealthService.isNetworkHealthy() ? 
                       RecoveryStrategy.EXPONENTIAL_BACKOFF : 
                       RecoveryStrategy.NETWORK_RECOVERY;
                       
            case DATABASE_DEADLOCK:
            case DATABASE_TIMEOUT:
                return RecoveryStrategy.EXPONENTIAL_BACKOFF;
                
            case DATABASE_CONNECTION_POOL:
                return RecoveryStrategy.DATABASE_RECOVERY;
                
            case SERVER_OVERLOAD:
                return RecoveryStrategy.DELAYED_RETRY;
                
            case STATE_INCONSISTENCY:
                return RecoveryStrategy.STATE_RECONCILIATION;
                
            default:
                return RecoveryStrategy.LINEAR_BACKOFF;
        }
    }
    
    /**
     * 执行恢复策略
     */
    private ProcessingDetail executeRecoveryStrategy(Prompt prompt, String originalStatus, 
                                                   ErrorCategory errorCategory, RecoveryStrategy strategy, 
                                                   Exception originalException, long startTime) {
        
        Integer promptId = prompt.getPromptId();
        int maxRetries = getMaxRetriesForStrategy(strategy);
        int attempt = 0;
        
        while (attempt < maxRetries) {
            attempt++;
            
            try {
                // 执行策略特定的预处理
                performRecoveryPreprocessing(strategy, attempt, originalException);
                
                // 计算延迟
                long delay = calculateRetryDelay(strategy, attempt, errorCategory);
                if (delay > 0) {
                    logger.debug("Prompt ID: {} 等待 {}ms 后进行第 {} 次恢复尝试", promptId, delay, attempt);
                    Thread.sleep(delay);
                }
                
                // 重试处理
                boolean result = executePromptProcessing(prompt);
                
                if (result) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    recordProcessingHistory(promptId, originalStatus, errorCategory, strategy, 
                                          true, processingTime, attempt, "恢复成功");
                    
                    logger.info("Prompt ID: {} 恢复成功 - 策略: {} - 尝试次数: {} - 耗时: {}ms", 
                               promptId, strategy, attempt, processingTime);
                    
                    return new ProcessingDetail(promptId, true, errorCategory, strategy, 
                                              processingTime, "恢复成功");
                }
                
            } catch (Exception retryException) {
                logger.warn("Prompt ID: {} 第 {} 次恢复尝试失败: {} - {}", 
                           promptId, attempt, strategy, retryException.getMessage());
                
                // 如果是最后一次尝试，记录失败
                if (attempt >= maxRetries) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    recordProcessingHistory(promptId, originalStatus, errorCategory, strategy, 
                                          false, processingTime, attempt, "恢复失败: " + retryException.getMessage());
                    
                    logger.error("Prompt ID: {} 恢复失败 - 策略: {} - 最大尝试次数: {} - 总耗时: {}ms", 
                               promptId, strategy, maxRetries, processingTime);
                    
                    return new ProcessingDetail(promptId, false, errorCategory, strategy, 
                                              processingTime, "恢复失败: " + retryException.getMessage());
                }
            }
        }
        
        // 不应该到达这里
        long processingTime = System.currentTimeMillis() - startTime;
        return new ProcessingDetail(promptId, false, errorCategory, strategy, 
                                  processingTime, "恢复失败：未知原因");
    }
    
    /**
     * 恢复预处理
     */
    private void performRecoveryPreprocessing(RecoveryStrategy strategy, int attempt, Exception originalException) {
        switch (strategy) {
            case NETWORK_RECOVERY:
                // 网络恢复操作
                networkHealthService.resetStats();
                networkHealthService.performHealthCheck();
                break;
                
            case DATABASE_RECOVERY:
                // 数据库恢复操作
                databaseHealthService.resetConnectionPool();
                break;
                
            case STATE_RECONCILIATION:
                // 状态协调不需要预处理
                break;
                
            default:
                // 其他策略不需要特殊预处理
                break;
        }
    }
    
    /**
     * 计算重试延迟
     */
    private long calculateRetryDelay(RecoveryStrategy strategy, int attempt, ErrorCategory errorCategory) {
        switch (strategy) {
            case IMMEDIATE_RETRY:
                return 0;
                
            case OPTIMISTIC_LOCK_RETRY:
                // 乐观锁冲突使用较短延迟
                return 200 + (attempt * 300) + (long)(Math.random() * 200);
                
            case LINEAR_BACKOFF:
                return 1000 * attempt;
                
            case EXPONENTIAL_BACKOFF:
                long exponentialDelay = 1000 * (1L << Math.min(attempt, 6));
                double jitter = 0.1 + Math.random() * 0.1;
                return exponentialDelay + (long)(exponentialDelay * jitter);
                
            case DELAYED_RETRY:
                return 5000 + (attempt * 2000); // 5秒基础延迟
                
            case NETWORK_RECOVERY:
                return 3000 + (attempt * 1000); // 网络恢复延迟
                
            case DATABASE_RECOVERY:
                return 2000 + (attempt * 500);  // 数据库恢复延迟
                
            default:
                return 1000 + (attempt * 1000);
        }
    }
    
    /**
     * 获取策略的最大重试次数
     */
    private int getMaxRetriesForStrategy(RecoveryStrategy strategy) {
        switch (strategy) {
            case OPTIMISTIC_LOCK_RETRY:
                return optimisticLockRetryService.getRecommendedConcurrencyLevel() > 3 ? 8 : 5;
                
            case EXPONENTIAL_BACKOFF:
                return 6;
                
            case LINEAR_BACKOFF:
                return 4;
                
            case IMMEDIATE_RETRY:
                return 3;
                
            case DELAYED_RETRY:
                return 3;
                
            case NETWORK_RECOVERY:
                return networkHealthService.getRecommendedRetries();
                
            case DATABASE_RECOVERY:
                return databaseHealthService.getRecommendedRetries();
                
            case CIRCUIT_BREAKER_FALLBACK:
                return 1; // 熔断器降级只尝试一次
                
            default:
                return 3;
        }
    }
    
    /**
     * 使用重试机制更新Prompt状态
     */
    private boolean updatePromptStatusWithRetry(Prompt prompt, String newStatus, String operationName) {
        try {
            // 使用事务重试服务进行状态更新
            transactionRetryService.executeWithRetry(() -> {
                // 使用乐观锁重试服务确保并发安全
                optimisticLockRetryService.executeWithOptimisticLockRetry(
                    () -> {
                        prompt.setStatusName(newStatus);
                        prompt.setSubmissionTime(LocalDateTime.now());
                        promptRepository.save(prompt);
                        return null;
                    },
                    () -> {
                        // 刷新实体获取最新版本
                        Optional<Prompt> refreshedPrompt = promptRepository.findById(prompt.getPromptId());
                        if (refreshedPrompt.isPresent()) {
                            // 假设有version字段进行乐观锁控制
                            // prompt.setVersion(refreshedPrompt.get().getVersion());
                        }
                        return null;
                    },
                    operationName + "[更新Prompt状态]"
                );
                return null;
            }, operationName);
            
            logger.info("{}成功，Prompt ID: {}, 新状态: {}", operationName, prompt.getPromptId(), newStatus);
            return true;
            
        } catch (Exception e) {
            logger.error("{}失败，Prompt ID: {}, 目标状态: {}, 错误: {}", 
                        operationName, prompt.getPromptId(), newStatus, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取或创建熔断器
     */
    private CircuitBreakerState getOrCreateCircuitBreaker(String operationName) {
        return circuitBreakers.computeIfAbsent(operationName, 
            k -> new CircuitBreakerState(5, 60000)); // 5次失败，60秒超时
    }
    
    /**
     * 记录处理历史
     */
    private void recordProcessingHistory(Integer promptId, String originalStatus, ErrorCategory errorCategory,
                                       RecoveryStrategy strategy, boolean successful, 
                                       long processingTimeMs, int attemptCount, String errorDetails) {
        ProcessingRecord record = new ProcessingRecord(
            promptId, originalStatus, errorCategory, strategy, successful, processingTimeMs, attemptCount, errorDetails
        );
        
        synchronized (processingHistory) {
            processingHistory.add(record);
            if (processingHistory.size() > MAX_HISTORY_SIZE) {
                processingHistory.remove(0);
            }
        }
    }
    
    /**
     * 处理详情类
     */
    public static class ProcessingDetail {
        private final Integer promptId;
        private final boolean successful;
        private final ErrorCategory errorCategory;
        private final RecoveryStrategy strategy;
        private final long processingTimeMs;
        private final String message;
        
        public ProcessingDetail(Integer promptId, boolean successful, ErrorCategory errorCategory,
                              RecoveryStrategy strategy, long processingTimeMs, String message) {
            this.promptId = promptId;
            this.successful = successful;
            this.errorCategory = errorCategory;
            this.strategy = strategy;
            this.processingTimeMs = processingTimeMs;
            this.message = message;
        }
        
        // Getters
        public Integer getPromptId() { return promptId; }
        public boolean isSuccessful() { return successful; }
        public ErrorCategory getErrorCategory() { return errorCategory; }
        public RecoveryStrategy getStrategy() { return strategy; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getMessage() { return message; }
    }
    
    /**
     * 处理结果类
     */
    public static class ProcessingResult {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final long totalTimeMs;
        private final List<ProcessingDetail> details;
        
        public ProcessingResult(int totalCount, int successCount, int failureCount, 
                              long totalTimeMs, List<ProcessingDetail> details) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.totalTimeMs = totalTimeMs;
            this.details = details;
        }
        
        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount : 0.0;
        }
        
        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public List<ProcessingDetail> getDetails() { return details; }
    }
    
    /**
     * 处理统计信息
     */
    public ProcessingStatistics getProcessingStatistics() {
        Map<ErrorCategory, Integer> errorCounts = new HashMap<>();
        errorCategoryCounters.forEach((key, value) -> errorCounts.put(key, value.get()));
        
        Map<String, CircuitBreakerInfo> circuitBreakerInfos = new HashMap<>();
        circuitBreakers.forEach((name, state) -> {
            circuitBreakerInfos.put(name, new CircuitBreakerInfo(
                state.isOpen(), state.getConsecutiveFailures(), state.getSuccessRate()
            ));
        });
        
        return new ProcessingStatistics(
            totalProcessed.get(),
            successfulProcessed.get(),
            failedProcessed.get(),
            errorCounts,
            circuitBreakerInfos,
            new ArrayList<>(processingHistory)
        );
    }
    
    /**
     * 处理统计信息类
     */
    public static class ProcessingStatistics {
        private final long totalProcessed;
        private final long successfulProcessed;
        private final long failedProcessed;
        private final Map<ErrorCategory, Integer> errorCategoryCounts;
        private final Map<String, CircuitBreakerInfo> circuitBreakers;
        private final List<ProcessingRecord> recentHistory;
        
        public ProcessingStatistics(long totalProcessed, long successfulProcessed, 
                                   long failedProcessed, Map<ErrorCategory, Integer> errorCategoryCounts,
                                   Map<String, CircuitBreakerInfo> circuitBreakers,
                                   List<ProcessingRecord> recentHistory) {
            this.totalProcessed = totalProcessed;
            this.successfulProcessed = successfulProcessed;
            this.failedProcessed = failedProcessed;
            this.errorCategoryCounts = errorCategoryCounts;
            this.circuitBreakers = circuitBreakers;
            this.recentHistory = recentHistory;
        }
        
        public double getSuccessRate() {
            return totalProcessed > 0 ? (double) successfulProcessed / totalProcessed : 0.0;
        }
        
        // Getters
        public long getTotalProcessed() { return totalProcessed; }
        public long getSuccessfulProcessed() { return successfulProcessed; }
        public long getFailedProcessed() { return failedProcessed; }
        public Map<ErrorCategory, Integer> getErrorCategoryCounts() { return errorCategoryCounts; }
        public Map<String, CircuitBreakerInfo> getCircuitBreakers() { return circuitBreakers; }
        public List<ProcessingRecord> getRecentHistory() { return recentHistory; }
    }
    
    /**
     * 熔断器信息类
     */
    public static class CircuitBreakerInfo {
        private final boolean open;
        private final int consecutiveFailures;
        private final double successRate;
        
        public CircuitBreakerInfo(boolean open, int consecutiveFailures, double successRate) {
            this.open = open;
            this.consecutiveFailures = consecutiveFailures;
            this.successRate = successRate;
        }
        
        // Getters
        public boolean isOpen() { return open; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public double getSuccessRate() { return successRate; }
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalProcessed.set(0);
        successfulProcessed.set(0);
        failedProcessed.set(0);
        errorCategoryCounters.clear();
        circuitBreakers.clear();
        synchronized (processingHistory) {
            processingHistory.clear();
        }
        logger.info("未提交Prompt处理统计信息已重置");
    }
}
