package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.EmrContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EMR病历内容数据访问层接口
 * 提供基于组合键（SOURCE_TABLE + SOURCE_ID）的去重查询方法
 * 
 * <p>索引优化建议：</p>
 * <ul>
 *   <li>IDX_EMR_CONTENT_SOURCE (SOURCE_TABLE, SOURCE_ID) - 去重查询组合索引</li>
 *   <li>IDX_EMR_CONTENT_PATIENT_ID (PATIENT_ID) - 患者ID查询索引</li>
 * </ul>
 * 
 * @author System
 * @version 1.0
 * @since 2026-01-11
 * @see EmrContent
 */
@Repository
public interface EmrContentRepository extends JpaRepository<EmrContent, Long> {
    
    /**
     * 使用源表名+源表ID组合查找EMR记录（推荐用于去重判断）
     * 
     * <p>利用IDX_EMR_CONTENT_SOURCE组合索引提升查询性能</p>
     * 
     * @param sourceTable 源表名称（如emr.emr_content、nursing.nursing_record）
     * @param sourceId 源表记录ID
     * @return 匹配的EMR记录，若不存在返回Optional.empty()
     */
    @Query("SELECT e FROM EmrContent e WHERE e.sourceTable = :sourceTable AND e.sourceId = :sourceId")
    Optional<EmrContent> findBySourceTableAndSourceId(
            @Param("sourceTable") String sourceTable, 
            @Param("sourceId") String sourceId);
    
    /**
     * 检查指定源表名+源表ID组合的记录是否存在
     * 
     * <p>使用COUNT查询优化，避免加载完整实体</p>
     * 
     * @param sourceTable 源表名称
     * @param sourceId 源表记录ID
     * @return 存在返回true，不存在返回false
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmrContent e WHERE e.sourceTable = :sourceTable AND e.sourceId = :sourceId")
    boolean existsBySourceTableAndSourceId(
            @Param("sourceTable") String sourceTable, 
            @Param("sourceId") String sourceId);
    
    /**
     * 按患者ID查询所有EMR记录
     * 
     * <p>按记录日期降序排序，新记录在前</p>
     * <p>利用IDX_EMR_CONTENT_PATIENT_ID索引提升查询性能</p>
     * 
     * @param patientId 主服务器患者ID（住院流水号）
     * @return 该患者的所有EMR记录列表，按记录日期降序排序
     */
    @Query("SELECT e FROM EmrContent e WHERE e.patientId = :patientId ORDER BY e.recordDate DESC")
    List<EmrContent> findByPatientId(@Param("patientId") String patientId);
    
    /**
     * 统计指定患者的EMR记录数
     * 
     * @param patientId 主服务器患者ID
     * @return 该患者的EMR记录数量
     */
    @Query("SELECT COUNT(e) FROM EmrContent e WHERE e.patientId = :patientId")
    long countByPatientId(@Param("patientId") String patientId);
    
    /**
     * 按源表名统计记录数
     * 用于统计不同数据源的记录量
     * 
     * @param sourceTable 源表名称
     * @return 该源表的记录数量
     */
    @Query("SELECT COUNT(e) FROM EmrContent e WHERE e.sourceTable = :sourceTable")
    long countBySourceTable(@Param("sourceTable") String sourceTable);
    
    /**
     * 根据病人ID查询未删除的病历记录列表
     * 用于接口：获取病人病历记录列表
     * 
     * @param patientId 病人ID
     * @return 病历记录列表，包含ID、DOC_TYPE_NAME、DOC_TITLE_TIME字段
     */
    @Query("SELECT e.id, e.docTypeName, e.docTitleTime FROM EmrContent e WHERE e.patientId = :patientId AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.docTitleTime DESC")
    List<Object[]> findByPatientIdAndDeleteMarkZero(@Param("patientId") String patientId);
    
    /**
     * 根据记录ID查询病历内容
     * 用于接口：获取病历记录内容
     * 
     * @param id 记录ID
     * @return 病历内容
     */
    @Query("SELECT e.content FROM EmrContent e WHERE e.id = :id AND (e.deleteMark IS NULL OR e.deleteMark = 0)")
    String findContentById(@Param("id") Long id);
    
