package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.EmrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * EMR_RECORD表数据访问接口
 * 提供对EMR_RECORD表的CRUD操作和自定义查询
 * 
 * @since 1.0.0
 */
@Repository
public interface EmrRecordRepository extends JpaRepository<EmrRecord, String> {

    /**
     * 根据患者ID查询未删除的病历记录
     * 
     * @param patientId 患者ID
     * @return 病历记录列表
     */
    @Query(value = "SELECT e FROM EmrRecord e WHERE e.patientId = :patientId AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrRecord> findByPatientId(@Param("patientId") String patientId);

    /**
     * 根据患者ID和文档类型查询未删除的病历记录
     * 
     * @param patientId 患者ID
     * @param docTypeName 文档类型名称
     * @return 病历记录列表
     */
    @Query(value = "SELECT e FROM EmrRecord e WHERE e.patientId = :patientId AND e.docTypeName = :docTypeName AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrRecord> findByPatientIdAndDocTypeName(@Param("patientId") String patientId, @Param("docTypeName") String docTypeName);

    /**
     * 根据患者ID查询入院记录内容
     * 
     * 该方法使用JPQL查询语法，查询指定患者的入院记录内容。
     * 查询条件包括：
     * - 患者ID匹配
     * - 文档类型为'入院记录'
     * - 未删除的记录（deleteMark为NULL或0）
     * - 按记录时间降序排列
     * 
     * 注意：该方法使用JPQL而非原生SQL，确保实体映射正确。
     * 之前错误地使用了nativeQuery=true，导致ORA-00942表不存在错误。
     * 
     * @param patientId 患者ID，不能为空
     * @return 入院记录内容列表，按记录时间降序排列
     * @throws IllegalArgumentException 如果patientId为空
     * 
     * @example
     * List<String> admissionContents = repository.findAdmissionRecordContentByPatientId("PAT001");
     * // 返回: ["入院记录内容1", "入院记录内容2"]
     */
    @Query(value = "SELECT e.content FROM EmrRecord e WHERE e.patientId = :patientId AND e.docTypeName = '入院记录' AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<String> findAdmissionRecordContentByPatientId(@Param("patientId") String patientId);

    /**
     * 根据患者ID查询手术相关记录
     * 
     * @param patientId 患者ID
     * @return 手术记录列表
     */
    @Query(value = "SELECT e.* FROM EMR_RECORD e WHERE e.PATIENTID = :patientId AND (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0)", nativeQuery = true)
    List<EmrRecord> findOperationsByPatientId(@Param("patientId") String patientId);

    /**
     * 查询患者手术相关记录的唯一记录日期列表
     * 
     * @param patientId 患者ID
     * @return 唯一的记录日期列表，按日期升序排列
     */
    @Query(value = "SELECT DISTINCT TRUNC(e.RECORD_DATE) FROM EMR_RECORD e WHERE e.PATIENTID = :patientId AND (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0) AND e.RECORD_DATE IS NOT NULL ORDER BY TRUNC(e.RECORD_DATE) ASC", nativeQuery = true)
    List<Date> findDistinctRecordDatesByPatientId(@Param("patientId") String patientId);

    /**
     * 查询有手术记录的患者ID列表
     * 
     * @return 有手术记录的患者ID列表
     */
    @Query(value = "SELECT DISTINCT e.PATIENTID FROM EMR_RECORD e WHERE (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0)", nativeQuery = true)
    List<String> findPatientsWithOperations();

    /**
     * 查询在院且有未分析手术记录的患者ID列表
     * 
     * @return 在院且有未分析手术记录的患者ID列表
     */
    @Query(value = "SELECT DISTINCT p.PATIENTID FROM PATIENTS p " +
           "INNER JOIN EMR_RECORD e ON p.PATIENTID = e.PATIENTID " +
           "LEFT JOIN SURGERYNAME s ON p.PATIENTID = s.PATIENTID " +
           "WHERE p.ISINHOSPITAL = 1 " +
           "AND (e.DOC_TYPE_NAME LIKE '%手术记录%' OR e.DOC_TYPE_NAME LIKE '%操作记录%') " +
           "AND (e.DELETEMARK IS NULL OR e.DELETEMARK = 0) " +
           "AND s.PATIENTID IS NULL", nativeQuery = true)
    List<String> findInHospitalPatientsWithUnanalyzedOperations();

    /**
     * 根据患者ID和文档类型列表查询病历记录
     * 
     * @param patientId 患者ID
     * @param docTypeNames 文档类型列表
     * @return 病历记录列表
     */
    @Query(value = "SELECT e FROM EmrRecord e WHERE e.patientId = :patientId AND e.docTypeName IN :docTypeNames AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrRecord> findByPatientIdAndDocTypeNames(@Param("patientId") String patientId, @Param("docTypeNames") List<String> docTypeNames);

    /**
     * 根据患者ID查询特定时间范围内的病历记录
     * 
     * @param patientId 患者ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 病历记录列表
     */
    @Query(value = "SELECT e FROM EmrRecord e WHERE e.patientId = :patientId AND e.recordDate BETWEEN :startDate AND :endDate AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.recordDate DESC")
    List<EmrRecord> findByPatientIdAndRecordDateBetween(@Param("patientId") String patientId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据病人ID查询未删除的病历记录列表
     * 用于接口1：获取病人病历记录列表
     * 
     * @param patientId 病人ID
     * @return 病历记录列表，包含ID、DOC_TYPE_NAME、DOC_TITLE_TIME字段
     */
    @Query(value = "SELECT e.id, e.docTypeName, e.docTitleTime FROM EmrRecord e WHERE e.patientId = :patientId AND (e.deleteMark IS NULL OR e.deleteMark = 0) ORDER BY e.docTitleTime DESC")
    List<Object[]> findByPatientIdAndDeleteMarkZero(@Param("patientId") String patientId);

    /**
     * 根据记录ID查询病历内容
     * 用于接口2：获取病历记录内容
     * 
     * @param id 记录ID
     * @return 病历内容
     */
    @Query(value = "SELECT e.content FROM EmrRecord e WHERE e.id = :id AND (e.deleteMark IS NULL OR e.deleteMark = 0)")
    String findContentById(@Param("id") String id);

    /**
     * 查询指定科室在院且有未分析手术记录的患者ID列表
     * 
     * @param departments 目标科室列表
     * @return 在院且有未分析手术记录的患者ID列表
     */
    @Query(value = "SELECT DISTINCT p.PATIENTID FROM PATIENTS p " +
           "INNER JOIN EMR_RECORD e ON p.PATIENTID = e.PATIENTID " +
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
