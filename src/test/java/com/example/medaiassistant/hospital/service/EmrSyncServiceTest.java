package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.model.EmrContent;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EmrSyncService核心服务单元测试
 * 使用Mockito进行业务逻辑层测试，不加载Spring上下文
 * 
 * <p><strong>测试覆盖</strong>：</p>
 * <ul>
 *   <li>参数验证测试 (3个) - 验证null/空patientId、患者不存在的情况</li>
 *   <li>数据同步测试 (3个) - 验证无记录、成功导入、重复记录更新</li>
 *   <li>数据转换测试 (4个) - 验证字段映射、CLOB处理、时间戳转换、null处理</li>
 *   <li>去重逻辑测试 (3个) - 验证SOURCE_TABLE+SOURCE_ID去重逻辑</li>
 *   <li>性能测试 (2个) - 验证批量转换和稀疏数据处理性能</li>
 * </ul>
 * 
 * <p><strong>TDD流程</strong>：</p>
 * <ol>
 *   <li>红阶段：编写13个失败测试用例</li>
 *   <li>绿阶段：实现EmrSyncService使测试通过</li>
 *   <li>重构阶段：添加性能测试，优化代码结构</li>
 * </ol>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-11
 * @see EmrSyncService
 * @see EmrContentRepository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmrSyncService 核心服务测试")
class EmrSyncServiceTest {

    @Mock
    private EmrContentRepository emrContentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SqlExecutionService sqlExecutionService;

    @Mock
    private HospitalConfigService hospitalConfigService;

    @Mock
    private TemplateHotUpdateService templateHotUpdateService;

    private EmrSyncService emrSyncService;

    @BeforeEach
    void setUp() {
        emrSyncService = new EmrSyncService(
                sqlExecutionService,
                emrContentRepository,
                patientRepository,
                hospitalConfigService,
                templateHotUpdateService
        );
        // 设置默认配置属性
        ReflectionTestUtils.setField(emrSyncService, "templateFilePath", "sql/hospital-local/emr-content-query.json");
        ReflectionTestUtils.setField(emrSyncService, "defaultHospitalId", "Local");
    }

    // ==================== 参数验证测试 ====================

    @Nested
    @DisplayName("参数验证测试")
    class ParameterValidationTests {

        @Test
        @DisplayName("当patientId为null时，应返回-1")
        void testImportEmrContent_NullPatientId_ReturnsNegative() {
            // Given
            String nullPatientId = null;

            // When
            int result = emrSyncService.importEmrContent(nullPatientId);

            // Then
            assertEquals(-1, result, "null patientId应返回-1");
            verifyNoInteractions(patientRepository);
        }

        @Test
        @DisplayName("当patientId为空字符串时，应返回-1")
        void testImportEmrContent_EmptyPatientId_ReturnsNegative() {
            // Given
            String emptyPatientId = "";

            // When
            int result = emrSyncService.importEmrContent(emptyPatientId);

            // Then
            assertEquals(-1, result, "空patientId应返回-1");
            verifyNoInteractions(patientRepository);
        }

        @Test
        @DisplayName("当患者不存在时，应返回-1")
        void testImportEmrContent_PatientNotFound_ReturnsNegative() {
            // Given
            String patientId = "990500000178405-1";
            when(patientRepository.findByPatientId(patientId)).thenReturn(null);

            // When
            int result = emrSyncService.importEmrContent(patientId);

            // Then
            assertEquals(-1, result, "患者不存在应返回-1");
            verify(patientRepository).findByPatientId(patientId);
        }
    }

    // ==================== 数据同步测试 ====================

    @Nested
    @DisplayName("数据同步测试")
    class DataSyncTests {

        @Test
        @DisplayName("当Oracle无记录时，应返回0")
        void testImportEmrContent_NoRecords_ReturnsZero() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            
            // 模拟Oracle返回空数据
            mockOracleReturnsEmptyData();

            // When
            int result = emrSyncService.importEmrContent(patientId);

            // Then
            assertEquals(0, result, "无记录时应返回0");
        }

        @Test
        @DisplayName("成功导入EMR记录时，应返回导入数量")
        void testImportEmrContent_Success_ReturnsCount() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            
            // 模拟Oracle返回3条数据
            mockOracleReturnsData(3);
            
