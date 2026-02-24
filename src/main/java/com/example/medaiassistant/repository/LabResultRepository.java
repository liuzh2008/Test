package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.sql.Timestamp;
import java.util.List;

/**
 * 检验结果数据访问层
 * <p>提供检验结果的 CRUD 操作和自定义查询方法</p>
 * 
 * @author MedAiAssistant
 * @since 1.0
 */
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    
    /**
     * 根据患者ID查找所有检验结果
     * 
     * @param patientId 患者ID
     * @return 检验结果列表
     */
    List<LabResult> findByPatientId(String patientId);

    /**
     * 根据患者ID和分析状态查找检验结果
     * 
     * @param patientId 患者ID
     * @param isAnalyzed 分析状态（0=未分析，1=已分析）
     * @return 检验结果列表
     */
    List<LabResult> findByPatientIdAndIsAnalyzed(String patientId, Integer isAnalyzed);
    
    /**
     * 根据患者ID、检验项目名称和报告时间查找检验结果
     * 用于重复记录检查
     * 
     * @param patientId 患者ID
     * @param labName 检验项目名称
     * @param labReportTime 检验报告时间 (Timestamp类型)
     * @return 匹配的检验结果列表
     */
    List<LabResult> findByPatientIdAndLabNameAndLabReportTime(String patientId, String labName, Timestamp labReportTime);
    
    /**
     * 根据患者ID、检验项目名称、报告时间和检验结果值查找检验结果
     * 用于更精确的重复记录检查（增加检验结果值的检查）
     * LabResult字段已改为VARCHAR类型，可直接使用标准字符串比较
     * 
     * @param patientId 患者ID
     * @param labName 检验项目名称
     * @param labReportTime 检验报告时间 (Timestamp类型)
     * @param labResult 检验结果值
     * @return 匹配的检验结果列表
     */
    List<LabResult> findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
        String patientId, String labName, Timestamp labReportTime, String labResult);
}
