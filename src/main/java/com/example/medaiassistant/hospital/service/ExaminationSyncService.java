package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.config.ExamSyncConfig;
import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 检查结果同步服务
 * 负责从医院HIS系统（Oracle）同步检查结果数据到主服务器数据库
 * 
 * <p><strong>核心功能</strong>：</p>
 * <ul>
 *   <li>从Oracle HIS系统查询检查结果数据</li>
 *   <li>字段映射与数据转换（含CLOB字段处理）</li>
 *   <li>重复记录检测与更新策略（存在则更新，不存在则插入）</li>
 *   <li>单个患者和批量患者导入</li>
 * </ul>
 * 
 * <p><strong>字段映射规范</strong>（SQL别名 → 目标字段）：</p>
 * <ul>
 *   <li>EXAMINATION_ID → examinationId（主键）</li>
 *   <li>CHECK_NAME → checkName</li>
 *   <li>CHECK_TYPE → checkType</li>
 *   <li>CHECK_DESCRIPTION → checkDescription（CLOB）</li>
 *   <li>CHECK_CONCLUSION → checkConclusion（CLOB）</li>
 *   <li>CHECK_ISSUE_TIME → checkIssueTime</li>
 *   <li>CHECK_EXECUTE_TIME → checkExecuteTime</li>
 *   <li>CHECK_REPORT_TIME → checkReportTime</li>
 *   <li>UPDATE_DT → updateDt</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.1
 * @since 2025-12-30
 * @see com.example.medaiassistant.model.ExaminationResult
 * @see com.example.medaiassistant.repository.ExaminationResultRepository
 */
@Service
@Slf4j
public class ExaminationSyncService {
    
    private final SqlExecutionService sqlExecutionService;
    private final ExaminationResultRepository examinationResultRepository;
    private final PatientRepository patientRepository;
    private final HospitalConfigService hospitalConfigService;
    private final TemplateHotUpdateService templateHotUpdateService;
    private final ExamSyncConfig examSyncConfig;
    
    /**
     * 构造函数 - 依赖注入
     */
    public ExaminationSyncService(
            SqlExecutionService sqlExecutionService,
            ExaminationResultRepository examinationResultRepository,
            PatientRepository patientRepository,
            HospitalConfigService hospitalConfigService,
            TemplateHotUpdateService templateHotUpdateService,
            ExamSyncConfig examSyncConfig) {
        this.sqlExecutionService = sqlExecutionService;
        this.examinationResultRepository = examinationResultRepository;
        this.patientRepository = patientRepository;
        this.hospitalConfigService = hospitalConfigService;
        this.templateHotUpdateService = templateHotUpdateService;
        this.examSyncConfig = examSyncConfig;
    }
    
