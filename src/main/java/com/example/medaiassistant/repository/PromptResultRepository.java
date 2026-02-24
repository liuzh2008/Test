package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.dto.PatientPromptResultDTO;
import com.example.medaiassistant.dto.PromptDetailSimpleDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptResultRepository extends JpaRepository<PromptResult, Integer> {

        @Query("SELECT new com.example.medaiassistant.dto.PatientPromptResultDTO(" +
                        "pr.resultId, pr.promptId, pr.originalResultContent, pr.modifiedResultContent, pr.status, " +
                        "pr.executionTime, pr.createdAt, pr.updatedAt, pr.lastModifiedBy, pr.isRead, " +
                        "p.promptTemplateName, p.objectiveContent, p.dailyRecords, p.statusName) " +
                        "FROM PromptResult pr " +
                        "JOIN Prompt p ON pr.promptId = p.promptId " +
                        "WHERE p.patientId = :patientId " +
                        "AND pr.deleted = 0 " +
                        "AND (p.promptTemplateName LIKE '%病情小结%' " +
                        "OR p.promptTemplateName LIKE '%查房记录%' " +
                        "OR p.promptTemplateName LIKE '%入院记录%') " +
                        "ORDER BY pr.executionTime DESC")
        List<PatientPromptResultDTO> findMedicalSummaryByPatientId(@Param("patientId") String patientId);

        @Query(value = "SELECT pr.*, p.PromptTemplateName, p.ObjectiveContent, p.DailyRecords, p.StatusName " +
                        "FROM promptresult pr " +
                        "JOIN prompts p ON pr.PromptId = p.PromptId " +
                        "WHERE p.PatientId = :patientId " +
                        "AND pr.deleted = 0", nativeQuery = true)
        List<Map<String, Object>> findPromptDetailsByPatientId(@Param("patientId") String patientId);

        /**
         * 根据患者ID查询轻量级Prompt详情列表
         * 只返回必要字段，避免传输大文本内容以提高性能
         * 
         * @param patientId 患者ID
         * @return 轻量级Prompt详情列表，按执行时间降序排序
         */
        @Query("SELECT new com.example.medaiassistant.dto.PromptDetailSimpleDTO(" +
                        "pr.resultId, pr.promptId, p.promptTemplateName, pr.status, " +
                        "pr.executionTime, pr.createdAt, pr.updatedAt, p.statusName, pr.isRead) " +
                        "FROM PromptResult pr " +
                        "JOIN Prompt p ON pr.promptId = p.promptId " +
                        "WHERE p.patientId = :patientId " +
                        "AND pr.deleted = 0 " +
                        "ORDER BY pr.executionTime DESC")
        List<PromptDetailSimpleDTO> findPromptSimpleDetailsByPatientId(@Param("patientId") String patientId);

        /**
         * 根据结果ID查询完整的Prompt详情
         * 包含所有字段，用于按需加载完整信息
         * 
         * @param resultId 结果ID
         * @return 完整的Prompt详情Optional对象
         */
        @Query("SELECT pr FROM PromptResult pr WHERE pr.resultId = :resultId AND pr.deleted = 0")
        Optional<PromptResult> findByIdWithDetails(@Param("resultId") Integer resultId);

        @Query("SELECT pr FROM PromptResult pr WHERE pr.promptId = :promptId AND pr.deleted = 0")
        List<PromptResult> findByPromptId(@Param("promptId") Integer promptId);

    @Query(value = "SELECT COUNT(*) FROM promptresult pr " +
                    "JOIN prompts p ON pr.PromptId = p.PromptId " +
                    "WHERE p.PatientId = :patientId " +
                    "AND pr.deleted = 0 " +
                    "AND p.PromptTemplateName IN ('诊断分析','鉴别诊断分析','诊疗计划建议') " +
                    "AND pr.IsRead = 1 " +
                    "AND pr.ExecutionTime >= :fromTime", nativeQuery = true)
    int countReadAiResultsSince(@Param("patientId") String patientId, @Param("fromTime") java.time.LocalDateTime fromTime);

    /**
     * 统计指定患者自给定时间以来已阅读完成的“诊断分析”结果数量。
     * <p>
     * 该方法只统计 PromptTemplateName 为 "诊断分析" 的 PromptResult 记录，且要求：
     * <ul>
     *   <li>prompts.PatientId = patientId</li>
     *   <li>prompts.PromptTemplateName = '诊断分析'</li>
     *   <li>promptresult.IsRead = 1（已读）</li>
     *   <li>promptresult.ExecutionTime &gt;= fromTime</li>
     * </ul>
     * 主要用于中午查房记录生成场景中，判断“诊断分析”是否已阅读完成。
     * </p>
     *
     * @param patientId 患者ID
     * @param fromTime  起算时间（通常为当天0点）
     * @return 已读“诊断分析”结果的数量
     */
    @Query(value = "SELECT COUNT(*) FROM promptresult pr " +
                    "JOIN prompts p ON pr.PromptId = p.PromptId " +
                    "WHERE p.PatientId = :patientId " +
                    "AND pr.deleted = 0 " +
                    "AND p.PromptTemplateName = '诊断分析' " +
                    "AND pr.IsRead = 1 " +
                    "AND pr.ExecutionTime >= :fromTime", nativeQuery = true)
    int countReadDiagnosisAnalysisSince(@Param("patientId") String patientId, @Param("fromTime") java.time.LocalDateTime fromTime);
}
