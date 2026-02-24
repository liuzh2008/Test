package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.PatientSyncResult;
import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.util.DateConverter;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 病人数据同步服务
 * 负责执行医院HIS系统中的病人数据同步到主服务器数据库
 * 实现三向对比算法：新增、更新、标记出院
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-08
 */
@Service
@Transactional
@Slf4j
public class PatientSyncService {
    
    private final SqlExecutionService sqlExecutionService;
    private final PatientRepository patientRepository;
    private final SyncLogService syncLogService;
    private final TemplateHotUpdateService templateHotUpdateService;
    
    public PatientSyncService(
            SqlExecutionService sqlExecutionService,
            PatientRepository patientRepository,
            HospitalConfigService hospitalConfigService,
            SyncLogService syncLogService,
            TemplateHotUpdateService templateHotUpdateService) {
        this.sqlExecutionService = sqlExecutionService;
        this.patientRepository = patientRepository;
        // hospitalConfigService参数保留但不在类中存储，以保持API兼容性
        this.syncLogService = syncLogService;
        this.templateHotUpdateService = templateHotUpdateService;
    }
    
    /**
     * 同步指定科室的病人数据
     * 
     * @param hospitalId 医院ID
     * @param deptName 科室名称
     * @return 同步结果
     */
    public PatientSyncResult syncPatients(String hospitalId, String deptName) {
        long startTime = System.currentTimeMillis();
        PatientSyncResult result = new PatientSyncResult(hospitalId, deptName);
        
        try {
            // 1. 从Oracle获取病人列表
            List<Map<String, Object>> oraclePatients = fetchOraclePatients(hospitalId, deptName);
            result.setOraclePatientCount(oraclePatients.size());
            
            // 2. 从主服务器获取病人列表
            List<Patient> mainServerPatients = patientRepository.findByDepartmentAndIsInHospital(deptName, true);
            result.setMainServerPatientCount(mainServerPatients.size());
            
            // 3. 执行同步逻辑
            SyncOperationResult operationResult = executeSync(oraclePatients, mainServerPatients, deptName);
            
            // 4. 更新结果
            result.setAddedCount(operationResult.getAddedCount());
            result.setUpdatedCount(operationResult.getUpdatedCount());
            result.setDischargedCount(operationResult.getDischargedCount());
            result.setSuccess(true);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("病人数据同步失败 - 医院: {}, 科室: {}", hospitalId, deptName, e);
        } finally {
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            
            // 5. 记录同步日志
            syncLogService.logPatientSync(result);
        }
        
        return result;
    }
    
