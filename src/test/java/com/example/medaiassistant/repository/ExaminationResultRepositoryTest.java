package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.ExaminationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExaminationResultRepository 数据访问层测试
 * 任务2：验证按ExaminationID查询的方法
 * 
 * 测试用例（全部通过）：
 * - findByExaminationId 返回正确结果 ✅
 * - existsByExaminationId 正确判断存在性 ✅
 * - 边界条件测试（null/空字符串） ✅
 * - 方法一致性测试 ✅
 * 
 * @author System
 * @version 1.2
 * @since 2025-12-30
 */
@TestConfig(description = "检查结果Repository扩展功能测试")
@DisplayName("ExaminationResultRepository 扩展功能测试")
@Transactional
class ExaminationResultRepositoryTest {

    @Autowired
    private ExaminationResultRepository repository;

    // ==================== findByExaminationId 测试 ====================

    /**
     * 测试：当ExaminationID存在时，findByExaminationId应返回对应记录
     * 验收标准：findByExaminationId返回正确结果
     */
    @Test
    @DisplayName("findByExaminationId - 存在的ExaminationID应返回对应记录")
    void findByExaminationId_ExistingId_ShouldReturnResult() {
        // Given - 准备测试数据
        String testExaminationId = "TEST_EXAM_" + System.currentTimeMillis();
        ExaminationResult testResult = createTestExaminationResult(testExaminationId, "TEST_PATIENT_001");
        repository.save(testResult);
        repository.flush();

        // When - 调用待测试方法
        Optional<ExaminationResult> found = repository.findByExaminationId(testExaminationId);

        // Then - 验证结果
        assertTrue(found.isPresent(), "应该找到对应的检查结果记录");
        assertEquals(testExaminationId, found.get().getExaminationId(), "ExaminationID应匹配");
        assertEquals("TEST_PATIENT_001", found.get().getPatientId(), "PatientID应匹配");
    }

    /**
     * 测试：当ExaminationID不存在时，findByExaminationId应返回空Optional
     * 验收标准：findByExaminationId返回正确结果
     */
    @Test
    @DisplayName("findByExaminationId - 不存在的ExaminationID应返回空")
    void findByExaminationId_NonExistingId_ShouldReturnEmpty() {
        // Given - 使用一个不存在的ID
        String nonExistingId = "NON_EXISTING_EXAM_" + System.currentTimeMillis();

        // When - 调用待测试方法
        Optional<ExaminationResult> found = repository.findByExaminationId(nonExistingId);

        // Then - 验证结果
        assertFalse(found.isPresent(), "不存在的ExaminationID应返回空Optional");
    }

    /**
     * 测试：当ExaminationID为null时，findByExaminationId应返回空Optional
     * 边界条件测试
     */
    @Test
    @DisplayName("findByExaminationId - null参数应返回空")
    void findByExaminationId_NullId_ShouldReturnEmpty() {
        // When - 调用待测试方法
        Optional<ExaminationResult> found = repository.findByExaminationId(null);

        // Then - 验证结果
        assertFalse(found.isPresent(), "null ExaminationID应返回空Optional");
    }

    /**
     * 测试：当ExaminationID为空字符串时，findByExaminationId应返回空Optional
     * 边界条件测试
     */
    @Test
    @DisplayName("findByExaminationId - 空字符串参数应返回空")
    void findByExaminationId_EmptyString_ShouldReturnEmpty() {
        // When - 调用待测试方法
        Optional<ExaminationResult> found = repository.findByExaminationId("");

        // Then - 验证结果
        assertFalse(found.isPresent(), "空字符串ExaminationID应返回空Optional");
    }

    // ==================== existsByExaminationId 测试 ====================

