package com.example.medaiassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NightlySyncResult POJO 单元测试
 * 验证同步结果统计类的所有getter/setter和初始值
 * 
 * @author System
 * @version 1.0
 * @since 2026-01-13
 */
@DisplayName("NightlySyncResult POJO 单元测试")
class NightlySyncResultTest {

    private NightlySyncResult result;

    @BeforeEach
    void setUp() {
        result = new NightlySyncResult();
    }

    @Nested
    @DisplayName("初始值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("success 默认值应为 false")
        void successShouldDefaultToFalse() {
            assertFalse(result.isSuccess(), "success 默认值应为 false");
        }

        @Test
        @DisplayName("errorMessage 默认值应为 null")
        void errorMessageShouldDefaultToNull() {
            assertNull(result.getErrorMessage(), "errorMessage 默认值应为 null");
        }

        @Test
        @DisplayName("durationMs 默认值应为 0")
        void durationMsShouldDefaultToZero() {
            assertEquals(0L, result.getDurationMs(), "durationMs 默认值应为 0");
        }

        @Test
        @DisplayName("patientSyncSuccessDepts 默认值应为 0")
        void patientSyncSuccessDeptsDefaultToZero() {
            assertEquals(0, result.getPatientSyncSuccessDepts(), "patientSyncSuccessDepts 默认值应为 0");
        }

        @Test
        @DisplayName("patientSyncFailedDepts 默认值应为 0")
        void patientSyncFailedDeptsDefaultToZero() {
            assertEquals(0, result.getPatientSyncFailedDepts(), "patientSyncFailedDepts 默认值应为 0");
        }

        @Test
        @DisplayName("totalPatients 默认值应为 0")
        void totalPatientsDefaultToZero() {
            assertEquals(0, result.getTotalPatients(), "totalPatients 默认值应为 0");
        }

        @Test
        @DisplayName("labSyncSuccess 默认值应为 0")
        void labSyncSuccessDefaultToZero() {
            assertEquals(0, result.getLabSyncSuccess(), "labSyncSuccess 默认值应为 0");
        }

        @Test
        @DisplayName("labSyncFailed 默认值应为 0")
        void labSyncFailedDefaultToZero() {
            assertEquals(0, result.getLabSyncFailed(), "labSyncFailed 默认值应为 0");
        }

        @Test
        @DisplayName("examSyncSuccess 默认值应为 0")
        void examSyncSuccessDefaultToZero() {
            assertEquals(0, result.getExamSyncSuccess(), "examSyncSuccess 默认值应为 0");
        }

        @Test
        @DisplayName("examSyncFailed 默认值应为 0")
        void examSyncFailedDefaultToZero() {
            assertEquals(0, result.getExamSyncFailed(), "examSyncFailed 默认值应为 0");
        }

        @Test
        @DisplayName("emrSyncSuccess 默认值应为 0")
        void emrSyncSuccessDefaultToZero() {
            assertEquals(0, result.getEmrSyncSuccess(), "emrSyncSuccess 默认值应为 0");
        }

        @Test
        @DisplayName("emrSyncFailed 默认值应为 0")
        void emrSyncFailedDefaultToZero() {
            assertEquals(0, result.getEmrSyncFailed(), "emrSyncFailed 默认值应为 0");
        }
    }

    @Nested
    @DisplayName("Getter/Setter 测试")
    class GetterSetterTests {

        @Test
        @DisplayName("success getter/setter 正常工作")
        void successGetterSetterShouldWork() {
            result.setSuccess(true);
            assertTrue(result.isSuccess(), "设置后 success 应为 true");
            
            result.setSuccess(false);
            assertFalse(result.isSuccess(), "设置后 success 应为 false");
        }

        @Test
        @DisplayName("errorMessage getter/setter 正常工作")
        void errorMessageGetterSetterShouldWork() {
            String expectedMessage = "测试错误信息";
            result.setErrorMessage(expectedMessage);
            assertEquals(expectedMessage, result.getErrorMessage(), "errorMessage 应匹配设置值");
        }

        @Test
        @DisplayName("durationMs getter/setter 正常工作")
        void durationMsGetterSetterShouldWork() {
            long expectedDuration = 12345L;
            result.setDurationMs(expectedDuration);
            assertEquals(expectedDuration, result.getDurationMs(), "durationMs 应匹配设置值");
        }

        @Test
        @DisplayName("patientSyncSuccessDepts getter/setter 正常工作")
        void patientSyncSuccessDeptsGetterSetterShouldWork() {
            int expected = 5;
            result.setPatientSyncSuccessDepts(expected);
            assertEquals(expected, result.getPatientSyncSuccessDepts(), "patientSyncSuccessDepts 应匹配设置值");
        }

        @Test
        @DisplayName("patientSyncFailedDepts getter/setter 正常工作")
        void patientSyncFailedDeptsGetterSetterShouldWork() {
            int expected = 2;
            result.setPatientSyncFailedDepts(expected);
            assertEquals(expected, result.getPatientSyncFailedDepts(), "patientSyncFailedDepts 应匹配设置值");
        }

        @Test
        @DisplayName("totalPatients getter/setter 正常工作")
        void totalPatientsGetterSetterShouldWork() {
            int expected = 100;
            result.setTotalPatients(expected);
            assertEquals(expected, result.getTotalPatients(), "totalPatients 应匹配设置值");
        }

