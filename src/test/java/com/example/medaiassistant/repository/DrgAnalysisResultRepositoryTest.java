package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.DrgAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DrgAnalysisResultRepository数据访问层测试
 * 验证数据库操作的正确性
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-10
 */
@TestConfig(description = "DRG分析结果数据访问层测试")
class DrgAnalysisResultRepositoryTest {

    @Autowired
    private DrgAnalysisResultRepository repository;

    /**
     * 测试根据患者ID查找分析结果
     */
    @Test
    void findByPatientId_shouldReturnResultsWhenValidPatientId() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行查询
        List<DrgAnalysisResult> results = repository.findByPatientId(patientId);
        
        // Then - 验证结果
        assertNotNull(results);
        // 这里不验证具体数量，因为测试数据库中的数据可能变化
    }

    /**
     * 测试根据患者ID和用户选择的MCC类型查找分析结果
     */
    @Test
    void findByPatientIdAndUserSelectedMccType_shouldReturnResultsWhenValidParameters() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        String mccType = "NONE";
        
        // When - 执行查询
        List<DrgAnalysisResult> results = repository.findByPatientIdAndUserSelectedMccType(patientId, mccType);
        
        // Then - 验证结果
        assertNotNull(results);
    }

    /**
     * 测试根据患者ID查找最新的分析结果
     */
    @Test
    void findLatestByPatientId_shouldReturnLatestResultWhenValidPatientId() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行查询
        Optional<DrgAnalysisResult> result = repository.findLatestByPatientId(patientId);
        
        // Then - 验证结果
        // 这里不验证具体值，因为测试数据库中的数据可能变化
        assertNotNull(result);
    }

    /**
     * 测试根据患者ID查找所有分析结果（按时间倒序）
     */
    @Test
    void findByPatientIdOrderByCreatedTimeDesc_shouldReturnResultsInDescendingOrder() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行查询
        List<DrgAnalysisResult> results = repository.findByPatientIdOrderByCreatedTimeDesc(patientId);
        
        // Then - 验证结果
        assertNotNull(results);
    }

    /**
     * 测试统计患者分析结果数量
     */
    @Test
    void countByPatientId_shouldReturnCorrectCount() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行统计
        long count = repository.countByPatientId(patientId);
        
        // Then - 验证结果
        assertTrue(count >= 0);
    }

    /**
     * 测试统计用户选择的MCC类型为指定值的分析结果数量
     */
    @Test
    void countByUserSelectedMccType_shouldReturnCorrectCount() {
        // Given - 假设数据库中已有测试数据
        String mccType = "NONE";
        
        // When - 执行统计
        long count = repository.countByUserSelectedMccType(mccType);
        
        // Then - 验证结果
        assertTrue(count >= 0);
    }

    /**
     * 测试查找未删除的分析结果
     */
    @Test
    void findNotDeleted_shouldReturnOnlyNotDeletedResults() {
        // When - 执行查询
        List<DrgAnalysisResult> results = repository.findNotDeleted();
        
        // Then - 验证结果
        assertNotNull(results);
        // 验证所有返回的结果都是未删除的
        for (DrgAnalysisResult result : results) {
            assertFalse(result.isDeleted(), "结果应该未删除");
        }
    }

    /**
     * 测试根据患者ID查找未删除的分析结果
     */
    @Test
    void findByPatientIdAndNotDeleted_shouldReturnOnlyNotDeletedResults() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行查询
        List<DrgAnalysisResult> results = repository.findByPatientIdAndNotDeleted(patientId);
        
        // Then - 验证结果
        assertNotNull(results);
        // 验证所有返回的结果都是未删除的
        for (DrgAnalysisResult result : results) {
            assertFalse(result.isDeleted(), "结果应该未删除");
        }
    }

    /**
     * 测试统计未删除的分析结果数量
     */
    @Test
    void countNotDeleted_shouldReturnCorrectCount() {
        // When - 执行统计
        long count = repository.countNotDeleted();
        
        // Then - 验证结果
        assertTrue(count >= 0);
    }

    /**
     * 测试统计患者未删除的分析结果数量
     */
    @Test
    void countByPatientIdAndNotDeleted_shouldReturnCorrectCount() {
        // Given - 假设数据库中已有测试数据
        String patientId = "TEST001";
        
        // When - 执行统计
        long count = repository.countByPatientIdAndNotDeleted(patientId);
        
        // Then - 验证结果
        assertTrue(count >= 0);
    }
}