    /**
     * 根据患者ID查询入院记录内容
     * 查询条件：docTypeName LIKE '%入院记录%' 且未删除且内容非空
     * 
     * 修改说明：
     * - 使用模糊匹配兼容多种命名变体（如"24小时入院记录"、"入院记录摘要"等）
     * - 2026-02-12: 优化内容过滤条件，移除 CONTENT <> '' 判断，仅保留 CONTENT IS NOT NULL
     *   解决CLOB字段处理空字符串时可能导致的查询问题
     * 
     * @param patientId 患者ID
     * @return 入院记录内容列表，按记录时间降序排列
     */
    @Query("SELECT e.content FROM EmrContent e WHERE e.patientId = :patientId AND e.docTypeName LIKE '%入院记录%' AND (e.deleteMark IS NULL OR e.deleteMark = 0) AND e.content IS NOT NULL ORDER BY e.recordDate DESC")
    List<String> findAdmissionRecordContentByPatientId(@Param("patientId") String patientId);
    
    /**
     * 检查患者是否有入院记录
     * 
     * 修改说明：使用模糊匹配兼容多种命名变体
     * 
     * @param patientId 患者ID
     * @return 存在返回true，不存在返回false
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmrContent e WHERE e.patientId = :patientId AND e.docTypeName LIKE '%入院记录%' AND (e.deleteMark IS NULL OR e.deleteMark = 0)")
    boolean hasAdmissionRecord(@Param("patientId") String patientId);
    
    /**
     * 根据患者ID和文档类型查询未删除的病历记录
     * 
     * @param patientId 患者ID
     * @param docTypeName 文档类型名称
     * @return 病历记录列表
     */
    @Query("SELECT e FROM EmrContent e WHERE e.patientId = :patientId AND e.docTypeName = :docTypeName AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrContent> findByPatientIdAndDocTypeName(@Param("patientId") String patientId, @Param("docTypeName") String docTypeName);
    
    /**
     * 根据患者ID和文档类型列表查询病历记录
     * 
     * @param patientId 患者ID
     * @param docTypeNames 文档类型列表
     * @return 病历记录列表
     */
    @Query("SELECT e FROM EmrContent e WHERE e.patientId = :patientId AND e.docTypeName IN :docTypeNames AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrContent> findByPatientIdAndDocTypeNames(@Param("patientId") String patientId, @Param("docTypeNames") List<String> docTypeNames);
    
    /**
     * 根据患者ID查询手术相关记录
     * 查询条件：docTypeName包含'手术记录'或'操作记录'
     * 
     * @param patientId 患者ID
     * @return 手术记录列表
     */
    @Query("SELECT e FROM EmrContent e WHERE e.patientId = :patientId AND (e.docTypeName LIKE '%手术记录%' OR e.docTypeName LIKE '%操作记录%') AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrContent> findOperationsByPatientId(@Param("patientId") String patientId);
    
    /**
     * 查询在院且有未分析手术记录的患者ID列表
     * 联合PATIENTS表和SURGERYNAME表查询
     * 
     * @return 在院且有未分析手术记录的患者ID列表
     */
    @Query(value = "SELECT DISTINCT p.PATIENTID FROM PATIENTS p " +
           "INNER JOIN EMR_CONTENT e ON p.PATIENTID = e.PATIENT_ID " +
           "LEFT JOIN SURGERYNAME s ON p.PATIENTID = s.PATIENTID " +
           "WHERE p.ISINHOSPITAL = 1 " +
           "AND (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') " +
           "AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0) " +
           "AND s.PATIENTID IS NULL", nativeQuery = true)
    List<String> findInHospitalPatientsWithUnanalyzedOperations();
    
    /**
     * 查询指定科室在院且有未分析手术记录的患者ID列表
     * 
     * @param departments 目标科室列表
     * @return 在院且有未分析手术记录的患者ID列表
     */
    @Query(value = "SELECT DISTINCT p.PATIENTID FROM PATIENTS p " +
           "INNER JOIN EMR_CONTENT e ON p.PATIENTID = e.PATIENT_ID " +
           "LEFT JOIN SURGERYNAME s ON p.PATIENTID = s.PATIENTID " +
           "WHERE p.ISINHOSPITAL = 1 " +
           "AND p.DEPARTMENT IN :departments " +
           "AND (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') " +
           "AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0) " +
           "AND s.PATIENTID IS NULL", nativeQuery = true)
    List<String> findInHospitalPatientsWithUnanalyzedOperationsByDepartments(@Param("departments") List<String> departments);
    
    /**
     * 安全方法：查询指定科室在院且有未分析手术记录的患者ID列表（处理空或null列表）
     * 
     * @param departments 目标科室列表（可为空或null）
     * @return 在院且有未分析手术记录的患者ID列表，如果科室列表为空或null则返回空列表
     */
    default List<String> findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(List<String> departments) {
        if (departments == null || departments.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return findInHospitalPatientsWithUnanalyzedOperationsByDepartments(departments);
    }
}
