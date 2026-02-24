package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 患者数据访问层床号过滤功能测试
 * 
 * @author Cline
 * @since 2025-11-27
 */
@TestConfig(description = "患者数据访问层床号过滤功能测试")
class PatientRepositoryBedFilterTest {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * 测试按床号列表查询患者
     * 
     * 验证能够正确查询指定床号列表的在院患者
     */
    @Test
    void testFindByBedNumbersAndIsInHospital() {
        // 准备测试数据
        List<String> bedNumbers = List.of("101", "102");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByBedNumbersAndIsInHospital(bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        // 验证所有返回的患者都属于指定床号且在院
        for (Patient patient : patients) {
            assertTrue(bedNumbers.contains(patient.getBedNumber()), "患者床号应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按科室+床号组合查询患者
     * 
     * 验证能够正确查询指定科室和床号列表的在院患者
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospital() {
        // 准备测试数据
        List<String> departments = List.of("心血管一病区");
        List<String> bedNumbers = List.of("101", "102");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospital(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        // 验证所有返回的患者都属于指定科室和床号且在院
        for (Patient patient : patients) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应匹配");
            assertTrue(bedNumbers.contains(patient.getBedNumber()), "患者床号应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按科室+床号组合分页查询患者
     * 
     * 验证能够正确分页查询指定科室和床号列表的在院患者
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospitalWithPagination() {
        // 准备测试数据
        List<String> departments = List.of("心血管一病区");
        List<String> bedNumbers = List.of("101", "102");
        boolean isInHospital = true;
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<Patient> page = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospital(departments, bedNumbers, isInHospital, pageable);

        // 验证结果
        assertNotNull(page);
        List<Patient> patients = page.getContent();
        // 验证所有返回的患者都属于指定科室和床号且在院
        for (Patient patient : patients) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应匹配");
            assertTrue(bedNumbers.contains(patient.getBedNumber()), "患者床号应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试安全方法处理空床号列表
     * 
     * 验证当床号列表为空时，安全方法返回空列表
     */
    @Test
    void testFindByBedNumbersAndIsInHospitalSafe_EmptyBedNumbers() {
        // 准备测试数据
        List<String> bedNumbers = Collections.emptyList();
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByBedNumbersAndIsInHospitalSafe(bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当床号列表为空时，应返回空列表");
    }

    /**
     * 测试安全方法处理空科室列表
     * 
     * 验证当科室列表为空时，安全方法返回空列表
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospitalSafe_EmptyDepartments() {
        // 准备测试数据
        List<String> departments = Collections.emptyList();
        List<String> bedNumbers = List.of("101", "102");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospitalSafe(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当科室列表为空时，应返回空列表");
    }

    /**
     * 测试安全方法处理空床号和科室列表
     * 
     * 验证当床号和科室列表都为空时，安全方法返回空列表
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospitalSafe_EmptyLists() {
        // 准备测试数据
        List<String> departments = Collections.emptyList();
        List<String> bedNumbers = Collections.emptyList();
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospitalSafe(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当科室和床号列表都为空时，应返回空列表");
    }

    /**
     * 测试安全方法处理null床号列表
     * 
     * 验证当床号列表为null时，安全方法返回空列表
     */
    @Test
    void testFindByBedNumbersAndIsInHospitalSafe_NullBedNumbers() {
        // 准备测试数据
        List<String> bedNumbers = null;
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByBedNumbersAndIsInHospitalSafe(bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当床号列表为null时，应返回空列表");
    }

    /**
     * 测试安全方法处理null科室列表
     * 
     * 验证当科室列表为null时，安全方法返回空列表
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospitalSafe_NullDepartments() {
        // 准备测试数据
        List<String> departments = null;
        List<String> bedNumbers = List.of("101", "102");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospitalSafe(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当科室列表为null时，应返回空列表");
    }

    /**
     * 测试安全方法处理null床号和科室列表
     * 
     * 验证当床号和科室列表都为null时，安全方法返回空列表
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospitalSafe_NullLists() {
        // 准备测试数据
        List<String> departments = null;
        List<String> bedNumbers = null;
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospitalSafe(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        assertTrue(patients.isEmpty(), "当科室和床号列表都为null时，应返回空列表");
    }

    /**
     * 测试按单个床号查询患者
     * 
     * 验证能够正确查询指定单个床号的在院患者
     */
    @Test
    void testFindByBedNumbersAndIsInHospital_SingleBedNumber() {
        // 准备测试数据
        List<String> bedNumbers = List.of("101");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByBedNumbersAndIsInHospital(bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        // 验证所有返回的患者都属于指定床号且在院
        for (Patient patient : patients) {
            assertTrue(bedNumbers.contains(patient.getBedNumber()), "患者床号应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }

    /**
     * 测试按单个科室和单个床号查询患者
     * 
     * 验证能够正确查询指定单个科室和单个床号的在院患者
     */
    @Test
    void testFindByDepartmentsAndBedNumbersAndIsInHospital_SingleDepartmentAndBedNumber() {
        // 准备测试数据
        List<String> departments = List.of("心血管一病区");
        List<String> bedNumbers = List.of("101");
        boolean isInHospital = true;

        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndBedNumbersAndIsInHospital(departments, bedNumbers, isInHospital);

        // 验证结果
        assertNotNull(patients);
        // 验证所有返回的患者都属于指定科室和床号且在院
        for (Patient patient : patients) {
            assertTrue(departments.contains(patient.getDepartment()), "患者科室应匹配");
            assertTrue(bedNumbers.contains(patient.getBedNumber()), "患者床号应匹配");
            assertTrue(patient.getIsInHospital(), "患者应在院状态");
        }
    }
}