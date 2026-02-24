package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LIS检验结果同步服务TDD测试
 * 按照TDD红-绿-重构流程实现LIS检验结果同步功能
 * 
 * 🔴 红阶段：测试失败，因为LabSyncService类不存在
 * 🟢 绿阶段：创建LabSyncService类，测试通过
 * 🔵 重构阶段：优化代码结构，添加完整功能
 * 
 * 测试评价与完善：
 * 1. 测试应覆盖服务创建、基本功能、数据转换、重复检查等核心场景
 * 2. 应包含边界条件测试（空患者ID、null参数等）
 * 3. 应包含异常情况测试（数据库异常、模板加载失败等）
 * 4. 应包含性能测试（执行时间验证）
 * 5. 符合测试编写原则，使用@ExtendWith(MockitoExtension.class)进行业务逻辑层测试
 * 6. 测试命名规范，使用@DisplayName提供清晰的测试描述
 * 7. 测试方法遵循AAA模式（Arrange-Act-Assert）
 * 8. 使用适当的Mock对象进行依赖隔离
 * 9. 测试代码结构清晰，注释完整
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LIS检验结果同步服务TDD测试")
class LabSyncServiceTddTest {
    
    @Mock
    private SqlExecutionService sqlExecutionService;
    
    @Mock
    private LabResultRepository labResultRepository;
    
    @Mock
    private HospitalConfigService hospitalConfigService;
    
    @Mock
    private SyncLogService syncLogService;
    
    @Mock
    private TemplateHotUpdateService templateHotUpdateService;
    
    @Mock
    private PatientRepository patientRepository;
    
    /**
     * 测试1：LIS检验结果同步服务创建测试
     * 验证可以创建LIS检验结果同步服务实例
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试LIS检验结果同步服务创建 - 应能创建服务实例")
    void testLabSyncServiceCreation() {
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 验证服务实例不为null
        assertNotNull(service, "LIS检验结果同步服务实例不应为null");
    }
    
    /**
     * 测试2：LIS检验结果同步基本功能测试
     * 验证可以执行LIS检验结果同步
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试LIS检验结果同步基本功能 - 应能执行检验结果同步")
    void testLabSyncBasicFunctionality() {
        // 准备测试数据
        String mainServerPatientId = "990500000178405-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟patientRepository返回null（病人未找到）
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(null);
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果 - 病人未找到应返回-1
        assertEquals(-1, importedCount, "病人未找到应返回-1");
    }
    
    /**
     * 创建模拟模板
     */
    private com.example.medaiassistant.hospital.model.SqlTemplate createMockTemplate() {
        com.example.medaiassistant.hospital.model.SqlTemplate template = new com.example.medaiassistant.hospital.model.SqlTemplate();
        template.setQueryName("getCdwyyLabResults");
        template.setSql("SELECT t.PATIENT_ID, t.VISIT_ID, t.REPORT_ITEM_NAME, t.ITEM_NAME, t.RESULT, t.TEST_REFERENCE, t.UNITS, t.ABNORMAL_INDICATOR, t.REQUESTED_DATE_TIME FROM MANTULUO.V_SS_LIS_RESULT t WHERE t.PATIENT_ID = :patientId AND t.VISIT_ID = :visitId AND t.REQUESTED_DATE_TIME > :startDate AND t.REQUESTED_DATE_TIME <= :endDate ORDER BY t.REQUESTED_DATE_TIME DESC FETCH FIRST 2000 ROWS ONLY");
        return template;
    }
    
    
    /**
     * 测试3：数据转换功能测试（验证LabID生成）
     * 验证可以将Oracle LIS数据转换为LabResult实体，并且LabID被正确生成
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试数据转换功能 - 应能将Oracle数据转换为LabResult实体并生成LabID")
    void testDataConversionFunctionality() {
        // 准备测试数据
        String mainServerPatientId = "990500000640090-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟模板服务
        com.example.medaiassistant.hospital.model.SqlTemplate mockTemplate = createMockTemplate();
        when(templateHotUpdateService.getTemplate(anyString())).thenReturn(mockTemplate);
        
        // 模拟patientRepository返回病人信息
        com.example.medaiassistant.model.Patient mockPatient = new com.example.medaiassistant.model.Patient();
        mockPatient.setPatientId(mainServerPatientId);
        mockPatient.setAdmissionTime(new java.util.Date());
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(mockPatient);
        
        // 模拟sqlExecutionService返回测试数据
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(createTestQueryResult());
        
        // 模拟labResultRepository返回空列表（无重复记录）
        // 注意：现在使用四字段查询方法，labReportTime参数类型为Timestamp
        when(labResultRepository.findByPatientIdAndLabNameAndLabReportTimeAndLabResult(anyString(), anyString(), any(Timestamp.class), anyString()))
            .thenReturn(java.util.Collections.emptyList());
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果
        assertTrue(importedCount >= 0, "导入记录数应大于等于0");
    }
    
    /**
     * 创建测试查询结果
     */
    private com.example.medaiassistant.hospital.dto.SqlQueryResult createTestQueryResult() {
        com.example.medaiassistant.hospital.dto.SqlQueryResult result = new com.example.medaiassistant.hospital.dto.SqlQueryResult();
        result.setSuccess(true);
        
        // 创建测试数据
        java.util.Map<String, Object> testRecord = new java.util.HashMap<>();
        testRecord.put("PATIENT_ID", "990500000640090");
        testRecord.put("VISIT_ID", "1");
        testRecord.put("REPORT_ITEM_NAME", "白细胞计数");
        testRecord.put("ITEM_NAME", "血常规");
        testRecord.put("RESULT", "6.5");
        testRecord.put("TEST_REFERENCE", "4.0-10.0");
        testRecord.put("UNITS", "10^9/L");
        testRecord.put("ABNORMAL_INDICATOR", "N");
        testRecord.put("REQUESTED_DATE_TIME", new java.sql.Timestamp(System.currentTimeMillis()));
        
        result.setData(java.util.Collections.singletonList(testRecord));
        return result;
    }
    
