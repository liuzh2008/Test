package com.example.medaiassistant.service;

import com.example.medaiassistant.drg.matching.PrimaryDiagnosisProcedureMatcher;
import com.example.medaiassistant.dto.drg.DrgCatalog;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.dto.drg.PatientDiagnosis;
import com.example.medaiassistant.dto.drg.PatientProcedure;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DrgMatchingService单元测试
 * 
 * TDD红阶段：创建会失败的测试用例
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DrgMatchingService 单元测试")
class DrgMatchingServiceTest {

    @Mock
    private PrimaryDiagnosisProcedureMatcher primaryMatcher;

    @InjectMocks
    private DrgMatchingService drgMatchingService;

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
    @DisplayName("应该调用PrimaryMatcher并返回匹配结果")
    void shouldCallPrimaryMatcherAndReturnMatchingResult() {
        // Given
        when(primaryMatcher.match(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(expectedResult);

        // When
        MatchingResult actualResult = drgMatchingService.matchPrimaryDiagnosisAndProcedure(patientData, drgCatalog);

        // Then
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getPrimaryDiagnoses()).containsExactly("心房颤动");
        assertThat(actualResult.getPrimaryProcedures()).containsExactly("经皮左心耳封堵术");
    }

    @Test
    @DisplayName("应该处理空患者数据")
    void shouldHandleEmptyPatientData() {
        // Given
        PatientData emptyPatientData = new PatientData(null, null);
        when(primaryMatcher.match(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(List.of(), List.of()));

        // When
        MatchingResult result = drgMatchingService.matchPrimaryDiagnosisAndProcedure(emptyPatientData, drgCatalog);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }

    @Test
    @DisplayName("应该处理空DRG目录")
    void shouldHandleEmptyDrgCatalog() {
        // Given
        DrgCatalog emptyCatalog = new DrgCatalog(List.of());
        when(primaryMatcher.match(any(PatientData.class), any(DrgCatalog.class)))
            .thenReturn(new MatchingResult(List.of(), List.of()));

        // When
        MatchingResult result = drgMatchingService.matchPrimaryDiagnosisAndProcedure(patientData, emptyCatalog);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryDiagnoses()).isEmpty();
        assertThat(result.getPrimaryProcedures()).isEmpty();
    }
}
