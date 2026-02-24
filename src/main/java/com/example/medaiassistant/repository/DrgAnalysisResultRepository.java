package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DrgAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DRG分析结果Repository接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Repository
public interface DrgAnalysisResultRepository extends JpaRepository<DrgAnalysisResult, Long> {
    
    /**
     * 根据患者ID查找分析结果
     * 
     * @param patientId 患者ID
     * @return 分析结果列表
     */
    List<DrgAnalysisResult> findByPatientId(String patientId);
    
    /**
     * 根据患者ID和用户选择的MCC类型查找分析结果
     * 
     * @param patientId 患者ID
     * @param userSelectedMccType 用户选择的MCC类型
     * @return 分析结果列表
     */
    List<DrgAnalysisResult> findByPatientIdAndUserSelectedMccType(String patientId, String userSelectedMccType);
    
    /**
     * 根据DRG ID查找分析结果
     * 
     * @param drgId DRG ID
     * @return 分析结果列表
     */
    List<DrgAnalysisResult> findByDrgId(Long drgId);
    
    /**
     * 根据患者ID查找最新的分析结果
     * 
     * @param patientId 患者ID
     * @return 最新的分析结果
     */
    @Query("SELECT d FROM DrgAnalysisResult d WHERE d.patientId = ?1 AND d.deleted = 0 ORDER BY d.createdTime DESC")
    Optional<DrgAnalysisResult> findLatestByPatientId(String patientId);
    
    /**
     * 根据患者ID查找所有分析结果（按时间倒序）
     * 
     * @param patientId 患者ID
     * @return 分析结果列表（按时间倒序）
     */
    @Query("SELECT d FROM DrgAnalysisResult d WHERE d.patientId = ?1 AND d.deleted = 0 ORDER BY d.createdTime DESC")
    List<DrgAnalysisResult> findByPatientIdOrderByCreatedTimeDesc(String patientId);
    
    /**
     * 根据创建时间范围查找分析结果
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分析结果列表
     */
    List<DrgAnalysisResult> findByCreatedTimeBetween(Timestamp startTime, Timestamp endTime);
    
    /**
     * 根据用户选择的MCC类型查找分析结果
     * 
     * @param userSelectedMccType 用户选择的MCC类型
     * @return 分析结果列表
     */
    List<DrgAnalysisResult> findByUserSelectedMccType(String userSelectedMccType);
    
    /**
     * 统计患者分析结果数量
     * 
     * @param patientId 患者ID
     * @return 分析结果数量
     */
    long countByPatientId(String patientId);
    
    /**
     * 统计用户选择的MCC类型为指定值的分析结果数量
     * 
     * @param userSelectedMccType 用户选择的MCC类型
     * @return 分析结果数量
     */
    long countByUserSelectedMccType(String userSelectedMccType);
    
    /**
     * 更新用户选择的MCC类型
     * 
     * @param resultId 分析结果ID
     * @param mccType 用户选择的MCC类型
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE DrgAnalysisResult d SET d.userSelectedMccType = ?2 WHERE d.resultId = ?1")
    int updateUserSelectedMccType(Long resultId, String mccType);
    
    /**
     * 更新最终DRG编码
     * 
     * @param resultId 分析结果ID
     * @param finalDrgCode 最终DRG编码
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE DrgAnalysisResult d SET d.finalDrgCode = ?2 WHERE d.resultId = ?1")
    int updateFinalDrgCode(Long resultId, String finalDrgCode);
    
    /**
     * 软删除分析结果
     * 
     * @param resultId 分析结果ID
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE DrgAnalysisResult d SET d.deleted = 1 WHERE d.resultId = ?1")
    int softDelete(Long resultId);
    
    /**
     * 批量软删除分析结果
     * 
     * @param resultIds 分析结果ID列表
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE DrgAnalysisResult d SET d.deleted = 1 WHERE d.resultId IN ?1")
    int batchSoftDelete(List<Long> resultIds);
    
    /**
     * 查找未删除的分析结果
     * 
     * @return 未删除的分析结果列表
     */
    @Query("SELECT d FROM DrgAnalysisResult d WHERE d.deleted = 0")
    List<DrgAnalysisResult> findNotDeleted();
    
    /**
     * 根据患者ID查找未删除的分析结果
     * 
     * @param patientId 患者ID
     * @return 未删除的分析结果列表
     */
    @Query("SELECT d FROM DrgAnalysisResult d WHERE d.patientId = ?1 AND d.deleted = 0")
    List<DrgAnalysisResult> findByPatientIdAndNotDeleted(String patientId);
    
    /**
     * 根据患者ID和用户选择的MCC类型查找未删除的分析结果
     * 
     * @param patientId 患者ID
     * @param userSelectedMccType 用户选择的MCC类型
     * @return 未删除的分析结果列表
     */
    @Query("SELECT d FROM DrgAnalysisResult d WHERE d.patientId = ?1 AND d.userSelectedMccType = ?2 AND d.deleted = 0")
    List<DrgAnalysisResult> findByPatientIdAndUserSelectedMccTypeAndNotDeleted(String patientId, String userSelectedMccType);
    
    /**
     * 统计未删除的分析结果数量
     * 
     * @return 未删除的分析结果数量
     */
    @Query("SELECT COUNT(d) FROM DrgAnalysisResult d WHERE d.deleted = 0")
    long countNotDeleted();
    
    /**
     * 根据患者ID统计未删除的分析结果数量
     * 
     * @param patientId 患者ID
     * @return 未删除的分析结果数量
     */
    @Query("SELECT COUNT(d) FROM DrgAnalysisResult d WHERE d.patientId = ?1 AND d.deleted = 0")
    long countByPatientIdAndNotDeleted(String patientId);
}
