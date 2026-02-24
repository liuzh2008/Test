package com.example.medaiassistant.service;

import com.example.medaiassistant.config.PromptServiceConfig;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.util.AESEncryptionUtil;
import com.example.medaiassistant.model.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Prompt提交服务
 * 负责处理Prompt的提交逻辑，包括状态管理、重试机制和错误处理
 * 
 * 主要功能：
 * 1. 提交Prompt到执行服务器
 * 2. 管理Prompt状态转换
 * 3. 实现重试机制
 * 4. 错误处理和日志记录
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@Service
@EnableScheduling
public class PromptSubmissionService {
    private static final Logger logger = LoggerFactory.getLogger(PromptSubmissionService.class);

    private final PromptRepository promptRepository;
    private final PromptServiceConfig promptServiceConfig;
    private final ServerConfigService serverConfigService;
    private final ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository;

    /**
     * 提交服务启用状态标志
     * 独立控制提交服务的启停
     */
    private volatile boolean submissionEnabled = false;

    /**
     * 构造函数，依赖注入必要的服务实例
     * 
     * @param promptRepository            Prompt数据访问层
     * @param promptServiceConfig         Prompt服务配置
     * @param serverConfigService         服务器配置服务
     * @param encryptedDataTempRepository 加密数据临时表数据访问层
     */
    public PromptSubmissionService(PromptRepository promptRepository,
            PromptServiceConfig promptServiceConfig,
            ServerConfigService serverConfigService,
            ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository) {
        this.promptRepository = promptRepository;
        this.promptServiceConfig = promptServiceConfig;
        this.serverConfigService = serverConfigService;
        this.executionEncryptedDataTempRepository = executionEncryptedDataTempRepository;
    }

    /**
     * 提交Prompt到执行服务器
     * 将Prompt状态更新为"已提交"，并设置提交时间
     * 
     * @param promptId Prompt ID
     * @return 提交结果，true表示成功，false表示失败
     */
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Transactional(transactionManager = "transactionManager")
    public boolean submitPrompt(Integer promptId) {
        try {
            logger.info("开始提交Prompt ID: {}", promptId);

            Optional<Prompt> promptOptional = promptRepository.findById(promptId);
            if (!promptOptional.isPresent()) {
                logger.warn("未找到Prompt ID: {}", promptId);
                return false;
            }

            Prompt prompt = promptOptional.get();

            // 检查当前状态是否允许提交
            if (!isValidStateForSubmission(prompt.getStatusName())) {
                logger.warn("Prompt ID: {} 当前状态为 {}，不允许提交", promptId, prompt.getStatusName());
                return false;
            }

            // 更新Prompt状态为"已提交"
            prompt.setStatusName("已提交");
            prompt.setSubmissionTime(LocalDateTime.now());
            prompt.setProcessingStartTime(LocalDateTime.now());

            // 重置重试计数
            if (prompt.getRetryCount() == null) {
                prompt.setRetryCount(0);
            }

            promptRepository.save(prompt);

            logger.info("Prompt ID: {} 提交成功，状态已更新为'已提交'", promptId);
            return true;

        } catch (Exception e) {
            logger.error("提交Prompt ID: {} 失败: {}", promptId, e.getMessage(), e);
            throw new RuntimeException("提交Prompt失败", e);
        }
    }

    /**
     * 批量提交Prompt
     * 支持批量处理多个Prompt的提交
     * 
     * @param promptIds Prompt ID列表
     * @return 成功提交的数量
     */
    @Transactional(transactionManager = "transactionManager")
    public int batchSubmitPrompts(java.util.List<Integer> promptIds) {
        int successCount = 0;

        for (Integer promptId : promptIds) {
            try {
                if (submitPrompt(promptId)) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("批量提交Prompt ID: {} 失败: {}", promptId, e.getMessage());
                // 继续处理其他Prompt，不中断整个批量操作
            }
        }

        logger.info("批量提交完成 - 总数: {}, 成功: {}", promptIds.size(), successCount);
        return successCount;
    }

