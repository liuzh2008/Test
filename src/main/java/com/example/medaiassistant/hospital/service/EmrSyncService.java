package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.hospital.util.PatientIdParser;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * EMR病历内容同步服务
 * 负责从医院HIS系统（Oracle）同步EMR病历内容数据到主服务器数据库
 * 
 * <p><strong>核心功能</strong>：</p>
 * <ul>
 *   <li>从Oracle HIS系统查询EMR病历内容数据</li>
 *   <li>字段映射与数据转换（含CLOB字段处理）</li>
 *   <li>基于SOURCE_TABLE + SOURCE_ID的重复记录检测与更新策略</li>
 *   <li>单个患者和批量患者导入</li>
 * </ul>
 * 
 * <p><strong>去重策略</strong>：使用SOURCE_TABLE + SOURCE_ID组合作为唯一标识</p>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-11
 * @see EmrContent
 * @see EmrContentRepository
 */
@Service
@Slf4j
public class EmrSyncService {
    
    /** EMR病历内容源表名称常量 */
    private static final String SOURCE_TABLE_EMR = "emr.emr_content";
    
    private final SqlExecutionService sqlExecutionService;
    private final EmrContentRepository emrContentRepository;
    private final PatientRepository patientRepository;
    private final HospitalConfigService hospitalConfigService;
    private final TemplateHotUpdateService templateHotUpdateService;
    
    @Value("${emr.sync.template.path:sql/hospital-local/emr-content-query.json}")
    private String templateFilePath;
    
    @Value("${emr.sync.default.hospital:Local}")
    private String defaultHospitalId;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param sqlExecutionService SQL执行服务
     * @param emrContentRepository EMR内容数据访问层
     * @param patientRepository 患者数据访问层
     * @param hospitalConfigService 医院配置服务
     * @param templateHotUpdateService SQL模板热更新服务
     */
    public EmrSyncService(
            SqlExecutionService sqlExecutionService,
            EmrContentRepository emrContentRepository,
            PatientRepository patientRepository,
            HospitalConfigService hospitalConfigService,
            TemplateHotUpdateService templateHotUpdateService) {
        this.sqlExecutionService = sqlExecutionService;
        this.emrContentRepository = emrContentRepository;
        this.patientRepository = patientRepository;
        this.hospitalConfigService = hospitalConfigService;
        this.templateHotUpdateService = templateHotUpdateService;
    }
    
