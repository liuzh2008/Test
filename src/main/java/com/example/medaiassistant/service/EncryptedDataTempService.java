package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.Clob;
import java.util.Optional;

/**
 * 加密数据临时表服务类
 * 处理状态流转、事务管理和日志记录
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-11
 */
@Service
public class EncryptedDataTempService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptedDataTempService.class);

    @Autowired
    private ExecutionServerEncryptedDataTempRepository executionEncryptedDataTempRepository;

    @Autowired
    @Qualifier("executionEntityManagerFactory")
    private EntityManager entityManager;

    /**
     * 创建新的加密数据记录
     * 
     * @param encryptedData 加密数据Clob对象
     * @param requestId     请求ID
     * @param source        数据来源
     * @return 保存后的EncryptedDataTemp对象
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp createEncryptedData(Clob encryptedData, String requestId, String source) {
        EncryptedDataTemp data = new EncryptedDataTemp();

        // 生成唯一ID - 优先使用requestId，否则生成新的ID
        String uniqueId = generateUniqueDataId(requestId);
        data.setId(uniqueId);
        data.setEncryptedData(encryptedData);
        data.setRequestId(requestId);
        data.setSource(source);
        data.setStatus(DataStatus.RECEIVED);

        EncryptedDataTemp savedData = executionEncryptedDataTempRepository.save(data);
        logger.info("创建加密数据记录成功，ID: {}, 状态: {}", savedData.getId(), DataStatus.RECEIVED);
        return savedData;
    }

    /**
     * 更新数据状态（带事务管理和缓存清理机制）
     * 使用统一的缓存管理策略：操作前清理缓存 -> 验证状态转换 -> 使用JPQL更新 -> 操作后清理缓存
     * 
     * @param id           数据ID
     * @param targetStatus 目标状态
     * @param errorMessage 错误信息（可选）
     * @return 更新后的数据
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果状态转换不允许
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp updateStatus(String id, DataStatus targetStatus, String errorMessage) {
        logger.info("开始使用JPQL更新数据状态，强制清理Hibernate缓存，ID: {}, 目标状态: {}", id, targetStatus);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体进行验证
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：验证状态转换是否允许
        if (!currentStatus.canTransitionTo(targetStatus)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 %s", currentStatus, targetStatus);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第四步：严格验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("ENCRYPTED_DATA字段为空，无法进行状态更新，这会违反数据库约束，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第五步：使用JPQL直接更新状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateStatusOnly(id, targetStatus);

        if (updatedRows == 0) {
            throw new IllegalStateException("状态更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL状态更新成功，更新了 {} 行记录，ID: {}, 状态: {} -> {}", updatedRows, id, currentStatus, targetStatus);

        // 第六步：强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第七步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第八步：重新加载数据验证更新结果
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("状态更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();

        // 验证状态更新成功
        if (updatedData.getStatus() != targetStatus) {
            logger.error("状态更新未生效，期望状态: {}, 实际状态: {}, ID: {}", targetStatus, updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, targetStatus, errorMessage);

        logger.info("数据状态已通过JPQL更新成功，ID: {}, 状态: {} -> {}, ENCRYPTED_DATA字段完整保留",
                id, currentStatus, targetStatus);

        return updatedData;
    }

    /**
     * 解密数据并更新状态（使用JPQL直接更新，避免Hibernate缓存问题）
     * 使用JPQL直接更新解密数据和状态，避免实体状态管理问题
     * 用于解决解密操作中的ORA-01407约束违规错误
     * 
     * @param id            数据ID
     * @param decryptedData 解密后的数据Clob对象
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果状态转换不允许
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp decryptData(String id, Clob decryptedData) {
        logger.info("开始使用JPQL更新解密数据，强制清理Hibernate缓存，ID: {}", id);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体进行验证
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：验证状态转换是否允许
        if (!currentStatus.canTransitionTo(DataStatus.DECRYPTED)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 %s", currentStatus, DataStatus.DECRYPTED);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第四步：严格验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("ENCRYPTED_DATA字段为空，数据状态异常，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第五步：使用JPQL直接更新解密数据和状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateDecryptedData(id, decryptedData, DataStatus.DECRYPTED);

        if (updatedRows == 0) {
            throw new IllegalStateException("解密数据更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL解密数据更新成功，更新了 {} 行记录，ID: {}", updatedRows, id);

        // 第六步：强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第七步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第八步：重新加载数据验证更新结果
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("解密数据更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();

        // 验证状态更新成功
        if (updatedData.getStatus() != DataStatus.DECRYPTED) {
            logger.error("状态更新未生效，期望状态: DECRYPTED, 实际状态: {}, ID: {}", updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.DECRYPTED, null);

        logger.info("解密数据已通过JPQL更新成功，ID: {}, 状态: {} -> DECRYPTED, ENCRYPTED_DATA字段完整保留",
                id, currentStatus);

        return updatedData;
    }

    /**
     * 开始处理数据（使用JPQL直接更新，避免Hibernate缓存问题）
     * 使用JPQL直接更新状态，避免实体状态管理问题
     * 用于解决处理操作中的ORA-01407约束违规错误
     * 
     * @param id 数据ID
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果当前状态不允许开始处理
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp startProcessing(String id) {
        logger.info("开始使用JPQL更新开始处理状态，强制清理Hibernate缓存，ID: {}", id);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体进行验证
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：验证状态转换是否允许
        if (!currentStatus.canTransitionTo(DataStatus.PROCESSING)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 %s", currentStatus, DataStatus.PROCESSING);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第四步：严格验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("ENCRYPTED_DATA字段为空，数据状态异常，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第五步：使用JPQL直接更新状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateStatusOnly(id, DataStatus.PROCESSING);

        if (updatedRows == 0) {
            throw new IllegalStateException("开始处理状态更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL开始处理状态更新成功，更新了 {} 行记录，ID: {}", updatedRows, id);

        // 第六步：强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第七步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第八步：重新加载数据验证更新结果
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("开始处理状态更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();

        // 验证状态更新成功
        if (updatedData.getStatus() != DataStatus.PROCESSING) {
            logger.error("状态更新未生效，期望状态: PROCESSING, 实际状态: {}, ID: {}", updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.PROCESSING, null);

        logger.info("开始处理状态已通过JPQL更新成功，ID: {}, 状态: {} -> PROCESSING, ENCRYPTED_DATA字段完整保留",
                id, currentStatus);

        return updatedData;
    }

    /**
     * 完成数据处理并设置执行结果（使用JPQL直接更新，避免Hibernate缓存问题）
     * 使用JPQL直接更新执行结果、处理时间和状态，避免实体状态管理问题
     * 用于解决完成处理操作中的ORA-01407约束违规错误
     * 
     * @param id              数据ID
     * @param executionResult 执行结果Clob对象
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果当前状态不允许完成处理
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp completeProcessing(String id, Clob executionResult) {
        logger.info("开始使用JPQL更新完成处理状态，强制清理Hibernate缓存，ID: {}", id);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体进行验证
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：验证状态转换是否允许
        if (!currentStatus.canTransitionTo(DataStatus.PROCESSED)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 %s", currentStatus, DataStatus.PROCESSED);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第四步：严格验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("ENCRYPTED_DATA字段为空，数据状态异常，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        logger.info("正在使用JPQL更新ENCRYPTED_DATA_TEMP表的EXECUTION_RESULT字段，数据ID: {}", id);
        logger.debug("执行结果内容长度: {}", getClobLength(executionResult));

        // 第五步：使用JPQL直接更新执行结果和状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateExecutionResult(id, executionResult, DataStatus.PROCESSED);

        if (updatedRows == 0) {
            throw new IllegalStateException("完成处理状态更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL完成处理状态更新成功，更新了 {} 行记录，ID: {}, 执行结果长度: {} 字符",
                updatedRows, id, getClobLength(executionResult));

        // 第六步：强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第七步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第八步：重新加载数据验证更新结果
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("完成处理状态更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();

        // 验证状态更新成功
        if (updatedData.getStatus() != DataStatus.PROCESSED) {
            logger.error("状态更新未生效，期望状态: PROCESSED, 实际状态: {}, ID: {}", updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.PROCESSED, null);

        logger.info("完成处理状态已通过JPQL更新成功，ID: {}, 状态: {} -> PROCESSED, ENCRYPTED_DATA字段完整保留",
                id, currentStatus);

        return updatedData;
    }

    /**
     * 获取Clob对象的长度
     * 
     * @param clob Clob对象
     * @return Clob长度，如果无法获取则返回-1
     */
    private long getClobLength(Clob clob) {
        try {
            if (clob != null) {
                return clob.length();
            }
            return -1;
        } catch (Exception e) {
            logger.warn("无法获取Clob长度: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 完成数据处理
     * 
     * @param id 数据ID
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果当前状态不允许完成处理
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp completeProcessing(String id) {
        return updateStatus(id, DataStatus.PROCESSED, null);
    }

    /**
     * 标记结果已加密（使用JPQL直接更新，避免Hibernate缓存问题）
     * 使用JPQL直接更新状态，严格保持ENCRYPTED_DATA字段不变
     * 只允许从PROCESSED状态转换到ENCRYPTED状态
     * 用于解决加密结果操作中的ORA-01407约束违规错误
     * 
     * @param id              数据ID
     * @param encryptedResult 加密后的结果Clob对象（当前实现中此参数被忽略，保持原始加密数据不变）
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果当前状态不是PROCESSED或不允许加密操作
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp encryptResult(String id, Clob encryptedResult) {
        logger.info("开始使用JPQL更新加密结果状态，强制清理Hibernate缓存，ID: {}", id);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体进行验证
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：严格检查：只允许从PROCESSED状态转换到ENCRYPTED状态
        if (currentStatus != DataStatus.PROCESSED) {
            String errorMsg = String.format("encryptResult方法只能在PROCESSED状态下调用，当前状态: %s, ID: %s",
                    currentStatus, id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第四步：严格验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("ENCRYPTED_DATA字段为空，数据状态异常，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 重要说明：此方法严格遵守规范，只更新状态，绝不修改ENCRYPTED_DATA字段
        // ENCRYPTED_DATA字段保持原始加密数据不变
        logger.info("将数据状态从PROCESSED更新为ENCRYPTED，严格保持ENCRYPTED_DATA字段不变，ID: {}", id);

        // 第五步：使用JPQL直接更新状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateStatusOnly(id, DataStatus.ENCRYPTED);

        if (updatedRows == 0) {
            throw new IllegalStateException("加密结果状态更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL加密结果状态更新成功，更新了 {} 行记录，ID: {}", updatedRows, id);

        // 第六步：强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第七步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第八步：重新加载数据验证更新结果
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("加密结果状态更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();

        // 验证状态更新成功
        if (updatedData.getStatus() != DataStatus.ENCRYPTED) {
            logger.error("状态更新未生效，期望状态: ENCRYPTED, 实际状态: {}, ID: {}", updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在且未被修改
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.ENCRYPTED, null);

        logger.info("加密结果状态已通过JPQL更新成功，ID: {}, 状态: {} -> ENCRYPTED, ENCRYPTED_DATA字段完整保留",
                id, currentStatus);

        return updatedData;
    }

    /**
     * 标记数据已发送
     * 使用JPQL直接更新状态字段，避免实体状态管理问题
     * 只更新状态和更新时间，不触及其他字段，特别是CLOB字段
     * 用于解决ORA-01407约束违规错误
     * 
     * 此方法专门用于解决主服务器轮询服务中的ORA-01407约束违规错误：
     * - 使用JPQL直接更新，完全绕过Hibernate的实体状态管理
     * - 只更新状态和更新时间，不触及其他字段，特别是CLOB字段
     * - 添加详细的调试日志，便于问题诊断
     * - 实现缓存状态检查和清理机制，确保数据一致性
     * 
     * @param id 数据ID
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     * @throws IllegalStateException    如果当前状态不允许标记为已发送
     * @throws IllegalStateException    如果ENCRYPTED_DATA字段为空
     * @throws IllegalStateException    如果状态更新未生效
     * @throws RuntimeException         如果JPQL更新失败
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp markAsSent(String id) {
        logger.info("开始使用JPQL更新标记数据为已发送状态，ID: {}", id);

        // 第一步：验证数据存在性和状态转换允许性
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 验证状态转换是否允许
        if (!currentStatus.canTransitionTo(DataStatus.SENT)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 SENT", currentStatus);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 验证ENCRYPTED_DATA字段不为空
        if (data.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段为空，无法标记为已发送，ID: " + id);
        }

        // 第二步：使用JPQL直接更新状态，避免实体状态管理问题
        int updatedRows = executionEncryptedDataTempRepository.updateStatusOnly(id, DataStatus.SENT);

        if (updatedRows == 0) {
            throw new IllegalStateException("状态更新失败，数据可能已被其他进程修改，ID: " + id);
        }

        logger.info("JPQL更新成功，更新了 {} 行记录，ID: {}", updatedRows, id);

        // 第三步：详细的调试日志 - 检查缓存状态
        EncryptedDataTemp cachedEntity = entityManager.find(EncryptedDataTemp.class, id);
        logger.debug("JPQL更新后缓存中的实体状态: {}", cachedEntity != null ? cachedEntity.getStatus() : "null");

        // 强制刷新到数据库
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 使用新的查询重新加载数据，确保获取最新状态
        Optional<EncryptedDataTemp> updatedDataOptional = executionEncryptedDataTempRepository.findById(id);
        if (updatedDataOptional.isEmpty()) {
            throw new IllegalStateException("状态更新后数据丢失，ID: " + id);
        }

        EncryptedDataTemp updatedData = updatedDataOptional.get();
        logger.debug("清理缓存后数据库中的实体状态: {}", updatedData.getStatus());

        // 验证状态更新成功
        if (updatedData.getStatus() != DataStatus.SENT) {
            logger.error("状态更新未生效，期望状态: SENT, 实际状态: {}, ID: {}", updatedData.getStatus(), id);
            throw new IllegalStateException("状态更新未生效，当前状态: " + updatedData.getStatus() + ", ID: " + id);
        }

        // 验证ENCRYPTED_DATA字段仍然存在
        if (updatedData.getEncryptedData() == null) {
            throw new IllegalStateException("ENCRYPTED_DATA字段在JPQL更新后丢失，ID: " + id);
        }

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.SENT, null);

        logger.info("数据已通过JPQL更新成功标记为已发送状态，ID: {}, ENCRYPTED_DATA字段完整保留", id);
        return updatedData;
    }

    /**
     * 标记数据处理错误（增强缓存清理版本）
     * 使用强制缓存清理确保实体状态一致性，避免Hibernate缓存导致的ORA-01407错误
     * 
     * @param id           数据ID
     * @param errorMessage 错误信息
     * @return 更新后的EncryptedDataTemp对象
     * @throws IllegalArgumentException 如果未找到对应ID的数据记录
     */
    @Transactional(transactionManager = "executionTransactionManager", rollbackFor = Exception.class)
    public EncryptedDataTemp markAsError(String id, String errorMessage) {
        logger.info("开始标记数据错误状态，强制清理Hibernate缓存，ID: {}", id);

        // 第一步：强制清理缓存，确保从数据库重新加载最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 第二步：从数据库重新加载实体
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        if (optionalData.isEmpty()) {
            throw new IllegalArgumentException("未找到ID为 " + id + " 的数据记录");
        }

        EncryptedDataTemp data = optionalData.get();
        DataStatus currentStatus = data.getStatus();

        // 第三步：验证关键字段完整性
        if (data.getEncryptedData() == null) {
            logger.error("ENCRYPTED_DATA字段为空，数据状态异常，ID: {}", id);
            throw new IllegalStateException("ENCRYPTED_DATA字段为空，无法标记为错误状态，ID: " + id);
        }

        // 第四步：检查状态转换是否允许
        if (!currentStatus.canTransitionTo(DataStatus.ERROR)) {
            String errorMsg = String.format("不允许从状态 %s 转换到 ERROR", currentStatus);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第五步：更新状态和错误信息
        data.setStatus(DataStatus.ERROR);

        // 设置错误信息
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            try {
                data.setErrorMessage(new javax.sql.rowset.serial.SerialClob(errorMessage.toCharArray()));
            } catch (Exception e) {
                logger.warn("设置错误信息时发生异常: {}", e.getMessage());
            }
        }

        // 第六步：保存前再次验证字段完整性
        if (data.getEncryptedData() == null) {
            String errorMsg = String.format("保存前检测到ENCRYPTED_DATA字段为空，停止操作以避免约束违规，ID: %s", id);
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 第七步：保存并强制刷新
        EncryptedDataTemp updatedData = executionEncryptedDataTempRepository.save(data);
        executionEncryptedDataTempRepository.flush();
        logger.debug("已强制刷新到数据库，ID: {}", id);

        // 第八步：再次清理缓存，确保后续操作使用最新数据
        entityManager.clear();
        logger.debug("已清理持久化上下文缓存，ID: {}", id);

        // 记录状态变更日志
        logStatusChange(id, currentStatus, DataStatus.ERROR, errorMessage);

        logger.info("数据已成功标记为错误状态，ID: {}, ENCRYPTED_DATA字段完整保留", id);
        return updatedData;
    }

    /**
     * 根据ID获取数据
     * 
     * @param id 数据ID
     * @return Optional包装的EncryptedDataTemp对象
     */
    public Optional<EncryptedDataTemp> findById(String id) {
        return executionEncryptedDataTempRepository.findById(id);
    }

    /**
     * 记录状态变更日志
     * 
     * @param id           数据ID
     * @param fromStatus   原状态
     * @param toStatus     目标状态
     * @param errorMessage 错误信息（可选）
     */
    private void logStatusChange(String id, DataStatus fromStatus, DataStatus toStatus, String errorMessage) {
        String logMessage = String.format("数据状态变更 - ID: %s, 从: %s, 到: %s",
                id, fromStatus, toStatus);

        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            logMessage += String.format(", 错误信息: %s", errorMessage);
        }

        if (toStatus == DataStatus.ERROR) {
            logger.error(logMessage);
        } else {
            logger.info(logMessage);
        }
    }

    /**
     * 检查数据是否处于最终状态
     * 
     * @param id 数据ID
     * @return true如果是最终状态，false否则或数据不存在
     */
    public boolean isFinalStatus(String id) {
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        return optionalData.map(data -> data.getStatus().isFinal()).orElse(false);
    }

    /**
     * 获取数据当前状态
     * 
     * @param id 数据ID
     * @return Optional包装的当前状态
     */
    public Optional<DataStatus> getCurrentStatus(String id) {
        Optional<EncryptedDataTemp> optionalData = executionEncryptedDataTempRepository.findById(id);
        return optionalData.map(EncryptedDataTemp::getStatus);
    }

    /**
     * 生成唯一的数据ID（简化版本）
     * 不再进行数据库检查，避免复杂的冲突处理
     * 如果有冲突，在保存时通过幂等性操作处理
     * 
     * @param requestId 请求ID（可选）
     * @return 数据ID
     */
    private String generateUniqueDataId(String requestId) {
        // 如果提供了requestId且不为空，直接使用它
        if (requestId != null && !requestId.trim().isEmpty()) {
            logger.info("使用提供的requestId作为数据ID: {}", requestId.trim());
            return requestId.trim();
        }

        // 生成新的ID（不进行数据库检查）
        return generateNewUniqueId();
    }

    /**
     * 生成一个新的ID
     * 使用时间戳+线程ID+随机数组合，不进行数据库唯一性检查
     * 
     * @return 新生成的ID
     */
    private String generateNewUniqueId() {
        String basePrefix = "cdwyy";

        // 使用时间戳 + 线程ID + 随机数确保唯一性
        long timestamp = System.currentTimeMillis();
        long threadId = Thread.currentThread().threadId();
        int randomSuffix = (int) (Math.random() * 10000);

        String candidateId = String.format("%s%d_%d_%d", basePrefix, timestamp, threadId, randomSuffix);

        logger.info("生成新的数据ID: {}", candidateId);
        return candidateId;
    }
}
