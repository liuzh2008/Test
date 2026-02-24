package com.example.medaiassistant.service;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.config.ApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SurgeryAnalysisService科室过滤功能单元测试
 * 
 * 测试SurgeryAnalysisService服务中的科室过滤逻辑，包括：
 * 1. 科室过滤开关控制
 * 2. 目标科室列表处理
 * 3. 手术分析任务过滤
 * 4. 异常处理机制
 * 
 * @author Cline
 * @since 2025-11-01
 */
@ExtendWith(MockitoExtension.class)
class SurgeryAnalysisServiceDepartmentFilterTest {

    @Mock
    private EmrRecordRepository emrRecordRepository;

    @Mock
    private EmrContentRepository emrContentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApiProperties apiProperties;

    private SchedulingProperties schedulingProperties;
    private SurgeryAnalysisService surgeryAnalysisService;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
        surgeryAnalysisService = new SurgeryAnalysisService(
            emrRecordRepository,
            emrContentRepository,
            restTemplate,
            apiProperties,
            schedulingProperties
        );
    }

    /**
     * 测试启用科室过滤时的患者查询逻辑
     * 当科室过滤启用且配置了目标科室时，应该只查询目标科室的患者
     */
    @Test
    void testFindPatientsWithUnanalyzedOperations_WithDepartmentFilterEnabled() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Arrays.asList("心血管一病区", "心血管二病区"));
        
        // 模拟目标科室患者数据
        List<String> expectedPatients = Arrays.asList("patient1", "patient2");
                
        // 模拟 Repository 调用 - 现在使用 emrContentRepository
        when(emrContentRepository.findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Arrays.asList("心血管一病区", "心血管二病区"))
        )).thenReturn(expectedPatients);
        
        // 执行测试
        List<String> actualPatients = surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        
        // 验证结果
        assertNotNull(actualPatients);
        assertEquals(2, actualPatients.size());
        assertEquals(expectedPatients, actualPatients);
                
        // 验证 Repository 调用
        verify(emrContentRepository).findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Arrays.asList("心血管一病区", "心血管二病区"))
        );
    }

    /**
     * 测试禁用科室过滤时的患者查询逻辑
     * 当科室过滤禁用时，应该查询所有在院患者
     */
    @Test
    void testFindPatientsWithUnanalyzedOperations_WithDepartmentFilterDisabled() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(false);
        
        // 模拟所有在院患者数据
        List<String> expectedPatients = Arrays.asList("patient1", "patient2", "patient3");
                
        // 模拟 Repository 调用 - 现在使用 emrContentRepository
        when(emrContentRepository.findInHospitalPatientsWithUnanalyzedOperations())
            .thenReturn(expectedPatients);
        
        // 执行测试
        List<String> actualPatients = surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        
        // 验证结果
        assertNotNull(actualPatients);
        assertEquals(3, actualPatients.size());
        assertEquals(expectedPatients, actualPatients);
                
        // 验证 Repository 调用
        verify(emrContentRepository).findInHospitalPatientsWithUnanalyzedOperations();
        verify(emrContentRepository, never()).findInHospitalPatientsWithUnanalyzedOperationsByDepartments(any());
    }

    /**
     * 测试空目标科室列表处理
     * 当科室过滤启用但目标科室列表为空时，应该返回空结果
     */
    @Test
    void testFindPatientsWithUnanalyzedOperations_WithEmptyTargetDepartments() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Collections.emptyList());
        
        // 模拟 Repository 调用返回空列表 - 现在使用 emrContentRepository
        when(emrContentRepository.findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Collections.emptyList())
        )).thenReturn(Collections.emptyList());
        
        // 执行测试
        List<String> actualPatients = surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        
        // 验证结果
        assertNotNull(actualPatients);
        assertTrue(actualPatients.isEmpty());
                
        // 验证 Repository 调用
        verify(emrContentRepository).findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Collections.emptyList())
        );
    }

    /**
     * 测试null目标科室列表处理
     * 当科室过滤启用但目标科室列表为null时，应该返回空结果
     */
    @Test
    void testFindPatientsWithUnanalyzedOperations_WithNullTargetDepartments() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(null);
        
        // 模拟 Repository 调用返回空列表
        // 注意: getTargetDepartments() 会将 null 转换为空列表，所以这里应该 mock 空列表 - 现在使用 emrContentRepository
        when(emrContentRepository.findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Collections.emptyList())
        )).thenReturn(Collections.emptyList());
        
        // 执行测试
        List<String> actualPatients = surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        
        // 验证结果
        assertNotNull(actualPatients);
        assertTrue(actualPatients.isEmpty());
                
        // 验证 Repository 调用
        verify(emrContentRepository).findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(
            eq(Collections.emptyList())
        );
    }

    /**
     * 测试科室过滤配置检查方法
     * 验证配置检查逻辑的正确性
     */
    @Test
    void testIsDepartmentFilterEnabled() {
        // 测试启用状态
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        assertTrue(surgeryAnalysisService.isDepartmentFilterEnabled());

        // 测试禁用状态
        schedulingProperties.getTimer().setDepartmentFilterEnabled(false);
        assertFalse(surgeryAnalysisService.isDepartmentFilterEnabled());
    }

    /**
     * 测试目标科室列表获取方法
     * 验证目标科室列表的正确获取
     */
    @Test
    void testGetTargetDepartments() {
        // 准备测试数据
        List<String> expectedDepartments = Arrays.asList("心血管一病区", "心血管二病区");
        schedulingProperties.getTimer().setTargetDepartments(expectedDepartments);

        // 执行测试
        List<String> actualDepartments = surgeryAnalysisService.getTargetDepartments();

        // 验证结果
        assertNotNull(actualDepartments);
        assertEquals(expectedDepartments, actualDepartments);
    }

    /**
     * 测试空目标科室列表获取
     * 验证空列表的正确处理
     */
    @Test
    void testGetTargetDepartments_EmptyList() {
        // 准备测试数据
        schedulingProperties.getTimer().setTargetDepartments(Collections.emptyList());

        // 执行测试
        List<String> actualDepartments = surgeryAnalysisService.getTargetDepartments();

        // 验证结果
        assertNotNull(actualDepartments);
        assertTrue(actualDepartments.isEmpty());
    }

    /**
     * 测试null目标科室列表获取
     * 验证null列表的正确处理
     */
    @Test
    void testGetTargetDepartments_NullList() {
        // 准备测试数据
        schedulingProperties.getTimer().setTargetDepartments(null);

        // 执行测试
        List<String> actualDepartments = surgeryAnalysisService.getTargetDepartments();

        // 验证结果
        assertNotNull(actualDepartments);
        assertTrue(actualDepartments.isEmpty());
    }

    /**
     * 测试科室过滤状态日志记录
     * 验证启用和禁用状态下的日志记录逻辑
     */
    @Test
    void testDepartmentFilterStatusLogging() {
        // 测试启用状态
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Arrays.asList("心血管一病区"));
        
        // 模拟Repository调用
        when(emrRecordRepository.findInHospitalPatientsWithUnanalyzedOperationsByDepartmentsSafe(any()))
            .thenReturn(Collections.emptyList());
        
        // 这里主要验证没有异常抛出，实际日志记录在集成测试中验证
        assertDoesNotThrow(() -> {
            surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        });

        // 测试禁用状态
        schedulingProperties.getTimer().setDepartmentFilterEnabled(false);
        
        // 模拟Repository调用
        when(emrRecordRepository.findInHospitalPatientsWithUnanalyzedOperations())
            .thenReturn(Collections.emptyList());
        
        assertDoesNotThrow(() -> {
            surgeryAnalysisService.findPatientsWithUnanalyzedOperations();
        });
    }
}
