package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Prompt生成日志服务
 * 
 * <p>用于在生成Prompt时记录患者关键信息到专用日志文件，便于追踪和审计。</p>
 * 
 * <h3>功能说明</h3>
 * <ul>
 *   <li>记录Prompt生成时的患者关键信息：科室、病人ID、床号、姓名</li>
 *   <li>支持两种调用方式：按patientId查询后记录、直接使用Patient对象记录</li>
 *   <li>自动处理空值和异常情况</li>
 * </ul>
 * 
 * <h3>日志配置</h3>
 * <ul>
 *   <li>日志文件位置：logs/prompt-generation.log</li>
 *   <li>日志格式：时间|科室=xxx|病人ID=xxx|床号=xxx|姓名=xxx|模板类型=xxx|模板名称=xxx</li>
 *   <li>滚动策略：按日期和大小滚动（单文件50MB，保留7天，总大小上限500MB）</li>
 *   <li>自动清理：启动时自动删除过期日志</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 方式1：根据patientId查询后记录
 * promptGenerationLogService.logPromptGeneration("12345", "诊断分析", "诊断分析");
 * 
 * // 方式2：直接使用Patient对象记录（避免重复查询）
 * promptGenerationLogService.logPromptGenerationWithPatient(patient, "诊断分析", "诊断分析");
 * }</pre>
 * 
 * @author System
 * @version 1.0.0
 * @since 2025-12-27
 * @see org.slf4j.Logger
 * @see PatientRepository
 */
@Service
public class PromptGenerationLogService {
    
    /**
     * Prompt生成专用Logger
     * <p>对应logback-spring.xml中配置的PromptGenerationLogger，
     * 将日志输出到独立的prompt-generation.log文件</p>
     */
    private static final Logger promptLogger = LoggerFactory.getLogger("PromptGenerationLogger");
    
    /** 患者数据访问仓库，用于查询患者信息 */
    private final PatientRepository patientRepository;
    
    /**
     * 构造函数，通过依赖注入初始化服务
     * 
     * @param patientRepository 患者数据访问仓库
     */
    public PromptGenerationLogService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }
    
    /**
     * 记录Prompt生成日志（根据patientId查询患者信息）
     * 
     * <p>该方法会根据传入的patientId从数据库查询患者信息，
     * 然后将科室、病人ID、床号、姓名等信息记录到专用日志文件中。</p>
     * 
     * <h4>日志格式</h4>
     * <pre>科室=xxx|病人ID=xxx|床号=xxx|姓名=xxx|模板类型=xxx|模板名称=xxx</pre>
     * 
     * <h4>异常处理</h4>
     * <ul>
     *   <li>如果患者不存在，记录WARN级别日志</li>
     *   <li>如果发生异常，记录ERROR级别日志</li>
     * </ul>
     * 
     * @param patientId 患者ID，用于查询患者信息
     * @param promptType 模板类型，如"诊断分析"、"诊疗计划"
     * @param promptName 模板名称，如"诊断分析"、"诊疗计划补充"
     */
    public void logPromptGeneration(String patientId, String promptType, String promptName) {
        try {
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                String department = patient.getDepartment() != null ? patient.getDepartment() : "";
                String bedNumber = patient.getBedNumber() != null ? patient.getBedNumber() : "";
                String name = patient.getName() != null ? patient.getName() : "";
                
                String logMessage = String.format("科室=%s|病人ID=%s|床号=%s|姓名=%s|模板类型=%s|模板名称=%s",
                        department,
                        patientId,
                        bedNumber,
                        name,
                        promptType,
                        promptName);
                
                promptLogger.info(logMessage);
            } else {
                promptLogger.warn("科室=未知|病人ID={}|床号=未知|姓名=未知|模板类型={}|模板名称={}|备注=患者信息未找到",
                        patientId, promptType, promptName);
            }
        } catch (Exception e) {
            promptLogger.error("记录Prompt生成日志失败|病人ID={}|错误={}", patientId, e.getMessage());
        }
    }
    
    /**
     * 记录Prompt生成日志（直接使用Patient对象，避免重复查询）
     * 
     * <p>该方法直接使用传入的Patient对象记录日志，
     * 适用于调用方已经持有Patient对象的场景，可以避免重复查询数据库。</p>
     * 
     * <h4>日志格式</h4>
     * <pre>科室=xxx|病人ID=xxx|床号=xxx|姓名=xxx|模板类型=xxx|模板名称=xxx</pre>
     * 
     * <h4>异常处理</h4>
     * <ul>
     *   <li>如果Patient对象为空，记录WARN级别日志</li>
     *   <li>如果发生异常，记录ERROR级别日志</li>
     * </ul>
     * 
     * @param patient 患者对象，包含科室、床号、姓名等信息
     * @param promptType 模板类型，如"诊断分析"、"诊疗计划"
     * @param promptName 模板名称，如"诊断分析"、"诊疗计划补充"
     */
    public void logPromptGenerationWithPatient(Patient patient, String promptType, String promptName) {
        try {
            if (patient != null) {
                String department = patient.getDepartment() != null ? patient.getDepartment() : "";
                String patientId = patient.getPatientId() != null ? patient.getPatientId() : "";
                String bedNumber = patient.getBedNumber() != null ? patient.getBedNumber() : "";
                String name = patient.getName() != null ? patient.getName() : "";
                
                String logMessage = String.format("科室=%s|病人ID=%s|床号=%s|姓名=%s|模板类型=%s|模板名称=%s",
                        department,
                        patientId,
                        bedNumber,
                        name,
                        promptType,
                        promptName);
                
                promptLogger.info(logMessage);
            } else {
                promptLogger.warn("科室=未知|病人ID=未知|床号=未知|姓名=未知|模板类型={}|模板名称={}|备注=患者对象为空",
                        promptType, promptName);
            }
        } catch (Exception e) {
            promptLogger.error("记录Prompt生成日志失败|错误={}", e.getMessage());
        }
    }
}
