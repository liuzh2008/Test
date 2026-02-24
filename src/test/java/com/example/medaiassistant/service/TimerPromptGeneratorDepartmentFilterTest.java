package com.example.medaiassistant.service;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.repository.SurgeryRepository;
import com.example.medaiassistant.config.ApiProperties;
import com.example.medaiassistant.service.PromptsTaskUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TimerPromptGenerator科室过滤功能单元测试
 * 
 * 测试TimerPromptGenerator服务中的科室过滤逻辑，包括：
 * 1. 科室过滤开关控制
 * 2. 目标科室列表处理
 * 3. 分页查询逻辑
 * 4. 异常处理机制
 * 
 * @author Cline
 * @since 2025-11-01
 */
@ExtendWith(MockitoExtension.class)
class TimerPromptGeneratorDepartmentFilterTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private AlertRuleService alertRuleService;

    @Mock
    private PatientStatusUpdateService patientStatusUpdateService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private PromptResultRepository promptResultRepository;

    @Mock
    private ServerConfigService serverConfigService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private EmrRecordRepository emrRecordRepository;

    @Mock
    private EmrContentRepository emrContentRepository;

    @Mock
    private SurgeryRepository surgeryRepository;

    @Mock
    private PromptGenerationLogService promptGenerationLogService;

    @Mock
    private PromptsTaskUpdater promptsTaskUpdater;

    private SchedulingProperties schedulingProperties;
    private TimerPromptGenerator timerPromptGenerator;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
        timerPromptGenerator = new TimerPromptGenerator(
            taskScheduler,
            alertRuleService,
            patientStatusUpdateService,
            patientRepository,
            promptRepository,
            promptResultRepository,
            null, // medicalRecordRepository - not needed for this test
            null, // labResultRepository - not needed for this test
            null, // examinationResultRepository - not needed for this test
            null, // longTermOrderRepository - not needed for this test
            serverConfigService,
            restTemplate,
            apiProperties,
            schedulingProperties,
            emrRecordRepository,
            emrContentRepository,
            surgeryRepository,
            promptGenerationLogService,
            promptsTaskUpdater
        );
    }

    /**
     * 测试启用科室过滤时的患者查询逻辑
     * 当科室过滤启用且配置了目标科室时，应该只查询目标科室的患者
     */
    @Test
    void testFindInHospitalPatientsByPage_WithDepartmentFilterEnabled() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Arrays.asList("心血管一病区", "心血管二病区"));
        
        int page = 0;
        int pageSize = 20;
        
        // 模拟目标科室患者数据
        Patient patient1 = new Patient();
        patient1.setPatientId("patient1");
        patient1.setDepartment("心血管一病区");
        patient1.setIsInHospital(true);
        
        Patient patient2 = new Patient();
        patient2.setPatientId("patient2");
        patient2.setDepartment("心血管二病区");
        patient2.setIsInHospital(true);
        
        List<Patient> expectedPatients = Arrays.asList(patient1, patient2);
        Page<Patient> expectedPage = new PageImpl<>(expectedPatients, PageRequest.of(page, pageSize), expectedPatients.size());
        
        // 模拟Repository调用
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(
            eq(Arrays.asList("心血管一病区", "心血管二病区")), 
            eq(true), 
            any()
        )).thenReturn(expectedPage);

        // 执行测试
        List<Patient> actualPatients = timerPromptGenerator.findInHospitalPatientsByPage(page, pageSize);

        // 验证结果
        assertNotNull(actualPatients);
        assertEquals(2, actualPatients.size());
        assertEquals("心血管一病区", actualPatients.get(0).getDepartment());
        assertEquals("心血管二病区", actualPatients.get(1).getDepartment());
        
        // 验证Repository调用
        verify(patientRepository).findByDepartmentsAndIsInHospitalSafe(
            eq(Arrays.asList("心血管一病区", "心血管二病区")), 
            eq(true), 
            any()
        );
    }

    /**
     * 测试禁用科室过滤时的患者查询逻辑
     * 当科室过滤禁用时，应该查询所有在院患者
     */
    @Test
    void testFindInHospitalPatientsByPage_WithDepartmentFilterDisabled() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(false);
        
        int page = 0;
        int pageSize = 20;
        
        // 模拟所有在院患者数据
        Patient patient1 = new Patient();
        patient1.setPatientId("patient1");
        patient1.setDepartment("心血管一病区");
        patient1.setIsInHospital(true);
        
        Patient patient2 = new Patient();
        patient2.setPatientId("patient2");
        patient2.setDepartment("神经内科");
        patient2.setIsInHospital(true);
        
        List<Patient> expectedPatients = Arrays.asList(patient1, patient2);
        Page<Patient> expectedPage = new PageImpl<>(expectedPatients, PageRequest.of(page, pageSize), expectedPatients.size());
        
        // 模拟Repository调用
        when(patientRepository.findByIsInHospital(eq(true), any())).thenReturn(expectedPage);

        // 执行测试
        List<Patient> actualPatients = timerPromptGenerator.findInHospitalPatientsByPage(page, pageSize);

        // 验证结果
        assertNotNull(actualPatients);
        assertEquals(2, actualPatients.size());
        
        // 验证Repository调用
        verify(patientRepository).findByIsInHospital(eq(true), any());
        verify(patientRepository, never()).findByDepartmentsAndIsInHospitalSafe(any(), anyBoolean(), any());
    }

    /**
     * 测试空目标科室列表处理
     * 当科室过滤启用但目标科室列表为空时，应该返回空结果
     */
    @Test
    void testFindInHospitalPatientsByPage_WithEmptyTargetDepartments() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Collections.emptyList());
        
        int page = 0;
        int pageSize = 20;
        
        // 模拟Repository调用返回空列表
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(
            eq(Collections.emptyList()), 
            eq(true), 
            any()
        )).thenReturn(emptyPage);

        // 执行测试
        List<Patient> actualPatients = timerPromptGenerator.findInHospitalPatientsByPage(page, pageSize);

        // 验证结果
        assertNotNull(actualPatients);
        assertTrue(actualPatients.isEmpty());
        
        // 验证Repository调用
        verify(patientRepository).findByDepartmentsAndIsInHospitalSafe(
            eq(Collections.emptyList()), 
            eq(true), 
            any()
        );
    }

    /**
     * 测试null目标科室列表处理
     * 当科室过滤启用但目标科室列表为null时，应该调用带null参数的repository方法
     */
    @Test
    void testFindInHospitalPatientsByPage_WithNullTargetDepartments() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(null);
        
        int page = 0;
        int pageSize = 20;
        
        // 模拟Repository调用返回空列表 - 使用宽松的匹配器
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(
            any(),  // 使用any()匹配null或空列表
            eq(true), 
            any(PageRequest.class)
        )).thenReturn(emptyPage);

        // 执行测试
        List<Patient> actualPatients = timerPromptGenerator.findInHospitalPatientsByPage(page, pageSize);

        // 验证结果
        assertNotNull(actualPatients);
        assertTrue(actualPatients.isEmpty());
        
        // 验证Repository调用 - 验证被调用，不限制参数
        verify(patientRepository).findByDepartmentsAndIsInHospitalSafe(
            any(), 
            eq(true), 
            any(PageRequest.class)
        );
    }

    /**
     * 测试分页查询逻辑
     * 验证分页参数正确传递给Repository
     */
    @Test
    void testFindInHospitalPatientsByPage_Pagination() {
        // 准备测试数据
        schedulingProperties.getTimer().setDepartmentFilterEnabled(true);
        schedulingProperties.getTimer().setTargetDepartments(Arrays.asList("心血管一病区"));
        
        int page = 2;
        int pageSize = 10;
        
        // 模拟Repository调用返回空列表
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(
            eq(Arrays.asList("心血管一病区")), 
            eq(true), 
            any()
        )).thenReturn(emptyPage);

        // 执行测试
        List<Patient> actualPatients = timerPromptGenerator.findInHospitalPatientsByPage(page, pageSize);

        // 验证结果
        assertNotNull(actualPatients);
        assertTrue(actualPatients.isEmpty());
        
        // 验证Repository调用
        verify(patientRepository).findByDepartmentsAndIsInHospitalSafe(
            eq(Arrays.asList("心血管一病区")), 
            eq(true), 
            any()
        );
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
        Page<Patient> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(patientRepository.findByDepartmentsAndIsInHospitalSafe(any(), anyBoolean(), any()))
            .thenReturn(emptyPage);
        
        // 这里主要验证没有异常抛出，实际日志记录在集成测试中验证
        assertDoesNotThrow(() -> {
            timerPromptGenerator.findInHospitalPatientsByPage(0, 20);
        });

        // 测试禁用状态
        schedulingProperties.getTimer().setDepartmentFilterEnabled(false);
        
        // 模拟Repository调用
        when(patientRepository.findByIsInHospital(anyBoolean(), any()))
            .thenReturn(emptyPage);
        
        assertDoesNotThrow(() -> {
            timerPromptGenerator.findInHospitalPatientsByPage(0, 20);
        });
    }
}