        @Test
        @DisplayName("labSyncSuccess getter/setter 正常工作")
        void labSyncSuccessGetterSetterShouldWork() {
            int expected = 95;
            result.setLabSyncSuccess(expected);
            assertEquals(expected, result.getLabSyncSuccess(), "labSyncSuccess 应匹配设置值");
        }

        @Test
        @DisplayName("labSyncFailed getter/setter 正常工作")
        void labSyncFailedGetterSetterShouldWork() {
            int expected = 5;
            result.setLabSyncFailed(expected);
            assertEquals(expected, result.getLabSyncFailed(), "labSyncFailed 应匹配设置值");
        }

        @Test
        @DisplayName("examSyncSuccess getter/setter 正常工作")
        void examSyncSuccessGetterSetterShouldWork() {
            int expected = 90;
            result.setExamSyncSuccess(expected);
            assertEquals(expected, result.getExamSyncSuccess(), "examSyncSuccess 应匹配设置值");
        }

        @Test
        @DisplayName("examSyncFailed getter/setter 正常工作")
        void examSyncFailedGetterSetterShouldWork() {
            int expected = 10;
            result.setExamSyncFailed(expected);
            assertEquals(expected, result.getExamSyncFailed(), "examSyncFailed 应匹配设置值");
        }

        @Test
        @DisplayName("emrSyncSuccess getter/setter 正常工作")
        void emrSyncSuccessGetterSetterShouldWork() {
            int expected = 88;
            result.setEmrSyncSuccess(expected);
            assertEquals(expected, result.getEmrSyncSuccess(), "emrSyncSuccess 应匹配设置值");
        }

        @Test
        @DisplayName("emrSyncFailed getter/setter 正常工作")
        void emrSyncFailedGetterSetterShouldWork() {
            int expected = 12;
            result.setEmrSyncFailed(expected);
            assertEquals(expected, result.getEmrSyncFailed(), "emrSyncFailed 应匹配设置值");
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("errorMessage 可设置为空字符串")
        void errorMessageCanBeEmptyString() {
            result.setErrorMessage("");
            assertEquals("", result.getErrorMessage(), "errorMessage 应可设置为空字符串");
        }

        @Test
        @DisplayName("数值字段可设置为最大值")
        void numericFieldsCanBeMaxValue() {
            result.setDurationMs(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, result.getDurationMs(), "durationMs 应可设置为 Long.MAX_VALUE");

            result.setTotalPatients(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, result.getTotalPatients(), "totalPatients 应可设置为 Integer.MAX_VALUE");
        }
    }

    @Nested
    @DisplayName("综合场景测试")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("对象可正确实例化")
        void objectCanBeInstantiated() {
            NightlySyncResult newResult = new NightlySyncResult();
            assertNotNull(newResult, "应能正确实例化 NightlySyncResult");
        }

        @Test
        @DisplayName("多字段同时设置后状态正确")
        void multipleFieldsCanBeSetSimultaneously() {
            // 模拟完整的同步结果
            result.setSuccess(true);
            result.setDurationMs(60000L);
            result.setPatientSyncSuccessDepts(2);
            result.setPatientSyncFailedDepts(0);
            result.setTotalPatients(25);
            result.setLabSyncSuccess(25);
            result.setLabSyncFailed(0);
            result.setExamSyncSuccess(24);
            result.setExamSyncFailed(1);
            result.setEmrSyncSuccess(25);
            result.setEmrSyncFailed(0);

            // 验证所有字段状态
            assertTrue(result.isSuccess(), "success 应为 true");
            assertEquals(60000L, result.getDurationMs(), "durationMs 应正确");
            assertEquals(2, result.getPatientSyncSuccessDepts(), "patientSyncSuccessDepts 应正确");
            assertEquals(0, result.getPatientSyncFailedDepts(), "patientSyncFailedDepts 应正确");
            assertEquals(25, result.getTotalPatients(), "totalPatients 应正确");
            assertEquals(25, result.getLabSyncSuccess(), "labSyncSuccess 应正确");
            assertEquals(0, result.getLabSyncFailed(), "labSyncFailed 应正确");
            assertEquals(24, result.getExamSyncSuccess(), "examSyncSuccess 应正确");
            assertEquals(1, result.getExamSyncFailed(), "examSyncFailed 应正确");
            assertEquals(25, result.getEmrSyncSuccess(), "emrSyncSuccess 应正确");
            assertEquals(0, result.getEmrSyncFailed(), "emrSyncFailed 应正确");
        }

        @Test
        @DisplayName("失败场景结果记录正确")
        void failureScenarioRecordedCorrectly() {
            // 模拟同步失败的结果
            result.setSuccess(false);
            result.setErrorMessage("数据库连接超时");
            result.setDurationMs(5000L);
            result.setPatientSyncSuccessDepts(1);
            result.setPatientSyncFailedDepts(1);

            // 验证失败状态
            assertFalse(result.isSuccess(), "success 应为 false");
            assertEquals("数据库连接超时", result.getErrorMessage(), "errorMessage 应正确");
            assertEquals(5000L, result.getDurationMs(), "durationMs 应正确");
            assertEquals(1, result.getPatientSyncSuccessDepts(), "patientSyncSuccessDepts 应正确");
            assertEquals(1, result.getPatientSyncFailedDepts(), "patientSyncFailedDepts 应正确");
        }
    }
}
