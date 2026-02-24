package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * LIS检验结果同步服务
 * 负责执行医院LIS系统中的检验结果数据同步到主服务器数据库
 * 实现从Oracle LIS系统获取检验结果并插入到MySQL数据库
 * 
 * 根据文档中的流程：
 * 1. 从Oracle LIS系统查询原始数据
 * 2. 数据转换与处理（字段映射、数据类型转换、空值处理）
 * 3. 重复记录检查（基于PatientID、LabName、LabReportTime）
 * 4. 数据插入到MySQL服务器
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-12
 */
@Service
@Slf4j
public class LabSyncService {
    
    private final SqlExecutionService sqlExecutionService;
    private final LabResultRepository labResultRepository;
    private final PatientRepository patientRepository;
    private final TemplateHotUpdateService templateHotUpdateService;
    private final HospitalConfigService hospitalConfigService;
    
    /**
     * 默认医院ID配置（仅作为回退值，优先使用HospitalConfigService获取的配置）
     */
    @Value("${hospital.default.id:hospital-Local}")
    private String defaultHospitalId;
    
    public LabSyncService(
            SqlExecutionService sqlExecutionService,
            LabResultRepository labResultRepository,
            PatientRepository patientRepository,
            HospitalConfigService hospitalConfigService,
            SyncLogService syncLogService,
            TemplateHotUpdateService templateHotUpdateService) {
        this.sqlExecutionService = sqlExecutionService;
        this.labResultRepository = labResultRepository;
        this.patientRepository = patientRepository;
        this.hospitalConfigService = hospitalConfigService;
        this.templateHotUpdateService = templateHotUpdateService;
    }
    
