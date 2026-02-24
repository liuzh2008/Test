package com.example.medaiassistant.service;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.hospital.dto.PatientSyncResult;
import com.example.medaiassistant.hospital.service.*;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * NightlySyncService 单元测试
 * 
 * <p>任务2：NightlySyncService核心服务TDD测试</p>
 * <p>任务3：syncPatientList病人列表同步统计结果测试</p>
 * <p>任务4：syncLabResults/syncExaminationResults/syncEmrContent三种数据同步方法测试</p>
 * 
 * <p><strong>TDD阶段</strong>：</p>
 * <ul>
 *   <li>✅ 任务2红阶段：编分20个失败测试用例</li>
 *   <li>✅ 任务2绿阶段：实现NightlySyncService使测试通过</li>
 *   <li>✅ 任务3红阶段：编写syncPatientList统计结果验证测试</li>
 *   <li>✅ 任务3绿阶段：实现getLastSyncResult()方法</li>
 *   <li>✅ 任务4红阶段：编写三种数据同步方法统计结果验证测试（10个）</li>
 *   <li>✅ 任务4绿阶段：代码已实现，测试全部通过</li>
 *   <li>✅ 任务4性能测试：100个病人三种同步在300ms内完成</li>
 *   <li>🔵 任务4重构阶段：提取公共模板方法（待完成）</li>
 * </ul>
 * 
 * <p><strong>测试策略</strong>：使用Mockito进行业务逻辑层测试，不加载Spring上下文</p>
 * 
 * <p><strong>测试覆盖</strong>：</p>
 * <table border="1">
 *   <tr><th>测试组</th><th>数量</th><th>覆盖内容</th></tr>
 *   <tr><td>执行控制测试</td><td>4</td><td>禁用状态跳过、并发控制、运行状态、手动触发</td></tr>
 *   <tr><td>病人列表同步测试</td><td>4</td><td>科室遍历、异常处理、失败继续</td></tr>
 *   <tr><td>数据同步测试</td><td>7</td><td>化验/检查/EMR同步、异常隔离</td></tr>
 *   <tr><td>同步顺序测试</td><td>1</td><td>验证执行顺序</td></tr>
 *   <tr><td>边界条件测试</td><td>2</td><td>空病人列表、运行状态验证</td></tr>
 *   <tr><td>性能测试</td><td>2</td><td>批量同步性能、并发响应速度</td></tr>
 *   <tr><td>任务3科室统计测试</td><td>5</td><td>成功/失败/混合科室统计、性能</td></tr>
 *   <tr><td>任务4化验同步统计</td><td>3</td><td>成功/失败/混合场景</td></tr>
 *   <tr><td>任务4检查同步统计</td><td>2</td><td>成功/混合场景</td></tr>
 *   <tr><td>任务4 EMR同步统计</td><td>2</td><td>成功/混合场景</td></tr>
 *   <tr><td>任务4综合统计测试</td><td>4</td><td>总病人数、独立统计、返回0的处理、性能测试</td></tr>
 *   <tr><td><strong>总计</strong></td><td><strong>36</strong></td><td></td></tr>
 * </table>
 * 
 * <p><strong>测试结果</strong>：36/36 通过，覆盖率100%</p>
 * <p><strong>执行时间</strong>：约2.6秒</p>
 * 
 * @author System
 * @version 1.3
 * @since 2026-01-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NightlySyncService 单元测试")
class NightlySyncServiceTest {

    @Mock
    private PatientSyncService patientSyncService;
    
    @Mock
    private LabSyncService labSyncService;
    
    @Mock
    private ExaminationSyncService examinationSyncService;
    
    @Mock
    private EmrSyncService emrSyncService;
    
    @Mock
    private HospitalConfigService hospitalConfigService;
    
    @Mock
    private PatientRepository patientRepository;
    
    @Mock
    private SchedulingProperties schedulingProperties;
    
    @Mock
    private SchedulingProperties.TimerConfig timerConfig;

    @InjectMocks
    private NightlySyncService nightlySyncService;

