package com.example.medaiassistant.service;

import com.example.medaiassistant.config.PromptServiceConfig;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.util.AESEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.sql.Clob;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Prompt轮询服务
 * 负责轮询ENCRYPTED_DATA_TEMP表，处理已处理状态的数据
 * 提取执行结果并更新Prompt表状态
 * 
 * 主要功能：
 * 1. 定时轮询已提交的Prompt
 * 2. 处理加密数据并解密
 * 3. 更新Prompt状态和执行结果
 * 4. 错误处理和重试机制
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@Service
@EnableScheduling
public class PromptPollingService {
    private static final Logger logger = LoggerFactory.getLogger(PromptPollingService.class);

    private final ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository;
    private final EncryptedDataTempService encryptedDataTempService;
    private final PromptResultRepository promptResultRepository;
    private final PromptRepository promptRepository;
    private final ServerConfigService serverConfigService;
    private final OptimisticLockRetryService optimisticLockRetryService;
    private final PromptServiceConfig promptServiceConfig;

    /**
     * 轮询服务启用状态标志
     * 默认值为false，表示服务器启动时轮询服务处于禁用状态
     * 需要通过API接口手动启用轮询服务
     */
    private volatile boolean pollingEnabled = false;

    /**
     * 构造函数，依赖注入必要的服务实例
     */
    public PromptPollingService(ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository,
            EncryptedDataTempService encryptedDataTempService,
            PromptResultRepository promptResultRepository,
            PromptRepository promptRepository,
            ServerConfigService serverConfigService,
            OptimisticLockRetryService optimisticLockRetryService,
            PromptServiceConfig promptServiceConfig) {
        this.executionEncryptedDataTempRepository = executionEncryptedDataTempRepository;
        this.encryptedDataTempService = encryptedDataTempService;
        this.promptResultRepository = promptResultRepository;
        this.promptRepository = promptRepository;
        this.serverConfigService = serverConfigService;
        this.optimisticLockRetryService = optimisticLockRetryService;
        this.promptServiceConfig = promptServiceConfig;
    }

