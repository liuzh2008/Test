package com.example.medaiassistant.hospital.repository;

import com.example.medaiassistant.hospital.model.SyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 同步日志数据访问接口
 * 提供同步日志的CRUD操作和查询功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    
    /**
     * 根据医院ID查询同步日志
     * 
     * @param hospitalId 医院ID
     * @return 同步日志列表
     */
    List<SyncLog> findByHospitalId(String hospitalId);
    
    /**
     * 根据医院ID分页查询同步日志
     * 
     * @param hospitalId 医院ID
     * @param pageable 分页参数
     * @return 分页的同步日志
     */
    Page<SyncLog> findByHospitalId(String hospitalId, Pageable pageable);
    
    /**
     * 根据医院ID和同步类型查询同步日志
     * 
     * @param hospitalId 医院ID
     * @param syncType 同步类型
     * @return 同步日志列表
     */
    List<SyncLog> findByHospitalIdAndSyncType(String hospitalId, String syncType);
    
    /**
     * 根据医院ID和状态查询同步日志
     * 
     * @param hospitalId 医院ID
     * @param status 状态
     * @return 同步日志列表
     */
    List<SyncLog> findByHospitalIdAndStatus(String hospitalId, String status);
    
    /**
     * 根据状态查询同步日志
     * 
     * @param status 状态
     * @return 同步日志列表
     */
    List<SyncLog> findByStatus(String status);
    
    /**
     * 查询运行中的同步日志
     * 
     * @return 运行中的同步日志列表
     */
    default List<SyncLog> findRunningLogs() {
        return findByStatus("RUNNING");
    }
    
    /**
     * 查询成功的同步日志
     * 
     * @return 成功的同步日志列表
     */
    default List<SyncLog> findSuccessLogs() {
        return findByStatus("SUCCESS");
    }
    
    /**
     * 查询失败的同步日志
     * 
     * @return 失败的同步日志列表
     */
    default List<SyncLog> findFailedLogs() {
        return findByStatus("FAILED");
    }
    
    /**
     * 根据时间范围查询同步日志
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 同步日志列表
     */
    @Query("SELECT s FROM SyncLog s WHERE s.startTime BETWEEN :startDate AND :endDate")
    List<SyncLog> findByStartTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 根据医院ID和时间范围查询同步日志
     * 
     * @param hospitalId 医院ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 同步日志列表
     */
    @Query("SELECT s FROM SyncLog s WHERE s.hospitalId = :hospitalId AND s.startTime BETWEEN :startDate AND :endDate")
    List<SyncLog> findByHospitalIdAndStartTimeBetween(
            @Param("hospitalId") String hospitalId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
    
    /**
     * 统计医院的成功同步次数
     * 
     * @param hospitalId 医院ID
     * @return 成功同步次数
     */
    @Query("SELECT COUNT(s) FROM SyncLog s WHERE s.hospitalId = :hospitalId AND s.status = 'SUCCESS'")
    long countSuccessByHospitalId(@Param("hospitalId") String hospitalId);
    
    /**
     * 统计医院的失败同步次数
     * 
     * @param hospitalId 医院ID
     * @return 失败同步次数
     */
    @Query("SELECT COUNT(s) FROM SyncLog s WHERE s.hospitalId = :hospitalId AND s.status = 'FAILED'")
    long countFailedByHospitalId(@Param("hospitalId") String hospitalId);
    
    /**
     * 获取医院最近一次成功的同步日志
     * 
     * @param hospitalId 医院ID
     * @return 最近一次成功的同步日志
     */
    @Query("SELECT s FROM SyncLog s WHERE s.hospitalId = :hospitalId AND s.status = 'SUCCESS' ORDER BY s.endTime DESC")
    List<SyncLog> findLatestSuccessByHospitalId(@Param("hospitalId") String hospitalId, Pageable pageable);
    
    /**
     * 获取医院最近一次同步日志
     * 
     * @param hospitalId 医院ID
     * @return 最近一次同步日志
     */
    @Query("SELECT s FROM SyncLog s WHERE s.hospitalId = :hospitalId ORDER BY s.startTime DESC")
    List<SyncLog> findLatestByHospitalId(@Param("hospitalId") String hospitalId, Pageable pageable);
    
    /**
     * 统计总同步记录数
     * 
     * @param hospitalId 医院ID
     * @return 总同步记录数
     */
    default long countTotalByHospitalId(String hospitalId) {
        return countByHospitalId(hospitalId);
    }
    
    /**
     * 统计医院同步记录数
     * 
     * @param hospitalId 医院ID
     * @return 同步记录数
     */
    long countByHospitalId(String hospitalId);
    
    /**
     * 删除指定时间之前的同步日志（用于日志归档）
     * 
     * @param beforeDate 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SyncLog s WHERE s.createdAt < :beforeDate")
    int deleteByCreatedAtBefore(@Param("beforeDate") Date beforeDate);
}
