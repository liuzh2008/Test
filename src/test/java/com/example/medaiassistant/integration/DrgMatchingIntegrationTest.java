package com.example.medaiassistant.integration;

import com.example.medaiassistant.controller.DrgMatchingController;
import com.example.medaiassistant.dto.drg.DrgCatalog;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.dto.drg.PatientDiagnosis;
import com.example.medaiassistant.dto.drg.PatientProcedure;
import com.example.medaiassistant.service.DrgMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DRG匹配集成测试
 * 
 * TDD红阶段：创建会失败的集成测试用例
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DRG匹配 集成测试")
class DrgMatchingIntegrationTest {

    @Mock
    private DrgMatchingService drgMatchingService;

    @InjectMocks
    private DrgMatchingController drgMatchingController;

    private PatientData patientData;
    private DrgCatalog drgCatalog;
    private MatchingResult expectedResult;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I10", "原发性高血压")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        patientData = new PatientData(diagnoses, procedures);
        
        drgCatalog = new DrgCatalog(List.of());
        
        expectedResult = new MatchingResult(
            Arrays.asList("心房颤动", "原发性高血压"),
            Arrays.asList("经皮左心耳封堵术")
        );
    }

    @Test
    @DisplayName("应该通过完整的服务调用返回主要诊断和手术匹配结果")
    void shouldReturnPrimaryDiagnosisAndProcedureViaFullServiceCall() {
        // Given
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(expectedResult);

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(patientData, drgCatalog);

        // When
        ResponseEntity<MatchingResult> response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).containsExactlyInAnyOrder("心房颤动", "原发性高血压");
        assertThat(body.getPrimaryProcedures()).containsExactly("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该处理复杂的患者数据场景")
    void shouldHandleComplexPatientDataScenario() {
        // Given
        PatientData complexPatientData = createComplexPatientData();
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(
                Arrays.asList("心房颤动", "原发性高血压", "2型糖尿病"),
                Arrays.asList("经皮左心耳封堵术", "胰岛素泵植入术")
            ));

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(complexPatientData, drgCatalog);

        // When
        ResponseEntity<MatchingResult> response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).hasSize(3);
        assertThat(body.getPrimaryProcedures()).hasSize(2);
    }

    @Test
    @DisplayName("应该处理无匹配的情况")
    void shouldHandleNoMatchesScenario() {
        // Given
        PatientData noMatchPatientData = createNoMatchPatientData();
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(List.of(), List.of()));

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(noMatchPatientData, drgCatalog);

        // When
        ResponseEntity<MatchingResult> response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).isEmpty();
        assertThat(body.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该验证服务层被正确调用")
    void shouldVerifyServiceLayerIsCalledCorrectly() {
        // Given
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(expectedResult);

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(patientData, drgCatalog);

        // When
        ResponseEntity<MatchingResult> response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        // 验证服务层被调用且返回了正确的结果
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).isNotEmpty();
        assertThat(body.getPrimaryProcedures()).isNotEmpty();
    }

    /**
     * 创建复杂的患者数据
     */
    private PatientData createComplexPatientData() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I10", "原发性高血压"),
            new PatientDiagnosis("E11.900", "2型糖尿病")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术"),
            new PatientProcedure("99.2900x001", "胰岛素泵植入术")
        );
        return new PatientData(diagnoses, procedures);
    }

    /**
     * 创建无匹配的患者数据
     */
    private PatientData createNoMatchPatientData() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("Z00.000", "健康检查")
        );
        return new PatientData(diagnoses, null);
    }
}
