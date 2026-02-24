package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 加密数据临时表Repository接口
 * 提供对ENCRYPTED_DATA_TEMP表的数据访问操作
 */
@Repository
public interface EncryptedDataTempRepository extends JpaRepository<EncryptedDataTemp, String> {

        /**
         * 根据状态查询数据列表
         */
        List<EncryptedDataTemp> findByStatus(DataStatus status);

        /**
         * 根据状态列表查询数据列表，按接收时间升序排列
         * 用于轮询服务查询未处理记录
         */
        List<EncryptedDataTemp> findByStatusInOrderByReceivedTimeAsc(List<DataStatus> statuses);

        /**
         * 根据请求ID查询数据
         * 注意：简化设计下，REQUEST_ID等于ID，因此等价于findById
         */
        Optional<EncryptedDataTemp> findByRequestId(String requestId);

        /**
         * 根据请求ID查询所有数据（用于处理历史重复记录清理）
         * 注意：简化设计下，此方法主要用于数据维护
         */
        List<EncryptedDataTemp> findAllByRequestId(String requestId);

        /**
         * 根据请求ID查询最新的一条记录（按创建时间倒序）
         */
        @Query("SELECT e FROM EncryptedDataTemp e WHERE e.requestId = :requestId ORDER BY e.createdAt DESC")
        Optional<EncryptedDataTemp> findLatestByRequestId(@Param("requestId") String requestId);

        /**
         * 根据状态和来源查询数据列表
         */
        List<EncryptedDataTemp> findByStatusAndSource(DataStatus status, String source);

        /**
         * 查询非最终状态的数据列表
         */
        @Query("SELECT e FROM EncryptedDataTemp e WHERE e.status NOT IN (com.example.medaiassistant.model.DataStatus.SENT, com.example.medaiassistant.model.DataStatus.ERROR)")
        List<EncryptedDataTemp> findNonFinalStatusData();

        /**
         * 批量更新状态
         */
        @Transactional
        @Modifying
        @Query("UPDATE EncryptedDataTemp e SET e.status = :status, e.updatedAt = :updatedAt WHERE e.id IN :ids")
        int updateStatusByIds(@Param("ids") List<String> ids, @Param("status") DataStatus status,
                        @Param("updatedAt") Timestamp updatedAt);

        /**
         * 根据状态统计数量
         */
        long countByStatus(DataStatus status);

        /**
         * 查询指定时间范围内创建的数据
         */
        List<EncryptedDataTemp> findByCreatedAtBetween(Timestamp startTime, Timestamp endTime);

        /**
         * 查询指定时间范围内更新的数据
         */
        List<EncryptedDataTemp> findByUpdatedAtBetween(Timestamp startTime, Timestamp endTime);

        /**
         * 使用JPQL直接更新状态字段，避免实体状态管理问题
         * 只更新状态和更新时间，不触及其他字段，特别是CLOB字段
         * 用于解决ORA-01407约束违规错误
         * 
         * @param id     数据ID
         * @param status 目标状态
         * @return 更新的记录数
         */
        @Transactional
        @Modifying
        @Query("UPDATE EncryptedDataTemp e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
        int updateStatusOnly(@Param("id") String id, @Param("status") DataStatus status);

        /**
         * 使用JPQL直接更新解密数据和状态，避免实体状态管理问题
         * 只更新解密数据、状态和更新时间，不触及ENCRYPTED_DATA字段
         * 用于解决解密操作中的ORA-01407约束违规错误
         * 
         * @param id            数据ID
         * @param decryptedData 解密后的数据
         * @param status        目标状态
         * @return 更新的记录数
         */
        @Transactional
        @Modifying
        @Query("UPDATE EncryptedDataTemp e SET e.decryptedData = :decryptedData, e.status = :status, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
        int updateDecryptedData(@Param("id") String id, @Param("decryptedData") java.sql.Clob decryptedData,
                        @Param("status") DataStatus status);

        /**
         * 使用JPQL直接更新执行结果、处理时间和状态，避免实体状态管理问题
         * 用于解决完成处理操作中的EXECUTION_RESULT字段保存问题
         * 
         * @param id              数据ID
         * @param executionResult 执行结果Clob对象
         * @param status          目标状态
         * @param processedTime   处理时间
         * @return 更新的记录数
         */
        @Transactional
        @Modifying
        @Query("UPDATE EncryptedDataTemp e SET e.executionResult = :executionResult, e.status = :status, e.processedTime = :processedTime, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
        int updateExecutionResult(@Param("id") String id,
                        @Param("executionResult") java.sql.Clob executionResult,
                        @Param("status") DataStatus status,
                        @Param("processedTime") java.sql.Timestamp processedTime);
}