    /**
     * 从Oracle数据库获取病人列表（简化版本）
     * 使用完整的SQL语句和命名参数，移除复杂的模板变量替换逻辑
     * 
     * @throws RuntimeException 当Oracle连接失败或查询失败时抛出异常，防止误将所有病人标记为出院
     */
    private List<Map<String, Object>> fetchOraclePatients(String hospitalId, String deptName) {
        log.info("从Oracle获取病人列表（简化版）- 医院: {}, 科室: {}", hospitalId, deptName);
        
        try {
            // 根据医院ID确定使用哪个SQL模板
            String queryName = getPatientSyncQueryName(hospitalId);
            log.info("使用的SQL模板名称: {}", queryName);
            
            // 确保模板已加载（按需动态加载）
            ensureTemplateLoaded(hospitalId, queryName);
            
            // 获取模板
            com.example.medaiassistant.hospital.model.SqlTemplate template = 
                templateHotUpdateService.getTemplate(queryName);
            if (template == null) {
                log.error("SQL模板未找到 - 查询名称: {}", queryName);
                throw new RuntimeException("SQL模板未找到: " + queryName);
            }
            
            // 获取有效的SQL语句（优先使用sql字段）
            String sql = template.getEffectiveSql();
            if (sql == null || sql.trim().isEmpty()) {
                log.error("SQL语句为空 - 查询名称: {}", queryName);
                throw new RuntimeException("SQL语句为空: " + queryName);
            }
            
            // 调试信息：SQL语句
            log.info("使用的SQL语句:\n{}", sql);
            
            // 创建SQL查询请求
            SqlQueryRequest request = new SqlQueryRequest();
            request.setSql(sql);
            request.setDatabaseType("his");
            request.setParameters(Map.of("DEPT_NAME", deptName));
            request.setMaxRows(1000); // 限制最大返回行数
            request.setTimeoutSeconds(60); // 设置超时时间
            
            // 执行查询
            log.info("开始执行Oracle数据库查询 - 医院: {}, 科室: {}", hospitalId, deptName);
            SqlQueryResult queryResult = sqlExecutionService.executeQuery(hospitalId, request);
            
                // 调试信息：数据库连接和查询结果
                if (queryResult.isSuccess()) {
                    log.info("数据库连接成功 - 医院: {}", hospitalId);
                    List<Map<String, Object>> data = queryResult.getData();
                    log.info("查询到的数据量: {} 条记录", data.size());
                    
                    // ========== 新增：容错过滤 ==========
                    log.info("========== 开始数据过滤 ==========");
                    List<Map<String, Object>> validData = new ArrayList<>();
                    List<Map<String, Object>> invalidData = new ArrayList<>();
                    
                    for (int i = 0; i < data.size(); i++) {
                        Map<String, Object> record = data.get(i);
                        Object patiId = record.get("PATI_ID");
                        Object visitId = record.get("VISIT_ID");
                        
                        if (patiId == null || visitId == null) {
                            log.warn("跳过无效记录 {}: PATI_ID={}, VISIT_ID={}, PATI_NAME={}, DEPT_NAME={}", 
                                i + 1, patiId, visitId, record.get("PATI_NAME"), record.get("DEPT_NAME"));
                            invalidData.add(record);
                        } else {
                            validData.add(record);
                        }
                    }
                    
                    if (!invalidData.isEmpty()) {
                        log.warn("数据过滤完成: 共{}条记录，跳过{}条无效记录（PATI_ID或VISIT_ID为null）", 
                            data.size(), invalidData.size());
                        
                        // 记录详细的无效数据信息
                        log.warn("跳过的无效记录详情:");
                        for (int i = 0; i < invalidData.size(); i++) {
                            Map<String, Object> record = invalidData.get(i);
                            log.warn("  记录 {}: PATI_ID={}, VISIT_ID={}, PATI_NAME={}, DEPT_NAME={}", 
                                i + 1, record.get("PATI_ID"), record.get("VISIT_ID"), 
                                record.get("PATI_NAME"), record.get("DEPT_NAME"));
                        }
                    }
                    
                    log.info("数据过滤完成: 剩余{}条有效记录", validData.size());
                    log.info("========== 数据过滤结束 ==========");
                    // ========== 过滤结束 ==========
                    
                    // 使用过滤后的有效数据
                    data = validData;
                    
                    // ========== 只输出有效病人列表 ==========
                    log.info("========== 有效病人列表（简化版）==========");
                    for (int i = 0; i < data.size(); i++) {
                        Map<String, Object> record = data.get(i);
                        log.info("记录 {}: PATI_ID={}, VISIT_ID={}, PATI_NAME={}, DEPT_NAME={}, BED_NO={}", 
                            i + 1,
                            record.get("PATI_ID"),
                            record.get("VISIT_ID"),
                            record.get("PATI_NAME"),
                            record.get("DEPT_NAME"),
                            record.get("BED_NO"));
                    }
                    log.info("========== 病人列表结束 ==========");
                    // ========== 调试信息结束 ==========
                    
                    return data;
            } else {
                String errorMsg = String.format("Oracle数据库查询失败 - 医院: %s, 科室: %s, 错误: %s", 
                    hospitalId, deptName, queryResult.getErrorMessage());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
        } catch (RuntimeException e) {
            // RuntimeException直接抛出（包括上面抛出的异常）
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Oracle数据库连接异常 - 医院: %s, 科室: %s, 错误: %s", 
                hospitalId, deptName, e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * 根据医院ID获取病人同步查询名称
     */
    private String getPatientSyncQueryName(String hospitalId) {
        // 根据医院ID映射到对应的SQL模板查询名称
        switch (hospitalId) {
            case "cdwyy":
                return "getCdwyyInHospitalPatientsForSync";
            case "hospital-001":
                return "getHospital001InHospitalPatientsForSync";
            default:
                // 默认使用cdwyy的查询模板
                log.warn("未找到医院 {} 对应的SQL模板，使用默认模板", hospitalId);
                return "getCdwyyInHospitalPatientsForSync";
        }
    }
    
    
    /**
     * 执行同步逻辑
     */
    private SyncOperationResult executeSync(List<Map<String, Object>> oraclePatients,
                                          List<Patient> mainServerPatients,
                                          String deptName) {
        SyncOperationResult result = new SyncOperationResult();
        
        // 构建映射
        Map<String, Map<String, Object>> oracleMap = buildOracleMap(oraclePatients);
        Map<String, Patient> mainServerMap = buildMainServerMap(mainServerPatients);
        
        // 处理新增病人
        List<Patient> patientsToAdd = identifyPatientsToAdd(oracleMap, mainServerMap);
        if (!patientsToAdd.isEmpty()) {
            patientRepository.saveAll(patientsToAdd);
            result.setAddedCount(patientsToAdd.size());
        }
        
        // 处理需要更新的病人
        List<Patient> patientsToUpdate = identifyPatientsToUpdate(oracleMap, mainServerMap);
        if (!patientsToUpdate.isEmpty()) {
            patientRepository.saveAll(patientsToUpdate);
            result.setUpdatedCount(patientsToUpdate.size());
        }
        
        // 处理需要标记出院的病人
        List<Patient> patientsToDischarge = identifyPatientsToDischarge(oracleMap, mainServerMap);
        if (!patientsToDischarge.isEmpty()) {
            patientRepository.saveAll(patientsToDischarge);
            result.setDischargedCount(patientsToDischarge.size());
        }
        
        return result;
    }
    
    /**
     * 构建Oracle病人映射
     * 
     * 将Oracle数据库查询返回的病人列表转换为Map结构，以便快速查找和比对。
     * 使用"_"作为PATI_ID和VISIT_ID的连接符生成唯一键（如：990500000769439_1）。
     * 
     * @param oraclePatients Oracle数据库查询返回的病人数据列表
     * @return 以"PATI_ID_VISIT_ID"为键的病人数据映射
     */
    private Map<String, Map<String, Object>> buildOracleMap(List<Map<String, Object>> oraclePatients) {
        // 数据应该已经在fetchOraclePatients方法中过滤过了
        // 这里使用容错处理
        Map<String, Map<String, Object>> result = new HashMap<>();
        int skippedCount = 0;
        int duplicateCount = 0;
        
        for (Map<String, Object> record : oraclePatients) {
            Object patiIdObj = record.get("PATI_ID");
            Object visitIdObj = record.get("VISIT_ID");
            
            // 再次检查（理论上不应该发生，因为已经在fetchOraclePatients中过滤了）
            if (patiIdObj == null || visitIdObj == null) {
                log.warn("构建映射时跳过无效记录: PATI_ID={}, VISIT_ID={}, PATI_NAME={}", 
                    patiIdObj, visitIdObj, record.get("PATI_NAME"));
                skippedCount++;
                continue;
            }
            
            String patiId = String.valueOf(patiIdObj);
            String visitId = String.valueOf(visitIdObj);
            
            // 使用"_"作为连接符
            String key = patiId + "_" + visitId;
            
            // 检查重复键
            if (result.containsKey(key)) {
                log.warn("发现重复的病人记录:");
                log.warn("现有记录: PATI_ID={}, VISIT_ID={}, PATI_NAME={}, DEPT_NAME={}", 
                    result.get(key).get("PATI_ID"), result.get(key).get("VISIT_ID"), 
                    result.get(key).get("PATI_NAME"), result.get(key).get("DEPT_NAME"));
                log.warn("新记录: PATI_ID={}, VISIT_ID={}, PATI_NAME={}, DEPT_NAME={}", 
                    patiId, visitId, record.get("PATI_NAME"), record.get("DEPT_NAME"));
                
                // 跳过重复记录，继续处理其他数据
                log.warn("跳过重复记录: {}", key);
                duplicateCount++;
                continue;
            }
            
            result.put(key, record);
        }
        
        if (skippedCount > 0 || duplicateCount > 0) {
            log.warn("构建映射统计: 成功处理{}条记录，跳过{}条无效记录，跳过{}条重复记录", 
                result.size(), skippedCount, duplicateCount);
        } else {
            log.info("构建映射完成: 成功处理{}条记录", result.size());
        }
        
        return result;
    }
    
    /**
     * 构建主服务器病人映射
     * 
     * 将主服务器数据库中的病人实体列表转换为Map结构，以便快速查找和比对。
     * 使用"_"作为PATI_ID和VISIT_ID的连接符生成唯一键，与Oracle映射保持一致。
     * 
     * @param mainServerPatients 主服务器数据库中的病人实体列表
     * @return 以"PATI_ID_VISIT_ID"为键的病人实体映射
     */
    private Map<String, Patient> buildMainServerMap(List<Patient> mainServerPatients) {
        return mainServerPatients.stream()
            .collect(Collectors.toMap(
                // 使用"_"作为连接符，与Oracle映射保持一致
                p -> {
                    if (p.getPatiId() == null || p.getVisitId() == null) {
                        log.warn("主服务器病人记录缺少关键字段: patientId={}, patiId={}, visitId={}", 
                            p.getPatientId(), p.getPatiId(), p.getVisitId());
                        // 使用patientId作为备用键
                        return p.getPatientId() != null ? p.getPatientId() : "invalid_" + System.currentTimeMillis();
                    }
                    return p.getPatiId() + "_" + p.getVisitId();
                },
                p -> p
            ));
    }
    
    /**
     * 识别需要新增的病人
     */
    private List<Patient> identifyPatientsToAdd(Map<String, Map<String, Object>> oracleMap,
                                               Map<String, Patient> mainServerMap) {
        return oracleMap.keySet().stream()
            .filter(key -> !mainServerMap.containsKey(key))
            .map(key -> convertToPatient(oracleMap.get(key)))
            .collect(Collectors.toList());
    }
    
    /**
     * 识别需要更新的病人
     */
    private List<Patient> identifyPatientsToUpdate(Map<String, Map<String, Object>> oracleMap,
                                                  Map<String, Patient> mainServerMap) {
        return oracleMap.keySet().stream()
            .filter(key -> mainServerMap.containsKey(key))
            .map(key -> {
                Patient existing = mainServerMap.get(key);
                Map<String, Object> oracleData = oracleMap.get(key);
                return updatePatientIfNeeded(existing, oracleData);
            })
            .filter(Objects::nonNull) // 只返回需要更新的
            .collect(Collectors.toList());
    }
    
    /**
     * 识别需要标记出院的病人
     */
    private List<Patient> identifyPatientsToDischarge(Map<String, Map<String, Object>> oracleMap,
                                                     Map<String, Patient> mainServerMap) {
        return mainServerMap.keySet().stream()
            .filter(key -> !oracleMap.containsKey(key))
            .map(key -> {
                Patient patient = mainServerMap.get(key);
                patient.setIsInHospital(false);
                patient.setDischargeTime(new Date());
                return patient;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 将Oracle数据转换为Patient实体
     * 
     * 将从Oracle HIS系统查询到的病人数据转换为主服务器的Patient实体对象。
     * 复合主键patientId使用"_"作为PATI_ID和VISIT_ID的连接符（如：990500000769439_1）。
     * 
     * @param oracleData Oracle数据库查询返回的单条病人数据Map
     * @return 转换后的Patient实体对象
     */
    private Patient convertToPatient(Map<String, Object> oracleData) {
        Patient patient = new Patient();
        
        // 设置复合主键（使用"_"作为连接符，与映射逻辑保持一致）
        String patiId = String.valueOf(oracleData.get("PATI_ID"));
        String visitId = String.valueOf(oracleData.get("VISIT_ID"));
        patient.setPatientId(patiId + "_" + visitId);
        
        // 设置HIS系统原始ID
        patient.setPatiId(patiId);
        patient.setVisitId(Integer.parseInt(visitId));
        
        // 设置基本信息
        patient.setPatientNo(String.valueOf(oracleData.get("PATIENT_NO")));
        patient.setName(String.valueOf(oracleData.get("PATI_NAME")));
        patient.setGender(String.valueOf(oracleData.get("GENDER_CODE")));
        
        // 日期转换（使用DateConverter工具类）
        patient.setDateOfBirth(DateConverter.convertToDate(oracleData.get("BIRTHDATE")));
        patient.setAdmissionTime(DateConverter.convertToDate(oracleData.get("IN_DATE")));
        
        // 科室和床位信息
        patient.setDepartment(String.valueOf(oracleData.get("DEPT_NAME")));
        patient.setBedNumber(String.valueOf(oracleData.get("BED_NO")));
        
        // 身份信息
        patient.setIdCard(String.valueOf(oracleData.get("IDCARD_NO")));
        
        // 默认值设置
        patient.setIsInHospital(true);
        patient.setStatus("普通");
        patient.setMedicalRecordNumber(patient.getPatientNo());
        
        return patient;
    }
    
    /**
     * 更新病人信息（如果需要）
     * 比较现有病人和Oracle数据的差异，如果有变化则更新
     */
    private Patient updatePatientIfNeeded(Patient existing, Map<String, Object> oracleData) {
        boolean needsUpdate = false;
        Patient updatedPatient = new Patient();
        
        // 复制现有病人的ID
        updatedPatient.setPatientId(existing.getPatientId());
        updatedPatient.setPatiId(existing.getPatiId());
        updatedPatient.setVisitId(existing.getVisitId());
        
        // 使用辅助方法比较和更新字段
        needsUpdate |= compareAndUpdateStringField(
            existing.getName(), 
            String.valueOf(oracleData.get("PATI_NAME")), 
            updatedPatient::setName, 
            existing.getName()
        );
        
        needsUpdate |= compareAndUpdateStringField(
            existing.getGender(), 
            String.valueOf(oracleData.get("GENDER_CODE")), 
            updatedPatient::setGender, 
            existing.getGender()
        );
        
        needsUpdate |= compareAndUpdateDateField(
            existing.getDateOfBirth(), 
            DateConverter.convertToDate(oracleData.get("BIRTHDATE")), 
            updatedPatient::setDateOfBirth, 
            existing.getDateOfBirth()
        );
        
        needsUpdate |= compareAndUpdateDateField(
            existing.getAdmissionTime(), 
            DateConverter.convertToDate(oracleData.get("IN_DATE")), 
            updatedPatient::setAdmissionTime, 
            existing.getAdmissionTime()
        );
        
        needsUpdate |= compareAndUpdateStringField(
            existing.getDepartment(), 
            String.valueOf(oracleData.get("DEPT_NAME")), 
            updatedPatient::setDepartment, 
            existing.getDepartment()
        );
        
        needsUpdate |= compareAndUpdateStringField(
            existing.getBedNumber(), 
            String.valueOf(oracleData.get("BED_NO")), 
            updatedPatient::setBedNumber, 
            existing.getBedNumber()
        );
        
        needsUpdate |= compareAndUpdateStringField(
            existing.getIdCard(), 
            String.valueOf(oracleData.get("IDCARD_NO")), 
            updatedPatient::setIdCard, 
            existing.getIdCard()
        );
        
        // 复制其他字段
        updatedPatient.setPatientNo(existing.getPatientNo());
        updatedPatient.setIsInHospital(existing.getIsInHospital());
        updatedPatient.setStatus(existing.getStatus());
        updatedPatient.setMedicalRecordNumber(existing.getMedicalRecordNumber());
        updatedPatient.setDischargeTime(existing.getDischargeTime());
        
        // 只有需要更新时才返回更新后的病人
        return needsUpdate ? updatedPatient : null;
    }
    
    /**
     * 比较并更新字符串字段
     * 如果新值与旧值不同，则设置新值并返回true
     */
    private boolean compareAndUpdateStringField(String oldValue, String newValue, 
                                               java.util.function.Consumer<String> setter, 
                                               String defaultValue) {
        if (!newValue.equals(oldValue)) {
            setter.accept(newValue);
            return true;
        } else {
            setter.accept(defaultValue);
            return false;
        }
    }
    
    /**
     * 比较并更新日期字段
     * 如果新值与旧值不同，则设置新值并返回true
     */
    private boolean compareAndUpdateDateField(Date oldValue, Date newValue, 
                                             java.util.function.Consumer<Date> setter, 
                                             Date defaultValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            setter.accept(newValue);
            return true;
        } else {
            setter.accept(defaultValue);
            return false;
        }
    }
    
    /**
     * 同步操作结果内部类
     */
    private static class SyncOperationResult {
        private int addedCount;
        private int updatedCount;
        private int dischargedCount;
        
        public int getAddedCount() {
            return addedCount;
        }
        
        public void setAddedCount(int addedCount) {
            this.addedCount = addedCount;
        }
        
        public int getUpdatedCount() {
            return updatedCount;
        }
        
        public void setUpdatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
        }
        
        public int getDischargedCount() {
            return dischargedCount;
        }
        
        public void setDischargedCount(int dischargedCount) {
            this.dischargedCount = dischargedCount;
        }
    }
    
    /**
     * 确保模板已加载
     * 根据医院ID和查询名称加载对应的模板文件
     * 只加载当前医院需要的模板，不加载其他医院模板
     */
    private void ensureTemplateLoaded(String hospitalId, String queryName) {
        // 首先检查模板是否已存在
        com.example.medaiassistant.hospital.model.SqlTemplate template = 
            templateHotUpdateService.getTemplate(queryName);
        if (template != null) {
            log.debug("模板已加载: {}", queryName);
            return;
        }
        
        // 模板未加载，根据医院ID确定模板文件路径
        String templateFilePath = getTemplateFilePathByHospitalId(hospitalId);
        if (templateFilePath == null) {
            log.error("无法确定模板文件路径 - 医院: {}", hospitalId);
            throw new RuntimeException(String.format("无法确定模板文件路径 - 医院: %s", hospitalId));
        }
        
        // 加载模板
        log.info("开始加载模板 - 医院: {}, 文件路径: {}", hospitalId, templateFilePath);
        com.example.medaiassistant.hospital.model.SqlTemplate loadedTemplate = 
            templateHotUpdateService.loadTemplate(templateFilePath);
        
        if (loadedTemplate == null) {
            log.error("模板加载失败 - 医院: {}, 文件路径: {}", hospitalId, templateFilePath);
            throw new RuntimeException(String.format(
                "模板加载失败 - 医院: %s, 文件路径: %s", hospitalId, templateFilePath));
        }
        
        log.info("模板加载成功 - 医院: {}, 查询名称: {}", hospitalId, queryName);
    }
    
    /**
     * 根据医院ID获取模板文件路径
     * 只返回当前医院需要的模板文件路径
     */
    private String getTemplateFilePathByHospitalId(String hospitalId) {
        // 根据医院ID映射到对应的模板文件
        switch (hospitalId) {
            case "cdwyy":
                return "sql/cdwyy/patient-sync-query.json";
            case "hospital-001":
                return "sql/hospital-001/patient-sync-query.json";
            default:
                // 对于未知医院，可以尝试使用默认模板或抛出异常
                log.warn("未知医院ID: {}，尝试使用默认模板", hospitalId);
                return "sql/cdwyy/patient-sync-query.json"; // 默认使用cdwyy模板
        }
    }
}
