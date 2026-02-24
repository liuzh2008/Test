package com.example.medaiassistant.util;

import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 重复数据清理工具
 * 用于清理ENCRYPTED_DATA_TEMP表中的重复REQUEST_ID记录
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-30
 */
@Component
public class DuplicateDataCleanupUtil {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDataCleanupUtil.class);

    private final EncryptedDataTempRepository encryptedDataTempRepository;

    public DuplicateDataCleanupUtil(EncryptedDataTempRepository encryptedDataTempRepository) {
        this.encryptedDataTempRepository = encryptedDataTempRepository;
    }

    /**
     * 清理所有重复的REQUEST_ID记录
     * 对于每个重复的REQUEST_ID，保留最新创建的记录，删除其他记录
     * 
     * @return 清理的重复记录数量
     */
    @Transactional
    public int cleanupAllDuplicateRequestIds() {
        logger.info("开始清理重复的REQUEST_ID记录...");

        try {
            // 查询所有记录
            List<EncryptedDataTemp> allRecords = encryptedDataTempRepository.findAll();

            if (allRecords.isEmpty()) {
                logger.info("没有找到任何记录，无需清理");
                return 0;
            }

            logger.info("找到 {} 条记录，开始分析重复数据", allRecords.size());

            // 按REQUEST_ID分组
            Map<String, List<EncryptedDataTemp>> groupedRecords = allRecords.stream()
                    .filter(record -> record.getRequestId() != null && !record.getRequestId().trim().isEmpty())
                    .collect(Collectors.groupingBy(EncryptedDataTemp::getRequestId));

            int totalDuplicates = 0;

            // 处理每个REQUEST_ID组
            for (Map.Entry<String, List<EncryptedDataTemp>> entry : groupedRecords.entrySet()) {
                String requestId = entry.getKey();
                List<EncryptedDataTemp> records = entry.getValue();

                if (records.size() > 1) {
                    logger.warn("发现重复的REQUEST_ID: {} - 共 {} 条记录", requestId, records.size());
                    int cleaned = cleanupDuplicateRecords(requestId, records);
                    totalDuplicates += cleaned;
                }
            }

            logger.info("重复数据清理完成，共清理 {} 条重复记录", totalDuplicates);
            return totalDuplicates;

        } catch (Exception e) {
            logger.error("清理重复数据时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("清理重复数据失败", e);
        }
    }

    /**
     * 清理指定REQUEST_ID的重复记录
     * 保留最新创建的记录，删除其他记录
     * 
     * @param requestId 请求ID
     * @param records   该REQUEST_ID对应的所有记录
     * @return 清理的记录数量
     */
    private int cleanupDuplicateRecords(String requestId, List<EncryptedDataTemp> records) {
        if (records.size() <= 1) {
            return 0;
        }

        try {
            // 按创建时间排序，选择最新的记录
            EncryptedDataTemp latestRecord = records.stream()
                    .filter(record -> record.getCreatedAt() != null)
                    .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                    .orElse(records.get(0)); // 如果没有创建时间，选择第一条

            logger.info("保留最新记录 - ID: {}, REQUEST_ID: {}, 创建时间: {}",
                    latestRecord.getId(), requestId, latestRecord.getCreatedAt());

            int deletedCount = 0;

            // 删除其他重复记录
            for (EncryptedDataTemp record : records) {
                if (!record.getId().equals(latestRecord.getId())) {
                    try {
                        encryptedDataTempRepository.delete(record);
                        logger.info("已删除重复记录 - ID: {}, REQUEST_ID: {}, 创建时间: {}",
                                record.getId(), requestId, record.getCreatedAt());
                        deletedCount++;
                    } catch (Exception deleteException) {
                        logger.error("删除重复记录失败 - ID: {}, 错误: {}",
                                record.getId(), deleteException.getMessage());
                    }
                }
            }

            return deletedCount;

        } catch (Exception e) {
            logger.error("清理REQUEST_ID: {} 的重复记录时发生异常: {}", requestId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 检查是否存在重复的REQUEST_ID
     * 
     * @return 重复的REQUEST_ID数量
     */
    public int checkDuplicateRequestIds() {
        try {
            List<EncryptedDataTemp> allRecords = encryptedDataTempRepository.findAll();

            Map<String, List<EncryptedDataTemp>> groupedRecords = allRecords.stream()
                    .filter(record -> record.getRequestId() != null && !record.getRequestId().trim().isEmpty())
                    .collect(Collectors.groupingBy(EncryptedDataTemp::getRequestId));

            int duplicateGroups = 0;

            for (Map.Entry<String, List<EncryptedDataTemp>> entry : groupedRecords.entrySet()) {
                if (entry.getValue().size() > 1) {
                    duplicateGroups++;
                    logger.warn("发现重复REQUEST_ID: {} - {} 条记录",
                            entry.getKey(), entry.getValue().size());
                }
            }

            if (duplicateGroups == 0) {
                logger.info("未发现重复的REQUEST_ID记录");
            } else {
                logger.warn("总共发现 {} 个重复的REQUEST_ID", duplicateGroups);
            }

            return duplicateGroups;

        } catch (Exception e) {
            logger.error("检查重复REQUEST_ID时发生异常: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 清理指定的REQUEST_ID重复记录
     * 
     * @param requestId 要清理的REQUEST_ID
     * @return 清理的记录数量
     */
    @Transactional
    public int cleanupSpecificRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            logger.warn("REQUEST_ID为空，无法清理");
            return 0;
        }

        try {
            List<EncryptedDataTemp> records = encryptedDataTempRepository.findAllByRequestId(requestId);

            if (records.size() <= 1) {
                logger.info("REQUEST_ID: {} 没有重复记录", requestId);
                return 0;
            }

            logger.info("开始清理REQUEST_ID: {} 的重复记录，共 {} 条", requestId, records.size());
            return cleanupDuplicateRecords(requestId, records);

        } catch (Exception e) {
            logger.error("清理REQUEST_ID: {} 时发生异常: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("清理指定REQUEST_ID失败", e);
        }
    }

    /**
     * 获取重复数据清理统计报告
     * 
     * @return 清理统计报告
     */
    public String getDuplicateCleanupReport() {
        try {
            List<EncryptedDataTemp> allRecords = encryptedDataTempRepository.findAll();

            Map<String, List<EncryptedDataTemp>> groupedRecords = allRecords.stream()
                    .filter(record -> record.getRequestId() != null && !record.getRequestId().trim().isEmpty())
                    .collect(Collectors.groupingBy(EncryptedDataTemp::getRequestId));

            int totalRecords = allRecords.size();
            int uniqueRequestIds = groupedRecords.size();
            int duplicateGroups = 0;
            int totalDuplicateRecords = 0;

            for (List<EncryptedDataTemp> records : groupedRecords.values()) {
                if (records.size() > 1) {
                    duplicateGroups++;
                    totalDuplicateRecords += (records.size() - 1); // 减去要保留的1条记录
                }
            }

            return String.format(
                    "重复数据统计报告:\n" +
                            "- 总记录数: %d\n" +
                            "- 唯一REQUEST_ID数: %d\n" +
                            "- 重复REQUEST_ID组数: %d\n" +
                            "- 可清理的重复记录数: %d",
                    totalRecords, uniqueRequestIds, duplicateGroups, totalDuplicateRecords);

        } catch (Exception e) {
            logger.error("生成清理报告时发生异常: {}", e.getMessage(), e);
            return "生成清理报告失败: " + e.getMessage();
        }
    }
}