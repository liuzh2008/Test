package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.PatientSaveRequest;
import com.example.medaiassistant.dto.PatientSaveResponse;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 患者保存服务单元测试
 * 
 * 测试PatientSaveService的保存患者数据逻辑
 * 
 * @author System
 * @since 2025-12-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("患者保存服务单元测试")
class PatientSaveServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientSaveService patientSaveService;

    private PatientSaveRequest validRequest;
    private Patient existingPatient;

    @BeforeEach
    void setUp() {
        // 准备有效的请求数据
        validRequest = new PatientSaveRequest();
        validRequest.setPatientId("TEST_001");
        validRequest.setName("测试患者");
        validRequest.setDepartment("测试科室");
        validRequest.setStatus("普通");
        validRequest.setGender("男");
        validRequest.setDateOfBirth(new Date());
        validRequest.setBedNumber("101");
        validRequest.setAdmissionTime(new Date());
        validRequest.setIsInHospital(true);

        // 准备现有患者数据
        existingPatient = new Patient();
        existingPatient.setPatientId("TEST_001");
        existingPatient.setName("原始患者");
        existingPatient.setDepartment("原始科室");
        existingPatient.setStatus("普通");
        existingPatient.setGender("男");
        existingPatient.setDateOfBirth(new Date());
        existingPatient.setBedNumber("101");
        existingPatient.setAdmissionTime(new Date());
        existingPatient.setIsInHospital(true);
    }

    /**
     * 测试保存新患者数据
     */
    @Test
    @DisplayName("测试保存新患者数据 - 应返回成功响应")
    void saveNewPatientShouldReturnSuccessResponse() {
        // 模拟患者不存在
        when(patientRepository.findById("TEST_001")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            return savedPatient;
        });

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(validRequest);

        // 验证结果
        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("created", response.getOperation(), "操作类型应为created");
        assertEquals("TEST_001", response.getPatientId(), "患者ID应匹配");
        assertNotNull(response.getMessage(), "消息不应为null");
        assertNotNull(response.getTimestamp(), "时间戳不应为null");

        // 验证方法调用
        verify(patientRepository, times(1)).findById("TEST_001");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    /**
     * 测试更新现有患者数据
     */
    @Test
    @DisplayName("测试更新现有患者数据 - 应返回成功响应")
    void updateExistingPatientShouldReturnSuccessResponse() {
        // 模拟患者已存在
        when(patientRepository.findById("TEST_001")).thenReturn(Optional.of(existingPatient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            return savedPatient;
        });

        // 准备更新数据
        validRequest.setName("更新后的患者");
        validRequest.setDepartment("更新科室");
        validRequest.setStatus("病重");
        validRequest.setGender("女");
        validRequest.setBedNumber("202");

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(validRequest);

        // 验证结果
        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("updated", response.getOperation(), "操作类型应为updated");
        assertEquals("TEST_001", response.getPatientId(), "患者ID应匹配");

        // 验证方法调用
        verify(patientRepository, times(1)).findById("TEST_001");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    /**
     * 测试缺少必填字段的请求
     */
    @Test
    @DisplayName("测试缺少必填字段的请求 - 应返回验证错误")
    void savePatientWithMissingRequiredFieldsShouldReturnValidationError() {
        // 准备缺少必填字段的请求
        PatientSaveRequest invalidRequest = new PatientSaveRequest();
        invalidRequest.setPatientId("TEST_MISSING_001");
        invalidRequest.setName("测试患者");
        // 缺少department字段
        // 缺少status字段

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(invalidRequest);

        // 验证结果
        assertFalse(response.isSuccess(), "响应应表示失败");
        assertEquals("error", response.getOperation(), "操作类型应为error");
        assertEquals("TEST_MISSING_001", response.getPatientId(), "患者ID应匹配");
        assertTrue(response.getMessage().contains("数据验证失败"), "消息应包含验证失败信息");
        assertTrue(response.getMessage().contains("科室不能为空") || response.getMessage().contains("状态不能为空"), 
                "消息应包含具体的验证错误");

        // 验证方法没有被调用
        verify(patientRepository, never()).findById(anyString());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    /**
     * 测试字段长度超过限制的请求
     */
    @Test
    @DisplayName("测试字段长度超过限制的请求 - 应返回验证错误")
    void savePatientWithFieldLengthExceededShouldReturnValidationError() {
        // 准备字段长度超过限制的请求
        PatientSaveRequest invalidRequest = new PatientSaveRequest();
        invalidRequest.setPatientId("TEST_LENGTH_001");
        invalidRequest.setName("测试患者".repeat(100)); // 超过255字符
        invalidRequest.setDepartment("测试科室");
        invalidRequest.setStatus("普通".repeat(20)); // 超过20字符

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(invalidRequest);

        // 验证结果
        assertFalse(response.isSuccess(), "响应应表示失败");
        assertEquals("error", response.getOperation(), "操作类型应为error");
        assertEquals("TEST_LENGTH_001", response.getPatientId(), "患者ID应匹配");
        assertTrue(response.getMessage().contains("数据验证失败"), "消息应包含验证失败信息");
        assertTrue(response.getMessage().contains("长度不能超过"), "消息应包含长度限制错误");

        // 验证方法没有被调用
        verify(patientRepository, never()).findById(anyString());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    /**
     * 测试数据库异常处理
     */
    @Test
    @DisplayName("测试数据库异常处理 - 应返回数据库错误")
    void savePatientWithDatabaseExceptionShouldReturnDatabaseError() {
        // 模拟数据库异常
        when(patientRepository.findById("TEST_001")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(validRequest);

        // 验证结果
        assertFalse(response.isSuccess(), "响应应表示失败");
        assertEquals("error", response.getOperation(), "操作类型应为error");
        assertEquals("TEST_001", response.getPatientId(), "患者ID应匹配");
        assertTrue(response.getMessage().contains("数据库操作失败"), "消息应包含数据库错误信息");

        // 验证方法调用
        verify(patientRepository, times(1)).findById("TEST_001");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    /**
     * 测试完整的患者数据保存
     */
    @Test
    @DisplayName("测试完整的患者数据保存 - 应返回成功响应")
    void savePatientWithAllFieldsShouldReturnSuccessResponse() {
        // 准备完整的请求数据
        PatientSaveRequest fullRequest = createFullTestPatientRequest("TEST_FULL_001");

        // 模拟患者不存在
        when(patientRepository.findById("TEST_FULL_001")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            return savedPatient;
        });

        // 执行测试
        PatientSaveResponse response = patientSaveService.savePatient(fullRequest);

        // 验证结果
        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("created", response.getOperation(), "操作类型应为created");
        assertEquals("TEST_FULL_001", response.getPatientId(), "患者ID应匹配");

        // 验证方法调用
        verify(patientRepository, times(1)).findById("TEST_FULL_001");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    /**
     * 创建完整的测试患者请求
     */
    private PatientSaveRequest createFullTestPatientRequest(String patientId) {
        PatientSaveRequest request = new PatientSaveRequest();
        request.setPatientId(patientId);
        request.setIdCard("510123198001011234");
        request.setMedicalRecordNumber("MRN_TEST001");
        request.setName("完整患者");
        request.setGender("男");
        request.setDateOfBirth(new Date());
        request.setBedNumber("301");
        request.setAdmissionTime(new Date());
        request.setDischargeTime(null);
        request.setIsInHospital(true);
        request.setDepartment("心血管一病区");
        request.setImportantInformation("这是一个完整的测试患者数据");
        request.setPatiId("PATI_TEST001");
        request.setVisitId(1);
        request.setPatientNo("PATIENT_NO_001");
        request.setDrgsResult("DRGs分析结果：测试");
        request.setDrgsSevereComplication(false);
        request.setDrgsCommonComplication(false);
        request.setStatus("普通");
        return request;
    }
}