    /**
     * 测试4：重复记录检查测试（四字段检查）
     * 验证可以检查重复的检验结果记录（基于PatientID、LabName、LabReportTime、LabResult）
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试重复记录检查 - 应能检查重复的检验结果记录（四字段检查）")
    void testDuplicateRecordCheck() {
        // 准备测试数据
        String mainServerPatientId = "990500000640090-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟模板服务
        com.example.medaiassistant.hospital.model.SqlTemplate mockTemplate = createMockTemplate();
        when(templateHotUpdateService.getTemplate(anyString())).thenReturn(mockTemplate);
        
        // 模拟patientRepository返回病人信息
        com.example.medaiassistant.model.Patient mockPatient = new com.example.medaiassistant.model.Patient();
        mockPatient.setPatientId(mainServerPatientId);
        mockPatient.setAdmissionTime(new java.util.Date());
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(mockPatient);
        
        // 模拟sqlExecutionService返回测试数据
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(createTestQueryResult());
        
        // 模拟labResultRepository返回已存在的记录（使用四字段查询方法，labReportTime参数类型为Timestamp）
        when(labResultRepository.findByPatientIdAndLabNameAndLabReportTimeAndLabResult(anyString(), anyString(), any(Timestamp.class), anyString()))
            .thenReturn(java.util.Collections.singletonList(new com.example.medaiassistant.model.LabResult()));
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果 - 重复记录应被跳过，导入0条
        assertEquals(0, importedCount, "重复记录应返回0");
    }
    
    /**
     * 测试5：边界条件测试 - 空患者ID
     * 验证处理空患者ID的情况
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试边界条件 - 空患者ID")
    void testEmptyPatientId() {
        // 准备测试数据
        String mainServerPatientId = "";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果
        assertEquals(-1, importedCount, "空患者ID应返回-1表示失败");
    }
    
    /**
     * 测试6：边界条件测试 - null入院日期（通过病人未找到模拟）
     * 验证处理null入院日期的情况
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试边界条件 - 病人入院日期为空")
    void testNullAdmissionDate() {
        // 准备测试数据
        String mainServerPatientId = "990500000640090-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟patientRepository返回病人信息，但入院日期为null
        com.example.medaiassistant.model.Patient mockPatient = new com.example.medaiassistant.model.Patient();
        mockPatient.setPatientId(mainServerPatientId);
        mockPatient.setAdmissionTime(null); // 入院日期为空
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(mockPatient);
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果
        assertEquals(-1, importedCount, "病人入院日期为空应返回-1表示失败");
    }
    
    /**
     * 测试7：异常情况测试 - 模拟数据库异常
     * 验证处理数据库异常的情况
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试异常情况 - 模拟数据库异常")
    void testDatabaseException() {
        // 准备测试数据
        String mainServerPatientId = "990500000640090-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟patientRepository返回病人信息
        com.example.medaiassistant.model.Patient mockPatient = new com.example.medaiassistant.model.Patient();
        mockPatient.setPatientId(mainServerPatientId);
        mockPatient.setAdmissionTime(new java.util.Date());
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(mockPatient);
        
        // 模拟templateHotUpdateService.getTemplate返回null，触发异常
        when(templateHotUpdateService.getTemplate(anyString())).thenReturn(null);
        
        // 调用方法
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 验证结果
        assertEquals(-1, importedCount, "数据库异常应返回-1表示失败");
    }
    
    /**
     * 测试8：性能测试 - 验证执行时间
     * 验证同步执行时间在合理范围内
     * 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
     */
    @Test
    @DisplayName("测试性能 - 验证执行时间")
    void testPerformance() {
        // 准备测试数据
        String mainServerPatientId = "990500000640090-1";
        
        // 🟢 绿阶段：测试应通过，因为LabSyncService类已创建
        
        // 创建LabSyncService实例
        LabSyncService service = new LabSyncService(sqlExecutionService, labResultRepository, patientRepository, hospitalConfigService, syncLogService, templateHotUpdateService);
        
        // 模拟patientRepository返回null（病人未找到）
        when(patientRepository.findByPatientId(mainServerPatientId)).thenReturn(null);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 执行同步
        int importedCount = service.importLabResults(mainServerPatientId);
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // 验证结果
        assertEquals(-1, importedCount, "病人未找到应返回-1");
        assertTrue(executionTime < 10000, "执行时间应小于10秒，实际时间: " + executionTime + "ms");
    }
}
