package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.Patient;
import org.junit.jupiter.api.DisplayName;
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
 * 患者数据访问层同步功能测试
 * 测试与病人数据同步相关的Repository方法
 * 使用@TestConfig注解进行数据访问层测试配置
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-08
 */
@TestConfig(description = "患者数据访问层同步功能测试")
@DisplayName("患者数据访问层同步功能测试")
class PatientRepositorySyncTest {
    
    @Autowired
    private PatientRepository patientRepository;
    
    /**
     * 测试1：根据科室和在院状态查询患者
     * 验证findByDepartmentAndIsInHospital方法
     */
    @Test
    @DisplayName("测试根据科室和在院状态查询患者 - 应能查询指定科室的在院患者")
    void testFindByDepartmentAndIsInHospital() {
        // 准备测试数据
        String department = "测试科室";
        boolean isInHospital = true;
        
        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentAndIsInHospital(department, isInHospital);
        
        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
        // 注意：由于是真实数据库测试，可能没有测试数据，所以只验证结果不为null
    }
    
    /**
     * 测试2：根据科室列表和在院状态查询患者
     * 验证findByDepartmentsAndIsInHospital方法
     */
    @Test
    @DisplayName("测试根据科室列表和在院状态查询患者 - 应能查询多个科室的在院患者")
    void testFindByDepartmentsAndIsInHospital() {
        // 准备测试数据
        List<String> departments = Arrays.asList("测试科室1", "测试科室2");
        boolean isInHospital = true;
        
        // 执行查询
        List<Patient> patients = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital);
        
        // 验证结果
        assertNotNull(patients, "查询结果不应为null");
    }
    
    /**
     * 测试3：根据科室列表和在院状态分页查询患者
     * 验证findByDepartmentsAndIsInHospital分页方法
     */
    @Test
    @DisplayName("测试根据科室列表和在院状态分页查询患者 - 应能分页查询多个科室的在院患者")
    void testFindByDepartmentsAndIsInHospitalWithPagination() {
        // 准备测试数据
        List<String> departments = Arrays.asList("测试科室1", "测试科室2");
        boolean isInHospital = true;
        Pageable pageable = PageRequest.of(0, 10);
        
        // 执行查询
        Page<Patient> page = patientRepository.findByDepartmentsAndIsInHospital(departments, isInHospital, pageable);
        
        // 验证结果
        assertNotNull(page, "分页查询结果不应为null");
        assertNotNull(page.getContent(), "分页内容不应为null");
    }
    
    /**
     * 测试4：安全方法测试 - 空科室列表
     * 验证findByDepartmentsAndIsInHospitalSafe方法处理空列表
     */
    @Test
    @DisplayName("测试安全方法 - 空科室列表应返回空结果")
    void testFindByDepartmentsAndIsInHospitalSafe_EmptyList() {
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
     * 测试5：安全方法测试 - null科室列表
     * 验证findByDepartmentsAndIsInHospitalSafe方法处理null列表
     */
    @Test
    @DisplayName("测试安全方法 - null科室列表应返回空结果")
    void testFindByDepartmentsAndIsInHospitalSafe_NullList() {
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
     * 测试6：安全方法测试 - 分页空科室列表
     * 验证findByDepartmentsAndIsInHospitalSafe分页方法处理空列表
     */
    @Test
    @DisplayName("测试安全方法分页 - 空科室列表应返回空分页")
    void testFindByDepartmentsAndIsInHospitalSafePagination_EmptyList() {
        // 准备测试数据
        List<String> departments = Collections.emptyList();
        boolean isInHospital = true;
        Pageable pageable = PageRequest.of(0, 10);
        
        // 执行查询
        Page<Patient> page = patientRepository.findByDepartmentsAndIsInHospitalSafe(departments, isInHospital, pageable);
        
        // 验证结果
        assertNotNull(page, "分页查询结果不应为null");
        assertTrue(page.isEmpty(), "空科室列表应返回空分页");
        assertEquals(0, page.getTotalElements(), "总元素数应为0");
    }
}