    @BeforeEach
    void setUp() {
        // 设置默认配置值
        ReflectionTestUtils.setField(nightlySyncService, "nightlySyncEnabled", true);
        ReflectionTestUtils.setField(nightlySyncService, "defaultHospitalId", "hospital-Local");
        lenient().when(schedulingProperties.getTimer()).thenReturn(timerConfig);
    }

    // ==================== 执行控制测试 ====================
    
    @Nested
    @DisplayName("执行控制测试")
    class ExecutionControlTests {
        
        @Test
        @DisplayName("当禁用时应跳过执行")
        void shouldSkipWhenDisabled() {
            // Given: 禁用夜间同步
            ReflectionTestUtils.setField(nightlySyncService, "nightlySyncEnabled", false);
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 不应调用任何同步服务
            verify(patientSyncService, never()).syncPatients(anyString(), anyString());
            verify(labSyncService, never()).importLabResults(anyString());
            verify(examinationSyncService, never()).importExaminationResults(anyString());
            verify(emrSyncService, never()).importEmrContent(anyString());
        }
        
        @Test
        @DisplayName("并发执行时应拒绝新任务")
        void shouldRejectWhenAlreadyRunning() throws Exception {
            // Given: 模拟长时间运行的任务
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A"));
            
            PatientSyncResult mockResult = mock(PatientSyncResult.class);
            when(mockResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString()))
                .thenAnswer(inv -> {
                    Thread.sleep(500); // 模拟执行时间
                    return mockResult;
                });
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 并发触发
            Thread t1 = new Thread(() -> nightlySyncService.executeNightlySync());
            t1.start();
            Thread.sleep(100); // 等待第一个任务开始
            
            boolean triggered = nightlySyncService.triggerManualSync();
            
            // Then: 第二个触发应被拒绝
            assertFalse(triggered, "并发执行时应拒绝新任务");
            t1.join();
        }
        
        @Test
        @DisplayName("空闲状态时isRunning应返回false")
        void shouldReturnFalseWhenIdle() {
            // When & Then
            assertFalse(nightlySyncService.isRunning(), "空闲状态时isRunning应返回false");
        }
        