    /**
     * 独立的提交任务调度器
     * 定期查询并提交待处理的Prompt
     * 使用配置文件中的间隔时间，实现真正的独立调度
     */
    @Scheduled(fixedDelayString = "${prompt.submission.interval:10000}")
    @Transactional(transactionManager = "transactionManager")
    public void submitPendingPrompts() {
        if (!submissionEnabled || !promptServiceConfig.getSubmission().isEnabled()) {
            logger.debug("提交服务已禁用，跳过执行");
            return;
        }

        try {
            // 分页查询待处理Prompt（状态为"待处理"）
            int pageSize = promptServiceConfig.getSubmission().getPageSize();
            List<Prompt> pendingPrompts = promptRepository.findByStatusName("待处理");

            if (pendingPrompts.isEmpty()) {
                logger.debug("没有待处理的Prompt，跳过执行");
                return;
            }

            // 限制处理数量
            if (pendingPrompts.size() > pageSize) {
                pendingPrompts = pendingPrompts.subList(0, pageSize);
            }

            logger.info("开始执行独立提交任务 - 找到 {} 个待处理的Prompt", pendingPrompts.size());

            int submittedCount = 0;
            for (Prompt prompt : pendingPrompts) {
                try {
                    // 调用提交方法，将状态从"待处理"更新为"已提交"
                    if (submitPromptInternal(prompt)) {
                        submittedCount++;
                    }
                } catch (Exception e) {
                    logger.error("提交Prompt ID: {} 失败: {}", prompt.getPromptId(), e.getMessage());
                }
            }

            logger.info("独立提交任务完成 - 查询到 {} 个待处理Prompt，成功提交 {} 个",
                    pendingPrompts.size(), submittedCount);

        } catch (Exception e) {
            logger.error("独立提交任务执行失败", e);
        }
    }

    /**
     * 内部提交方法
     * 将单个Prompt从"待处理"状态更新为"已提交"状态，并直接保存到ENCRYPTED_DATA_TEMP表
     * 
     * @param prompt 要提交的Prompt对象
     * @return 提交结果，true表示成功，false表示失败
     */
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Transactional(transactionManager = "transactionManager")
    private boolean submitPromptInternal(Prompt prompt) {
        Integer promptId = prompt.getPromptId();

        try {
            logger.info("开始提交Prompt ID: {}", promptId);

            // 检查当前状态是否允许提交
            if (!"待处理".equals(prompt.getStatusName())) {
                logger.warn("Prompt ID: {} 当前状态为 {}，不允许提交", promptId, prompt.getStatusName());
                return false;
            }

            // 1. 组合Prompt内容
            String combinedPrompt = combinePromptContent(prompt);
            logger.debug("组合后的Prompt内容长度: {} 字符", combinedPrompt.length());

            // 2. 加密Prompt内容
            String encryptedData = encryptPrompt(combinedPrompt);
            logger.info("Prompt加密完成, 加密后长度: {} 字符", encryptedData.length());

            // 3. 直接保存到ENCRYPTED_DATA_TEMP表
            boolean saveSuccess = saveToEncryptedDataTemp(encryptedData, promptId);

            if (saveSuccess) {
                // 4. 保存成功，更新Prompt状态为"已提交"
                prompt.setStatusName("已提交");
                prompt.setSubmissionTime(LocalDateTime.now());
                prompt.setProcessingStartTime(LocalDateTime.now());

                // 重置重试计数
                if (prompt.getRetryCount() == null) {
                    prompt.setRetryCount(0);
                }

                promptRepository.save(prompt);

                logger.info("Prompt ID: {} 提交成功，状态已更新为'已提交'", promptId);
                return true;
            } else {
                // 保存失败，保持状态为"待处理"
                logger.error("Prompt ID: {} 保存到ENCRYPTED_DATA_TEMP表失败，保持状态为'待处理'", promptId);
                return false;
            }

        } catch (Exception e) {
            logger.error("提交Prompt ID: {} 失败: {}", promptId, e.getMessage(), e);
            // 保存失败，保持状态为"待处理"
            return false;
        }
    }

    /**
     * 检查Prompt状态是否允许提交
     * 只有"待处理"状态的Prompt才允许提交
     * 
     * @param statusName 当前状态
     * @return true如果允许提交，false否则
     */
    boolean isValidStateForSubmission(String statusName) {
        // 修正：只有"待处理"状态的Prompt才允许提交
        return "待处理".equals(statusName);
    }

    /**
     * 启用提交服务
     */
    public void enableSubmission() {
        this.submissionEnabled = true;
        logger.info("提交服务已启用");
    }

    /**
     * 禁用提交服务
     */
    public void disableSubmission() {
        this.submissionEnabled = false;
        logger.info("提交服务已禁用");
    }

    /**
     * 检查提交服务是否启用
     */
    public boolean isSubmissionEnabled() {
        return submissionEnabled;
    }

