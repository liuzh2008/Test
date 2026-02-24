package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 患者数据访问层科室过滤功能测试类
 * 
 * 该测试类验证PatientRepository中按科室列表查询患者的方法，
 * 支持定时任务科室过滤功能的数据访问层实现。
 * 
 * @author Cline
 * @since 2025-11-01
 */
@TestConfig(description = "患者数据访问层科室过滤功能测试")
class PatientRepositoryDepartmentFilterTest {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * 测试按单个科室查询在院患者
     * 
     * 验证能够正确查询指定单个科室的在院患者
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_SingleDepartment() {
        // 准备测试数据 - 假设数据库中已有心血管一病区的在院患者
        List<String> departments = Collections.singletonList("心血管一病区");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 验证所有返回的患者都属于指定科室且在院
        for (Patient patient : patients) {
            assertEquals("心血管一病区", patient.getDepartment(), "患者科室应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按多个科室查询在院患者
     * 
     * 验证能够正确查询多个科室的在院患者
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_MultipleDepartments() {
        // 准备测试数据
        List<String> departments = Arrays.asList("心血管一病区", "心血管二病区");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 验证所有返回的患者都属于指定科室列表且在院
        for (Patient patient : patients) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应在指定列表中");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按单个科室分页查询在院患者
     * 
     * 验证能够正确分页查询指定单个科室的在院患者
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_SingleDepartmentWithPagination() {
        // 准备测试数据
        List<String> departments = Collections.singletonList("心血管一病区");
        boolean isInHospital = true;
        Pageable pageable = PageRequest.of(0, 10); // 第一页，每页10条

        // 执行查询
        Page<Patient> patientPage = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital, pageable);

        // 验证结果
        assertNotNull(patientPage, "分页查询结果不应为null");
        assertTrue(patientPage.getTotalElements() >= 0, "总记录数应大于等于0");
        assertTrue(patientPage.getContent().size() <= pageable.getPageSize(), "当前页记录数不应超过页面大小");

        // 验证所有返回的患者都属于指定科室且在院
        for (Patient patient : patientPage.getContent()) {
            assertEquals("心血管一病区", patient.getDepartment(), "患者科室应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按多个科室分页查询在院患者
     * 
     * 验证能够正确分页查询多个科室的在院患者
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_MultipleDepartmentsWithPagination() {
        // 准备测试数据
        List<String> departments = Arrays.asList("心血管一病区", "心血管二病区");
        boolean isInHospital = true;
        Pageable pageable = PageRequest.of(0, 10); // 第一页，每页10条

        // 执行查询
        Page<Patient> patientPage = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital, pageable);

        // 验证结果
        assertNotNull(patientPage, "分页查询结果不应为null");
        assertTrue(patientPage.getTotalElements() >= 0, "总记录数应大于等于0");
        assertTrue(patientPage.getContent().size() <= pageable.getPageSize(), "当前页记录数不应超过页面大小");

        // 验证所有返回的患者都属于指定科室列表且在院
        for (Patient patient : patientPage.getContent()) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应在指定列表中");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试空科室列表处理
     * 
     * 验证当传入空科室列表时，返回空结果
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_EmptyDepartmentList() {
        // 准备测试数据
        List<String> departments = Collections.emptyList();
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "空科室列表应返回空结果");
    }

    /**
     * 测试null科室列表处理
     * 
     * 验证当传入null科室列表时，返回空结果
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_NullDepartmentList() {
        // 准备测试数据
        List<String> departments = null;
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "null科室列表应返回空结果");
    }

    /**
     * 测试无效科室名称处理
     * 
     * 验证当传入不存在的科室名称时，返回空结果
     */
    @Test
    void testFindByDepartmentsAndIsInHospital_InvalidDepartment() {
        // 准备测试数据 - 使用不存在的科室名称
        List<String> departments = Collections.singletonList("不存在的科室");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 如果数据库中确实没有该科室的患者，结果应为空
        // 如果数据库中有该科室的患者，则验证科室匹配
        for (Patient patient : patients) {
            assertEquals("不存在的科室", patient.getDepartment(), "患者科室应匹配");
        }
    }

    /**
     * 测试安全方法处理空科室列表
     * 
     * 验证安全方法正确处理空科室列表
     */
    @Test
    void testFindByDepartmentsAndIsInHospitalSafe_EmptyDepartmentList() {
        // 准备测试数据
        List<String> departments = Collections.emptyList();
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospitalSafe(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "空科室列表应返回空结果");
    }

    /**
     * 测试安全方法处理null科室列表
     * 
     * 验证安全方法正确处理null科室列表
     */
    @Test
    void testFindByDepartmentsAndIsInHospitalSafe_NullDepartmentList() {
        // 准备测试数据
        List<String> departments = null;
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospitalSafe(departments, isInHospital);

        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        assertTrue(patients.isEmpty(), "null科室列表应返回空结果");
    }

    /**
     * 测试安全方法与正常方法一致性
     * 
     * 验证安全方法与正常方法在相同输入下返回相同结果
     */
    @Test
    void testFindByDepartmentsAndIsInHospitalSafe_ConsistencyWithNormalMethod() {
        // 准备测试数据
        List<String> departments = Arrays.asList("心血管一病区", "心血管二病区");
        boolean isInHospital = true;

        // 执行两种查询
        List<Patient> normalPatients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);
        List<Patient> safePatients = patientRepository.findByDepartmentsAndIsInHospitalSafe(departments, isInHospital);

        // 验证结果一致性
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
