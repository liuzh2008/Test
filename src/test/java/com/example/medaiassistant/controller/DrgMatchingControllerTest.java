package com.example.medaiassistant.controller;

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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DrgMatchingController单元测试
 * 
 * TDD红阶段：创建会失败的REST API测试用例
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DrgMatchingController 单元测试")
class DrgMatchingControllerTest {

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
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        patientData = new PatientData(diagnoses, procedures);
        
        drgCatalog = new DrgCatalog(List.of());
        
        expectedResult = new MatchingResult(
            Arrays.asList("心房颤动"),
            Arrays.asList("经皮左心耳封堵术")
        );
    }

    @Test
    @DisplayName("应该调用服务层并返回匹配结果")
    void shouldCallServiceAndReturnMatchingResult() {
        // Given
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(expectedResult);

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(patientData, drgCatalog);

        // When
        var response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).containsExactly("心房颤动");
        assertThat(body.getPrimaryProcedures()).containsExactly("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该处理空患者数据")
    void shouldHandleEmptyPatientData() {
        // Given
        PatientData emptyPatientData = new PatientData(null, null);
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(List.of(), List.of()));

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(emptyPatientData, drgCatalog);

        // When
        var response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).isEmpty();
        assertThat(body.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该处理空DRG目录")
    void shouldHandleEmptyDrgCatalog() {
        // Given
        DrgCatalog emptyCatalog = new DrgCatalog(List.of());
        when(drgMatchingService.matchPrimaryDiagnosisAndProcedure(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(List.of(), List.of()));

        DrgMatchingController.DrgMatchingRequest request = 
            new DrgMatchingController.DrgMatchingRequest(patientData, emptyCatalog);

        // When
        var response = drgMatchingController.matchPrimaryDiagnosisAndProcedure(request);

        // Then
        assertThat(response).isNotNull();
        assertTrue(response.getStatusCode().is2xxSuccessful(), "HTTP非2xx");
        var body = java.util.Objects.requireNonNull(response.getBody(), "响应体为空");
        assertThat(body.getPrimaryDiagnoses()).isEmpty();
        assertThat(body.getPrimaryProcedures()).isEmpty();
    }

}