    /**
     * 获取待提交的Prompt数量
     * 统计状态为"待处理"的Prompt数量
     * 
     * @return 待提交的Prompt数量
     */
    public long getPendingSubmissionCount() {
        try {
            return promptRepository.countByStatusName("待处理");
        } catch (Exception e) {
            logger.error("获取待提交Prompt数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取提交统计信息
     * 
     * @return 提交统计信息
     */
    public String getSubmissionStats() {
        long pendingCount = getPendingSubmissionCount();
        long submittedCount = promptRepository.countByStatusName("已提交");

        return String.format("提交统计 - 待处理: %d, 已提交: %d", pendingCount, submittedCount);
    }

    /**
     * 获取提交服务配置信息
     * 
     * @return 提交服务配置信息
     */
    public String getSubmissionConfigInfo() {
        return String.format("提交服务配置 - 最大重试次数: %d, 重试间隔: %dms",
                promptServiceConfig.getSubmission().getMaxRetries(),
                promptServiceConfig.getSubmission().getRetryInterval());
    }

    /**
     * 检查提交服务健康状态
     * 
     * @return 健康状态信息
     */
    public String healthCheck() {
        try {
            // 简单的健康检查：验证数据库连接和配置
            long totalPrompts = promptRepository.count();
            boolean configValid = promptServiceConfig.getSubmission() != null;

            return String.format("提交服务健康状态 - 数据库连接: 正常, 配置: %s, 总Prompt数: %d",
                    configValid ? "有效" : "无效", totalPrompts);
        } catch (Exception e) {
            return "提交服务健康状态 - 异常: " + e.getMessage();
        }
    }

    /**
     * 组合Prompt内容
     * 
     * @param prompt Prompt对象
     * @return 组合后的Prompt内容
     */
    private String combinePromptContent(Prompt prompt) {
        StringBuilder combined = new StringBuilder();

        if (prompt.getObjectiveContent() != null) {
            combined.append(prompt.getObjectiveContent()).append("\n");
        }

        if (prompt.getDailyRecords() != null) {
            combined.append(prompt.getDailyRecords()).append("\n");
        }

        if (prompt.getPromptTemplateContent() != null) {
            combined.append(prompt.getPromptTemplateContent());
        }

        return combined.toString();
    }

    /**
     * 加密Prompt内容
     * 
     * @param plainPrompt 明文Prompt内容
     * @return 加密后的数据
     * @throws Exception 加密失败时抛出异常
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
     * 保存加密数据到ENCRYPTED_DATA_TEMP表
     * 
     * @param encryptedData 加密后的数据
     * @param promptId      Prompt ID
     * @return 保存结果，true表示成功，false表示失败
     */
    @Transactional(transactionManager = "executionTransactionManager", propagation = Propagation.REQUIRES_NEW)
    private boolean saveToEncryptedDataTemp(String encryptedData, Integer promptId) {
        try {
            String requestId = "cdwyy" + promptId;

            // 检查是否已存在相同requestId的记录
            Optional<EncryptedDataTemp> existingRecord = executionEncryptedDataTempRepository.findByRequestId(requestId);
            if (existingRecord.isPresent()) {
                logger.warn("执行服务器ENCRYPTED_DATA_TEMP表中已存在requestId为 {} 的记录，跳过保存", requestId);
                return true; // 视为成功，避免重复创建
            }

            // 创建新的EncryptedDataTemp记录
            EncryptedDataTemp encryptedDataTemp = new EncryptedDataTemp();
            encryptedDataTemp.setId(requestId);
            encryptedDataTemp.setRequestId(requestId);
            encryptedDataTemp.setSource("PROMPT_SUBMISSION_SERVICE");
            encryptedDataTemp.setStatus(DataStatus.RECEIVED);

            // 将加密数据转换为Clob
            try {
                logger.info("创建执行服务器ENCRYPTED_DATA_TEMP记录，requestId: {}, 数据长度: {}", requestId, encryptedData.length());

                // 临时解决方案：创建一个简单的Clob对象
                // 注意：这只是一个临时解决方案，实际生产环境需要正确的Clob实现
                java.sql.Clob clob = new javax.sql.rowset.serial.SerialClob(encryptedData.toCharArray());
                encryptedDataTemp.setEncryptedData(clob);

            } catch (Exception e) {
                logger.error("创建Clob对象失败: {}", e.getMessage());
                return false;
            }

            // 保存到执行服务器数据库
            executionEncryptedDataTempRepository.save(encryptedDataTemp);

            logger.info("成功保存到执行服务器ENCRYPTED_DATA_TEMP表，requestId: {}, Prompt ID: {}", requestId, promptId);
            return true;

        } catch (Exception e) {
            logger.error("保存到执行服务器ENCRYPTED_DATA_TEMP表失败, Prompt ID: {}, 错误: {}", promptId, e.getMessage());
            return false;
        }
    }
}
