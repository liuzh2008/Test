package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 仅科室过滤策略测试
 * 测试DepartmentOnlyFilterStrategy的功能和行为
 */
@ExtendWith(MockitoExtension.class)
public class DepartmentOnlyFilterStrategyTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SchedulingProperties.TimerConfig timerConfig;

    @Mock
    private Pageable pageable;

    /**
     * 测试仅科室过滤策略的基本功能
     * 验证策略名称、适用条件和过滤逻辑
     */
    @Test
    void testDepartmentOnlyFilterStrategyBasicFunctions() {
        DepartmentOnlyFilterStrategy strategy = new DepartmentOnlyFilterStrategy();

        // 测试策略名称
        assertEquals("仅科室过滤", strategy.getStrategyName());

        // 测试适用条件
        assertTrue(!strategy.isApplicable(SchedulingProperties.TimerConfig.FilterMode.NONE));
        assertTrue(strategy.isApplicable(SchedulingProperties.TimerConfig.FilterMode.DEPARTMENT_ONLY));
    }

    /**
     * 测试仅科室过滤策略的过滤逻辑
     * 验证策略会返回指定科室的所有在院患者
     */
    @Test
    void testDepartmentOnlyFilterStrategyFilterLogic() {
        DepartmentOnlyFilterStrategy strategy = new DepartmentOnlyFilterStrategy();

        // 准备测试数据
        Patient patient1 = new Patient();
        patient1.setPatientId("1");
        patient1.setDepartment("心血管一病区");
        patient1.setBedNumber("101");
        patient1.setIsInHospital(true);

        Patient patient2 = new Patient();
        patient2.setPatientId("2");
        patient2.setDepartment("心血管一病区");
        patient2.setBedNumber("102");
        patient2.setIsInHospital(true);

        List<Patient> testPatients = List.of(patient1, patient2);
        Page<Patient> testPage = new PageImpl<>(testPatients, pageable, testPatients.size());

        // 设置Mock行为
        List<String> targetDepartments = List.of("心血管一病区");
        when(timerConfig.getTargetDepartments()).thenReturn(targetDepartments);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(targetDepartments, true, pageable)).thenReturn(testPage);

        // 执行测试
        List<Patient> result = strategy.filterPatients(patientRepository, timerConfig, pageable);

        // 验证结果
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getPatientId());
        assertEquals("2", result.get(1).getPatientId());
        assertEquals("心血管一病区", result.get(0).getDepartment());
        assertEquals("心血管一病区", result.get(1).getDepartment());
    }

    /**
     * 测试仅科室过滤策略处理空结果
     * 验证当没有符合条件的患者时，策略返回空列表
     */
    @Test
    void testDepartmentOnlyFilterStrategyWithEmptyResult() {
        DepartmentOnlyFilterStrategy strategy = new DepartmentOnlyFilterStrategy();

        // 设置Mock行为，返回空结果
        List<String> targetDepartments = List.of("不存在的科室");
        when(timerConfig.getTargetDepartments()).thenReturn(targetDepartments);
        
        // 先创建空的Page对象，避免在stubbing中使用mock对象
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(targetDepartments, true, pageable)).thenReturn(emptyPage);

        // 执行测试
        List<Patient> result = strategy.filterPatients(patientRepository, timerConfig, pageable);

        // 验证结果
        assertTrue(result.isEmpty());
    }
}
