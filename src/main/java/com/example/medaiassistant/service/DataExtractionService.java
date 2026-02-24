package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 数据提取与预处理服务
 * 负责从临时表提取已解密数据并进行预处理，包括数据验证、状态更新和内容预处理
 * 
 * @module 模块3：数据提取与预处理模块
 * @function 从临时表提取已解密数据，添加数据验证机制，更新状态为PROCESSING
 * @author System
 * @version 1.0
 * @since 2025-09-11
 */
@Service
public class DataExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(DataExtractionService.class);

    @Autowired
    private EncryptedDataTempRepository encryptedDataTempRepository;

    @Autowired
    private EncryptedDataTempService encryptedDataTempService;

    /**
     * 提取所有已解密的数据记录
     * 
     * @return 已解密的数据记录列表
     */
    public List<EncryptedDataTemp> extractAllDecryptedData() {
        List<EncryptedDataTemp> decryptedDataList = encryptedDataTempRepository.findByStatus(DataStatus.DECRYPTED);
        logger.info("提取到 {} 条已解密数据记录", decryptedDataList.size());
        return decryptedDataList;
    }

    /**
     * 提取并预处理单个数据记录
     * 
     * @param id 数据记录ID
     * @return 预处理后的数据内容
     * @throws IllegalArgumentException 如果数据不存在或状态不正确
     * @throws IllegalStateException 如果数据验证失败
     */
    @Transactional
    public String extractAndPreprocessData(String id) {
        Optional<EncryptedDataTemp> optionalData = encryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        
        // 验证数据状态
        if (data.getStatus() != DataStatus.DECRYPTED) {
            throw new IllegalStateException("数据状态不正确，期望: DECRYPTED, 实际: " + data.getStatus());
        }

        // 数据验证
        validateData(data);

        // 提取解密数据
        String decryptedContent = extractDecryptedContent(data);
        
        // 预处理数据
        String preprocessedContent = preprocessData(decryptedContent);
        
        // 更新状态为处理中
        encryptedDataTempService.startProcessing(id);
        
        logger.info("数据提取与预处理完成 - ID: {}, 原始长度: {}, 预处理后长度: {}", 
                id, decryptedContent.length(), preprocessedContent.length());
        
        return preprocessedContent;
    }

    /**
     * 批量提取并预处理数据
     * 
     * @param ids 数据记录ID列表
     * @return 预处理后的数据内容列表
     */
    @Transactional
    public List<String> batchExtractAndPreprocessData(List<String> ids) {
        return ids.stream()
                .map(this::extractAndPreprocessData)
                .toList();
    }

    /**
     * 数据验证
     * 
     * @param data 加密数据临时记录
     * @throws IllegalStateException 如果数据验证失败
     */
    private void validateData(EncryptedDataTemp data) {
        // 1. 检查解密数据是否存在
        if (data.getDecryptedData() == null) {
            throw new IllegalStateException("解密数据为空");
        }

        // 2. 检查请求ID是否存在
        if (data.getRequestId() == null || data.getRequestId().trim().isEmpty()) {
            throw new IllegalStateException("请求ID为空");
        }

        // 3. 检查数据来源是否存在
        if (data.getSource() == null || data.getSource().trim().isEmpty()) {
            throw new IllegalStateException("数据来源为空");
        }

        // 4. 检查解密数据内容是否有效
        try {
            String content = data.getDecryptedData().getSubString(1, (int) data.getDecryptedData().length());
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalStateException("解密数据内容为空");
            }
            
            // 5. 检查内容长度（最小长度验证）
            if (content.length() < 10) {
                throw new IllegalStateException("解密数据内容过短，长度: " + content.length());
            }
            
            // 6. 检查内容格式（基本格式验证）
            if (!isValidContentFormat(content)) {
                throw new IllegalStateException("解密数据格式无效");
            }
            
        } catch (SQLException e) {
            throw new IllegalStateException("读取解密数据时发生SQL异常: " + e.getMessage());
        }

        logger.debug("数据验证通过 - ID: {}, RequestId: {}", data.getId(), data.getRequestId());
    }

    /**
     * 检查内容格式是否有效
     * 
     * @param content 数据内容
     * @return true如果格式有效，false否则
     */
    private boolean isValidContentFormat(String content) {
        // 基本格式验证：检查是否包含常见医疗数据标记
        boolean hasJsonStructure = content.contains("{") && content.contains("}");
        boolean hasXmlStructure = content.contains("<") && content.contains(">");
        boolean hasMedicalKeywords = content.toLowerCase().contains("patient") || 
                                   content.toLowerCase().contains("medical") ||
                                   content.toLowerCase().contains("diagnosis");
        
        // 至少满足一种格式要求
        return hasJsonStructure || hasXmlStructure || hasMedicalKeywords;
    }

    /**
     * 提取解密数据内容
     * 
     * @param data 加密数据临时记录
     * @return 解密后的数据内容字符串
     */
    private String extractDecryptedContent(EncryptedDataTemp data) {
        try {
            Clob decryptedClob = data.getDecryptedData();
            if (decryptedClob == null) {
                throw new IllegalStateException("解密数据Clob为空");
            }
            
            long length = decryptedClob.length();
            if (length > Integer.MAX_VALUE) {
                throw new IllegalStateException("解密数据过长，无法处理");
            }
            
            String content = decryptedClob.getSubString(1, (int) length);
            if (content == null) {
                throw new IllegalStateException("解密数据内容为null");
            }
            
            return content;
            
        } catch (SQLException e) {
            throw new IllegalStateException("提取解密数据时发生SQL异常: " + e.getMessage());
        }
    }

    /**
     * 数据预处理
     * 
     * @param content 原始数据内容
     * @return 预处理后的数据内容
     */
    private String preprocessData(String content) {
        // 1. 去除首尾空白
        String processed = content.trim();
        
        // 2. 标准化换行符
        processed = processed.replace("\r\n", "\n").replace("\r", "\n");
        
        // 3. 移除多余的空行和空白
        processed = processed.replaceAll("(?m)^[ \t]+", ""); // 移除行首空白
        processed = processed.replaceAll("\\n{3,}", "\n\n"); // 限制连续空行
        
        // 4. 编码处理（确保UTF-8）
        processed = ensureUtf8Encoding(processed);
        
        // 5. 特殊字符处理
        processed = processSpecialCharacters(processed);
        
        logger.debug("数据预处理完成 - 原始长度: {}, 处理后长度: {}", content.length(), processed.length());
        return processed;
    }

    /**
     * 确保UTF-8编码
     * 
     * @param content 原始内容
     * @return UTF-8编码的内容
     */
    private String ensureUtf8Encoding(String content) {
        try {
            // 检查是否为有效的UTF-8字符串
            byte[] bytes = content.getBytes("UTF-8");
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            logger.warn("UTF-8编码处理失败，使用原始内容: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 处理特殊字符
     * 
     * @param content 原始内容
     * @return 处理后的内容
     */
    private String processSpecialCharacters(String content) {
        // 替换控制字符（除了换行和制表符）
        String processed = content.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        
        // 处理常见的XML/HTML实体 - 简化版本，避免语法错误
        processed = processed.replace("&", "&")
                           .replace("<", "<")
                           .replace(">", ">");
        
        return processed;
    }

    /**
     * 获取需要处理的数据记录数量
     * 
     * @return 已解密待处理的数据记录数量
     */
    public long getPendingProcessingCount() {
        return encryptedDataTempRepository.countByStatus(DataStatus.DECRYPTED);
    }

    /**
     * 检查是否有待处理的数据
     * 
     * @return true如果有待处理数据，false否则
     */
    public boolean hasPendingData() {
        return getPendingProcessingCount() > 0;
    }
}