    /**
     * 轮询Prompt表中状态为"已提交"的记录
     * 使用配置文件中的轮询间隔，实现真正的独立调度
     * 取消readOnly=true，确保内部独立事务能正确提交
     */
    @Scheduled(fixedDelayString = "${prompt.polling.interval:30000}")
    @Transactional(transactionManager = "transactionManager", isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void pollSubmittedPrompts() {
        if (!pollingEnabled) {
            logger.debug("轮询服务已禁用，跳过本次轮询");
            return;
        }
        try {
            logger.info("开始轮询Prompt表，查找状态为'已提交'和'SUBMISSION_STARTED'的数据...");

            // 查询所有状态为"已提交"的Prompt
            List<Prompt> submittedPrompts = promptRepository.findByStatusName("已提交");

            // 查询所有状态为"SUBMISSION_STARTED"的Prompt
            List<Prompt> submissionStartedPrompts = promptRepository.findByStatusName("SUBMISSION_STARTED");

            // 合并两个列表
            List<Prompt> allPrompts = new ArrayList<>();
            allPrompts.addAll(submittedPrompts);
            allPrompts.addAll(submissionStartedPrompts);

            if (allPrompts.isEmpty()) {
                logger.info("未找到状态为'已提交'或'SUBMISSION_STARTED'的Prompt数据");
                return;
            }

            logger.info("找到 {} 条状态为'已提交'或'SUBMISSION_STARTED'的Prompt数据 （已提交: {}, SUBMISSION_STARTED: {}）",
                    allPrompts.size(), submittedPrompts.size(), submissionStartedPrompts.size());

            int successCount = 0;
            int errorCount = 0;

            // 处理每条已提交的Prompt
            for (Prompt prompt : allPrompts) {
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
     * 处理已提交状态的Prompt
     * 检查对应的ENCRYPTED_DATA_TEMP表中是否有处理结果
     * 
     * 修复数据不一致问题：
     * 1. 处理所有可能的状态，而不仅仅是ENCRYPTED状态
     * 2. 增强状态检查和错误处理
     * 3. 提供更详细的日志记录
     * 
     * @param prompt 已提交的Prompt
     */
    private void processSubmittedPrompt(Prompt prompt) {
        try {
            Integer promptId = prompt.getPromptId();
            String requestId = "cdwyy" + promptId;

            logger.info("处理已提交的Prompt, ID: {}, 请求ID: {}", promptId, requestId);

            // 由于REQUEST_ID与ID相同且有唯一约束，直接使用findByRequestId查询
            Optional<EncryptedDataTemp> encryptedDataOptional;
            try {
                encryptedDataOptional = executionEncryptedDataTempRepository.findByRequestId(requestId);
            } catch (Exception e) {
                // 如果查询失败，记录错误并返回空
                logger.error("查询执行服务器REQUEST_ID数据失败 - Prompt ID: {}, Request ID: {}, 错误: {}", promptId, requestId,
                        e.getMessage(), e);
                encryptedDataOptional = Optional.empty();
            }

            if (encryptedDataOptional.isPresent()) {
                EncryptedDataTemp encryptedData = encryptedDataOptional.get();
                DataStatus currentStatus = encryptedData.getStatus();

                // 如果数据状态为ENCRYPTED，说明执行服务器已经处理完成
                if (currentStatus == DataStatus.ENCRYPTED) {
                    logger.info("找到对应的加密数据，开始处理Prompt ID: {}", promptId);
                    processEncryptedData(encryptedData);
                }
                // 如果状态为SENT，说明已经处理完成，需要更新Prompt状态
                else if (currentStatus == DataStatus.SENT) {
                    logger.info("Prompt ID: {} 对应的数据状态为SENT，但Prompt状态尚未更新，开始更新Prompt状态", promptId);
                    updatePromptStatusFromSentData(promptId, encryptedData);
                }
                // 如果状态为ERROR，记录错误信息
                else if (currentStatus == DataStatus.ERROR) {
                    logger.warn("Prompt ID: {} 对应的数据状态为ERROR，需要人工干预", promptId);
                    handleErrorStatus(promptId, encryptedData);
                }
                // 其他状态表示数据正在处理中
                else {
                    logger.info("Prompt ID: {} 对应的数据状态为: {}，等待处理完成",
                            promptId, currentStatus);
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
     * 处理已加密状态的数据
     * 从ENCRYPTED_DATA_TEMP表的EXECUTION_RESULT字段获取加密数据，解密后更新Prompt表状态
     * 
     * @param data 已加密的数据记录，包含EXECUTION_RESULT字段的加密数据
     * @throws IllegalArgumentException 当请求ID为空、执行结果数据为空或无法提取有效Prompt ID时抛出
     * @since 2025-10-02
     * @version 1.0.0
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
            throw new IllegalArgumentException("执行结果数据不能为空，数据ID: " + dataId);
        }

        // 从requestId中提取promptId（去掉"cdwyy"前缀）
        String promptId = extractPromptIdFromRequestId(requestId);
        if (promptId == null) {
            throw new IllegalArgumentException("无法从请求ID中提取有效的Prompt ID: " + requestId);
        }

        logger.info("提取到Prompt ID: {}", promptId);

        // 提取执行结果数据内容并进行解密
        String encryptedContent = extractClobContent(executionResult);
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            throw new IllegalArgumentException("执行结果数据内容为空，数据ID: " + dataId);
        }

        // 解密数据
        String resultContent = decryptData(encryptedContent);
        if (resultContent == null || resultContent.trim().isEmpty()) {
            throw new IllegalArgumentException("解密后的数据内容为空，数据ID: " + dataId);
        }

        logger.debug("加密数据内容长度: {} 字符", resultContent.length());

        // 输出解密后数据的前200个字符到控制台
        String previewContent = resultContent.length() > 200 ? resultContent.substring(0, 200) + "..." : resultContent;
        logger.info("解密后数据前200字符预览 - 数据ID: {}, Prompt ID: {}: {}",
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
            // 使用独立事务确保状态更新正确提交
            markDataAsSentInNewTransaction(dataId);
            logger.info("成功处理数据ID: {}, Prompt ID: {} 已更新 - 使用独立事务确保提交", dataId, promptId);
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
     * 添加重试机制处理网络不稳定导致的CLOB异常
     * 
     * @param clob Clob对象
     * @return 字符串内容，如果提取失败返回null
     */
    private String extractClobContent(Clob clob) {
        return extractClobContentSafely(clob);
    }

    /**
     * 安全的Clob内容提取方法，包含重试机制
     * 处理Oracle CLOB在网络不稳定时的"标记无效或未设置"异常
     * 
     * @param clob Clob对象
     * @return 字符串内容，如果提取失败返回null
     */
    private String extractClobContentSafely(Clob clob) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (clob != null) {
                    long length = clob.length();
                    if (length > 0) {
                        return clob.getSubString(1, (int) length);
                    }
                }
                return null;
            } catch (Exception e) {
                logger.warn("提取Clob内容失败，第{}次重试: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    logger.error("提取Clob内容最终失败: {}", e.getMessage(), e);
                    return null;
                }
                try {
                    Thread.sleep(100 * attempt); // 指数退避策略
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
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
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });

            // 如果包含顶层error字段，则是真正的错误响应
            if (jsonResponse.containsKey("error")) {
                Object errorObj = jsonResponse.get("error");
                logger.info("检测到JSON错误响应: {}", errorObj);
                return true;
            }

            // 检查其他常见的错误字段
            if (jsonResponse.containsKey("status")
                    && "error".equalsIgnoreCase(String.valueOf(jsonResponse.get("status")))) {
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
            boolean isLikelyError = content.contains("\"error\":") || // JSON格式的错误字段
                    content.contains("\"status\":\"error\"") || // JSON状态错误
                    content.startsWith("{\"error\":") || // 以错误开头的JSON
                    (content.contains("错误") && content.length() < 100) || // 短内容包含"错误"
                    content.contains("调用失败") || // 明确的失败指示
                    content.contains("服务不可用") || // 服务错误
                    content.contains("超时") || // 超时错误
                    content.contains("Timeout") || // 英文超时
                    content.contains("Exception:") || // 异常信息
                    content.contains("java.") || // Java异常
                    content.contains("org.springframework."); // Spring框架异常

            if (isLikelyError) {
                logger.info("备用检测到可能的错误响应，内容前100字符: {}", content.substring(0, Math.min(content.length(), 100)));
            }

            return isLikelyError;
        }
    }

    /**
     * 处理错误响应
     * 
     * @param dataId       数据ID
     * @param promptId     Prompt ID
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
     * 添加幂等性检查，避免重复处理导致的唯一约束冲突
     * 
     * @param promptId      Prompt ID
     * @param resultContent 执行结果内容
     * @return true如果更新成功，false如果更新失败
     */
    private boolean updatePromptStatus(String promptId, String resultContent) {
        try {
            // 幂等性检查：先检查是否已存在相同PROMPTID的记录
            List<PromptResult> existingResults = promptResultRepository.findByPromptId(Integer.parseInt(promptId));
            if (!existingResults.isEmpty()) {
                PromptResult existingResult = existingResults.get(0);
                if (existingResult.getOriginalResultContent() != null &&
                        !existingResult.getOriginalResultContent().trim().isEmpty()) {
                    // 存在有效记录 → 只更新Prompt状态，不重复插入
                    logger.info("检测到已存在的PromptResult记录，Prompt ID: {}，直接更新Prompt状态", promptId);
                    updatePromptStatusOnly(promptId, existingResult.getResultId());
                    return true;
                }
            }

            // 不存在有效记录 → 创建新记录并更新状态
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
     * @param promptId     Prompt ID
     * @param statusName   状态名称
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
                    "更新Prompt错误状态 - Prompt ID: " + promptId);
        } catch (Exception e) {
            logger.error("更新Prompt状态为错误时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("更新Prompt错误状态失败", e);
        }
    }

    /**
     * 更新Prompt状态和执行结果
     * 使用乐观锁重试机制处理并发更新冲突
     * 添加幂等性检查和严格的数据一致性验证，避免重复处理导致的唯一约束冲突
     * 
     * @param promptId      Prompt ID
     * @param statusName    状态名称
     * @param resultContent 执行结果内容
     */
    private void updatePromptStatusWithResult(String promptId, String statusName, String resultContent) {
        try {
            // 幂等性检查：先检查是否已存在相同PROMPTID的记录
            List<PromptResult> existingResults = promptResultRepository.findByPromptId(Integer.parseInt(promptId));
            if (!existingResults.isEmpty()) {
                PromptResult existingResult = existingResults.get(0);
                
                // 数据一致性验证：检查返回的PromptResult是否真的属于查询的PromptId
                if (!existingResult.getPromptId().equals(Integer.parseInt(promptId))) {
                    logger.error("严重数据一致性错误！查询PromptId={} 的PromptResult，但返回的记录PromptId={}，ResultId={}。" +
                            "这表明数据库中存在关联错误或数据损坏。将忽略此错误记录并创建新的PromptResult。", 
                            promptId, existingResult.getPromptId(), existingResult.getResultId());
                    // 不使用错误的记录，继续执行创建新记录的逻辑
                } else if (existingResult.getOriginalResultContent() != null &&
                        !existingResult.getOriginalResultContent().trim().isEmpty()) {
                    // 存在有效记录 → 直接更新Prompt状态，不重复插入
                    logger.info("检测到已存在的有效PromptResult记录，Prompt ID: {}，ResultId: {}，直接更新Prompt状态", 
                            promptId, existingResult.getResultId());
                    updatePromptStatusOnly(promptId, existingResult.getResultId());
                    return; // 关键退出点，避免重复插入
                }
            }

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
                        
                        PromptResult saved = promptResultRepository.save(promptResult);
                        logger.info("成功创建新的PromptResult记录，PromptId={}, ResultId={}", 
                                promptId, saved.getResultId());
                        return saved;
                    },
                    null, // entity refresher
                    "保存PromptResult - Prompt ID: " + promptId);

            // 使用乐观锁重试机制更新Prompt状态
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                    () -> {
                        Optional<Prompt> promptOptional = promptRepository.findById(Integer.parseInt(promptId));
                        if (promptOptional.isPresent()) {
                            Prompt prompt = promptOptional.get();
                            
                            // ResultId冲突检测：检查Prompt是否已经关联了其他ResultId
                            if (prompt.getResultId() != null && !prompt.getResultId().equals(savedResult.getResultId())) {
                                logger.warn("Prompt ID: {} 已经关联了另一个ResultId: {}，现在更新为新的ResultId: {}。" +
                                        "这可能表明之前存在数据错误或重复处理。", 
                                        promptId, prompt.getResultId(), savedResult.getResultId());
                            }
                            
                            prompt.setStatusName("已完成");
                            prompt.setExecutionTime(LocalDateTime.now());
                            prompt.setResultId(savedResult.getResultId());
                            // 不保存到EXECUTION_RESULT字段，只保存到PromptResult表
                            promptRepository.save(prompt);
                            logger.info("Prompt ID: {} 状态已更新为已完成，ResultId: {}", promptId, savedResult.getResultId());
                        } else {
                            logger.warn("未找到对应的Prompt记录，Prompt ID: {}", promptId);
                        }
                        return null;
                    },
                    null, // entity refresher
                    "更新Prompt状态 - Prompt ID: " + promptId);

        } catch (Exception e) {
            logger.error("更新Prompt状态和执行结果时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("更新Prompt状态失败", e);
        }
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
     * 从SENT状态的数据更新Prompt状态
     * 当ENCRYPTED_DATA_TEMP表状态为SENT但Prompt状态尚未更新时调用
     * 
     * @param promptId      Prompt ID
     * @param encryptedData 加密数据记录
     */
    private void updatePromptStatusFromSentData(Integer promptId, EncryptedDataTemp encryptedData) {
        try {
            logger.info("开始从SENT状态数据更新Prompt状态, Prompt ID: {}", promptId);

            // 使用乐观锁重试机制更新Prompt状态
            optimisticLockRetryService.executeWithOptimisticLockRetry(
                    () -> {
                        Optional<Prompt> promptOptional = promptRepository.findById(promptId);
                        if (promptOptional.isPresent()) {
                            Prompt prompt = promptOptional.get();

                            // 检查当前Prompt状态，避免重复更新
                            if (!"已完成".equals(prompt.getStatusName())) {
                                prompt.setStatusName("已完成");
                                prompt.setExecutionTime(LocalDateTime.now());
                                promptRepository.save(prompt);
                                logger.info("Prompt ID: {} 状态已从SENT数据更新为'已完成'", promptId);
                            } else {
                                logger.info("Prompt ID: {} 状态已经是'已完成'，无需更新", promptId);
                            }
                        } else {
                            logger.warn("未找到对应的Prompt记录，Prompt ID: {}", promptId);
                        }
                        return null;
                    },
                    null, // entity refresher
                    "从SENT状态更新Prompt状态 - Prompt ID: " + promptId);
        } catch (Exception e) {
            logger.error("从SENT状态数据更新Prompt状态时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
        }
    }

    /**
     * 在独立事务中更新Prompt状态
     * 使用REQUIRES_NEW传播确保在只读事务中也能正确提交
     * 添加flush()强制将更改写入数据库
     * 
     * @param promptId Prompt ID
     * @param resultId 已存在的PromptResult记录ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePromptStatusInNewTransaction(Integer promptId, Integer resultId) {
        try {
            Optional<Prompt> promptOptional = promptRepository.findById(promptId);
            if (promptOptional.isPresent()) {
                Prompt prompt = promptOptional.get();

                // 检查当前Prompt状态，避免重复更新
                if (!"已完成".equals(prompt.getStatusName())) {
                    prompt.setStatusName("已完成");
                    prompt.setExecutionTime(LocalDateTime.now());
                    prompt.setResultId(resultId);
                    promptRepository.save(prompt);

                    // 强制刷新到数据库，确保更改立即写入
                    promptRepository.flush();
                    logger.info("Prompt ID: {} 状态已更新为'已完成'，使用已存在的Result ID: {} - 已强制刷新到数据库", promptId, resultId);
                } else {
                    logger.info("Prompt ID: {} 状态已经是'已完成'，无需更新", promptId);
                }
            } else {
                logger.warn("未找到对应的Prompt记录，Prompt ID: {}", promptId);
            }
        } catch (Exception e) {
            logger.error("在独立事务中更新Prompt状态时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("独立事务更新Prompt状态失败", e);
        }
    }

    /**
     * 在独立事务中标记数据为已发送状态
     * 使用REQUIRES_NEW传播确保在只读事务中也能正确提交
     * 使用JPQL更新避免实体状态管理问题，不再需要强制flush
     * 
     * @param dataId 数据ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDataAsSentInNewTransaction(String dataId) {
        try {
            logger.info("在独立事务中开始标记数据为已发送状态，数据ID: {}", dataId);

            // 使用增强的JPQL更新方法，避免实体状态管理问题
            encryptedDataTempService.markAsSent(dataId);

            logger.info("数据ID: {} 已通过JPQL更新成功标记为已发送状态，无需强制刷新", dataId);
        } catch (Exception e) {
            logger.error("在独立事务中标记数据为已发送时发生异常 - 数据ID: {}: {}", dataId, e.getMessage(), e);

            // 提供更详细的错误诊断
            if (e.getMessage().contains("ORA-01407") || e.getMessage().contains("NULL")) {
                logger.error("检测到NULL约束违规异常，可能的原因：");
                logger.error("1. 实体在新事务中状态丢失");
                logger.error("2. Hibernate批处理异常");
                logger.error("3. 数据库连接问题");

                // 尝试重新查询验证
                try {
                    Optional<EncryptedDataTemp> recheckData = executionEncryptedDataTempRepository.findById(dataId);
                    if (recheckData.isPresent()) {
                        EncryptedDataTemp data = recheckData.get();
                        logger.error("重新查询结果 - ENCRYPTED_DATA是否为空: {}, 当前状态: {}",
                                data.getEncryptedData() == null, data.getStatus());
                    }
                } catch (Exception recheckEx) {
                    logger.error("重新查询失败: {}", recheckEx.getMessage());
                }
            }

            throw new RuntimeException("独立事务标记数据为已发送失败", e);
        }
    }

    /**
     * 仅更新Prompt状态，不创建新的PromptResult记录
     * 用于幂等性检查中检测到已存在记录时的场景
     * 
     * @param promptId Prompt ID
     * @param resultId 已存在的PromptResult记录ID
     */
    private void updatePromptStatusOnly(String promptId, Integer resultId) {
        try {
            // 使用独立事务更新Prompt状态
            updatePromptStatusInNewTransaction(Integer.parseInt(promptId), resultId);
        } catch (Exception e) {
            logger.error("仅更新Prompt状态时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("仅更新Prompt状态失败", e);
        }
    }

    /**
     * 处理ERROR状态的数据
     * 当ENCRYPTED_DATA_TEMP表状态为ERROR时调用
     * 
     * @param promptId      Prompt ID
     * @param encryptedData 加密数据记录
     */
    private void handleErrorStatus(Integer promptId, EncryptedDataTemp encryptedData) {
        try {
            logger.info("开始处理ERROR状态数据, Prompt ID: {}", promptId);

            // 提取错误信息
            String errorMessage = "执行服务器处理失败";
            if (encryptedData.getErrorMessage() != null) {
                try {
                    String errorContent = extractClobContent(encryptedData.getErrorMessage());
                    if (errorContent != null && !errorContent.trim().isEmpty()) {
                        errorMessage = errorContent;
                    }
                } catch (Exception e) {
                    logger.warn("提取错误信息失败: {}", e.getMessage());
                }
            }

            // 更新Prompt表状态为错误
            updatePromptStatusWithError(promptId.toString(), "执行失败", errorMessage);

            logger.warn("ERROR状态数据处理完成 - Prompt ID: {}", promptId);
        } catch (Exception e) {
            logger.error("处理ERROR状态数据时发生异常 - Prompt ID: {}: {}", promptId, e.getMessage(), e);
        }
    }

    /**
     * 获取当前轮询状态统计
     * 用于监控和调试
     * 
     * @return 轮询状态统计信息
     */
    public String getPollingStats() {
        long encryptedCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED);
        long sentCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.SENT);
        long errorCount = executionEncryptedDataTempRepository.countByStatus(DataStatus.ERROR);

        return String.format("执行服务器轮询状态统计 - 待处理: %d, 已发送: %d, 错误: %d",
                encryptedCount, sentCount, errorCount);
    }

    /**
     * 获取Prompt表状态统计
     * 统计待处理和SUBMISSION_STARTED状态的Prompt记录
     * 
     * @return Prompt状态统计信息
     */
    public String getPromptStatusStats() {
        long pendingCount = promptRepository.countByStatusName("待处理");
        long submissionStartedCount = promptRepository.countByStatusName("SUBMISSION_STARTED");

        return String.format("Prompt状态统计 - 待处理: %d, SUBMISSION_STARTED: %d",
                pendingCount, submissionStartedCount);
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
     * 获取轮询服务状态
     * 
     * @return true如果轮询服务已启用，false如果已禁用
     */
    public boolean isPollingEnabled() {
        return pollingEnabled;
    }

    /**
     * 获取轮询服务配置信息
     * 
     * @return 轮询服务配置信息
     */
    public String getPollingConfigInfo() {
        return String.format("轮询服务配置 - 轮询间隔: %dms, 最大重试次数: %d, 批量大小: %d",
                promptServiceConfig.getPolling().getInterval(),
                promptServiceConfig.getPolling().getMaxRetries(),
                promptServiceConfig.getPolling().getBatchSize());
    }

    /**
     * 检查轮询服务健康状态
     * 
     * @return 健康状态信息
     */
    public String healthCheck() {
        try {
            // 简单的健康检查：验证数据库连接和配置
            long totalPrompts = promptRepository.count();
            boolean configValid = promptServiceConfig.getPolling() != null;

            return String.format("轮询服务健康状态 - 数据库连接: 正常, 配置: %s, 总Prompt数: %d",
                    configValid ? "有效" : "无效", totalPrompts);
        } catch (Exception e) {
            return "轮询服务健康状态 - 异常: " + e.getMessage();
        }
    }
}
