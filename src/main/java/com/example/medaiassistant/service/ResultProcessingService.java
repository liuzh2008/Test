package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.util.AESEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialClob;
import java.sql.Clob;
import java.util.Random;
import java.util.UUID;

/**
 * 结果处理与存储服务
 * 处理LLM分析结果，进行加密存储和状态管理
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-11
 */
@Service
public class ResultProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ResultProcessingService.class);

    @Autowired
    private EncryptedDataTempService encryptedDataTempService;

    @Autowired
    private ServerConfigService serverConfigService;

    private final Random random = new Random();

    /**
     * 处理LLM分析结果并保存到临时表
     * 
     * @param requestId 请求ID
     * @param source    数据来源
     * @param llmResult LLM分析结果（JSON字符串）
     * @return 处理后的数据ID
     */
    public String processAndStoreResult(String requestId, String source, String llmResult) {
        try {
            logger.info("开始处理LLM分析结果，请求ID: {}, 来源: {}", requestId, source);

            // 1. 加密处理结果
            String encryptedResult = encryptLlmResult(llmResult);

            // 2. 创建Clob对象
            Clob encryptedClob = new SerialClob(encryptedResult.toCharArray());

            // 3. 保存到临时表
            EncryptedDataTemp savedData = encryptedDataTempService.createEncryptedData(
                    encryptedClob, requestId, source);

            // 4. 开始处理
            encryptedDataTempService.startProcessing(savedData.getId());

            // 5. 模拟处理过程（后续替换为真实处理逻辑）
            boolean processSuccess = simulateLlmResultProcessing();

            if (processSuccess) {
                // 6. 处理成功，将LLM结果保存到EXECUTION_RESULT字段并更新状态为PROCESSED
                Clob executionResultClob = new SerialClob(llmResult.toCharArray());
                encryptedDataTempService.completeProcessing(savedData.getId(), executionResultClob);
                logger.info("LLM分析结果处理成功，数据ID: {}, 执行结果已保存", savedData.getId());
                return savedData.getId();
            } else {
                // 7. 处理失败，更新状态为ERROR
                String errorMessage = "LLM分析结果处理失败：模拟处理错误";
                encryptedDataTempService.markAsError(savedData.getId(), errorMessage);
                logger.error("LLM分析结果处理失败，数据ID: {}, 错误: {}", savedData.getId(), errorMessage);
                throw new RuntimeException(errorMessage);
            }

        } catch (Exception e) {
            logger.error("处理LLM分析结果时发生异常，请求ID: {}, 错误: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("处理LLM分析结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加密LLM分析结果
     * 
     * @param llmResult LLM分析结果JSON字符串
     * @return 加密后的字符串
     */
    String encryptLlmResult(String llmResult) {
        try {
            // 从数据库配置获取加密密钥和盐值
            String encryptionKey = serverConfigService.getAesEncryptionKey();
            String encryptionSalt = serverConfigService.getAesEncryptionSalt();

            if (encryptionKey == null || encryptionKey.isEmpty() ||
                    encryptionSalt == null || encryptionSalt.isEmpty()) {
                throw new IllegalStateException("AES加密配置未正确设置");
            }

            String encrypted = AESEncryptionUtil.encrypt(llmResult, encryptionKey, encryptionSalt);
            logger.debug("LLM分析结果加密成功，原始长度: {}, 加密后长度: {}",
                    llmResult.length(), encrypted.length());

            return encrypted;
        } catch (Exception e) {
            logger.error("加密LLM分析结果失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模拟LLM分析结果处理（暂时使用模拟数据）
     * 后续替换为真实的LLM分析结果处理逻辑
     * 
     * @return 模拟的LLM分析结果JSON字符串
     */
    public String generateMockLlmResult() {
        // 模拟不同的分析结果类型
        String[] resultTypes = {
                "diagnosis_analysis",
                "treatment_recommendation",
                "risk_assessment",
                "medication_suggestion"
        };

        String resultType = resultTypes[random.nextInt(resultTypes.length)];

        // 生成模拟的LLM分析结果JSON
        return String.format("""
                {
                    "analysis_type": "%s",
                    "request_id": "%s",
                    "timestamp": "%s",
                    "confidence_score": %.2f,
                    "findings": [
                        {
                            "category": "clinical",
                            "description": "基于患者症状和检查结果的分析",
                            "severity": "moderate",
                            "recommendations": ["进一步检查", "专科会诊"]
                        }
                    ],
                    "summary": "这是一个模拟的LLM分析结果，用于测试结果处理流程",
                    "metadata": {
                        "model_version": "1.0.0",
                        "processing_time_ms": %d,
                        "language": "zh-CN"
                    }
                }
                """, resultType, UUID.randomUUID().toString(),
                java.time.Instant.now().toString(),
                random.nextDouble() * 0.5 + 0.5, // 0.5-1.0之间的置信度
                random.nextInt(1000) + 500); // 500-1500ms的处理时间
    }

    /**
     * 模拟处理过程（90%成功率）
     * 
     * @return 处理是否成功
     */
    private boolean simulateLlmResultProcessing() {
        // 模拟90%的成功率
        boolean success = random.nextDouble() < 0.9;

        if (!success) {
            logger.warn("模拟处理失败：随机生成的处理错误");
        }

        return success;
    }

    /**
     * 处理模拟的LLM分析结果（完整流程）
     * 
     * @param requestId 请求ID
     * @param source    数据来源
     * @return 处理后的数据ID
     */
    public String processMockLlmResult(String requestId, String source) {
        String mockResult = generateMockLlmResult();
        return processAndStoreResult(requestId, source, mockResult);
    }

    /**
     * 获取处理结果（解密后的数据）
     * 
     * @param dataId 数据ID
     * @return 解密后的LLM分析结果
     */
    public String getDecryptedResult(String dataId) {
        try {
            // 从数据库获取加密数据
            EncryptedDataTemp data = encryptedDataTempService.findById(dataId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到数据ID: " + dataId));

            if (data.getStatus() != DataStatus.PROCESSED) {
                throw new IllegalStateException("数据尚未处理完成，当前状态: " + data.getStatus());
            }

            // 获取加密配置
            String encryptionKey = serverConfigService.getAesEncryptionKey();
            String encryptionSalt = serverConfigService.getAesEncryptionSalt();

            // 读取加密数据
            String encryptedData = data.getEncryptedData().getSubString(1, (int) data.getEncryptedData().length());

            // 解密数据
            String decryptedResult = AESEncryptionUtil.decrypt(encryptedData, encryptionKey, encryptionSalt);

            logger.info("成功解密LLM分析结果，数据ID: {}", dataId);
            return decryptedResult;

        } catch (Exception e) {
            logger.error("解密LLM分析结果失败，数据ID: {}, 错误: {}", dataId, e.getMessage(), e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查处理状态
     * 
     * @param dataId 数据ID
     * @return 当前状态
     */
    public DataStatus checkProcessingStatus(String dataId) {
        return encryptedDataTempService.getCurrentStatus(dataId)
                .orElseThrow(() -> new IllegalArgumentException("未找到数据ID: " + dataId));
    }

    /**
     * 批量处理多个LLM分析结果
     * 
     * @param requestId  请求ID
     * @param source     数据来源
     * @param llmResults LLM分析结果列表
     * @return 成功处理的数据ID列表
     */
    public java.util.List<String> batchProcessResults(String requestId, String source,
            java.util.List<String> llmResults) {
        java.util.List<String> processedIds = new java.util.ArrayList<>();

        for (int i = 0; i < llmResults.size(); i++) {
            try {
                String individualRequestId = requestId + "_" + (i + 1);
                String dataId = processAndStoreResult(individualRequestId, source, llmResults.get(i));
                processedIds.add(dataId);
                logger.info("批量处理进度: {}/{} 完成", i + 1, llmResults.size());
            } catch (Exception e) {
                logger.error("批量处理第 {} 个结果失败: {}", i + 1, e.getMessage());
                // 继续处理其他结果
            }
        }

        return processedIds;
    }
}
