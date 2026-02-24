package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.config.ExamSyncConfig;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.model.ExaminationResult;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.ExaminationResultRepository;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExaminationSyncService 单元测试
 * 任务3：验证检查结果同步核心服务
 * 
 * 测试场景（全部通过 ✅）：
 * - UT-01: 服务创建成功 ✅
 * - UT-02: null patientId返回-1 ✅
 * - UT-03: 空字符串patientId返回-1 ✅
 * - UT-03b: 空白字符串patientId返回-1 ✅
 * - UT-04: 患者不存在返回-1 ✅
 * - UT-05: 无检查记录返回0 ✅
 * - UT-06: 正常导入返回正确计数 ✅
 * - UT-07: 重复记录更新而非插入 ✅
 * - UT-08: CLOB字段正确处理 ✅
 * - UT-08b: NULL CLOB字段正确处理 ✅
 * 
 * 测试结果：Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
 * 
 * @author TDD
 * @version 1.1
 * @since 2025-12-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExaminationSyncService 单元测试")
class ExaminationSyncServiceTest {

    @Mock
    private SqlExecutionService sqlExecutionService;

    @Mock
    private ExaminationResultRepository examinationResultRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalConfigService hospitalConfigService;

    @Mock
    private TemplateHotUpdateService templateHotUpdateService;

    @Mock
    private ExamSyncConfig examSyncConfig;

    private ExaminationSyncService examinationSyncService;

    @BeforeEach
    void setUp() {
        examinationSyncService = new ExaminationSyncService(
            sqlExecutionService,
            examinationResultRepository,
            patientRepository,
            hospitalConfigService,
            templateHotUpdateService,
            examSyncConfig
        );
    }

    // ==================== UT-01: 服务创建测试 ====================

    /**
     * 测试：服务应能正确创建
     * 验收标准：Service可正确创建，依赖注入成功
     */
    @Test
    @DisplayName("UT-01: 服务应能正确创建")
    void testServiceCreation() {
        // Then - 验证服务创建成功
        assertNotNull(examinationSyncService, "ExaminationSyncService应成功创建");
    }

    // ==================== UT-02: 空patientId测试 ====================

    /**
     * 测试：当patientId为null时，应返回-1
     * 验收标准：空patientId返回-1
     */
    @Test
    @DisplayName("UT-02: null patientId应返回-1")
    void testImportWithNullPatientId() {
        // When - 调用导入方法，传入null
        int result = examinationSyncService.importExaminationResults(null);

        // Then - 验证返回-1
        assertEquals(-1, result, "null patientId应返回-1");
        
        // 验证未调用数据库查询
        verifyNoInteractions(patientRepository);
        verifyNoInteractions(examinationResultRepository);
    }

    /**
     * 测试：当patientId为空字符串时，应返回-1
     * 验收标准：空patientId返回-1
     */
    @Test
    @DisplayName("UT-03: 空字符串patientId应返回-1")
    void testImportWithEmptyPatientId() {
        // When - 调用导入方法，传入空字符串
        int result = examinationSyncService.importExaminationResults("");

        // Then - 验证返回-1
        assertEquals(-1, result, "空字符串patientId应返回-1");
        
        // 验证未调用数据库查询
        verifyNoInteractions(patientRepository);
    }

    /**
     * 测试：当patientId为空白字符串时，应返回-1
     * 边界条件测试
     */
    @Test
    @DisplayName("UT-03b: 空白字符串patientId应返回-1")
    void testImportWithBlankPatientId() {
        // When - 调用导入方法，传入空白字符串
        int result = examinationSyncService.importExaminationResults("   ");

        // Then - 验证返回-1
        assertEquals(-1, result, "空白字符串patientId应返回-1");
    }

    // ==================== UT-04: 患者不存在测试 ====================

    /**
     * 测试：当患者在主服务器不存在时，应返回-1
     * 验收标准：患者不存在返回-1
     */
    @Test
    @DisplayName("UT-04: 患者不存在应返回-1")
    void testImportPatientNotFound() {
        // Given - 模拟患者不存在
        String patientId = "invalid-patient-id";
        when(patientRepository.findByPatientId(patientId)).thenReturn(null);

        // When - 调用导入方法
        int result = examinationSyncService.importExaminationResults(patientId);

        // Then - 验证返回-1
        assertEquals(-1, result, "患者不存在应返回-1");
        verify(patientRepository).findByPatientId(patientId);
    }

    // ==================== UT-05: 无检查记录测试 ====================

