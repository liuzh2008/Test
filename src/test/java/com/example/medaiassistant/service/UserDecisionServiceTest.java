package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.UserDecisionRequest;
import com.example.medaiassistant.enums.MccType;
import com.example.medaiassistant.model.DrgAnalysisResult;
import com.example.medaiassistant.repository.DrgAnalysisResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserDecisionService单元测试
 * 按照TDD红-绿-重构流程实现用户选择MCC类别功能
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-10
 */
@ExtendWith(MockitoExtension.class)
class UserDecisionServiceTest {

    @Mock
    private DrgAnalysisResultRepository repository;

    @InjectMocks
    private UserDecisionServiceImpl service;

    /**
     * 🔴 红阶段测试1：用户选择MCC类别并保存分析结果
     * 测试用户选择MCC类型并保存分析结果的功能
     */
    @Test
    void saveUserDecision_shouldSaveWithCorrectMccType() {
        // Given - 准备测试数据
        UserDecisionRequest request = new UserDecisionRequest();
        request.setResultId(1L);
        request.setPatientId("PAT001");
        request.setSelectedMccType(MccType.MCC);
        request.setOperator("test-user");

        DrgAnalysisResult existingResult = new DrgAnalysisResult();
        existingResult.setResultId(1L);
        existingResult.setPatientId("PAT001");
        existingResult.setFinalDrgCode("DRG001");

        DrgAnalysisResult savedResult = new DrgAnalysisResult();
        savedResult.setResultId(1L);
        savedResult.setPatientId("PAT001");
        savedResult.setUserSelectedMccType("MCC");
        savedResult.setFinalDrgCode("DRG001-MCC");

        // When - 设置Mock行为并执行测试
        when(repository.findById(1L)).thenReturn(Optional.of(existingResult));
        when(repository.save(any(DrgAnalysisResult.class))).thenReturn(savedResult);

        DrgAnalysisResult result = service.saveUserDecision(request);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("MCC", result.getUserSelectedMccType());
        assertEquals("DRG001-MCC", result.getFinalDrgCode());
        verify(repository).findById(1L);
        verify(repository).save(any(DrgAnalysisResult.class));
    }

    /**
     * 🔴 红阶段测试2：用户选择CC类别
     * 测试用户选择CC类型的功能
     */
    @Test
    void saveUserDecision_shouldSaveWithCcType() {
        // Given - 准备测试数据
        UserDecisionRequest request = new UserDecisionRequest();
        request.setResultId(2L);
        request.setSelectedMccType(MccType.CC);
        request.setOperator("test-user");

        DrgAnalysisResult existingResult = new DrgAnalysisResult();
        existingResult.setResultId(2L);
        existingResult.setFinalDrgCode("DRG002");

        DrgAnalysisResult savedResult = new DrgAnalysisResult();
        savedResult.setResultId(2L);
        savedResult.setUserSelectedMccType("CC");
        savedResult.setFinalDrgCode("DRG002-CC");

        // When - 设置Mock行为并执行测试
        when(repository.findById(2L)).thenReturn(Optional.of(existingResult));
        when(repository.save(any(DrgAnalysisResult.class))).thenReturn(savedResult);

        DrgAnalysisResult result = service.saveUserDecision(request);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("CC", result.getUserSelectedMccType());
        assertEquals("DRG002-CC", result.getFinalDrgCode());
        verify(repository).findById(2L);
        verify(repository).save(any(DrgAnalysisResult.class));
    }

    /**
     * 🔴 红阶段测试3：用户选择无并发症
     * 测试用户选择NONE类型的功能
     */
    @Test
    void saveUserDecision_shouldSaveWithNoneType() {
        // Given - 准备测试数据
        UserDecisionRequest request = new UserDecisionRequest();
        request.setResultId(3L);
        request.setSelectedMccType(MccType.NONE);
        request.setOperator("test-user");

        DrgAnalysisResult existingResult = new DrgAnalysisResult();
        existingResult.setResultId(3L);
        existingResult.setFinalDrgCode("DRG003");

        DrgAnalysisResult savedResult = new DrgAnalysisResult();
        savedResult.setResultId(3L);
        savedResult.setUserSelectedMccType("NONE");
        savedResult.setFinalDrgCode("DRG003");

        // When - 设置Mock行为并执行测试
        when(repository.findById(3L)).thenReturn(Optional.of(existingResult));
        when(repository.save(any(DrgAnalysisResult.class))).thenReturn(savedResult);

        DrgAnalysisResult result = service.saveUserDecision(request);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("NONE", result.getUserSelectedMccType());
        assertEquals("DRG003", result.getFinalDrgCode());
        verify(repository).findById(3L);
        verify(repository).save(any(DrgAnalysisResult.class));
    }

    /**
     * 🔴 红阶段测试4：根据分析结果ID查询决策结果
     * 测试根据ID查询分析结果的功能
     */
    @Test
    void getDecisionResult_shouldReturnResultWhenValidId() {
        // Given - 准备测试数据
        Long resultId = 1L;
        DrgAnalysisResult expectedResult = new DrgAnalysisResult();
        expectedResult.setResultId(resultId);
        expectedResult.setPatientId("PAT001");
        expectedResult.setUserSelectedMccType("MCC");

        // When - 设置Mock行为并执行测试
        when(repository.findById(resultId)).thenReturn(Optional.of(expectedResult));

        DrgAnalysisResult result = service.getDecisionResult(resultId);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals(resultId, result.getResultId());
        assertEquals("PAT001", result.getPatientId());
        assertEquals("MCC", result.getUserSelectedMccType());
        verify(repository).findById(resultId);
    }

    /**
     * 🔴 红阶段测试5：根据患者ID查询决策结果
     * 测试根据患者ID查询分析结果的功能
     */
    @Test
    void getDecisionResultsByPatientId_shouldReturnResultsWhenValidPatientId() {
        // Given - 准备测试数据
        String patientId = "PAT001";
        DrgAnalysisResult result1 = new DrgAnalysisResult();
        result1.setResultId(1L);
        result1.setPatientId(patientId);
        result1.setUserSelectedMccType("MCC");

        DrgAnalysisResult result2 = new DrgAnalysisResult();
        result2.setResultId(2L);
        result2.setPatientId(patientId);
        result2.setUserSelectedMccType("CC");

        List<DrgAnalysisResult> expectedResults = Arrays.asList(result1, result2);

        // When - 设置Mock行为并执行测试
        when(repository.findByPatientIdAndNotDeleted(patientId)).thenReturn(expectedResults);

        List<DrgAnalysisResult> results = service.getDecisionResultsByPatientId(patientId);

        // Then - 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(patientId, results.get(0).getPatientId());
        assertEquals(patientId, results.get(1).getPatientId());
        verify(repository).findByPatientIdAndNotDeleted(patientId);
    }

    /**
     * 🔴 红阶段测试6：分析结果不存在时抛出异常
     * 测试当分析结果不存在时的异常处理
     */
    @Test
    void saveUserDecision_shouldThrowExceptionWhenResultNotFound() {
        // Given - 准备测试数据
        UserDecisionRequest request = new UserDecisionRequest();
        request.setResultId(999L);
        request.setSelectedMccType(MccType.MCC);
        request.setOperator("test-user");

        // When - 设置Mock行为
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Then - 验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            service.saveUserDecision(request);
        });
        verify(repository).findById(999L);
        verify(repository, never()).save(any(DrgAnalysisResult.class));
    }
}