    /**
     * 导入EMR病历内容
     * 根据主服务器病人ID从Oracle HIS系统同步EMR病历内容
     * 
     * <p><strong>处理流程</strong>：</p>
     * <ol>
     *   <li>参数验证（空值检查）</li>
     *   <li>从主服务器查询病人信息</li>
     *   <li>解析患者ID（Oracle格式和目标格式）</li>
     *   <li>从Oracle HIS系统获取EMR病历内容</li>
     *   <li>插入/更新到主服务器数据库（按SOURCE_TABLE+SOURCE_ID去重）</li>
     * </ol>
     * 
     * @param mainServerPatientId 主服务器病人ID（格式：990500000178405-1）
     * @return 导入的记录数量，-1表示失败，0表示无EMR记录
     */
    public int importEmrContent(String mainServerPatientId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 参数验证
            if (mainServerPatientId == null || mainServerPatientId.trim().isEmpty()) {
                log.error("主服务器病人ID不能为空");
                return -1;
            }
            
            log.info("开始导入EMR病历内容 - 主服务器病人ID: {}", mainServerPatientId);
            
            // 2. 从主服务器查询病人信息
            Patient patient = getPatientFromMainServer(mainServerPatientId);
            if (patient == null) {
                log.error("病人未找到 - patientId: {}", mainServerPatientId);
                return -1;
            }
            
            // 3. 解析ID（使用工具类解析 PATI_ID 和 VISIT_ID）
            String oraclePatiId = PatientIdParser.parsePatiId(mainServerPatientId);
            Integer visitId = PatientIdParser.parseVisitId(mainServerPatientId);
            String targetPatientId = parseTargetPatientId(mainServerPatientId);
            
            log.info("解析完成 - Oracle病人ID: {}, VISIT_ID: {}, 目标PatientID: {}", 
                oraclePatiId, visitId, targetPatientId);
            
            // 4. 从Oracle HIS系统获取EMR病历内容
            List<Map<String, Object>> oracleEmrContents = fetchOracleEmrContents(oraclePatiId, visitId);
            log.info("从Oracle获取到 {} 条EMR病历记录", oracleEmrContents.size());
            
            if (oracleEmrContents.isEmpty()) {
                log.info("未找到EMR病历数据，导入完成");
                return 0;
            }
            
            // 5. 插入到主服务器数据库
            int importedCount = insertEmrContentToMainServer(oracleEmrContents, targetPatientId);
            
            log.info("EMR病历内容导入完成 - 成功导入 {} 条记录，总耗时: {}ms", 
                importedCount, System.currentTimeMillis() - startTime);
            
            return importedCount;
            
        } catch (Exception e) {
            log.error("EMR病历内容导入失败 - 主服务器病人ID: {}", mainServerPatientId, e);
            return -1;
        }
    }
    
    /**
     * 从Oracle HIS系统获取EMR病历内容数据
     * 
     * @param patiId Oracle病人ID（PATI_ID）
     * @param visitId 住院次数（VISIT_ID）
     * @return EMR病历内容列表
     */
    private List<Map<String, Object>> fetchOracleEmrContents(String patiId, Integer visitId) {
        log.info("从Oracle HIS系统获取EMR病历内容 - 病人ID: {}, VISIT_ID: {}", patiId, visitId);
        
        try {
            // 获取活动医院ID
            String hospitalId = getActiveHospitalId();
            
            // 动态构建模板文件路径（参考ExaminationSyncService模式）
            String dynamicTemplatePath = getTemplateFilePathByHospitalId(hospitalId);
            
            log.info("使用的SQL模板路径: {}, 医院ID: {}", dynamicTemplatePath, hospitalId);
            
            // 加载模板
            SqlTemplate template = templateHotUpdateService.loadTemplate(dynamicTemplatePath);
            if (template == null) {
                log.error("模板加载失败 - 医院: {}, 文件路径: {}", hospitalId, dynamicTemplatePath);
                throw new RuntimeException("模板加载失败 - 文件路径: " + dynamicTemplatePath);
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
            
            // 从模板中读取数据库类型，默认为 his
            String databaseType = template.getDatabaseType();
            if (databaseType == null || databaseType.trim().isEmpty()) {
                databaseType = "his";
            }
            request.setDatabaseType(databaseType);
            log.info("使用数据库类型: {}", databaseType);
            request.setParameters(Map.of("patiId", patiId, "visitId", visitId));
            request.setMaxRows(2000);
            request.setTimeoutSeconds(60);
            
            // 执行查询
            SqlQueryResult queryResult = sqlExecutionService.executeQuery(hospitalId, request);
            
            if (queryResult.isSuccess()) {
                List<Map<String, Object>> data = queryResult.getData();
                log.info("查询到的EMR病历数据量: {} 条记录", data.size());
                
                // 数据过滤：移除无效记录（已删除的记录）
                List<Map<String, Object>> validData = new ArrayList<>();
                for (Map<String, Object> record : data) {
                    if (isValidEmrRecord(record)) {
                        validData.add(record);
                    }
                }
                
                if (validData.size() < data.size()) {
                    log.warn("数据过滤: 共{}条记录，跳过{}条无效记录", 
                        data.size(), data.size() - validData.size());
                }
                
                return validData;
            } else {
                log.error("获取Oracle EMR病历内容失败 - 错误: {}", queryResult.getErrorMessage());
                throw new RuntimeException("获取Oracle EMR病历内容失败: " + queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("获取Oracle EMR病历内容异常 - 病人ID: {}", patiId, e);
            throw new RuntimeException("获取Oracle EMR病历内容异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将EMR病历内容插入到主服务器数据库
     * 逻辑：使用SOURCE_TABLE + SOURCE_ID组合判断重复，存在则更新，不存在则插入
     * 
     * @param oracleResults Oracle查询结果列表
     * @param targetPatientId 目标患者ID
     * @return 处理的记录数
     */
    @Transactional(transactionManager = "transactionManager")
    public int insertEmrContentToMainServer(List<Map<String, Object>> oracleResults, String targetPatientId) {
        int processedCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        
        try {
            for (Map<String, Object> record : oracleResults) {
                String sourceId = getStringValue(record, "SOURCE_ID");
                
                // 使用 SOURCE_TABLE + SOURCE_ID 组合判断重复
                Optional<EmrContent> existingResult = 
                    emrContentRepository.findBySourceTableAndSourceId(SOURCE_TABLE_EMR, sourceId);
                
                EmrContent emrContent;
                if (existingResult.isPresent()) {
                    // 存在则更新
                    emrContent = existingResult.get();
                    updateEmrContent(emrContent, record, targetPatientId);
                    updatedCount++;
                    log.debug("更新EMR记录 - SourceTable: {}, SourceID: {}", SOURCE_TABLE_EMR, sourceId);
                } else {
                    // 不存在则插入
                    emrContent = convertToEmrContent(record, targetPatientId);
                    emrContent.setSourceTable(SOURCE_TABLE_EMR);
                    emrContent.setSourceId(sourceId);
                    insertedCount++;
                    log.debug("插入新EMR记录 - SourceTable: {}, SourceID: {}", SOURCE_TABLE_EMR, sourceId);
                }
                
                emrContentRepository.save(emrContent);
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
     * 将Oracle数据转换为EmrContent实体
     * 
     * <p>根据字段映射规范，将Oracle查询结果转换为实体。
     * CLOB字段（CONTENT）会被正确处理。</p>
     * 
     * @param oracleData Oracle查询结果的Map数据，字段名为SQL别名
     * @param targetPatientId 目标患者ID
     * @return 转换后的EmrContent实体
     */
    public EmrContent convertToEmrContent(Map<String, Object> oracleData, String targetPatientId) {
        EmrContent result = new EmrContent();
        
        // 设置主服务器患者ID
        result.setPatientId(targetPatientId);
        
        // 字段映射 - 源数据字段
        result.setPatiId(getStringValue(oracleData, "PATI_ID"));
        result.setVisitId(getIntegerValue(oracleData, "VISIT_ID"));
        result.setPatiName(getStringValue(oracleData, "PATI_NAME"));
        result.setDeptCode(getStringValue(oracleData, "DEPT_CODE"));
        result.setDeptName(getStringValue(oracleData, "DEPT_NAME"));
        result.setDocTypeName(getStringValue(oracleData, "DOC_TYPE_NAME"));
        result.setCreateUserId(getStringValue(oracleData, "CREATEUSERID"));
        result.setCreateBy(getStringValue(oracleData, "CREATEBY"));
        result.setDeleteMark(getIntegerValue(oracleData, "DELETEMARK"));
        
        // CLOB字段处理
        result.setContent(getStringValue(oracleData, "CONTENT"));
        
        // 时间字段转换
        result.setRecordDate(convertToTimestamp(oracleData.get("RECORD_DATE")));
        result.setDocTitleTime(convertToTimestamp(oracleData.get("DOC_TITLE_TIME")));
        result.setModifiedOn(convertToTimestamp(oracleData.get("MODIFIEDON")));
        
        // SOURCE_ID字段
        result.setSourceId(getStringValue(oracleData, "SOURCE_ID"));
        
        log.debug("转换EmrContent完成 - SourceID: {}, PatientID: {}", 
            result.getSourceId(), targetPatientId);
        
        return result;
    }
    
    /**
     * 更新现有EMR病历内容记录
     * 
     * @param existing 现有记录
     * @param oracleData Oracle源数据
     * @param targetPatientId 目标患者ID
     */
    private void updateEmrContent(EmrContent existing, Map<String, Object> oracleData, String targetPatientId) {
        existing.setPatientId(targetPatientId);
        existing.setPatiId(getStringValue(oracleData, "PATI_ID"));
        existing.setVisitId(getIntegerValue(oracleData, "VISIT_ID"));
        existing.setPatiName(getStringValue(oracleData, "PATI_NAME"));
        existing.setDeptCode(getStringValue(oracleData, "DEPT_CODE"));
        existing.setDeptName(getStringValue(oracleData, "DEPT_NAME"));
        existing.setDocTypeName(getStringValue(oracleData, "DOC_TYPE_NAME"));
        existing.setContent(getStringValue(oracleData, "CONTENT"));
        existing.setCreateUserId(getStringValue(oracleData, "CREATEUSERID"));
        existing.setCreateBy(getStringValue(oracleData, "CREATEBY"));
        existing.setRecordDate(convertToTimestamp(oracleData.get("RECORD_DATE")));
        existing.setDocTitleTime(convertToTimestamp(oracleData.get("DOC_TITLE_TIME")));
        existing.setModifiedOn(convertToTimestamp(oracleData.get("MODIFIEDON")));
        existing.setDeleteMark(getIntegerValue(oracleData, "DELETEMARK"));
    }
    
    /**
     * 验证EMR病历记录是否有效
     * 过滤条件：DELETEMARK为null或0的记录为有效记录
     * 
     * @param record 记录数据
     * @return 是否有效
     */
    private boolean isValidEmrRecord(Map<String, Object> record) {
        // 必须有源记录ID
        Object sourceId = record.get("SOURCE_ID");
        if (sourceId == null || sourceId.toString().trim().isEmpty()) {
            return false;
        }
        
        // 检查删除标记（DELETEMARK != 0 的记录被过滤）
        Object deleteMark = record.get("DELETEMARK");
        if (deleteMark != null) {
            try {
                int deleteMarkValue = Integer.parseInt(deleteMark.toString());
                if (deleteMarkValue != 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // 解析失败，默认为有效记录
            }
        }
        
        return true;
    }
    
    /**
     * 从主服务器查询病人信息
     * 
     * @param patientId 患者ID
     * @return 患者实体，未找到返回null
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
     * 
     * @return 医院ID
     */
    private String getActiveHospitalId() {
        List<HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        if (configs != null && !configs.isEmpty()) {
            String activeId = configs.get(0).getId();
            log.info("从HospitalConfigService获取活动医院ID: {}", activeId);
            return activeId;
        }
        
        log.warn("HospitalConfigService未加载任何配置，使用默认医院ID: {}", defaultHospitalId);
        return defaultHospitalId;
    }
    
    /**
     * 根据医院ID构建模板文件路径
     * 
     * <p>动态构建模板文件路径，避免硬编码医院名称，参考 {@link ExaminationSyncService} 的实现模式。
     * 路径格式: sql/{hospitalId小写}/emr-content-query.json</p>
     * 
     * <p><strong>设计原则</strong>:</p>
     * <ul>
     *   <li>动态路径构建 - 根据医院ID自动生成模板路径，新增医院只需添加配置文件</li>
     *   <li>大小写统一 - 路径统一使用小写，避免Linux环境大小写敏感问题</li>
     *   <li>空值保护 - 空或null的医院ID使用默认值 hospital-local</li>
     * </ul>
     * 
     * <p><strong>路径生成示例</strong>:</p>
     * <ul>
     *   <li>testserver → sql/testserver/emr-content-query.json</li>
     *   <li>hospital-Local → sql/hospital-local/emr-content-query.json</li>
     *   <li>CDWYY → sql/cdwyy/emr-content-query.json</li>
     * </ul>
     * 
     * @param hospitalId 医院ID，可为空（将使用默认值 hospital-local）
     * @return 模板文件路径，格式为 sql/{hospitalId小写}/emr-content-query.json
     * @see ExamSyncConfig#getTemplateFilePath(String)
     */
    private String getTemplateFilePathByHospitalId(String hospitalId) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            log.warn("医院ID为空，使用默认模板路径");
            hospitalId = "hospital-local";
        }
        String templatePath = String.format("sql/%s/emr-content-query.json", hospitalId.toLowerCase());
        log.debug("构建EMR模板文件路径 - 医院: {}, 路径: {}", hospitalId, templatePath);
        return templatePath;
    }
    
    // parseOraclePatiId 已替换为 PatientIdParser.parsePatiId() 工具方法
    
    /**
     * 解析目标PatientID
     * 当前实现：直接返回主服务器病人ID
     * 
     * @param mainServerPatientId 主服务器病人ID
     * @return 目标PatientID
     */
    private String parseTargetPatientId(String mainServerPatientId) {
        return mainServerPatientId;
    }
    
    /**
     * 从数据记录Map中获取字符串值
     * 
     * @param map 数据Map
     * @param key 键名
     * @return 字符串值，null或空返回空字符串
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        String strValue = value.toString().trim();
        return strValue.isEmpty() ? "" : strValue;
    }
    
    /**
     * 从数据记录Map中获取Integer值
     * 
     * @param map 数据Map
     * @param key 键名
     * @return Integer值，解析失败返回null
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Integer转换失败 - key: {}, value: {}", key, value);
            return null;
        }
    }
    
    /**
     * 转换为Timestamp类型
     * 支持Timestamp、Date、String类型的转换
     * 
     * @param value 原始值
     * @return Timestamp，转换失败返回null
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
                if (strValue.isEmpty()) {
                    return null;
                }
                
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
     * 获取患者EMR病历内容的统计信息
     * 
     * @param mainServerPatientId 主服务器病人ID
     * @return 包含本地记录数的Map
     */
    public Map<String, Object> getEmrContentStats(String mainServerPatientId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取本地EMR_CONTENT表记录数
            long localCount = emrContentRepository.countByPatientId(mainServerPatientId);
            stats.put("localEmrCount", localCount);
            stats.put("patientId", mainServerPatientId);
            
        } catch (Exception e) {
            log.error("获取EMR统计信息失败 - patientId: {}", mainServerPatientId, e);
            stats.put("localEmrCount", -1L);
            stats.put("statsError", e.getMessage());
        }
        
        return stats;
    }
}
