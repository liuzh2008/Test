package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.util.AESEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Clob;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 轮询服务类
 * 定时轮询ENCRYPTED_DATA_TEMP表，处理已处理状态的数据
 * 提取执行结果并更新Prompt表状态
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-20
 */
@Service
@EnableScheduling
public class PollingService {

    private static final Logger logger = LoggerFactory.getLogger(PollingService.class);

    @Autowired
    private EncryptedDataTempRepository encryptedDataTempRepository;

    @Autowired
    private EncryptedDataTempService encryptedDataTempService;

    @Autowired
    private PromptResultRepository promptResultRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private ServerConfigService serverConfigService;

    @Autowired
    private OptimisticLockRetryService optimisticLockRetryService;
    
    @Autowired
    private Environment environment;

    /**
     * 轮询服务启用状态标志
     * 默认值为false，表示服务器启动时轮询服务处于禁用状态
     * 需要通过API接口手动启用轮询服务
     * 
     * @since 2025-09-22
     * @see AutoExecuteController#startAutoExecute(Integer)
     * @see AutoExecuteController#stopAutoExecute()
     */
    private volatile boolean pollingEnabled = false;


    /**
     * 轮询Prompt表中状态为"已提交"的记录
     * 每5秒执行一次，处理已提交但未完成的Prompt任务
     * 使用READ_COMMITTED隔离级别避免脏读，同时保证性能
     */
    @Scheduled(fixedDelayString = "${polling.interval:5000}")
    @Transactional(transactionManager = "transactionManager", isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void pollSubmittedPrompts() {
        if (!pollingEnabled) {
            logger.debug("轮询服务已禁用，跳过本次轮询");
            return;
        }
        try {
            logger.info("开始轮询Prompt表，查找状态为'已提交'的数据...");
            
            // 查询所有状态为"已提交"的Prompt
            List<Prompt> submittedPrompts = promptRepository.findByStatusName("已提交");
            
            if (submittedPrompts.isEmpty()) {
                logger.info("未找到状态为'已提交'的Prompt数据");
                return;
            }

            logger.info("找到 {} 条状态为'已提交'的Prompt数据", submittedPrompts.size());
            
            int successCount = 0;
            int errorCount = 0;

            // 处理每条已提交的Prompt
            for (Prompt prompt : submittedPrompts) {
                try {
                    processSubmittedPrompt(prompt);
                    successCount++;
                } catch (Exception e) {
                    logger.error("处理Prompt ID: {} 时发生错误: {}", prompt.getPromptId(), e.getMessage(), e);
                    errorCount++;
                }
            }

            logger.info("Prompt轮询处理完成 - 成功: {} 条, 失败: {} 条", successCount, errorCount);
            
        } catch (Exception e) {
            logger.error("轮询Prompt表时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理已加密状态的数据
     * 提取执行结果并更新Prompt表
     * 
     * @param data 已加密的数据
     */
    private void processEncryptedData(EncryptedDataTemp data) {
        String dataId = data.getId();
        String requestId = data.getRequestId();
        Clob executionResult = data.getExecutionResult();

        logger.info("开始处理数据ID: {}, 请求ID: {}", dataId, requestId);

        // 验证必要字段
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("请求ID不能为空，数据ID: " + dataId);
        }

        if (executionResult == null) {
            throw new IllegalArgumentException("执行结果不能为空，数据ID: " + dataId);
        }

        // 从requestId中提取promptId（去掉"cdwyy"前缀）
        String promptId = extractPromptIdFromRequestId(requestId);
        if (promptId == null) {
            throw new IllegalArgumentException("无法从请求ID中提取有效的Prompt ID: " + requestId);
        }

        logger.info("提取到Prompt ID: {}", promptId);

        // 提取执行结果内容并进行解密
        String encryptedContent = extractClobContent(executionResult);
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            throw new IllegalArgumentException("执行结果内容为空，数据ID: " + dataId);
        }

        // 解密数据
        String resultContent = decryptData(encryptedContent);
        if (resultContent == null || resultContent.trim().isEmpty()) {
            throw new IllegalArgumentException("解密后的数据内容为空，数据ID: " + dataId);
        }

        logger.debug("执行结果内容长度: {} 字符", resultContent.length());
        
        // 输出解密后数据的前200个字符到控制台
        String previewContent = resultContent.length() > 200 ? 
            resultContent.substring(0, 200) + "..." : resultContent;
        logger.info("解密后执行结果前200字符预览 - 数据ID: {}, Prompt ID: {}: {}", 
            dataId, promptId, previewContent);

        // 检查是否为错误响应
        if (isErrorResponse(resultContent)) {
            logger.warn("检测到错误响应，数据ID: {}, Prompt ID: {}", dataId, promptId);
            handleErrorResponse(dataId, promptId, resultContent);
            return;
        }

        // 更新Prompt表状态和执行结果
        boolean updateSuccess = updatePromptStatus(promptId, resultContent);

        if (updateSuccess) {
            // 只有在Prompt表成功更新后，才标记数据为已发送状态
            encryptedDataTempService.markAsSent(dataId);
            logger.info("成功处理数据ID: {}, Prompt ID: {} 已更新", dataId, promptId);
        } else {
            // 如果更新失败，保持数据状态为ENCRYPTED，以便后续轮询继续处理
            logger.warn("Prompt表更新失败，数据ID: {}, Prompt ID: {} 保持ENCRYPTED状态以便后续轮询", dataId, promptId);
        }
    }

    /**
     * 从请求ID中提取Prompt ID
     * 去掉"cdwyy"前缀
     * 
     * @param requestId 请求ID
     * @return 提取的Prompt ID，如果格式不正确返回null
     */
    private String extractPromptIdFromRequestId(String requestId) {
        if (requestId != null && requestId.startsWith("cdwyy")) {
            return requestId.substring(5); // 去掉"cdwyy"前缀
        }
        return null;
    }

    /**
     * 提取Clob对象的内容
     * 
     * @param clob Clob对象
     * @return 字符串内容，如果提取失败返回null
     */
    private String extractClobContent(Clob clob) {
        try {
            if (clob != null) {
                long length = clob.length();
                if (length > 0) {
                    return clob.getSubString(1, (int) length);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("提取Clob内容时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查是否为错误响应
     * 基于JSON格式的error字段检查，避免医疗内容误判
     * 
     * @param content 响应内容
     * @return true如果是错误响应，false否则
     */
    private boolean isErrorResponse(String content) {
        try {
            // 首先尝试解析为JSON，检查是否有顶层error字段
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> jsonResponse = objectMapper.readValue(content, 
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            
            // 如果包含顶层error字段，则是真正的错误响应
            if (jsonResponse.containsKey("error")) {
                Object errorObj = jsonResponse.get("error");
                logger.info("检测到JSON错误响应: {}", errorObj);
                return true;
            }
            
            // 检查其他常见的错误字段
            if (jsonResponse.containsKey("status") && "error".equalsIgnoreCase(String.valueOf(jsonResponse.get("status")))) {
                logger.info("检测到status为error的响应");
                return true;
            }
            
            // 如果没有检测到标准错误格式，返回false
            return false;
            
        } catch (Exception jsonParseException) {
            // 如果JSON解析失败，内容可能不是标准JSON格式
            logger.debug("内容不是标准JSON格式，使用备用错误检测: {}", jsonParseException.getMessage());
            
            // 备用检测逻辑：检查特定的错误模式，但避免过于宽泛的匹配
            // 只检测明显的错误指示符，避免医疗内容误判
            boolean isLikelyError = content.contains("\"error\":") ||  // JSON格式的错误字段
                                   content.contains("\"status\":\"error\"") ||  // JSON状态错误
                                   content.startsWith("{\"error\":") ||  // 以错误开头的JSON
                                   (content.contains("错误") && content.length() < 100) ||  // 短内容包含"错误"
                                   content.contains("调用失败") ||  // 明确的失败指示
                                   content.contains("服务不可用") ||  // 服务错误
                                   content.contains("超时") ||  // 超时错误
                                   content.contains("Timeout") ||  // 英文超时
                                   content.contains("Exception:") ||  // 异常信息
                                   content.contains("java.") ||  // Java异常
                                   content.contains("org.springframework.");  // Spring框架异常
            
            if (isLikelyError) {
                logger.info("备用检测到可能的错误响应，内容前100字符: {}", content.substring(0, Math.min(content.length(), 100)));
            }
            
            return isLikelyError;
        }
    }

    /**
     * 处理错误响应
     * 
     * @param dataId 数据ID
     * @param promptId Prompt ID
     * @param errorContent 错误内容
     */
    private void handleErrorResponse(String dataId, String promptId, String errorContent) {
        try {
            // 更新Prompt表状态为错误
            updatePromptStatusWithError(promptId, "执行失败", errorContent);
            
            // 标记数据为错误状态
            encryptedDataTempService.markAsError(dataId, "AI执行错误: " + errorContent);
            
            logger.warn("处理错误响应完成 - 数据ID: {}, Prompt ID: {}", dataId, promptId);
        } catch (Exception e) {
            logger.error("处理错误响应时发生异常 - 数据ID: {}, Prompt ID: {}: {}", 
                    dataId, promptId, e.getMessage(), e);
        }
    }

    /**
     * 更新Prompt表状态和执行结果
     * 
     * @param promptId Prompt ID
     * @param resultContent 执行结果内容
     * @return true如果更新成功，false如果更新失败
     */
    private boolean updatePromptStatus(String promptId, String resultContent) {
        try {
            // 调用PromptService更新状态和执行结果
            updatePromptStatusWithResult(promptId, "已完成", resultContent);
            logger.debug("Prompt ID: {} 状态更新成功", promptId);
            return true;
        } catch (Exception e) {
            logger.error("更新Prompt ID: {} 状态时发生错误: {}", promptId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新Prompt状态为错误
     * 使用乐观锁重试机制处理并发更新冲突
     * 
     * @param promptId Prompt ID
     * @param statusName 状态名称
     * @param errorContent 错误内容
     */
    private void updatePromptStatusWithError(String promptId, String statusName, String errorContent) {
        try {
            // 使用乐观锁重试机制更新Prompt状态
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                () -> {
                    Optional<Prompt> promptOptional = promptRepository.findById(Integer.parseInt(promptId));
                    if (promptOptional.isPresent()) {
                        Prompt prompt = promptOptional.get();
                        prompt.setStatusName(statusName);
                        prompt.setExecutionTime(LocalDateTime.now());
                        
                        // 保存错误信息到执行结果字段
                        prompt.setExecutionResult("错误: " + errorContent);
                        
                        promptRepository.save(prompt);
                        logger.info("Prompt ID: {} 状态已更新为: {}", promptId, statusName);
                    } else {
                        logger.warn("未找到对应的Prompt记录，Prompt ID: {}", promptId);
                    }
                    return null;
                },
                null, // entity refresher
                "更新Prompt错误状态 - Prompt ID: " + promptId
            );
        } catch (Exception e) {
            logger.error("更新Prompt状态为错误时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("更新Prompt错误状态失败", e);
        }
    }

    /**
     * 更新Prompt状态和执行结果
     * 使用乐观锁重试机制处理并发更新冲突
     * 
     * @param promptId Prompt ID
     * @param statusName 状态名称
     * @param resultContent 执行结果内容
     */
    private void updatePromptStatusWithResult(String promptId, String statusName, String resultContent) {
        try {
            // 使用乐观锁重试机制保存PromptResult
            PromptResult savedResult = optimisticLockRetryService.executeWithOptimisticLockRetry(
                () -> {
                    PromptResult promptResult = new PromptResult();
                    promptResult.setPromptId(Integer.parseInt(promptId));
                    promptResult.setOriginalResultContent(resultContent);
                    promptResult.setStatus("SUCCESS");
                    promptResult.setExecutionTime(LocalDateTime.now());
                    promptResult.setCreatedAt(LocalDateTime.now());
                    promptResult.setUpdatedAt(LocalDateTime.now());
                    promptResult.setIsRead(0);
                    promptResult.setDeleted(0);
                    return promptResultRepository.save(promptResult);
                },
                null, // entity refresher
                "保存PromptResult - Prompt ID: " + promptId
            );
            
            // 使用乐观锁重试机制更新Prompt状态
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                () -> {
                    Optional<Prompt> promptOptional = promptRepository.findById(Integer.parseInt(promptId));
                    if (promptOptional.isPresent()) {
                        Prompt prompt = promptOptional.get();
                        prompt.setStatusName("已完成");
                        prompt.setExecutionTime(LocalDateTime.now());
                        prompt.setResultId(savedResult.getResultId());
                        // 不保存到EXECUTION_RESULT字段，只保存到PromptResult表
                        promptRepository.save(prompt);
                        logger.info("Prompt ID: {} 状态已更新为已完成，结果已保存到PromptResult表", promptId);
                    } else {
                        logger.warn("未找到对应的Prompt记录，Prompt ID: {}", promptId);
                    }
                    return null;
                },
                null, // entity refresher
                "更新Prompt状态 - Prompt ID: " + promptId
            );
            
        } catch (Exception e) {
            logger.error("更新Prompt状态和执行结果时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("更新Prompt状态失败", e);
        }
    }

    /**
     * 获取当前轮询状态统计
     * 用于监控和调试
     * 
     * @return 轮询状态统计信息
     */
    public String getPollingStats() {
        long encryptedCount = encryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED);
        long sentCount = encryptedDataTempRepository.countByStatus(DataStatus.SENT);
        long errorCount = encryptedDataTempRepository.countByStatus(DataStatus.ERROR);
        
        return String.format("轮询状态统计 - 待处理: %d, 已发送: %d, 错误: %d", 
                encryptedCount, sentCount, errorCount);
    }

    /**
     * 手动触发轮询
     * 用于测试和调试目的
     */
    public void triggerPolling() {
        logger.info("手动触发轮询...");
        pollSubmittedPrompts();
        logger.info("手动轮询完成");
    }

    /**
     * 启用轮询服务
     */
    public void enablePolling() {
        pollingEnabled = true;
        logger.info("轮询服务已启用");
    }

    /**
     * 禁用轮询服务
     */
    public void disablePolling() {
        pollingEnabled = false;
        logger.info("轮询服务已禁用");
    }

    /**
     * 解密数据
     * 
     * @param encryptedContent 加密的内容
     * @return 解密后的内容，如果解密失败返回null
     */
    private String decryptData(String encryptedContent) {
        try {
            // 获取AES加密密钥和盐值
            String encryptionKey = serverConfigService.getAesEncryptionKey();
            String encryptionSalt = serverConfigService.getAesEncryptionSalt();
            
            if (encryptionKey == null || encryptionSalt == null) {
                logger.error("AES加密配置未设置，无法解密数据");
                return null;
            }
            
            // 解密数据
            String decryptedResult = AESEncryptionUtil.decrypt(encryptedContent, encryptionKey, encryptionSalt);
            logger.info("数据解密成功，解密后长度: {} 字符", decryptedResult.length());
            return decryptedResult;
        } catch (Exception e) {
            logger.error("解密数据时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理已提交状态的Prompt
     * 检查对应的ENCRYPTED_DATA_TEMP表中是否有处理结果
     * 
     * @param prompt 已提交的Prompt
     */
    private void processSubmittedPrompt(Prompt prompt) {
        try {
            Integer promptId = prompt.getPromptId();
            String requestId = "cdwyy" + promptId;
            
            logger.info("处理已提交的Prompt, ID: {}, 请求ID: {}", promptId, requestId);

            // 查询ENCRYPTED_DATA_TEMP表中对应的处理结果
            Optional<EncryptedDataTemp> encryptedDataOptional = encryptedDataTempRepository.findByRequestId(requestId);
            
            if (encryptedDataOptional.isPresent()) {
                EncryptedDataTemp encryptedData = encryptedDataOptional.get();
                
                // 如果数据状态为ENCRYPTED，说明执行服务器已经处理完成
                if (encryptedData.getStatus() == DataStatus.ENCRYPTED) {
                    logger.info("找到对应的加密数据，开始处理Prompt ID: {}", promptId);
                    processEncryptedData(encryptedData);
                } else {
                    logger.info("Prompt ID: {} 对应的数据状态为: {}，等待处理完成", 
                            promptId, encryptedData.getStatus());
                }
            } else {
                logger.info("Prompt ID: {} 尚未找到对应的加密数据，等待执行服务器处理", promptId);
            }
            
        } catch (Exception e) {
            logger.error("处理已提交Prompt时发生异常 - Prompt ID: {}: {}", 
                    prompt.getPromptId(), e.getMessage(), e);
        }
    }

    /**
     * 获取轮询服务状态
     * @return true如果轮询服务已启用，false如果已禁用
     */
    public boolean isPollingEnabled() {
        return pollingEnabled;
    }
    
    /**
     * 获取轮询服务详细状态
     * @return 轮询服务的详细状态信息，包含是否启用、最后一次轮询时间、下一次轮询时间、轮询间隔等
     */
    public java.util.Map<String, Object> getDetailedStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // 从配置文件获取轮询间隔（毫秒），默认5000毫秒
        String intervalStr = environment.getProperty("polling.interval", "5000");
        long intervalMs = Long.parseLong(intervalStr);
        int intervalSeconds = (int) (intervalMs / 1000);
        
        status.put("enabled", pollingEnabled);
        status.put("lastPolling", now.minusSeconds(intervalSeconds).toString());
        status.put("nextPolling", now.plusSeconds(intervalSeconds).toString());
        status.put("pollingInterval", intervalSeconds);
        status.put("pollingIntervalMs", intervalMs);
        status.put("serviceName", "Polling Service");
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
}