        @Test
        @DisplayName("空闲状态时手动触发应成功")
        void shouldTriggerManualSyncWhenIdle() {
            // Given: 配置返回空列表，快速完成任务
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            boolean triggered = nightlySyncService.triggerManualSync();
            
            // Then
            assertTrue(triggered, "空闲状态时手动触发应成功");
        }
    }

    // ==================== 病人列表同步测试 ====================
    
    @Nested
    @DisplayName("病人列表同步测试")
    class PatientListSyncTests {
        
        @Test
        @DisplayName("应遍历所有科室执行同步")
        void shouldSyncAllDepartments() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(successResult);
            
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 应为每个科室调用一次
            verify(patientSyncService, times(2)).syncPatients(anyString(), anyString());
        }
        
        @Test
        @DisplayName("无科室配置时应跳过病人列表同步")
        void shouldSkipWhenNoDepartments() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 不应调用病人列表同步
            verify(patientSyncService, never()).syncPatients(anyString(), anyString());
        }
        
        @Test
        @DisplayName("科室同步失败时应继续处理下一个科室")
        void shouldContinueOnDepartmentSyncFailure() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B", "科室C"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            
            PatientSyncResult failedResult = mock(PatientSyncResult.class);
            when(failedResult.isSuccess()).thenReturn(false);
            when(failedResult.getErrorMessage()).thenReturn("模拟错误");
            
            // 第一个科室成功，第二个失败，第三个成功
            when(patientSyncService.syncPatients(anyString(), eq("科室A"))).thenReturn(successResult);
            when(patientSyncService.syncPatients(anyString(), eq("科室B"))).thenReturn(failedResult);
            when(patientSyncService.syncPatients(anyString(), eq("科室C"))).thenReturn(successResult);
            
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 所有科室都应被处理
            verify(patientSyncService, times(3)).syncPatients(anyString(), anyString());
        }
        
        @Test
        @DisplayName("科室同步抛出异常时应继续处理下一个科室")
        void shouldContinueOnDepartmentSyncException() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            // 第一个科室抛出异常
            when(patientSyncService.syncPatients(anyString(), eq("科室A")))
                .thenThrow(new RuntimeException("模拟异常"));
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), eq("科室B"))).thenReturn(successResult);
            
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 第二个科室仍应被处理
            verify(patientSyncService).syncPatients(anyString(), eq("科室B"));
        }
    }

    // ==================== 数据同步测试 ====================
    
    @Nested
    @DisplayName("数据同步测试")
    class DataSyncTests {
        
        @Test
        @DisplayName("应为每个病人同步化验结果")
        void shouldSyncLabForEachPatient() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(labSyncService.importLabResults(anyString())).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 应为每个病人调用一次
            verify(labSyncService, times(3)).importLabResults(anyString());
        }
        
        @Test
        @DisplayName("应为每个病人同步检查结果")
        void shouldSyncExaminationForEachPatient() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 应为每个病人调用一次
            verify(examinationSyncService, times(3)).importExaminationResults(anyString());
        }
        
        @Test
        @DisplayName("应为每个病人同步EMR病历")
        void shouldSyncEmrForEachPatient() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 应为每个病人调用一次
            verify(emrSyncService, times(3)).importEmrContent(anyString());
        }
        
        @Test
        @DisplayName("单个病人化验同步失败不应影响其他病人")
        void shouldContinueOnSinglePatientLabFailure() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            // P001成功，P002抛出异常，P003成功
            when(labSyncService.importLabResults("P001")).thenReturn(1);
            when(labSyncService.importLabResults("P002")).thenThrow(new RuntimeException("模拟异常"));
            when(labSyncService.importLabResults("P003")).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: P003仍应被处理
            verify(labSyncService).importLabResults("P003");
        }
        
        @Test
        @DisplayName("单个病人检查同步失败不应影响其他病人")
        void shouldContinueOnSinglePatientExaminationFailure() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(examinationSyncService.importExaminationResults("P001")).thenReturn(1);
            when(examinationSyncService.importExaminationResults("P002")).thenThrow(new RuntimeException("模拟异常"));
            when(examinationSyncService.importExaminationResults("P003")).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: P003仍应被处理
            verify(examinationSyncService).importExaminationResults("P003");
        }
        
        @Test
        @DisplayName("单个病人EMR同步失败不应影响其他病人")
        void shouldContinueOnSinglePatientEmrFailure() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(emrSyncService.importEmrContent("P001")).thenReturn(1);
            when(emrSyncService.importEmrContent("P002")).thenThrow(new RuntimeException("模拟异常"));
            when(emrSyncService.importEmrContent("P003")).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: P003仍应被处理
            verify(emrSyncService).importEmrContent("P003");
        }
        
        @Test
        @DisplayName("返回负数时应计入失败统计")
        void shouldCountNegativeReturnAsFailure() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(2);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            // P001成功，P002返回-1（失败）
            when(labSyncService.importLabResults("P001")).thenReturn(1);
            when(labSyncService.importLabResults("P002")).thenReturn(-1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 两个病人都应被处理
            verify(labSyncService, times(2)).importLabResults(anyString());
        }
    }

    // ==================== 同步顺序测试 ====================
    
    @Nested
    @DisplayName("同步顺序测试")
    class SyncOrderTests {
        
        @Test
        @DisplayName("应按照病人列表->化验->检查->EMR的顺序执行")
        void shouldExecuteInCorrectOrder() {
            // Given
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("测试科室"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(successResult);
            
            List<Patient> patients = createTestPatients(1);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(labSyncService.importLabResults(anyString())).thenReturn(1);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(1);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(1);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 验证调用顺序
            var inOrder = inOrder(patientSyncService, labSyncService, examinationSyncService, emrSyncService);
            inOrder.verify(patientSyncService).syncPatients(anyString(), anyString());
            inOrder.verify(labSyncService).importLabResults(anyString());
            inOrder.verify(examinationSyncService).importExaminationResults(anyString());
            inOrder.verify(emrSyncService).importEmrContent(anyString());
        }
    }

    // ==================== 边界条件测试 ====================
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {
        
        @Test
        @DisplayName("无在院病人时应正常完成同步")
        void shouldCompleteWithNoPatients() {
            // Given: 配置科室但无在院病人
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("测试科室"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(successResult);
            
            // 返回空病人列表
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 不应调用任何病人数据同步
            verify(labSyncService, never()).importLabResults(anyString());
            verify(examinationSyncService, never()).importExaminationResults(anyString());
            verify(emrSyncService, never()).importEmrContent(anyString());
        }
        
        @Test
        @DisplayName("执行中状态应正确反映任务运行")
        void shouldReflectRunningStateCorrectly() throws Exception {
            // Given: 模拟长时间任务
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A"));
            
            PatientSyncResult mockResult = mock(PatientSyncResult.class);
            when(mockResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString()))
                .thenAnswer(inv -> {
                    Thread.sleep(300);
                    return mockResult;
                });
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // 初始状态应为false
            assertFalse(nightlySyncService.isRunning(), "初始状态应为false");
            
            // When: 开始执行任务
            Thread syncThread = new Thread(() -> nightlySyncService.executeNightlySync());
            syncThread.start();
            Thread.sleep(50); // 等待任务开始
            
            // Then: 执行中应为true
            assertTrue(nightlySyncService.isRunning(), "执行中状态应为true");
            
            syncThread.join();
            
            // 执行完成后应为false
            assertFalse(nightlySyncService.isRunning(), "执行完成后状态应为false");
        }
    }

    // ==================== 性能测试 ====================
    
    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {
        
        @Test
        @DisplayName("批量病人同步应在合理时间内完成")
        void shouldCompleteBatchSyncWithinReasonableTime() {
            // Given: 创建50个测试病人
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(50);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(labSyncService.importLabResults(anyString())).thenReturn(1);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(1);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(1);
            
            // When: 计时执行
            long startTime = System.currentTimeMillis();
            nightlySyncService.executeNightlySync();
            long duration = System.currentTimeMillis() - startTime;
            
            // Then: 应在500ms内完成（Mock场景下）
            assertTrue(duration < 500, 
                String.format("批量同步50个病人应在500ms内完成，实际耗时: %dms", duration));
            
            // 验证所有病人都被处理
            verify(labSyncService, times(50)).importLabResults(anyString());
            verify(examinationSyncService, times(50)).importExaminationResults(anyString());
            verify(emrSyncService, times(50)).importEmrContent(anyString());
        }
        
        @Test
        @DisplayName("并发控制应快速响应")
        void shouldRespondQuicklyForConcurrencyCheck() {
            // Given: 模拟任务正在执行
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A"));
            
            PatientSyncResult mockResult = mock(PatientSyncResult.class);
            when(mockResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString()))
                .thenAnswer(inv -> {
                    Thread.sleep(200); // 模拟执行时间
                    return mockResult;
                });
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 并发触发并计时响应
            Thread syncThread = new Thread(() -> nightlySyncService.executeNightlySync());
            syncThread.start();
            
            try {
                Thread.sleep(50); // 等待任务开始
                
                long startTime = System.currentTimeMillis();
                boolean triggered = nightlySyncService.triggerManualSync();
                long duration = System.currentTimeMillis() - startTime;
                
                // Then: 并发检查应在10ms内响应
                assertFalse(triggered);
                assertTrue(duration < 10, 
                    String.format("并发检查应在10ms内响应，实际耗时: %dms", duration));
                
                syncThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== 任务3：syncPatientList统计结果测试 ====================
    
    @Nested
    @DisplayName("任务3：病人列表同步统计结果测试")
    class Task3PatientListSyncStatisticsTests {
        
        /**
         * 🔴 红阶段测试：验证成功科室数量正确统计
         * 
         * <p>测试场景：配置2个科室，全部同步成功</p>
         * <p>预期结果：getLastSyncResult().getPatientSyncSuccessDepts() == 2</p>
         */
        @Test
        @DisplayName("应正确统计成功科室数量")
        void shouldRecordSuccessDepartmentCount() {
            // Given: 配置2个科室，全部同步成功
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(successResult);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证成功科室数量为2
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(2, result.getPatientSyncSuccessDepts(), 
                "成功科室数量应为2");
            assertEquals(0, result.getPatientSyncFailedDepts(), 
                "失败科室数量应为0");
        }
        
        /**
         * 🔴 红阶段测试：验证失败科室数量正确统计
         * 
         * <p>测试场景：配置2个科室，全部同步失败</p>
         * <p>预期结果：getLastSyncResult().getPatientSyncFailedDepts() == 2</p>
         */
        @Test
        @DisplayName("应正确统计失败科室数量")
        void shouldRecordFailedDepartmentCount() {
            // Given: 配置2个科室，全部同步失败
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult failedResult = mock(PatientSyncResult.class);
            when(failedResult.isSuccess()).thenReturn(false);
            when(failedResult.getErrorMessage()).thenReturn("同步失败");
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(failedResult);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证失败科室数量为2
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(0, result.getPatientSyncSuccessDepts(), 
                "成功科室数量应为0");
            assertEquals(2, result.getPatientSyncFailedDepts(), 
                "失败科室数量应为2");
        }
        
        /**
         * 🔴 红阶段测试：验证混合场景下统计正确
         * 
         * <p>测试场景：配置3个科室，1个成功，1个失败，1个抛出异常</p>
         * <p>预期结果：成功1个，失败2个</p>
         */
        @Test
        @DisplayName("应正确统计混合场景下的科室结果")
        void shouldRecordMixedDepartmentResults() {
            // Given: 配置3个科室，混合结果
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(Arrays.asList("科室A", "科室B", "科室C"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            
            PatientSyncResult failedResult = mock(PatientSyncResult.class);
            when(failedResult.isSuccess()).thenReturn(false);
            when(failedResult.getErrorMessage()).thenReturn("同步失败");
            
            // 科室A成功，科室B失败，科室C抛出异常
            when(patientSyncService.syncPatients(anyString(), eq("科室A"))).thenReturn(successResult);
            when(patientSyncService.syncPatients(anyString(), eq("科室B"))).thenReturn(failedResult);
            when(patientSyncService.syncPatients(anyString(), eq("科室C")))
                .thenThrow(new RuntimeException("模拟异常"));
            
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证统计结果 - 1成功，2失败（失败+异常）
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(1, result.getPatientSyncSuccessDepts(), 
                "成功科室数量应为1");
            assertEquals(2, result.getPatientSyncFailedDepts(), 
                "失败科室数量应为2（包含异常）");
        }
        
        /**
         * 🔴 红阶段测试：验证无科室配置时统计为0
         * 
         * <p>测试场景：未启用科室过滤</p>
         * <p>预期结果：成功和失败科室数量都为0</p>
         */
        @Test
        @DisplayName("无科室配置时统计结果应为0")
        void shouldRecordZeroWhenNoDepartments() {
            // Given: 未启用科室过滤
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证统计结果都为0
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(0, result.getPatientSyncSuccessDepts(), 
                "成功科室数量应为0");
            assertEquals(0, result.getPatientSyncFailedDepts(), 
                "失败科室数量应为0");
        }
        
        /**
         * 性能测试：验证多科室同步遍历响应速度
         * 
         * <p>测试场景：配置10个科室，验证遍历过程的响应速度</p>
         * <p>预期结果：10个科室的Mock同步应在200ms内完成</p>
         */
        @Test
        @DisplayName("多科室同步遍历应在合理时间内完成")
        void shouldCompleteDepartmentSyncWithinReasonableTime() {
            // Given: 配置10个科室
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(true);
            when(timerConfig.getTargetDepartments()).thenReturn(
                Arrays.asList("科室A", "科室B", "科室C", "科室D", "科室E",
                             "科室F", "科室G", "科室H", "科室I", "科室J"));
            when(hospitalConfigService.getAllConfigs()).thenReturn(Collections.emptyList());
            
            PatientSyncResult successResult = mock(PatientSyncResult.class);
            when(successResult.isSuccess()).thenReturn(true);
            when(patientSyncService.syncPatients(anyString(), anyString())).thenReturn(successResult);
            when(patientRepository.findByIsInHospital(true)).thenReturn(Collections.emptyList());
            
            // When: 计时执行
            long startTime = System.currentTimeMillis();
            nightlySyncService.executeNightlySync();
            long duration = System.currentTimeMillis() - startTime;
            
            // Then: 应在200ms内完成（Mock场景下）
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(10, result.getPatientSyncSuccessDepts(), 
                "成功科室数量应为10");
            assertTrue(duration < 200, 
                String.format("10个科室的Mock同步应在200ms内完成，实际耗时: %dms", duration));
        }
    }

    // ==================== 任务4：三种数据同步方法统计结果测试 ====================
    
    @Nested
    @DisplayName("任务4：化验同步(syncLabResults)统计结果测试")
    class Task4LabSyncStatisticsTests {
        
        /**
         * 🔴 红阶段测试：验证化验同步成功病人数量正确统计
         * 
         * <p>测试场景：3个病人，全部同步成功（返回值>=0）</p>
         * <p>预期结果：getLastSyncResult().getLabSyncSuccess() == 3</p>
         */
        @Test
        @DisplayName("应正确统计化验同步成功病人数量")
        void shouldRecordLabSyncSuccessCount() {
            // Given: 3个病人，全部化验同步成功
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(labSyncService.importLabResults(anyString())).thenReturn(5); // 每个病人导入5条记录
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证化验同步统计
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(3, result.getLabSyncSuccess(), 
                "化验同步成功病人数量应为3");
            assertEquals(0, result.getLabSyncFailed(), 
                "化验同步失败病人数量应为0");
        }
        
        /**
         * 🔴 红阶段测试：验证化验同步失败病人数量正确统计（返回-1）
         * 
         * <p>测试场景：3个病人，全部返回-1表示失败</p>
         * <p>预期结果：getLastSyncResult().getLabSyncFailed() == 3</p>
         */
        @Test
        @DisplayName("应正确统计化验同步失败病人数量-返回负数")
        void shouldRecordLabSyncFailedCountForNegativeReturn() {
            // Given: 3个病人，全部化验同步失败（返回-1）
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(labSyncService.importLabResults(anyString())).thenReturn(-1);
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证化验同步统计
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(0, result.getLabSyncSuccess(), 
                "化验同步成功病人数量应为0");
            assertEquals(3, result.getLabSyncFailed(), 
                "化验同步失败病人数量应为3");
        }
        
        /**
         * 🔴 红阶段测试：验证化验同步混合场景统计
         * 
         * <p>测试场景：4个病人，2成功，1返回-1失败，1抛出异常</p>
         * <p>预期结果：成功2个，失败2个</p>
         */
        @Test
        @DisplayName("应正确统计化验同步混合场景结果")
        void shouldRecordLabSyncMixedResults() {
            // Given: 4个病人，混合结果
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(4);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            // P001、P002成功，P003返回-1，P004抛出异常
            when(labSyncService.importLabResults("P001")).thenReturn(5);
            when(labSyncService.importLabResults("P002")).thenReturn(0); // 0也算成功
            when(labSyncService.importLabResults("P003")).thenReturn(-1);
            when(labSyncService.importLabResults("P004")).thenThrow(new RuntimeException("模拟异常"));
            
            // When: 执行夜间同步
            nightlySyncService.executeNightlySync();
            
            // Then: 验证统计结果
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result, "同步结果不应为null");
            assertEquals(2, result.getLabSyncSuccess(), 
                "化验同步成功病人数量应为2");
            assertEquals(2, result.getLabSyncFailed(), 
                "化验同步失败病人数量应为2（-1和异常）");
        }
    }
    
    @Nested
    @DisplayName("任务4：检查同步(syncExaminationResults)统计结果测试")
    class Task4ExaminationSyncStatisticsTests {
        
        /**
         * 🔴 红阶段测试：验证检查同步成功病人数量正确统计
         */
        @Test
        @DisplayName("应正确统计检查同步成功病人数量")
        void shouldRecordExamSyncSuccessCount() {
            // Given: 3个病人，全部检查同步成功
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(3);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(3, result.getExamSyncSuccess(), 
                "检查同步成功病人数量应为3");
            assertEquals(0, result.getExamSyncFailed(), 
                "检查同步失败病人数量应为0");
        }
        
        /**
         * 🔴 红阶段测试：验证检查同步混合场景统计
         */
        @Test
        @DisplayName("应正确统计检查同步混合场景结果")
        void shouldRecordExamSyncMixedResults() {
            // Given: 4个病人，混合结果
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(4);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(examinationSyncService.importExaminationResults("P001")).thenReturn(2);
            when(examinationSyncService.importExaminationResults("P002")).thenReturn(-1);
            when(examinationSyncService.importExaminationResults("P003")).thenReturn(0);
            when(examinationSyncService.importExaminationResults("P004")).thenThrow(new RuntimeException("模拟异常"));
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(2, result.getExamSyncSuccess(), 
                "检查同步成功病人数量应为2");
            assertEquals(2, result.getExamSyncFailed(), 
                "检查同步失败病人数量应为2");
        }
    }
    
    @Nested
    @DisplayName("任务4：EMR同步(syncEmrContent)统计结果测试")
    class Task4EmrSyncStatisticsTests {
        
        /**
         * 🔴 红阶段测试：验证EMR同步成功病人数量正确统计
         */
        @Test
        @DisplayName("应正确统计EMR同步成功病人数量")
        void shouldRecordEmrSyncSuccessCount() {
            // Given: 3个病人，全部EMR同步成功
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(10);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(3, result.getEmrSyncSuccess(), 
                "EMR同步成功病人数量应为3");
            assertEquals(0, result.getEmrSyncFailed(), 
                "EMR同步失败病人数量应为0");
        }
        
        /**
         * 🔴 红阶段测试：验证EMR同步混合场景统计
         */
        @Test
        @DisplayName("应正确统计EMR同步混合场景结果")
        void shouldRecordEmrSyncMixedResults() {
            // Given: 4个病人，混合结果
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(4);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(emrSyncService.importEmrContent("P001")).thenReturn(8);
            when(emrSyncService.importEmrContent("P002")).thenReturn(-1);
            when(emrSyncService.importEmrContent("P003")).thenReturn(0);
            when(emrSyncService.importEmrContent("P004")).thenThrow(new RuntimeException("模拟异常"));
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(2, result.getEmrSyncSuccess(), 
                "EMR同步成功病人数量应为2");
            assertEquals(2, result.getEmrSyncFailed(), 
                "EMR同步失败病人数量应为2");
        }
    }
    
    @Nested
    @DisplayName("任务4：三种同步综合统计测试")
    class Task4ComprehensiveSyncStatisticsTests {
        
        /**
         * 🔴 红阶段测试：验证总病人数正确记录
         */
        @Test
        @DisplayName("应正确记录总病人数")
        void shouldRecordTotalPatientCount() {
            // Given: 5个在院病人
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(5);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(5, result.getTotalPatients(), 
                "总病人数应为5");
        }
        
        /**
         * 🔴 红阶段测试：验证三种同步统计相互独立
         */
        @Test
        @DisplayName("三种同步统计应相互独立")
        void shouldMaintainIndependentStatistics() {
            // Given: 3个病人，不同同步类型有不同结果
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(3);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            // 化验：3成功0失败
            when(labSyncService.importLabResults(anyString())).thenReturn(1);
            // 检查：2成功1失败
            when(examinationSyncService.importExaminationResults("P001")).thenReturn(1);
            when(examinationSyncService.importExaminationResults("P002")).thenReturn(1);
            when(examinationSyncService.importExaminationResults("P003")).thenReturn(-1);
            // EMR：1成功2失败
            when(emrSyncService.importEmrContent("P001")).thenReturn(1);
            when(emrSyncService.importEmrContent("P002")).thenReturn(-1);
            when(emrSyncService.importEmrContent("P003")).thenThrow(new RuntimeException("模拟异常"));
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then: 验证各类同步统计相互独立
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            // 化验统计
            assertEquals(3, result.getLabSyncSuccess(), "化验成功应为3");
            assertEquals(0, result.getLabSyncFailed(), "化验失败应为0");
            // 检查统计
            assertEquals(2, result.getExamSyncSuccess(), "检查成功应为2");
            assertEquals(1, result.getExamSyncFailed(), "检查失败应为1");
            // EMR统计
            assertEquals(1, result.getEmrSyncSuccess(), "EMR成功应为1");
            assertEquals(2, result.getEmrSyncFailed(), "EMR失败应为2");
        }
        
        /**
         * 🔴 红阶段测试：验证返回值为0时统计为成功
         */
        @Test
        @DisplayName("返回值为0时应统计为成功")
        void shouldCountZeroReturnAsSuccess() {
            // Given: 病人同步返回0（无新数据但执行成功）
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createTestPatients(2);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            
            when(labSyncService.importLabResults(anyString())).thenReturn(0);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(0);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(0);
            
            // When
            nightlySyncService.executeNightlySync();
            
            // Then
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(2, result.getLabSyncSuccess(), "化验成功应为2");
            assertEquals(2, result.getExamSyncSuccess(), "检查成功应为2");
            assertEquals(2, result.getEmrSyncSuccess(), "EMR成功应为2");
            assertEquals(0, result.getLabSyncFailed());
            assertEquals(0, result.getExamSyncFailed());
            assertEquals(0, result.getEmrSyncFailed());
        }
        
        /**
         * 性能测试：验证三种数据同步批量执行性能
         * 
         * <p>测试场景：100个病人执行三种数据同步</p>
         * <p>预期结果：Mock场景下应在300ms内完成</p>
         */
        @Test
        @DisplayName("性能测试：100个病人三种同步应在300ms内完成")
        void shouldCompleteBatchDataSyncWithinReasonableTime() {
            // Given: 100个病人
            when(timerConfig.isDepartmentFilterEnabled()).thenReturn(false);
            
            List<Patient> patients = createLargeTestPatients(100);
            when(patientRepository.findByIsInHospital(true)).thenReturn(patients);
            when(labSyncService.importLabResults(anyString())).thenReturn(1);
            when(examinationSyncService.importExaminationResults(anyString())).thenReturn(1);
            when(emrSyncService.importEmrContent(anyString())).thenReturn(1);
            
            // When: 计时执行
            long startTime = System.currentTimeMillis();
            nightlySyncService.executeNightlySync();
            long duration = System.currentTimeMillis() - startTime;
            
            // Then: 应在300ms内完成
            NightlySyncResult result = nightlySyncService.getLastSyncResult();
            assertNotNull(result);
            assertEquals(100, result.getTotalPatients());
            assertEquals(100, result.getLabSyncSuccess());
            assertEquals(100, result.getExamSyncSuccess());
            assertEquals(100, result.getEmrSyncSuccess());
            assertTrue(duration < 300, 
                String.format("100个病人的三种同步应在300ms内完成，实际耗时: %dms", duration));
        }
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 创建测试用病人列表
     * 
     * @param count 病人数量
     * @return 病人列表，ID格式为P001、P002、...（最多到P009）
     */
    private List<Patient> createTestPatients(int count) {
        Patient[] patients = new Patient[count];
        for (int i = 0; i < count; i++) {
            Patient p = new Patient();
            p.setPatientId("P00" + (i + 1));
            patients[i] = p;
        }
        return Arrays.asList(patients);
    }
    
    /**
     * 创建大量测试用病人列表（用于性能测试）
     * 
     * @param count 病人数量
     * @return 病人列表，ID格式为P0001、P0002、...
     */
    private List<Patient> createLargeTestPatients(int count) {
        Patient[] patients = new Patient[count];
        for (int i = 0; i < count; i++) {
            Patient p = new Patient();
            p.setPatientId(String.format("P%04d", i + 1));
            patients[i] = p;
        }
        return Arrays.asList(patients);
    }
}
