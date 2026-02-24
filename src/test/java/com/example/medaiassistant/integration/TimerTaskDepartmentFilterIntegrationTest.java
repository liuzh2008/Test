package com.example.medaiassistant.integration;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务科室过滤集成测试
 * 
 * 使用自定义的@TestConfig注解，避免重复配置
 * 验证定时任务科室过滤功能的数据访问层实现
 * 
 * @author Cline
 * @since 2025-11-01
 */
@TestConfig(description = "定时任务科室过滤数据访问层测试")
@DisplayName("定时任务科室过滤 集成测试")
class TimerTaskDepartmentFilterIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * 🟢 绿阶段测试用例1：按单个科室查询在院患者
     * 验证能够正确查询指定单个科室的在院患者
     */
    @Test
    @DisplayName("应该正确查询单个科室的在院患者")
    void shouldFindPatientsBySingleDepartment() {
        // Given - 准备测试数据
        List<String> departments = Collections.singletonList("心血管一病区");
        boolean isInHospital = true;

        // When - 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // Then - 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 验证所有返回的患者都属于指定科室且在院
        for (Patient patient : patients) {
            assertEquals("心血管一病区", patient.getDepartment(), "患者科室应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 🟢 绿阶段测试用例2：按多个科室查询在院患者
     * 验证能够正确查询多个科室的在院患者
     */
    @Test
    @DisplayName("应该正确查询多个科室的在院患者")
    void shouldFindPatientsByMultipleDepartments() {
        // Given - 准备测试数据
        List<String> departments = Arrays.asList("心血管一病区", "心血管二病区");
        boolean isInHospital = true;

        // When - 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // Then - 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 验证所有返回的患者都属于指定科室列表且在院
        for (Patient patient : patients) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应在指定列表中");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 🟢 绿阶段测试用例3：空科室列表处理
     * 验证当传入空科室列表时，返回空结果
     */
    @Test
    @DisplayName("应该正确处理空科室列表")
    void shouldHandleEmptyDepartmentList() {
        // Given - 准备测试数据
        List<String> departments = Collections.emptyList();
        boolean isInHospital = true;

        // When - 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // Then - 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "空科室列表应返回空结果");
    }

    /**
     * 🟢 绿阶段测试用例4：null科室列表处理
     * 验证当传入null科室列表时，返回空结果
     */
    @Test
    @DisplayName("应该正确处理null科室列表")
    void shouldHandleNullDepartmentList() {
        // Given - 准备测试数据
        List<String> departments = null;
        boolean isInHospital = true;

        // When - 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // Then - 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "null科室列表应返回空结果");
    }

    /**
     * 🟢 绿阶段测试用例5：安全方法与正常方法一致性
     * 验证安全方法与正常方法在相同输入下返回相同结果
     */
    @Test
    @DisplayName("应该验证安全方法与正常方法一致性")
    void shouldVerifySafeMethodConsistency() {
        // Given - 准备测试数据
        List<String> departments = Arrays.asList("心血管一病区", "心血管二病区");
        boolean isInHospital = true;

        // When - 执行两种查询
        List<Patient> normalPatients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);
        List<Patient> safePatients = patientRepository.findByDepartmentsAndIsInHospitalSafe(departments, isInHospital);

        // Then - 验证结果一致性
        assertNotNull(normalPatients, "正常方法结果不应为null");
        assertNotNull(safePatients, "安全方法结果不应为null");
        assertEquals(normalPatients.size(), safePatients.size(), "两种方法应返回相同数量的记录");

        // 验证记录内容一致性
        for (int i = 0; i < normalPatients.size(); i++) {
            Patient normalPatient = normalPatients.get(i);
            Patient safePatient = safePatients.get(i);
            assertEquals(normalPatient.getPatientId(), safePatient.getPatientId(), "患者ID应一致");
            assertEquals(normalPatient.getDepartment(), safePatient.getDepartment(), "患者科室应一致");
            assertEquals(normalPatient.getIsInHospital(), safePatient.getIsInHospital(), "在院状态应一致");
        }
    }
}