    /**
     * 测试：当Oracle HIS系统中无检查记录时，应返回0
     * 验收标准：无检查记录返回0
     */
    @Test
    @DisplayName("UT-05: 无检查记录应返回0")
    void testImportNoExamResults() {
        // Given - 模拟患者存在但无检查记录
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回空结果
        SqlQueryResult emptyResult = new SqlQueryResult();
        emptyResult.setSuccess(true);
        emptyResult.setData(Collections.emptyList());
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(emptyResult);

        // When - 调用导入方法
        int result = examinationSyncService.importExaminationResults(patientId);

        // Then - 验证返回0
        assertEquals(0, result, "无检查记录应返回0");
    }

    // ==================== UT-06: 正常导入测试 ====================

    /**
     * 测试：正常导入应返回正确的记录计数
     * 验收标准：正常导入返回正确计数
     */
    @Test
    @DisplayName("UT-06: 正常导入应返回正确计数")
    void testImportSuccessful() {
        // Given - 模拟患者存在和检查记录
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回3条检查记录
        List<Map<String, Object>> examRecords = createTestExamRecords(3);
        SqlQueryResult queryResult = new SqlQueryResult();
        queryResult.setSuccess(true);
        queryResult.setData(examRecords);
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(queryResult);
        
        // 模拟无重复记录
        when(examinationResultRepository.findByExaminationId(anyString())).thenReturn(Optional.empty());
        
        // 模拟保存操作
        when(examinationResultRepository.save(any(ExaminationResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - 调用导入方法
        int result = examinationSyncService.importExaminationResults(patientId);

        // Then - 验证返回正确计数
        assertEquals(3, result, "应返回导入的记录数3");
        verify(examinationResultRepository, times(3)).save(any(ExaminationResult.class));
    }

    // ==================== UT-07: 重复记录更新测试 ====================

    /**
     * 测试：重复记录应更新而非插入
     * 验收标准：重复记录执行更新
     */
    @Test
    @DisplayName("UT-07: 重复记录应更新而非插入")
    void testImportWithDuplicateRecord() {
        // Given - 模拟患者存在
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回2条检查记录
        List<Map<String, Object>> examRecords = createTestExamRecords(2);
        SqlQueryResult queryResult = new SqlQueryResult();
        queryResult.setSuccess(true);
        queryResult.setData(examRecords);
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(queryResult);
        
        // 模拟第一条记录已存在（需要更新），第二条不存在（新插入）
        ExaminationResult existingResult = new ExaminationResult();
        existingResult.setExaminationId("EXAM_001");
        existingResult.setPatientId(patientId.replace("-", "_"));
        existingResult.setCheckConclusion("旧的检查结论");
        
        when(examinationResultRepository.findByExaminationId("EXAM_001"))
            .thenReturn(Optional.of(existingResult));
        when(examinationResultRepository.findByExaminationId("EXAM_002"))
            .thenReturn(Optional.empty());
        
        // 模拟保存操作
        when(examinationResultRepository.save(any(ExaminationResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - 调用导入方法
        int result = examinationSyncService.importExaminationResults(patientId);

        // Then - 验证返回2（1个更新+1个新增）
        assertEquals(2, result, "应返回处理的记录数2");
        verify(examinationResultRepository, times(2)).save(any(ExaminationResult.class));
    }

    // ==================== UT-08: CLOB字段处理测试 ====================

    /**
     * 测试：CLOB字段应正确转换和处理
     * 验收标准：CLOB字段正确处理
     */
    @Test
    @DisplayName("UT-08: CLOB字段应正确处理")
    void testConvertToExaminationResult() {
        // Given - 创建包含CLOB字段的测试数据
        Map<String, Object> oracleData = new HashMap<>();
        oracleData.put("EXAMINATION_ID", "EXAM_CLOB_001");
        oracleData.put("CHECK_NAME", "胸部CT");
        oracleData.put("CHECK_TYPE", "CT");
        oracleData.put("PATIENT_ID", "990500000178405");
        oracleData.put("CHECK_DESCRIPTION", "这是一段很长的检查所见描述，模拟CLOB字段内容...");
        oracleData.put("CHECK_CONCLUSION", "检查结论：未见明显异常。这也是CLOB字段内容。");
        oracleData.put("CHECK_ISSUE_TIME", new Timestamp(System.currentTimeMillis()));
        oracleData.put("CHECK_EXECUTE_TIME", new Timestamp(System.currentTimeMillis()));
        oracleData.put("CHECK_REPORT_TIME", new Timestamp(System.currentTimeMillis()));
        oracleData.put("UPDATE_DT", new Timestamp(System.currentTimeMillis()));

        String targetPatientId = "990500000178405_1";

        // When - 调用转换方法
        ExaminationResult result = examinationSyncService.convertToExaminationResult(oracleData, targetPatientId);

        // Then - 验证CLOB字段正确转换
        assertNotNull(result, "转换结果不应为null");
        assertEquals("EXAM_CLOB_001", result.getExaminationId(), "ExaminationID应正确映射");
        assertEquals("胸部CT", result.getCheckName(), "CheckName应正确映射");
        assertEquals("CT", result.getCheckType(), "CheckType应正确映射");
        assertEquals(targetPatientId, result.getPatientId(), "PatientID应为目标格式");
        assertNotNull(result.getCheckDescription(), "CheckDescription（CLOB）不应为null");
        assertTrue(result.getCheckDescription().contains("检查所见"), "CheckDescription应包含CLOB内容");
        assertNotNull(result.getCheckConclusion(), "CheckConclusion（CLOB）不应为null");
        assertTrue(result.getCheckConclusion().contains("检查结论"), "CheckConclusion应包含CLOB内容");
        assertNotNull(result.getCheckIssueTime(), "CheckIssueTime应正确转换");
        assertNotNull(result.getCheckReportTime(), "CheckReportTime应正确转换");
        assertEquals(0, result.getIsAnalyzed(), "IsAnalyzed应为0（未分析）");
    }

    /**
     * 测试：NULL的CLOB字段应正确处理
     * 边界条件测试
     */
    @Test
    @DisplayName("UT-08b: NULL CLOB字段应正确处理")
    void testConvertToExaminationResult_NullClobFields() {
        // Given - 创建CLOB字段为null的测试数据
        Map<String, Object> oracleData = new HashMap<>();
        oracleData.put("EXAMINATION_ID", "EXAM_NULL_CLOB");
        oracleData.put("CHECK_NAME", "血常规");
        oracleData.put("CHECK_TYPE", "检验");
        oracleData.put("PATIENT_ID", "990500000178405");
        oracleData.put("CHECK_DESCRIPTION", null);  // NULL CLOB
        oracleData.put("CHECK_CONCLUSION", null);   // NULL CLOB
        oracleData.put("CHECK_ISSUE_TIME", new Timestamp(System.currentTimeMillis()));

        String targetPatientId = "990500000178405_1";

        // When - 调用转换方法
        ExaminationResult result = examinationSyncService.convertToExaminationResult(oracleData, targetPatientId);

        // Then - 验证NULL CLOB字段处理
        assertNotNull(result, "转换结果不应为null");
        assertEquals("EXAM_NULL_CLOB", result.getExaminationId());
        // NULL CLOB应被处理为空字符串或保持null（根据实现决定）
        // 这里允许两种情况
        assertTrue(result.getCheckDescription() == null || result.getCheckDescription().isEmpty(),
            "NULL CheckDescription应为null或空字符串");
        assertTrue(result.getCheckConclusion() == null || result.getCheckConclusion().isEmpty(),
            "NULL CheckConclusion应为null或空字符串");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用Patient对象
     */
    private Patient createTestPatient(String patientId) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setName("测试患者");
        patient.setGender("男");
        patient.setDepartment("心血管一病区");
        patient.setBedNumber("01");
        patient.setAdmissionTime(new Date());
        patient.setIsInHospital(true);
        return patient;
    }

    /**
     * 设置医院配置相关的Mock
     */
    private void setupHospitalConfigMocks() {
        // 模拟医院配置列表
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        hospital.setId("hospital-Local");
        hospital.setName("本地测试医院");
        hospital.setIntegrationType("database");
        config.setHospital(hospital);
        when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.singletonList(config));
        
        // 模拟模板路径配置
        when(examSyncConfig.getTemplateFilePath(anyString()))
            .thenReturn("sql/hospital-local/exam-results-query.json");
        
        // 模拟模板加载
        SqlTemplate template = new SqlTemplate();
        template.setQueryName("getExamResults");
        template.setSql("SELECT * FROM V_EXAM_REPORT_INFO WHERE PATIENT_ID = :patientId");
        when(templateHotUpdateService.loadTemplate(anyString())).thenReturn(template);
    }

    /**
     * 创建测试用检查记录列表
     */
    private List<Map<String, Object>> createTestExamRecords(int count) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("EXAMINATION_ID", "EXAM_00" + i);
            record.put("CHECK_NAME", "检查项目" + i);
            record.put("CHECK_TYPE", "CT");
            record.put("PATIENT_ID", "990500000178405");
            record.put("CHECK_DESCRIPTION", "检查所见描述" + i);
            record.put("CHECK_CONCLUSION", "检查结论" + i);
            record.put("CHECK_ISSUE_TIME", new Timestamp(System.currentTimeMillis()));
            record.put("CHECK_EXECUTE_TIME", new Timestamp(System.currentTimeMillis()));
            record.put("CHECK_REPORT_TIME", new Timestamp(System.currentTimeMillis()));
            record.put("UPDATE_DT", new Timestamp(System.currentTimeMillis()));
            records.add(record);
        }
        return records;
    }
}
