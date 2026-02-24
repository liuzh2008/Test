package com.example.medaiassistant.repository.executionserver;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 执行服务器专用的EncryptedDataTemp Repository
 * 使用执行服务器数据源连接到执行服务器的ENCRYPTED_DATA_TEMP表
 * 
 * @author System
 * @version 1.0
 * @since 2025-10-12
 */
@Repository
@Transactional(transactionManager = "executionTransactionManager")
public interface ExecutionServerEncryptedDataTempRepository extends JpaRepository<EncryptedDataTemp, String> {

    /**
     * 根据请求ID查找记录
     * 
     * @param requestId 请求ID
     * @return 加密数据临时记录
     */
    Optional<EncryptedDataTemp> findByRequestId(String requestId);

    /**
     * 根据状态查找记录
     * 
     * @param status 数据状态
     * @return 加密数据临时记录列表
     */
    List<EncryptedDataTemp> findByStatus(DataStatus status);

    /**
     * 统计指定状态的记录数量
     * 
     * @param status 数据状态
     * @return 记录数量
     */
    long countByStatus(DataStatus status);

    /**
     * 根据状态列表查询数据列表，按接收时间升序排列
     * 用于轮询服务查询未处理记录
     * 
     * @param statuses 状态列表
     * @return 加密数据临时记录列表
     */
    List<EncryptedDataTemp> findByStatusInOrderByReceivedTimeAsc(List<DataStatus> statuses);

    /**
     * 更新记录状态（仅更新状态字段）
     * 使用JPQL避免实体状态管理问题
     * 
     * @param id 记录ID
     * @param status 新状态
     * @return 更新影响的行数
     */
    @Modifying
    @Query("UPDATE EncryptedDataTemp e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int updateStatusOnly(@Param("id") String id, @Param("status") DataStatus status);

    /**
     * 更新解密数据字段
     * 使用JPQL避免实体状态管理问题
     * 
     * @param id 记录ID
     * @param decryptedData 解密数据
     * @param status 新状态
     * @return 更新影响的行数
     */
    @Modifying
    @Query("UPDATE EncryptedDataTemp e SET e.decryptedData = :decryptedData, e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int updateDecryptedData(@Param("id") String id, @Param("decryptedData") java.sql.Clob decryptedData, @Param("status") DataStatus status);

    /**
     * 更新执行结果字段
     * 使用JPQL避免实体状态管理问题
     * 
     * @param id 记录ID
     * @param executionResult 执行结果
     * @param status 新状态
     * @return 更新影响的行数
     */
    @Modifying
    @Query("UPDATE EncryptedDataTemp e SET e.executionResult = :executionResult, e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int updateExecutionResult(@Param("id") String id, @Param("executionResult") java.sql.Clob executionResult, @Param("status") DataStatus status);

    /**
     * 更新错误信息字段
     * 使用JPQL避免实体状态管理问题
     * 
     * @param id 记录ID
     * @param errorMessage 错误信息
     * @param status 新状态
     * @return 更新影响的行数
     */
    @Modifying
    @Query("UPDATE EncryptedDataTemp e SET e.errorMessage = :errorMessage, e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int updateErrorMessage(@Param("id") String id, @Param("errorMessage") java.sql.Clob errorMessage, @Param("status") DataStatus status);
}