    /**
     * 获取患者检查结果的统计信息
     * 
     * @param mainServerPatientId 主服务器病人ID
     * @return 包含本地记录数和源表记录数的Map
     */
    public Map<String, Object> getExamResultStats(String mainServerPatientId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 解析ID
            String oraclePatientId = parseOraclePatientId(mainServerPatientId);
            String examResultPatientId = parseExamResultPatientId(mainServerPatientId);
            
            // 获取本地EXAMINATIONRESULTS表记录数
            long localCount = examinationResultRepository.countByPatientId(examResultPatientId);
            stats.put("localExamCount", localCount);
            
            // 获取源表V_EXAM_REPORT_INFO记录数
            long sourceCount = fetchSourceExamResultCount(oraclePatientId);
            stats.put("sourceExamCount", sourceCount);
            
            stats.put("oraclePatientId", oraclePatientId);
            stats.put("examResultPatientId", examResultPatientId);
            
        } catch (Exception e) {
            log.error("获取检查结果统计信息失败 - patientId: {}", mainServerPatientId, e);
            stats.put("localExamCount", -1L);
            stats.put("sourceExamCount", -1L);
            stats.put("statsError", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 从源表V_EXAM_REPORT_INFO获取记录数
     */
    private long fetchSourceExamResultCount(String oraclePatientId) {
        try {
            List<Map<String, Object>> results = fetchOracleExamResults(oraclePatientId);
            return results.size();
        } catch (Exception e) {
            log.warn("获取源表记录数失败 - oraclePatientId: {}", oraclePatientId, e);
            return -1L;
        }
    }
    
    /**
     * 导入检查结果
     * 根据主服务器病人ID从Oracle HIS系统同步检查结果
     * 
     * <p><strong>处理流程</strong>：</p>
     * <ol>
     *   <li>参数验证（空值检查）</li>
     *   <li>从主服务器查询病人信息</li>
     *   <li>解析患者ID（Oracle格式和目标格式）</li>
     *   <li>从Oracle HIS系统获取检查结果</li>
     *   <li>插入/更新到主服务器数据库</li>
     * </ol>
     * 
     * @param mainServerPatientId 主服务器病人ID（格式：990500000178405-1）
     * @return 导入的记录数量，-1表示失败，0表示无检查记录
     */
    public int importExaminationResults(String mainServerPatientId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 参数验证
            if (mainServerPatientId == null || mainServerPatientId.trim().isEmpty()) {
                log.error("主服务器病人ID不能为空");
                return -1;
            }
            
            log.info("开始导入检查结果 - 主服务器病人ID: {}", mainServerPatientId);
            
            // 2. 从主服务器查询病人信息
            Patient patient = getPatientFromMainServer(mainServerPatientId);
            if (patient == null) {
                log.error("病人未找到 - patientId: {}", mainServerPatientId);
                return -1;
            }
            
            // 3. 解析ID
            String oraclePatientId = parseOraclePatientId(mainServerPatientId);
            String examResultPatientId = parseExamResultPatientId(mainServerPatientId);
            
            log.info("解析完成 - Oracle患者ID: {}, ExamResult患者ID: {}", 
                oraclePatientId, examResultPatientId);
            
            // 4. 从Oracle HIS系统获取检查结果
            List<Map<String, Object>> oracleExamResults = fetchOracleExamResults(oraclePatientId);
            log.info("从Oracle获取到 {} 条检查结果记录", oracleExamResults.size());
            
            if (oracleExamResults.isEmpty()) {
                log.info("未找到检查结果数据，导入完成");
                return 0;
            }
            
            // 5. 插入到主服务器数据库
            int importedCount = insertExamResultsToMainServer(oracleExamResults, examResultPatientId);
            
            log.info("检查结果导入完成 - 成功导入 {} 条记录，总耗时: {}ms", 
                importedCount, System.currentTimeMillis() - startTime);
            
            return importedCount;
            
        } catch (Exception e) {
            log.error("检查结果导入失败 - 主服务器病人ID: {}", mainServerPatientId, e);
            return -1;
        }
    }
    
    /**
     * 从Oracle HIS系统获取检查结果数据
     */
    private List<Map<String, Object>> fetchOracleExamResults(String patientId) {
        log.info("从Oracle HIS系统获取检查结果 - 患者ID: {}", patientId);
        
        try {
            // 获取活动医院ID
            String hospitalId = getActiveHospitalId();
            
            // 获取模板路径
            String templateFilePath = examSyncConfig.getTemplateFilePath(hospitalId);
            log.info("使用的SQL模板路径: {}, 医院ID: {}", templateFilePath, hospitalId);
            
            // 加载模板
            SqlTemplate template = templateHotUpdateService.loadTemplate(templateFilePath);
            if (template == null) {
                log.error("模板加载失败 - 医院: {}, 文件路径: {}", hospitalId, templateFilePath);
                throw new RuntimeException("模板加载失败 - 文件路径: " + templateFilePath);
            }
            
            // 获取SQL语句
            String sql = template.getEffectiveSql();
            if (sql == null || sql.trim().isEmpty()) {
                log.error("SQL语句为空 - 医院: {}", hospitalId);
                throw new RuntimeException("SQL语句为空 - 医院: " + hospitalId);
            }
            
            // 创建SQL查询请求
            SqlQueryRequest request = new SqlQueryRequest();
            request.setSql(sql);
            request.setDatabaseType("his");
            request.setParameters(Map.of("patientId", patientId));
            request.setMaxRows(2000);
            request.setTimeoutSeconds(60);
            
            // 执行查询
            SqlQueryResult queryResult = sqlExecutionService.executeQuery(hospitalId, request);
            
            if (queryResult.isSuccess()) {
                List<Map<String, Object>> data = queryResult.getData();
                log.info("查询到的检查结果数据量: {} 条记录", data.size());
                
                // 数据过滤：移除无效记录
                List<Map<String, Object>> validData = new ArrayList<>();
                for (Map<String, Object> record : data) {
                    if (isValidExamResultRecord(record)) {
                        validData.add(record);
                    }
                }
                
                if (validData.size() < data.size()) {
                    log.warn("数据过滤: 共{}条记录，跳过{}条无效记录", 
                        data.size(), data.size() - validData.size());
                }
                
                return validData;
            } else {
                log.error("获取Oracle检查结果失败 - 错误: {}", queryResult.getErrorMessage());
                throw new RuntimeException("获取Oracle检查结果失败: " + queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("获取Oracle检查结果异常 - 患者ID: {}", patientId, e);
            throw new RuntimeException("获取Oracle检查结果异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将检查结果插入到主服务器数据库
     * 逻辑：先查询是否存在，存在则更新，不存在则插入
     */
    @Transactional(transactionManager = "transactionManager")
    private int insertExamResultsToMainServer(List<Map<String, Object>> oracleResults, String targetPatientId) {
        int processedCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        
        try {
            for (Map<String, Object> record : oracleResults) {
                String examinationId = getStringValue(record, "EXAMINATION_ID");
                
                // 检查是否已存在
                Optional<ExaminationResult> existingResult = 
                    examinationResultRepository.findByExaminationId(examinationId);
                
                ExaminationResult examResult;
                if (existingResult.isPresent()) {
                    // 存在则更新
                    examResult = existingResult.get();
                    updateExaminationResult(examResult, record, targetPatientId);
                    updatedCount++;
                    log.debug("更新检查记录 - ExaminationID: {}", examinationId);
                } else {
                    // 不存在则插入
                    examResult = convertToExaminationResult(record, targetPatientId);
                    insertedCount++;
                    log.debug("插入新检查记录 - ExaminationID: {}", examinationId);
                }
                
                examinationResultRepository.save(examResult);
                processedCount++;
            }
            
            log.info("主服务器数据库操作完成 - 新增: {} 条, 更新: {} 条", 
                insertedCount, updatedCount);
            
            return processedCount;
            
        } catch (Exception e) {
            log.error("插入主服务器数据库失败", e);
            throw e;
        }
    }
    
    /**
     * 将Oracle数据转换为ExaminationResult实体
     * 
     * <p>根据字段映射规范，将Oracle查询结果转换为实体。
     * CLOB字段（CHECK_DESCRIPTION、CHECK_CONCLUSION）会被正确处理。</p>
     * 
     * @param oracleData Oracle查询结果的Map数据，字段名为SQL别名
     * @param targetPatientId 目标患者ID（格式：990500000178405_1）
     * @return 转换后的ExaminationResult实体
     */
    public ExaminationResult convertToExaminationResult(Map<String, Object> oracleData, String targetPatientId) {
        ExaminationResult result = new ExaminationResult();
        
        // 设置检查申请号（主键）
        result.setExaminationId(getStringValue(oracleData, "EXAMINATION_ID"));
        
        // 设置患者ID
        result.setPatientId(targetPatientId);
        
        // 字段映射
        result.setCheckName(getStringValue(oracleData, "CHECK_NAME"));
        result.setCheckType(getStringValue(oracleData, "CHECK_TYPE"));
        
        // CLOB字段处理
        result.setCheckDescription(getStringValue(oracleData, "CHECK_DESCRIPTION"));
        result.setCheckConclusion(getStringValue(oracleData, "CHECK_CONCLUSION"));
        
        // 时间字段转换
        result.setCheckIssueTime(convertToTimestamp(oracleData.get("CHECK_ISSUE_TIME")));
        result.setCheckExecuteTime(convertToTimestamp(oracleData.get("CHECK_EXECUTE_TIME")));
        result.setCheckReportTime(convertToTimestamp(oracleData.get("CHECK_REPORT_TIME")));
        result.setUpdateDt(convertToTimestamp(oracleData.get("UPDATE_DT")));
        
        // 默认值设置
        result.setIsAnalyzed(0); // 0=未分析
        
        log.debug("转换ExaminationResult完成 - ExaminationID: {}, PatientID: {}", 
            result.getExaminationId(), targetPatientId);
        
        return result;
    }
    
    /**
     * 更新现有检查结果记录
     */
    private void updateExaminationResult(ExaminationResult existing, Map<String, Object> oracleData, String targetPatientId) {
        existing.setPatientId(targetPatientId);
        existing.setCheckName(getStringValue(oracleData, "CHECK_NAME"));
        existing.setCheckType(getStringValue(oracleData, "CHECK_TYPE"));
        existing.setCheckDescription(getStringValue(oracleData, "CHECK_DESCRIPTION"));
        existing.setCheckConclusion(getStringValue(oracleData, "CHECK_CONCLUSION"));
        existing.setCheckIssueTime(convertToTimestamp(oracleData.get("CHECK_ISSUE_TIME")));
        existing.setCheckExecuteTime(convertToTimestamp(oracleData.get("CHECK_EXECUTE_TIME")));
        existing.setCheckReportTime(convertToTimestamp(oracleData.get("CHECK_REPORT_TIME")));
        existing.setUpdateDt(convertToTimestamp(oracleData.get("UPDATE_DT")));
    }
    
    /**
     * 验证检查结果记录是否有效
     */
    private boolean isValidExamResultRecord(Map<String, Object> record) {
        // 必须有检查申请号
        Object examId = record.get("EXAMINATION_ID");
        if (examId == null || examId.toString().trim().isEmpty()) {
            return false;
        }
        
        // 必须有患者ID
        Object patientId = record.get("PATIENT_ID");
        if (patientId == null || patientId.toString().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 从主服务器查询病人信息
     */
    private Patient getPatientFromMainServer(String patientId) {
        try {
            log.info("查询病人信息 - patientId: {}", patientId);
            Patient patient = patientRepository.findByPatientId(patientId);
            if (patient == null) {
                log.warn("病人未找到 - patientId: {}", patientId);
            }
            return patient;
        } catch (Exception e) {
            log.error("查询病人信息失败 - patientId: {}", patientId, e);
            return null;
        }
    }
    
    /**
     * 获取当前活动的医院ID
     */
    private String getActiveHospitalId() {
        List<HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        if (configs != null && !configs.isEmpty()) {
            String activeId = configs.get(0).getId();
            log.info("从HospitalConfigService获取活动医院ID: {}", activeId);
            return activeId;
        }
        
        String defaultId = examSyncConfig.getDefaultHospitalId();
        log.warn("HospitalConfigService未加载任何配置，使用默认医院ID: {}", defaultId);
        return defaultId;
    }
    
    /**
     * 解析Oracle患者ID
     * 数据库中病人ID和检查结果表中病人ID格式一致，无需转换
     */
    private String parseOraclePatientId(String mainServerPatientId) {
        return mainServerPatientId;
    }
    
    /**
     * 解析ExamResult患者ID
     * 数据库中病人ID和检查结果表中病人ID格式一致，无需转换
     */
    private String parseExamResultPatientId(String mainServerPatientId) {
        return mainServerPatientId;
    }
    
    /**
     * 从数据记录Map中获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            return "";
        }
        return value.toString().trim();
    }
    
    /**
     * 转换为Timestamp类型
     */
    private Timestamp convertToTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Timestamp) {
                return (Timestamp) value;
            } else if (value instanceof Date) {
                return new Timestamp(((Date) value).getTime());
            } else if (value instanceof String) {
                String strValue = value.toString().trim();
                SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd"),
                    new SimpleDateFormat("yyyy/MM/dd")
                };
                
                for (SimpleDateFormat format : formats) {
                    try {
                        Date date = format.parse(strValue);
                        return new Timestamp(date.getTime());
                    } catch (Exception e) {
                        continue;
                    }
                }
                
                log.warn("日期时间格式无法解析: {}", strValue);
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("日期时间转换失败: {}", value, e);
            return null;
        }
    }
}