    /**
     * 测试：当ExaminationID存在时，existsByExaminationId应返回true
     * 验收标准：existsByExaminationId正确判断存在性
     */
    @Test
    @DisplayName("existsByExaminationId - 存在的ExaminationID应返回true")
    void existsByExaminationId_ExistingId_ShouldReturnTrue() {
        // Given - 准备测试数据
        String testExaminationId = "TEST_EXIST_" + System.currentTimeMillis();
        ExaminationResult testResult = createTestExaminationResult(testExaminationId, "TEST_PATIENT_002");
        repository.save(testResult);
        repository.flush();

        // When - 调用待测试方法
        boolean exists = repository.existsByExaminationId(testExaminationId);

        // Then - 验证结果
        assertTrue(exists, "存在的ExaminationID应返回true");
    }

    /**
     * 测试：当ExaminationID不存在时，existsByExaminationId应返回false
     * 验收标准：existsByExaminationId正确判断存在性
     */
    @Test
    @DisplayName("existsByExaminationId - 不存在的ExaminationID应返回false")
    void existsByExaminationId_NonExistingId_ShouldReturnFalse() {
        // Given - 使用一个不存在的ID
        String nonExistingId = "NON_EXIST_EXAM_" + System.currentTimeMillis();

        // When - 调用待测试方法
        boolean exists = repository.existsByExaminationId(nonExistingId);

        // Then - 验证结果
        assertFalse(exists, "不存在的ExaminationID应返回false");
    }

    /**
     * 测试：当ExaminationID为null时，existsByExaminationId应返回false
     * 边界条件测试
     */
    @Test
    @DisplayName("existsByExaminationId - null参数应返回false")
    void existsByExaminationId_NullId_ShouldReturnFalse() {
        // When - 调用待测试方法
        boolean exists = repository.existsByExaminationId(null);

        // Then - 验证结果
        assertFalse(exists, "null ExaminationID应返回false");
    }

    /**
     * 测试：当ExaminationID为空字符串时，existsByExaminationId应返回false
     * 边界条件测试
     */
    @Test
    @DisplayName("existsByExaminationId - 空字符串参数应返回false")
    void existsByExaminationId_EmptyString_ShouldReturnFalse() {
        // When - 调用待测试方法
        boolean exists = repository.existsByExaminationId("");

        // Then - 验证结果
        assertFalse(exists, "空字符串ExaminationID应返回false");
    }

    // ==================== 一致性测试 ====================

    /**
     * 测试：findByExaminationId与existsByExaminationId结果应保持一致
     * 验证两个方法的逻辑一致性
     */
    @Test
    @DisplayName("一致性测试 - findByExaminationId与existsByExaminationId结果应一致")
    void findAndExists_ShouldBeConsistent() {
        // Given - 准备测试数据
        String testExaminationId = "TEST_CONSISTENCY_" + System.currentTimeMillis();
        ExaminationResult testResult = createTestExaminationResult(testExaminationId, "TEST_PATIENT_003");
        repository.save(testResult);
        repository.flush();

        // When - 调用两个方法
        Optional<ExaminationResult> found = repository.findByExaminationId(testExaminationId);
        boolean exists = repository.existsByExaminationId(testExaminationId);

        // Then - 验证一致性
        assertEquals(found.isPresent(), exists, "findByExaminationId与existsByExaminationId结果应保持一致");
        assertTrue(found.isPresent(), "应该找到对应的检查结果记录");
        assertTrue(exists, "existsByExaminationId应返回true");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用ExaminationResult实体
     * 
     * @param examinationId 检查申请号（主键）
     * @param patientId 患者ID
     * @return 构建完成的测试实体
     */
    private ExaminationResult createTestExaminationResult(String examinationId, String patientId) {
        ExaminationResult result = new ExaminationResult();
        result.setExaminationId(examinationId);
        result.setPatientId(patientId);
        result.setCheckName("测试检查项目");
        result.setCheckType("CT");
        result.setCheckDescription("测试检查描述");
        result.setCheckConclusion("测试检查结论");
        result.setCheckIssueTime(new Timestamp(System.currentTimeMillis()));
        result.setCheckExecuteTime(new Timestamp(System.currentTimeMillis()));
        result.setCheckReportTime(new Timestamp(System.currentTimeMillis()));
        result.setUpdateDt(new Timestamp(System.currentTimeMillis()));
        result.setIsAnalyzed(0);
        return result;
    }
}
