package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DrgAnalysisInputSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DRG分析输入快照Repository接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-19
 */
@Repository
public interface DrgAnalysisInputSnapshotRepository extends JpaRepository<DrgAnalysisInputSnapshot, Long> {
    
    /**
     * 根据患者ID查找未删除的快照记录
     * 
     * @param patientId 患者ID
     * @return 快照记录列表
     */
    List<DrgAnalysisInputSnapshot> findByPatientIdAndDeletedFalse(String patientId);
    
    /**
     * 根据患者ID和目录版本查找未删除的快照记录
     * 
     * @param patientId 患者ID
     * @param catalogVersion 目录版本
     * @return 快照记录列表
     */
    List<DrgAnalysisInputSnapshot> findByPatientIdAndCatalogVersionAndDeletedFalse(String patientId, String catalogVersion);
    
    /**
     * 根据患者ID查找最新的快照记录
     * 
     * @param patientId 患者ID
     * @return 最新的快照记录
     */
    @Query("SELECT s FROM DrgAnalysisInputSnapshot s WHERE s.patientId = :patientId AND s.deleted = false ORDER BY s.createdTime DESC")
    Optional<DrgAnalysisInputSnapshot> findLatestByPatientId(@Param("patientId") String patientId);
    
    /**
     * 根据患者ID和目录版本查找最新的快照记录
     * 
     * @param patientId 患者ID
     * @param catalogVersion 目录版本
     * @return 最新的快照记录
     */
    @Query("SELECT s FROM DrgAnalysisInputSnapshot s WHERE s.patientId = :patientId AND s.catalogVersion = :catalogVersion AND s.deleted = false ORDER BY s.createdTime DESC")
    Optional<DrgAnalysisInputSnapshot> findLatestByPatientIdAndCatalogVersion(
            @Param("patientId") String patientId, 
            @Param("catalogVersion") String catalogVersion);
    
    /**
     * 根据诊断和手术集合大小查找相似的快照记录
     * 
     * @param patientId 患者ID
     * @param diagCount 诊断集合大小
     * @param procCount 手术集合大小
     * @return 相似的快照记录列表
     */
    @Query("SELECT s FROM DrgAnalysisInputSnapshot s WHERE s.patientId = :patientId AND s.diagCount = :diagCount AND s.procCount = :procCount AND s.deleted = false")
    List<DrgAnalysisInputSnapshot> findByPatientIdAndCounts(
            @Param("patientId") String patientId, 
            @Param("diagCount") Integer diagCount, 
            @Param("procCount") Integer procCount);
    
    /**
     * 查找所有未删除的快照记录
     * 
     * @return 未删除的快照记录列表
     */
    List<DrgAnalysisInputSnapshot> findByDeletedFalse();
    
    /**
     * 根据目录版本查找未删除的快照记录
     * 
     * @param catalogVersion 目录版本
     * @return 快照记录列表
     */
    List<DrgAnalysisInputSnapshot> findByCatalogVersionAndDeletedFalse(String catalogVersion);
    
    /**
     * 根据Prompt ID查找快照记录
     * 
     * @param promptId Prompt ID
     * @return 快照记录
     */
    Optional<DrgAnalysisInputSnapshot> findByPromptId(Long promptId);
    
    /**
     * 根据PromptResult ID查找快照记录
     * 
     * @param promptResultId PromptResult ID
     * @return 快照记录
     */
    Optional<DrgAnalysisInputSnapshot> findByPromptResultId(Long promptResultId);
    
    /**
     * 统计患者未删除的快照数量
     * 
     * @param patientId 患者ID
     * @return 快照数量
     */
    long countByPatientIdAndDeletedFalse(String patientId);
    
    /**
     * 软删除指定患者的快照记录
     * 
     * @param patientId 患者ID
     * @return 删除的记录数
     */
    @Query("UPDATE DrgAnalysisInputSnapshot s SET s.deleted = true WHERE s.patientId = :patientId AND s.deleted = false")
    int softDeleteByPatientId(@Param("patientId") String patientId);
    
    /**
     * 软删除指定快照记录
     * 
     * @param snapshotId 快照ID
     * @return 删除的记录数
     */
    @Query("UPDATE DrgAnalysisInputSnapshot s SET s.deleted = true WHERE s.snapshotId = :snapshotId AND s.deleted = false")
    int softDeleteById(@Param("snapshotId") Long snapshotId);
    
    /**
     * 查找需要清理的过期快照记录（创建时间早于指定阈值的记录）
     * 
     * @param cutoffTime 截止时间
     * @return 过期快照记录列表
     */
    @Query("SELECT s FROM DrgAnalysisInputSnapshot s WHERE s.createdTime < :cutoffTime AND s.deleted = false")
    List<DrgAnalysisInputSnapshot> findExpiredSnapshots(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
    
    /**
     * 调用gen_drg_input_snapshot存储过程
     * 
     * @param patientId 患者ID
     * @param diagnosisIds 诊断ID集合
     * @param surgeryIds 手术ID集合
     * @param catalogVersion 目录版本
     * @param lastSourceDiagCount 源表诊断条目数
     * @param lastSourceProcCount 源表手术条目数
     * @param operationSource 操作来源
     * @param forceReanalyze 是否强制重算
     * @return 新创建的快照ID，如果未创建则返回null
     */
    @Procedure(value = "gen_drg_input_snapshot")
    Long callGenDrgInputSnapshot(
            @Param("p_patient_id") String patientId,
            @Param("p_diagnosis_ids") String diagnosisIds, // JSON格式字符串
            @Param("p_surgery_ids") String surgeryIds,     // JSON格式字符串
            @Param("p_catalog_version") String catalogVersion,
            @Param("p_last_source_diag_count") Integer lastSourceDiagCount,
            @Param("p_last_source_proc_count") Integer lastSourceProcCount,
            @Param("p_operation_source") String operationSource,
            @Param("p_force_reanalyze") boolean forceReanalyze);
}