            // 模拟保存操作
            when(emrContentRepository.findBySourceTableAndSourceId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(emrContentRepository.save(any(EmrContent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            int result = emrSyncService.importEmrContent(patientId);

            // Then
            assertEquals(3, result, "应返回导入的记录数3");
            verify(emrContentRepository, times(3)).save(any(EmrContent.class));
        }

        @Test
        @DisplayName("重复记录应更新而非新增")
        void testImportEmrContent_DuplicateRecord_UpdatesExisting() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            
            // 模拟Oracle返回1条数据
            mockOracleReturnsData(1);
            
            // 模拟已存在的记录
            EmrContent existingRecord = new EmrContent();
            existingRecord.setId(1L);
            existingRecord.setSourceTable("emr.emr_content");
            existingRecord.setSourceId("source-id-0");
            when(emrContentRepository.findBySourceTableAndSourceId("emr.emr_content", "source-id-0"))
                    .thenReturn(Optional.of(existingRecord));
            when(emrContentRepository.save(any(EmrContent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            int result = emrSyncService.importEmrContent(patientId);

            // Then
            assertEquals(1, result, "应返回处理的记录数");
            verify(emrContentRepository, times(1)).save(existingRecord);
        }
    }

    // ==================== 数据转换测试 ====================

    @Nested
    @DisplayName("数据转换测试")
    class DataConversionTests {

        @Test
        @DisplayName("所有字段应正确映射")
        void testConvertToEmrContent_AllFieldsMapped() {
            // Given
            Map<String, Object> oracleData = createFullOracleData();
            String targetPatientId = "990500000178405-1";

            // When
            EmrContent result = emrSyncService.convertToEmrContent(oracleData, targetPatientId);

            // Then
            assertNotNull(result, "转换结果不应为null");
            assertEquals(targetPatientId, result.getPatientId(), "patientId应正确设置");
            assertEquals("pati-001", result.getPatiId(), "patiId应正确映射");
            assertEquals(1, result.getVisitId(), "visitId应正确映射");
            assertEquals("张三", result.getPatiName(), "patiName应正确映射");
            assertEquals("D001", result.getDeptCode(), "deptCode应正确映射");
            assertEquals("心血管科", result.getDeptName(), "deptName应正确映射");
            assertEquals("入院记录", result.getDocTypeName(), "docTypeName应正确映射");
            assertEquals("source-id-001", result.getSourceId(), "sourceId应正确映射");
        }

        @Test
        @DisplayName("CLOB字段应正确处理")
        void testConvertToEmrContent_ClobFieldHandled() {
            // Given
            Map<String, Object> oracleData = new HashMap<>();
            oracleData.put("SOURCE_ID", "source-001");
            oracleData.put("CONTENT", "这是一段很长的病历内容，包含患者的详细病情描述...");

            // When
            EmrContent result = emrSyncService.convertToEmrContent(oracleData, "patient-001");

            // Then
            assertNotNull(result.getContent(), "CONTENT字段应正确处理");
            assertEquals("这是一段很长的病历内容，包含患者的详细病情描述...", result.getContent());
        }

        @Test
        @DisplayName("时间戳字段应正确转换")
        void testConvertToEmrContent_TimestampConversion() {
            // Given
            Map<String, Object> oracleData = new HashMap<>();
            oracleData.put("SOURCE_ID", "source-001");
            Timestamp recordDate = new Timestamp(System.currentTimeMillis());
            oracleData.put("RECORD_DATE", recordDate);
            oracleData.put("DOC_TITLE_TIME", "2026-01-11 10:30:00");
            oracleData.put("MODIFIEDON", new Date());

            // When
            EmrContent result = emrSyncService.convertToEmrContent(oracleData, "patient-001");

            // Then
            assertEquals(recordDate, result.getRecordDate(), "Timestamp类型应直接使用");
            assertNotNull(result.getDocTitleTime(), "字符串日期应转换为Timestamp");
            assertNotNull(result.getModifiedOn(), "Date类型应转换为Timestamp");
        }

        @Test
        @DisplayName("null字段应正确处理")
        void testConvertToEmrContent_NullFieldsHandled() {
            // Given
            Map<String, Object> oracleData = new HashMap<>();
            oracleData.put("SOURCE_ID", "source-001");
            oracleData.put("PATI_NAME", null);
            oracleData.put("CONTENT", null);
            oracleData.put("RECORD_DATE", null);

            // When
            EmrContent result = emrSyncService.convertToEmrContent(oracleData, "patient-001");

            // Then
            assertNotNull(result, "结果不应为null");
            assertEquals("", result.getPatiName(), "null字符串字段应返回空字符串");
            assertEquals("", result.getContent(), "null CLOB字段应返回空字符串");
            assertNull(result.getRecordDate(), "null时间字段应保持null");
        }
    }

    // ==================== 性能测试 ====================

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {

        @Test
        @DisplayName("数据转换性能测试 - 批量转换应在合理时间内完成")
        void testConvertToEmrContent_Performance() {
            // Given - 准备100条测试数据
            String targetPatientId = "990500000178405-1";
            int recordCount = 100;
            List<Map<String, Object>> testDataList = new ArrayList<>();
            for (int i = 0; i < recordCount; i++) {
                testDataList.add(createFullOracleData());
            }

            // When - 执行批量转换并计时
            long startTime = System.currentTimeMillis();
            List<EmrContent> results = new ArrayList<>();
            for (Map<String, Object> data : testDataList) {
                results.add(emrSyncService.convertToEmrContent(data, targetPatientId));
            }
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Then - 验证性能：100条记录转换应在1秒内完成
            assertEquals(recordCount, results.size(), "应转换所有记录");
            assertTrue(executionTime < 1000, 
                    String.format("100条记录转换应在1秒内完成，实际耗时: %dms", executionTime));
            System.out.println("[性能指标] 100条记录转换耗时: " + executionTime + "ms，平均: " + (executionTime / recordCount) + "ms/条");
        }

        @Test
        @DisplayName("空值处理性能测试 - 处理稀疏数据应高效")
        void testNullFieldsHandling_Performance() {
            // Given - 准备50条含大量null字段的数据
            String targetPatientId = "990500000178405-1";
            int recordCount = 50;
            List<Map<String, Object>> sparseDataList = new ArrayList<>();
            for (int i = 0; i < recordCount; i++) {
                Map<String, Object> sparseData = new HashMap<>();
                sparseData.put("SOURCE_ID", "source-" + i);
                // 大部分字段为null，模拟稀疏数据
                sparseDataList.add(sparseData);
            }

            // When
            long startTime = System.currentTimeMillis();
            for (Map<String, Object> data : sparseDataList) {
                emrSyncService.convertToEmrContent(data, targetPatientId);
            }
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - 稀疏数据处理应快速
            assertTrue(executionTime < 500, 
                    String.format("50条稀疏记录处理应在500ms内完成，实际耗时: %dms", executionTime));
            System.out.println("[性能指标] 50条稀疏记录处理耗时: " + executionTime + "ms");
        }
    }

    // ==================== 去重逻辑测试 ====================

    @Nested
    @DisplayName("去重逻辑测试")
    class DeduplicationTests {

        @Test
        @DisplayName("应使用SOURCE_TABLE和SOURCE_ID组合进行去重判断")
        void testDeduplication_BySourceTableAndSourceId() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            mockOracleReturnsData(1);
            when(emrContentRepository.findBySourceTableAndSourceId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(emrContentRepository.save(any(EmrContent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            emrSyncService.importEmrContent(patientId);

            // Then
            verify(emrContentRepository).findBySourceTableAndSourceId("emr.emr_content", "source-id-0");
        }

        @Test
        @DisplayName("已存在记录应执行更新")
        void testDeduplication_UpdateExistingRecord() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            mockOracleReturnsData(1);
            
            EmrContent existingRecord = new EmrContent();
            existingRecord.setId(100L);
            existingRecord.setSourceTable("emr.emr_content");
            existingRecord.setSourceId("source-id-0");
            existingRecord.setContent("旧内容");
            when(emrContentRepository.findBySourceTableAndSourceId("emr.emr_content", "source-id-0"))
                    .thenReturn(Optional.of(existingRecord));
            when(emrContentRepository.save(any(EmrContent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            emrSyncService.importEmrContent(patientId);

            // Then
            verify(emrContentRepository).save(argThat(emr -> 
                emr.getId() != null && emr.getId().equals(100L)
            ));
        }

        @Test
        @DisplayName("新记录应执行插入并设置SOURCE_TABLE和SOURCE_ID")
        void testDeduplication_InsertNewRecord() {
            // Given
            String patientId = "990500000178405-1";
            Patient patient = createMockPatient(patientId);
            when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
            mockOracleReturnsData(1);
            when(emrContentRepository.findBySourceTableAndSourceId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(emrContentRepository.save(any(EmrContent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            emrSyncService.importEmrContent(patientId);

            // Then
            verify(emrContentRepository).save(argThat(emr -> 
                emr.getSourceTable() != null && 
                emr.getSourceTable().equals("emr.emr_content") &&
                emr.getSourceId() != null
            ));
        }
    }

    // ==================== 辅助方法 ====================

    private Patient createMockPatient(String patientId) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        // Patient类使用name字段
        return patient;
    }

    private Map<String, Object> createFullOracleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("SOURCE_ID", "source-id-001");
        data.put("PATI_ID", "pati-001");
        data.put("VISIT_ID", 1);
        data.put("PATI_NAME", "张三");
        data.put("DEPT_CODE", "D001");
        data.put("DEPT_NAME", "心血管科");
        data.put("DOC_TYPE_NAME", "入院记录");
        data.put("RECORD_DATE", new Timestamp(System.currentTimeMillis()));
        data.put("CONTENT", "病历内容...");
        data.put("CREATEUSERID", "user001");
        data.put("CREATEBY", "医生A");
        data.put("DOC_TITLE_TIME", new Timestamp(System.currentTimeMillis()));
        data.put("MODIFIEDON", new Timestamp(System.currentTimeMillis()));
        data.put("DELETEMARK", 0);
        return data;
    }

    private void mockOracleReturnsEmptyData() {
        // 模拟配置服务返回空配置
        HospitalConfig config = createMockHospitalConfig("Local");
        when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.singletonList(config));
        
        // 模拟模板加载
        SqlTemplate template = new SqlTemplate();
        template.setSql("SELECT * FROM emr.emr_content WHERE PATI_ID = :patiId");
        when(templateHotUpdateService.loadTemplate(anyString())).thenReturn(template);
        
        // 模拟SQL执行返回空数据
        SqlQueryResult emptyResult = SqlQueryResult.success(
                Collections.emptyList(), 
                Collections.emptyList(), 
                10L, 
                "SELECT * FROM emr.emr_content WHERE PATI_ID = :patiId"
        );
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(emptyResult);
    }

    private void mockOracleReturnsData(int count) {
        // 模拟配置服务返回配置
        HospitalConfig config = createMockHospitalConfig("Local");
        when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.singletonList(config));
        
        // 模拟模板加载
        SqlTemplate template = new SqlTemplate();
        template.setSql("SELECT * FROM emr.emr_content WHERE PATI_ID = :patiId");
        when(templateHotUpdateService.loadTemplate(anyString())).thenReturn(template);
        
        // 生成模拟数据
        List<Map<String, Object>> mockData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("SOURCE_ID", "source-id-" + i);
            record.put("PATI_ID", "pati-" + i);
            record.put("VISIT_ID", 1);
            record.put("PATI_NAME", "测试患者" + i);
            record.put("DEPT_CODE", "D00" + i);
            record.put("DEPT_NAME", "测试科室" + i);
            record.put("DOC_TYPE_NAME", "入院记录");
            record.put("RECORD_DATE", new Timestamp(System.currentTimeMillis()));
            record.put("CONTENT", "病历内容" + i);
            record.put("CREATEUSERID", "user" + i);
            record.put("CREATEBY", "医生" + i);
            record.put("DOC_TITLE_TIME", new Timestamp(System.currentTimeMillis()));
            record.put("MODIFIEDON", new Timestamp(System.currentTimeMillis()));
            record.put("DELETEMARK", 0);
            mockData.add(record);
        }
        
        // 模拟SQL执行返回数据
        SqlQueryResult dataResult = SqlQueryResult.success(
                mockData, 
                Arrays.asList("SOURCE_ID", "PATI_ID", "VISIT_ID", "PATI_NAME", "DEPT_CODE", "DEPT_NAME", 
                        "DOC_TYPE_NAME", "RECORD_DATE", "CONTENT", "CREATEUSERID", "CREATEBY", 
                        "DOC_TITLE_TIME", "MODIFIEDON", "DELETEMARK"), 
                10L, 
                "SELECT * FROM emr.emr_content WHERE PATI_ID = :patiId"
        );
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(dataResult);
    }
    
    /**
     * 创建模拟HospitalConfig
     */
    private HospitalConfig createMockHospitalConfig(String hospitalId) {
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        hospital.setId(hospitalId);
        hospital.setName("测试医院");
        hospital.setIntegrationType("database");
        config.setHospital(hospital);
        return config;
    }
}