    /**
     * 导入LIS检验结果
     * 根据主服务器病人ID自动获取入院日期并导入检验结果
     * 
     * @param mainServerPatientId 主服务器病人ID（格式：990500000178405-1）
     * @return 导入的记录数量，-1表示失败
     */
    public int importLabResults(String mainServerPatientId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 参数验证
            if (mainServerPatientId == null || mainServerPatientId.trim().isEmpty()) {
                log.error("主服务器病人ID不能为空");
                return -1;
            }
            
            log.info("开始导入LIS检验结果 - 主服务器病人ID: {}", mainServerPatientId);
            
            // 2. 从主服务器查询病人信息
            Patient patient = getPatientFromMainServer(mainServerPatientId);
            if (patient == null) {
                log.error("病人未找到 - patientId: {}", mainServerPatientId);
                return -1;
            }
            
            if (patient.getAdmissionTime() == null) {
                log.error("病人入院日期为空 - patientId: {}", mainServerPatientId);
                return -1;
            }
            
            // 获取病人详细信息
            String patientName = patient.getName();
            String patientGender = patient.getGender();
            String patientDepartment = patient.getDepartment();
            String patientBedNumber = patient.getBedNumber();
            
            log.info("病人详细信息 - ID: {}, 姓名: {}, 性别: {}, 科室: {}, 床位: {}, 入院日期: {}", 
                mainServerPatientId, patientName, patientGender, patientDepartment, patientBedNumber, patient.getAdmissionTime());
            
            // 3. 解析ID
            String oraclePatientId = parseOraclePatientId(mainServerPatientId); // 990500000178405
            String labResultPatientId = parseLabResultPatientId(mainServerPatientId); // 990500000178405_1
            String admissionDate = formatAdmissionDate(patient.getAdmissionTime()); // yyyy-MM-dd
            
            log.info("解析完成 - Oracle患者ID: {}, LabResult患者ID: {}, 入院日期: {}", 
                oraclePatientId, labResultPatientId, admissionDate);
            
            // 4. 解析就诊标识符
            String visitId = parseVisitId(mainServerPatientId);
            log.info("解析就诊标识符 - visitId: {}", visitId);
            
            // 5. 从Oracle LIS系统获取检验结果
            List<Map<String, Object>> oracleLabResults = fetchOracleLabResults(oraclePatientId, visitId, admissionDate);
            log.info("从Oracle获取到 {} 条检验结果记录", oracleLabResults.size());
            
            if (oracleLabResults.isEmpty()) {
                log.info("未找到检验结果数据，导入完成");
                return 0;
            }
            
            // 5. 插入到主服务器数据库
            int importedCount = insertLabResultsToMainServer(oracleLabResults, labResultPatientId);
            
            log.info("LIS检验结果导入完成 - 成功导入 {} 条记录，总耗时: {}ms", 
                importedCount, System.currentTimeMillis() - startTime);
            
            return importedCount;
            
        } catch (Exception e) {
            log.error("LIS检验结果导入失败 - 主服务器病人ID: {}", mainServerPatientId, e);
            return -1;
        }
    }
    
    /**
     * 从Oracle LIS系统获取检验结果数据
     * 
     * <p>根据文档中的GetOracleLabResults函数实现，动态加载SQL模板并执行查询。
     * 模板文件路径根据医院ID动态构建，直接使用文件路径获取模板，不依赖queryName。</p>
     * 
     * <p><strong>查询条件</strong>: patient_id = :patientId AND visit_id = :visitId</p>
     * 
     * <p><strong>模板加载流程</strong>:</p>
     * <ol>
     *   <li>从HospitalConfigService获取当前活动医院ID</li>
     *   <li>根据医院ID构建模板文件路径: sql/{hospitalId}/lab-results-query.json</li>
     *   <li>直接通过文件路径加载模板（优先从缓存获取）</li>
     *   <li>使用模板执行SQL查询</li>
     * </ol>
     * 
     * @param patientId Oracle患者ID（不含就诊标识符后缀）
     * @param visitId 就诊标识符
     * @param admissionDate 入院日期（格式: yyyy-MM-dd）
     * @return 检验结果数据列表，每条记录为Map结构
     * @throws RuntimeException 当模板加载失败或SQL执行失败时抛出
     */
    private List<Map<String, Object>> fetchOracleLabResults(String patientId, String visitId, String admissionDate) {
        log.info("从Oracle LIS系统获取检验结果 - 患者ID: {}, 就诊 ID: {}", patientId, visitId);
        
        try {
            // 从HospitalConfigService动态获取医院ID
            String hospitalId = getActiveHospitalId();
            
            // 根据医院ID构建模板文件路径
            String templateFilePath = getTemplateFilePathByHospitalId(hospitalId);
            log.info("使用的SQL模板路径: {}, 医院ID: {}", templateFilePath, hospitalId);
            
            // 直接通过文件路径加载模板（优先从缓存获取，不依赖queryName）
            com.example.medaiassistant.hospital.model.SqlTemplate template = 
                templateHotUpdateService.loadTemplate(templateFilePath);
            
            if (template == null) {
                log.error("模板加载失败 - 医院: {}, 文件路径: {}", hospitalId, templateFilePath);
                throw new RuntimeException("模板加载失败 - 文件路径: " + templateFilePath);
            }
            
            // 获取有效的SQL语句
            String sql = template.getEffectiveSql();
            if (sql == null || sql.trim().isEmpty()) {
                log.error("SQL语句为空 - 医院: {}", hospitalId);
                throw new RuntimeException("SQL语句为空 - 医院: " + hospitalId);
            }
            
            // 创建SQL查询请求
            SqlQueryRequest request = new SqlQueryRequest();
            request.setSql(sql);
            request.setDatabaseType("his");
            request.setParameters(Map.of(
                "patientId", patientId,
                "visitId", visitId
            ));
            request.setMaxRows(2000); // 限制最大返回行数，根据文档中的FETCH FIRST 2000 ROWS ONLY
            request.setTimeoutSeconds(60); // 设置超时时间
            
            // 执行查询
            log.info("开始执行Oracle LIS数据库查询 - 患者ID: {}, 就诊ID: {}", patientId, visitId);
            SqlQueryResult queryResult = sqlExecutionService.executeQuery(hospitalId, request);
            
            if (queryResult.isSuccess()) {
                List<Map<String, Object>> data = queryResult.getData();
                log.info("查询到的检验结果数据量: {} 条记录", data.size());
                
                // ========== 新增调试信息：化验项目名称 ==========
                if (!data.isEmpty()) {
                    // 收集所有化验项目名称
                    Set<String> labNameSet = new HashSet<>();
                    List<String> labNameList = new ArrayList<>();
                    
                    for (Map<String, Object> record : data) {
                        String labName = getStringValue(record, "LAB_NAME");
                        if (labName != null && !labName.isEmpty()) {
                            labNameSet.add(labName);
                            labNameList.add(labName);
                        }
                    }
                    
                    // 记录化验项目统计信息
                    log.info("医院内网化验结果列表 - 化验项目名称统计:");
                    log.info("  共查询到 {} 种不同的化验项目", labNameSet.size());
                    log.info("  化验项目名称列表: {}", labNameSet);
                    
                    // 统计每个化验项目的数量
                    Map<String, Integer> labNameCount = new HashMap<>();
                    for (String labName : labNameList) {
                        labNameCount.put(labName, labNameCount.getOrDefault(labName, 0) + 1);
                    }
                    
                    // 记录数量最多的前10个化验项目
                    List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(labNameCount.entrySet());
                    sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                    
                    log.info("  化验项目数量统计（前10个）:");
                    int limit = Math.min(10, sortedEntries.size());
                    for (int i = 0; i < limit; i++) {
                        Map.Entry<String, Integer> entry = sortedEntries.get(i);
                        log.info("    {}: {} 条记录", entry.getKey(), entry.getValue());
                    }
                    
                    // 记录病人ID信息
                    String firstPatientId = getStringValue(data.get(0), "PATIENT_ID");
                    log.info("  病人ID号: {}", firstPatientId);
                    log.info("  注：病人姓名从主服务器PATIENTS表获取，当前Oracle LIS查询仅返回病人ID");
                }
                // ========== 调试信息结束 ==========
                
                // 数据过滤：移除无效记录
                List<Map<String, Object>> validData = new ArrayList<>();
                for (Map<String, Object> record : data) {
                    if (isValidLabResultRecord(record)) {
                        validData.add(record);
                    }
                }
                
                if (validData.size() < data.size()) {
                    log.warn("数据过滤: 共{}条记录，跳过{}条无效记录", 
                        data.size(), data.size() - validData.size());
                }
                
                log.info("有效检验结果记录: {} 条", validData.size());
                return validData;
            } else {
                log.error("获取Oracle检验结果失败 - 错误: {}", queryResult.getErrorMessage());
                throw new RuntimeException("获取Oracle检验结果失败: " + queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("获取Oracle检验结果异常 - 患者ID: {}, 入院日期: {}", patientId, admissionDate, e);
            throw new RuntimeException("获取Oracle检验结果异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将检验结果插入到主服务器数据库
     * 逻辑：先检查重复，再插入
     * 利用数据库自增主键，无需手动生成LabID
     */
    @Transactional(transactionManager = "transactionManager")
    private int insertLabResultsToMainServer(List<Map<String, Object>> oracleResults, String targetPatientId) {
        int insertedCount = 0;
        int skippedCount = 0;
        
        try {
            for (int i = 0; i < oracleResults.size(); i++) {
                Map<String, Object> record = oracleResults.get(i);
                
                // 第一步：检查四字段重复记录（PatientID, LabName, LabReportTime, LabResult）
                if (isLabResultExists(record, targetPatientId)) {
                    skippedCount++;
                    log.debug("跳过重复记录 - 索引: {}, PatientID: {}", i, targetPatientId);
                    continue;
                }
                
                // 第二步：转换数据并插入新记录（不设置LabID，让数据库自动生成）
                LabResult labResult = convertToLabResult(record, targetPatientId);
                
                log.debug("准备插入新记录 - PatientID: {}, LabName: {}", 
                    targetPatientId, labResult.getLabName());
                
                labResultRepository.save(labResult);
                insertedCount++;
                
                log.debug("插入成功 - ID由数据库自动生成: {}", labResult.getId());
            }
            
            log.info("主服务器数据库操作完成 - 新增: {} 条, 跳过重复: {} 条", 
                insertedCount, skippedCount);
            
            return insertedCount;
            
        } catch (Exception e) {
            log.error("插入主服务器数据库失败", e);
            throw e;
        }
    }
    
    /**
     * 检查检验结果记录是否已存在
     * 根据文档中的IsMySqlLabRecordExists函数实现
     * 检查条件：PatientID、LabName、LabReportTime、LabResult
     * 
     * LabResult字段已改为VARCHAR类型，可直接使用四字段索引查询
     */
    private boolean isLabResultExists(Map<String, Object> record, String targetPatientId) {
        try {
            String labName = getStringValue(record, "LAB_NAME");
            Object reportTime = record.get("REPORT_TIME");
            String labResultValue = getStringValue(record, "LAB_RESULT");
            
            if (labName == null || reportTime == null || labResultValue == null) {
                return false;
            }
            
            Timestamp reportTimeTs = convertToMySqlDateTime(reportTime);
            if (reportTimeTs == null) {
                return false;
            }
            
            // LABRESULT已改为VARCHAR类型，直接使用四字段JPA查询
            List<LabResult> existingResults = labResultRepository.findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                targetPatientId, labName, reportTimeTs, labResultValue.trim());
            
            return !existingResults.isEmpty();
            
        } catch (Exception e) {
            log.error("检查重复记录失败", e);
            return false;
        }
    }
    
    /**
     * 将Oracle数据转换为LabResult实体
     * 
     * <p>根据文档中的字段映射关系，将Oracle查询结果转换为LabResult实体。
     * LabID不设置，由数据库触发器自动生成。</p>
     * 
     * <p><strong>字段别名映射规范（采用SQL别名统一）</strong>:</p>
     * <ul>
     *   <li>ITEM_NAME AS LAB_NAME → LabName（项目名称）</li>
     *   <li>REPORT_ITEM_NAME AS LAB_TYPE → LabType（项目类型）</li>
     *   <li>RESULT AS LAB_RESULT → LabResult</li>
     *   <li>TEST_REFERENCE AS REFERENCE_RANGE → ReferenceRange</li>
     *   <li>UNITS AS UNIT → Unit</li>
     *   <li>REQUESTED_DATE_TIME AS REPORT_TIME → LabReportTime</li>
     * </ul>
     * 
     * @param oracleData Oracle查询结果的Map数据，字段名为SQL别名
     * @param targetPatientId 目标患者ID（格式：990500000178405_1）
     * @return 转换后的LabResult实体
     * @see <a href="doc/迭代/医院数据同步/LIS检验结果SQL字段别名映射规范.md">字段别名映射规范</a>
     */
    private LabResult convertToLabResult(Map<String, Object> oracleData, String targetPatientId) {
        LabResult labResult = new LabResult();
        
        // 不设置LabID，让数据库自动生成
        
        // 设置患者ID
        labResult.setPatientId(targetPatientId);
        
        // 字段映射（使用SQL别名统一后的字段名）
        // SQL别名 -> MySQL字段
        labResult.setLabName(getStringValue(oracleData, "LAB_NAME"));       // ITEM_NAME AS LAB_NAME → LabName（项目名称）
        labResult.setLabType(getStringValue(oracleData, "LAB_TYPE"));       // REPORT_ITEM_NAME AS LAB_TYPE → LabType（项目类型）
        labResult.setLabResult(getStringValue(oracleData, "LAB_RESULT"));   // RESULT AS LAB_RESULT → LabResult
        labResult.setReferenceRange(getStringValue(oracleData, "REFERENCE_RANGE")); // TEST_REFERENCE AS REFERENCE_RANGE → ReferenceRange
        labResult.setUnit(getStringValue(oracleData, "UNIT"));              // UNITS AS UNIT → Unit
        
        // 异常标志处理
        String abnormalIndicator = getStringValue(oracleData, "ABNORMAL_INDICATOR");
        labResult.setAbnormalIndicator(abnormalIndicator != null ? abnormalIndicator : "");
        
        // 日期时间转换
        Object reportTime = oracleData.get("REPORT_TIME");  // REQUESTED_DATE_TIME AS REPORT_TIME
        if (reportTime != null) {
            labResult.setLabReportTime(convertToMySqlDateTime(reportTime));
            labResult.setLabIssueTime(convertToMySqlDateTime(reportTime));
        }
        
        // 默认值设置
        labResult.setIsAnalyzed(0); // 0=未分析，1=已分析
        
        log.debug("转换LabResult完成 - PatientID: {}, LabName: {}, LabType: {}", 
            targetPatientId, labResult.getLabName(), labResult.getLabType());
        
        return labResult;
    }
    
    /**
     * 从数据记录Map中获取字符串值
     * 
     * <p>根据文档中的CheckParameter函数实现，处理null值和空字符串。</p>
     * 
     * @param map 数据记录Map
     * @param key 要获取的字段名（使用SQL别名，如LAB_NAME、LAB_TYPE等）
     * @return 字段值，如果为null或空则返回空字符串
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            return "";
        }
        return value.toString().trim();
    }
    
    /**
     * 日期时间转换函数
     * 转换为Timestamp类型以匹配数据库TIMESTAMP字段
     */
    private Timestamp convertToMySqlDateTime(Object value) {
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
                // 尝试解析常见日期格式
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
    
    
    /**
     * 验证检验结果记录是否有效
     * 
     * <p>检查记录必须包含以下字段（使用SQL别名）：</p>
     * <ul>
     *   <li>PATIENT_ID - 患者ID</li>
     *   <li>LAB_NAME - 检验项目名称</li>
     *   <li>LAB_RESULT - 检验结果</li>
     * </ul>
     * 
     * @param record 检验结果记录Map
     * @return true表示记录有效，false表示记录无效应跳过
     */
    private boolean isValidLabResultRecord(Map<String, Object> record) {
        // 必须有患者ID
        Object patientId = record.get("PATIENT_ID");
        if (patientId == null || patientId.toString().trim().isEmpty()) {
            return false;
        }
        
        // 必须有检验项目名称（使用统一别名）
        Object labName = record.get("LAB_NAME");
        if (labName == null || labName.toString().trim().isEmpty()) {
            return false;
        }
        
        // 必须有检验结果（使用统一别名）
        Object labResult = record.get("LAB_RESULT");
        if (labResult == null || labResult.toString().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取当前活动的医院ID
     * 优先从HospitalConfigService获取已加载的配置，如果没有则使用默认值
     */
    private String getActiveHospitalId() {
        // 优先从HospitalConfigService获取第一个已加载的医院配置
        java.util.List<com.example.medaiassistant.hospital.model.HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        if (configs != null && !configs.isEmpty()) {
            String activeId = configs.get(0).getId();
            log.info("从HospitalConfigService获取活动医院ID: {}", activeId);
            return activeId;
        }
        
        // 回退到默认值
        log.warn("HospitalConfigService未加载任何配置，使用默认医院ID: {}", defaultHospitalId);
        return defaultHospitalId;
    }
    
    /**
     * 根据医院ID构建模板文件路径
     * 
     * <p>动态构建模板文件路径，避免硬编码医院名称。
     * 路径格式: sql/{hospitalId小写}/lab-results-query.json</p>
     * 
     * <p><strong>路径生成规则</strong>:</p>
     * <ul>
     *   <li>医院ID会转换为小写</li>
     *   <li>空或null的医院ID会使用默认值 hospital-Local</li>
     * </ul>
     * 
     * <p><strong>示例</strong>:</p>
     * <ul>
     *   <li>testserver → sql/testserver/lab-results-query.json</li>
     *   <li>hospital-Local → sql/hospital-local/lab-results-query.json</li>
     *   <li>CDWYY → sql/cdwyy/lab-results-query.json</li>
     * </ul>
     * 
     * @param hospitalId 医院ID，可为空（将使用默认值）
     * @return 模板文件路径
     */
    private String getTemplateFilePathByHospitalId(String hospitalId) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            log.warn("医院ID为空，使用默认模板路径");
            hospitalId = "hospital-Local";
        }
        String templatePath = String.format("sql/%s/lab-results-query.json", hospitalId.toLowerCase());
        log.debug("构建模板文件路径 - 医院: {}, 路径: {}", hospitalId, templatePath);
        return templatePath;
    }
    
    // ========== 新增辅助方法 ==========
    
    /**
     * 从主服务器查询病人信息
     */
    private Patient getPatientFromMainServer(String patientId) {
        try {
            log.info("查询病人信息 - patientId: {}", patientId);
            Patient patient = patientRepository.findByPatientId(patientId);
            if (patient == null) {
                log.warn("病人未找到 - patientId: {}", patientId);
            } else {
                // 获取病人姓名
                String patientName = patient.getName();
                log.info("找到病人信息 - patientId: {}, 病人姓名: {}, 入院日期: {}", 
                    patientId, patientName, patient.getAdmissionTime());
            }
            return patient;
        } catch (Exception e) {
            log.error("查询病人信息失败 - patientId: {}", patientId, e);
            return null;
        }
    }
    
    /**
     * 解析Oracle患者ID（从99050800248514_1或99050800248514-1解析出99050800248514）
     */
    private String parseOraclePatientId(String mainServerPatientId) {
        // 优先查找下划线：格式 99050800248514_1 → 99050800248514
        int underscoreIndex = mainServerPatientId.lastIndexOf("_");
        if (underscoreIndex > 0) {
            return mainServerPatientId.substring(0, underscoreIndex);
        }
        // 如果没有下划线，查找连字符：格式 99050800248514-1 → 99050800248514
        int dashIndex = mainServerPatientId.lastIndexOf("-");
        if (dashIndex > 0) {
            return mainServerPatientId.substring(0, dashIndex);
        }
        return mainServerPatientId;
    }
    
    /**
     * 解析LabResult患者ID（从990500000178405-1转换为990500000178405_1）
     */
    private String parseLabResultPatientId(String mainServerPatientId) {
        // 格式：990500000178405-1 → 990500000178405_1
        return mainServerPatientId.replace("-", "_");
    }
    
    /**
     * 解析就诊标识符（从99050800248514_1解析出1）
     */
    private String parseVisitId(String mainServerPatientId) {
        // 格式：99050800248514_1 → 1
        int underscoreIndex = mainServerPatientId.lastIndexOf("_");
        if (underscoreIndex > 0 && underscoreIndex < mainServerPatientId.length() - 1) {
            return mainServerPatientId.substring(underscoreIndex + 1);
        }
        // 如果没有下划线，尝试使用连字符
        int dashIndex = mainServerPatientId.lastIndexOf("-");
        if (dashIndex > 0 && dashIndex < mainServerPatientId.length() - 1) {
            return mainServerPatientId.substring(dashIndex + 1);
        }
        // 默认返回1
        return "1";
    }
    
    /**
     * 格式化入院日期
     */
    private String formatAdmissionDate(Date admissionTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(admissionTime);
    }
}
