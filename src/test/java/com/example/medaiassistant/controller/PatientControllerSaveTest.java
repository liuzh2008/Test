package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.PatientSaveRequest;
import com.example.medaiassistant.dto.PatientSaveResponse;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 患者控制器保存功能集成测试
 * 
 * 测试PatientController的保存患者数据API端点
 * 
 * @author System
 * @since 2025-12-10
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DisplayName("患者控制器保存功能集成测试")
class PatientControllerSaveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        patientRepository.deleteAll();
    }

    /**
     * 测试保存新患者数据
     */
    @Test
    @DisplayName("测试保存新患者数据 - 应返回成功响应")
    void saveNewPatientShouldReturnSuccessResponse() throws Exception {
        // 准备测试数据
        PatientSaveRequest request = createTestPatientRequest("TEST_NEW_001", "测试患者001", "测试科室", "普通");

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/patients/save-or-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        PatientSaveResponse response = objectMapper.readValue(responseContent, PatientSaveResponse.class);

        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("created", response.getOperation(), "操作类型应为created");
        assertEquals("TEST_NEW_001", response.getPatientId(), "患者ID应匹配");
        assertNotNull(response.getMessage(), "消息不应为null");
        assertNotNull(response.getTimestamp(), "时间戳不应为null");

        // 验证数据库
        Optional<Patient> savedPatient = patientRepository.findById("TEST_NEW_001");
        assertTrue(savedPatient.isPresent(), "患者应保存到数据库");
        assertEquals("测试患者001", savedPatient.get().getName(), "患者姓名应匹配");
        assertEquals("测试科室", savedPatient.get().getDepartment(), "科室应匹配");
        assertEquals("普通", savedPatient.get().getStatus(), "状态应匹配");
    }

    /**
     * 测试更新现有患者数据
     */
    @Test
    @DisplayName("测试更新现有患者数据 - 应返回成功响应")
    void updateExistingPatientShouldReturnSuccessResponse() throws Exception {
        // 先保存一个患者
        Patient existingPatient = createTestPatient("TEST_UPDATE_001", "原始患者", "原始科室", "普通");
        patientRepository.save(existingPatient);

        // 准备更新数据
        PatientSaveRequest request = createTestPatientRequest("TEST_UPDATE_001", "更新后的患者", "更新科室", "病重");
        request.setGender("女");
        request.setBedNumber("202");

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/patients/save-or-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        PatientSaveResponse response = objectMapper.readValue(responseContent, PatientSaveResponse.class);

        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("updated", response.getOperation(), "操作类型应为updated");
        assertEquals("TEST_UPDATE_001", response.getPatientId(), "患者ID应匹配");

        // 验证数据库
        Optional<Patient> updatedPatient = patientRepository.findById("TEST_UPDATE_001");
        assertTrue(updatedPatient.isPresent(), "患者应在数据库中");
        assertEquals("更新后的患者", updatedPatient.get().getName(), "患者姓名应更新");
        assertEquals("更新科室", updatedPatient.get().getDepartment(), "科室应更新");
        assertEquals("病重", updatedPatient.get().getStatus(), "状态应更新");
        assertEquals("女", updatedPatient.get().getGender(), "性别应更新");
        assertEquals("202", updatedPatient.get().getBedNumber(), "床位号应更新");
    }

    /**
     * 测试缺少必填字段的请求
     */
    @Test
    @DisplayName("测试缺少必填字段的请求 - 应返回验证错误")
    void savePatientWithMissingRequiredFieldsShouldReturnValidationError() throws Exception {
        // 准备缺少必填字段的测试数据
        PatientSaveRequest request = new PatientSaveRequest();
        request.setPatientId("TEST_MISSING_001");
        request.setName("测试患者");
        // 缺少department字段
        // 缺少status字段

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/patients/save-or-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        PatientSaveResponse response = objectMapper.readValue(responseContent, PatientSaveResponse.class);

        assertFalse(response.isSuccess(), "响应应表示失败");
        assertEquals("error", response.getOperation(), "操作类型应为error");
        assertEquals("TEST_MISSING_001", response.getPatientId(), "患者ID应匹配");
        assertTrue(response.getMessage().contains("数据验证失败"), "消息应包含验证失败信息");
        assertTrue(response.getMessage().contains("科室不能为空") || response.getMessage().contains("状态不能为空"), 
                "消息应包含具体的验证错误");

        // 验证数据库中没有保存数据
        Optional<Patient> savedPatient = patientRepository.findById("TEST_MISSING_001");
        assertFalse(savedPatient.isPresent(), "患者不应保存到数据库");
    }

    /**
     * 测试字段长度超过限制的请求
     */
    @Test
    @DisplayName("测试字段长度超过限制的请求 - 应返回验证错误")
    void savePatientWithFieldLengthExceededShouldReturnValidationError() throws Exception {
        // 准备字段长度超过限制的测试数据
        PatientSaveRequest request = new PatientSaveRequest();
        request.setPatientId("TEST_LENGTH_001");
        request.setName("测试患者".repeat(100)); // 超过255字符
        request.setDepartment("测试科室");
        request.setStatus("普通".repeat(20)); // 超过20字符

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/patients/save-or-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        PatientSaveResponse response = objectMapper.readValue(responseContent, PatientSaveResponse.class);

        assertFalse(response.isSuccess(), "响应应表示失败");
        assertEquals("error", response.getOperation(), "操作类型应为error");
        assertEquals("TEST_LENGTH_001", response.getPatientId(), "患者ID应匹配");
        assertTrue(response.getMessage().contains("数据验证失败"), "消息应包含验证失败信息");
        assertTrue(response.getMessage().contains("长度不能超过"), "消息应包含长度限制错误");

        // 验证数据库中没有保存数据
        Optional<Patient> savedPatient = patientRepository.findById("TEST_LENGTH_001");
        assertFalse(savedPatient.isPresent(), "患者不应保存到数据库");
    }

    /**
     * 测试完整的患者数据保存
     */
    @Test
    @DisplayName("测试完整的患者数据保存 - 应返回成功响应")
    void savePatientWithAllFieldsShouldReturnSuccessResponse() throws Exception {
        // 准备完整的测试数据
        PatientSaveRequest request = createFullTestPatientRequest("TEST_FULL_001");

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/patients/save-or-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证响应
        String responseContent = result.getResponse().getContentAsString();
        PatientSaveResponse response = objectMapper.readValue(responseContent, PatientSaveResponse.class);

        assertTrue(response.isSuccess(), "响应应表示成功");
        assertEquals("created", response.getOperation(), "操作类型应为created");
        assertEquals("TEST_FULL_001", response.getPatientId(), "患者ID应匹配");

        // 验证数据库
        Optional<Patient> savedPatient = patientRepository.findById("TEST_FULL_001");
        assertTrue(savedPatient.isPresent(), "患者应保存到数据库");
        assertEquals("完整患者", savedPatient.get().getName(), "患者姓名应匹配");
        assertEquals("心血管一病区", savedPatient.get().getDepartment(), "科室应匹配");
        assertEquals("普通", savedPatient.get().getStatus(), "状态应匹配");
        assertEquals("510123198001011234", savedPatient.get().getIdCard(), "身份证号应匹配");
        assertEquals("MRN_TEST001", savedPatient.get().getMedicalRecordNumber(), "病历号应匹配");
        assertEquals("男", savedPatient.get().getGender(), "性别应匹配");
        assertEquals("301", savedPatient.get().getBedNumber(), "床位号应匹配");
        assertTrue(savedPatient.get().getIsInHospital(), "是否在院应为true");
        assertEquals("这是一个完整的测试患者数据", savedPatient.get().getImportantInformation(), "重要信息应匹配");
        assertEquals("PATI_TEST001", savedPatient.get().getPatiId(), "患者内部ID应匹配");
        assertEquals(1, savedPatient.get().getVisitId(), "就诊ID应匹配");
        assertEquals("PATIENT_NO_001", savedPatient.get().getPatientNo(), "患者编号应匹配");
        assertEquals("DRGs分析结果：测试", savedPatient.get().getDrgsResult(), "DRGs结果应匹配");
        assertFalse(savedPatient.get().getDrgsSevereComplication(), "DRGs严重并发症应为false");
        assertFalse(savedPatient.get().getDrgsCommonComplication(), "DRGs常见并发症应为false");
    }

    /**
     * 创建测试患者请求
     */
    private PatientSaveRequest createTestPatientRequest(String patientId, String name, String department, String status) {
        PatientSaveRequest request = new PatientSaveRequest();
        request.setPatientId(patientId);
        request.setName(name);
        request.setGender("男");
        request.setDateOfBirth(new Date());
        request.setBedNumber("101");
        request.setAdmissionTime(new Date());
        request.setIsInHospital(true);
        request.setDepartment(department);
        request.setStatus(status);
        return request;
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

    /**
     * 创建测试患者实体
     */
    private Patient createTestPatient(String patientId, String name, String department, String status) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setName(name);
        patient.setGender("男");
        patient.setDateOfBirth(new Date());
        patient.setBedNumber("101");
        patient.setAdmissionTime(new Date());
        patient.setIsInHospital(true);
        patient.setDepartment(department);
        patient.setStatus(status);
        return patient;
    }
}
