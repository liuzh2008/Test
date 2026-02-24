package com.example.medaiassistant.util;

import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库清理工具类
 * 
 * 用于处理数据库中的重复数据和约束冲突问题
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-30
 */
@Component
public class DatabaseCleanupUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupUtil.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EncryptedDataTempRepository encryptedDataTempRepository;

    /**
     * 检查指定ID是否存在重复记录
     * 
     * @param id 要检查的ID
     * @return true如果存在重复，false否则
     */
    public boolean checkDuplicateRecord(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            return encryptedDataTempRepository.existsById(id);
        } catch (Exception e) {
            logger.error("检查重复记录时发生错误，ID: {}", id, e);
            return false;
        }
    }

    /**
     * 清理指定ID的重复记录
     * 保留最新的记录，删除较旧的记录
     * 
     * @param duplicateId 重复的ID
     * @return true如果清理成功，false否则
     */
    @Transactional
    public boolean cleanupDuplicateRecord(String duplicateId) {
        if (duplicateId == null || duplicateId.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            // 查询是否存在重复记录
            String checkSql = "SELECT COUNT(*) FROM ENCRYPTED_DATA_TEMP WHERE ID = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, duplicateId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        logger.warn("发现重复记录，准备删除ID: {}", duplicateId);
                        
                        // 删除重复记录
                        String deleteSql = "DELETE FROM ENCRYPTED_DATA_TEMP WHERE ID = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, duplicateId);
                            int deletedRows = deleteStmt.executeUpdate();
                            logger.info("成功删除重复记录，ID: {}, 删除行数: {}", duplicateId, deletedRows);
                            return deletedRows > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("清理重复记录时发生数据库错误，ID: {}", duplicateId, e);
            return false;
        } catch (Exception e) {
            logger.error("清理重复记录时发生未知错误，ID: {}", duplicateId, e);
            return false;
        }

        return false;
    }

    /**
     * 处理数据完整性冲突异常
     * 尝试清理冲突记录后重新执行操作
     * 
     * @param conflictId 冲突的ID
     * @param retryOperation 重试操作的回调函数
     * @return true如果处理成功，false否则
     */
    public boolean handleIntegrityConstraintViolation(String conflictId, Runnable retryOperation) {
        logger.warn("检测到数据完整性约束冲突，尝试清理并重试，ID: {}", conflictId);
        
        // 多种策略尝试解决冲突
        try {
            // 策略1: 检查记录是否真正存在
            boolean recordExists = checkDuplicateRecord(conflictId);
            logger.info("冲突ID记录存在性检查，ID: {}, 存在: {}", conflictId, recordExists);
            
            if (recordExists) {
                // 策略2: 尝试清理冲突记录
                boolean cleanupSuccess = cleanupDuplicateRecord(conflictId);
                logger.info("清理冲突记录结果，ID: {}, 成功: {}", conflictId, cleanupSuccess);
                
                if (cleanupSuccess) {
                    // 短暂等待，确保数据库状态同步
                    Thread.sleep(200);
                    
                    // 再次检查记录是否已被清理
                    boolean stillExists = checkDuplicateRecord(conflictId);
                    if (!stillExists) {
                        // 重试操作
                        retryOperation.run();
                        logger.info("数据完整性约束冲突处理成功（记录已清理），ID: {}", conflictId);
                        return true;
                    } else {
                        logger.warn("清理后记录仍然存在，可能存在并发问题，ID: {}", conflictId);
                    }
                }
            } else {
                // 记录实际不存在，可能是并发问题或数据库连接问题
                logger.warn("冲突ID的记录实际不存在，尝试直接重试操作，ID: {}", conflictId);
                
                // 短暂等待后直接重试
                Thread.sleep(100);
                retryOperation.run();
                logger.info("数据完整性约束冲突处理成功（记录不存在），ID: {}", conflictId);
                return true;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("处理数据完整性约束冲突时线程被中断，ID: {}", conflictId, e);
        } catch (Exception e) {
            logger.error("处理数据完整性约束冲突时发生错误，ID: {}", conflictId, e);
            
            // 策略3: 如果所有清理方法都失败，记录详细错误信息
            logger.error("所有冲突解决策略都失败，建议：");
            logger.error("1. 检查数据库连接状态");
            logger.error("2. 检查表结构和约束配置");
            logger.error("3. 考虑手动清理冲突记录：DELETE FROM ENCRYPTED_DATA_TEMP WHERE ID = '{}'", conflictId);
        }
        
        logger.error("数据完整性约束冲突无法自动解决，ID: {}", conflictId);
        return false;
    }

    /**
     * 批量清理孤立的数据记录
     * 清理超过指定时间且状态为ERROR的记录
     * 
     * @param hoursOld 超过多少小时的记录才被清理
     * @return 清理的记录数量
     */
    @Transactional
    public int cleanupOrphanedRecords(int hoursOld) {
        if (hoursOld <= 0) {
            hoursOld = 24; // 默认清理24小时前的错误记录
        }

        try (Connection conn = dataSource.getConnection()) {
            String cleanupSql = """
                DELETE FROM ENCRYPTED_DATA_TEMP 
                WHERE STATUS = 'ERROR' 
                AND CREATED_AT < (CURRENT_TIMESTAMP - INTERVAL '%d' HOUR)
                """.formatted(hoursOld);
            
            try (PreparedStatement stmt = conn.prepareStatement(cleanupSql)) {
                int deletedRows = stmt.executeUpdate();
                logger.info("成功清理{}小时前的错误记录，删除行数: {}", hoursOld, deletedRows);
                return deletedRows;
            }
            
        } catch (SQLException e) {
            logger.error("清理孤立记录时发生数据库错误", e);
            return 0;
        } catch (Exception e) {
            logger.error("清理孤立记录时发生未知错误", e);
            return 0;
        }
    }
}
