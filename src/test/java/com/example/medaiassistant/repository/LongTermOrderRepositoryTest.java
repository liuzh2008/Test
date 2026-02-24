package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.LongTermOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * LongTermOrderRepository数据访问层测试
 * <p>
 * 测试findByOrderId和existsByOrderId方法，用于支持医嘱同步功能。
 * 这些方法用于在同步时检查医嘱记录是否已存在，实现upsert策略。
 * </p>
 * 
 * <h2>测试用例覆盖</h2>
 * <ul>
 *   <li>RT-01~03: findByOrderId方法测试（存在/不存在/null参数）</li>
 *   <li>RT-04~06: existsByOrderId方法测试（存在/不存在/null参数）</li>
 *   <li>RT-07: 两个方法结果一致性测试</li>
 * </ul>
 * 
 * <h2>TDD状态</h2>
 * <ul>
 *   <li>红阶段：已完成（测试编译失败）</li>
 *   <li>绿阶段：已完成（所有测试通过）</li>
 *   <li>重构阶段：已完成（代码优化和注释完善）</li>
 * </ul>
 * 
 * @author TDD Generator
 * @version 1.2
 * @since 2026-01-10
 * @see LongTermOrderRepository#findByOrderId(Long)
 * @see LongTermOrderRepository#existsByOrderId(Long)
 */
@TestConfig(description = "长期医嘱Repository扩展方法测试")
@DisplayName("LongTermOrderRepository 扩展方法测试")
class LongTermOrderRepositoryTest {

    @Autowired
    private LongTermOrderRepository repository;

    // ==================== 辅助方法 ====================

    /**
     * 获取数据库中已存在的医嘱ID
     * 如果数据库为空，则跳过测试
     * 
     * @return 已存在的医嘱ID
     */
    private Long getExistingOrderId() {
        List<LongTermOrder> existingOrders = repository.findAll();
        assumeTrue(!existingOrders.isEmpty(), "数据库中没有医嘱数据，跳过此测试");
        return existingOrders.get(0).getOrderId();
    }

    /**
     * 获取一个不存在的医嘱ID
     * 
     * @return 不存在的医嘱ID
     */
    private Long getNonExistingOrderId() {
        return -999999L;
    }

    // ==================== findByOrderId 测试 ====================

    /**
     * RT-01: 验证存在的ID返回记录
     * 使用数据库中已存在的医嘱记录进行测试
     */
    @Test
    @DisplayName("RT-01: findByOrderId - 存在的ID应返回记录")
    void findByOrderId_ExistingId_ShouldReturnResult() {
        // Given: 数据库中存在医嘱记录
        Long existingOrderId = getExistingOrderId();
        
        // When: 调用findByOrderId方法
        Optional<LongTermOrder> result = repository.findByOrderId(existingOrderId);
        
        // Then: 应返回非空结果
        assertTrue(result.isPresent(), "存在的OrderId应返回记录");
        assertEquals(existingOrderId, result.get().getOrderId(), "返回的OrderId应与查询ID一致");
    }

    /**
     * RT-02: 验证不存在的ID返回空
     */
    @Test
    @DisplayName("RT-02: findByOrderId - 不存在的ID应返回空")
    void findByOrderId_NonExistingId_ShouldReturnEmpty() {
        // Given: 使用一个不存在的ID
        Long nonExistingId = getNonExistingOrderId();
        
        // When: 调用findByOrderId方法
        Optional<LongTermOrder> result = repository.findByOrderId(nonExistingId);
        
        // Then: 应返回空Optional
        assertFalse(result.isPresent(), "不存在的OrderId应返回空结果");
    }

    /**
     * RT-03: 验证null参数返回空
     */
    @Test
    @DisplayName("RT-03: findByOrderId - null参数应返回空")
    void findByOrderId_NullId_ShouldReturnEmpty() {
        // Given: null参数
        Long nullId = null;
        
        // When: 调用findByOrderId方法
        Optional<LongTermOrder> result = repository.findByOrderId(nullId);
        
        // Then: 应返回空Optional
        assertFalse(result.isPresent(), "null参数应返回空结果");
    }

    // ==================== existsByOrderId 测试 ====================

    /**
     * RT-04: 验证存在的ID返回true
     */
    @Test
    @DisplayName("RT-04: existsByOrderId - 存在的ID应返回true")
    void existsByOrderId_ExistingId_ShouldReturnTrue() {
        // Given: 数据库中存在医嘱记录
        Long existingOrderId = getExistingOrderId();
        
        // When: 调用existsByOrderId方法
        boolean exists = repository.existsByOrderId(existingOrderId);
        
        // Then: 应返回true
        assertTrue(exists, "存在的OrderId应返回true");
    }

    /**
     * RT-05: 验证不存在的ID返回false
     */
    @Test
    @DisplayName("RT-05: existsByOrderId - 不存在的ID应返回false")
    void existsByOrderId_NonExistingId_ShouldReturnFalse() {
        // Given: 使用一个不存在的ID
        Long nonExistingId = getNonExistingOrderId();
        
        // When: 调用existsByOrderId方法
        boolean exists = repository.existsByOrderId(nonExistingId);
        
        // Then: 应返回false
        assertFalse(exists, "不存在的OrderId应返回false");
    }

    /**
     * RT-06: 验证null参数返回false
     */
    @Test
    @DisplayName("RT-06: existsByOrderId - null参数应返回false")
    void existsByOrderId_NullId_ShouldReturnFalse() {
        // Given: null参数
        Long nullId = null;
        
        // When: 调用existsByOrderId方法
        boolean exists = repository.existsByOrderId(nullId);
        
        // Then: 应返回false
        assertFalse(exists, "null参数应返回false");
    }

    // ==================== 一致性测试 ====================

    /**
     * RT-07: 验证findByOrderId和existsByOrderId结果一致性
     */
    @Test
    @DisplayName("RT-07: findByOrderId和existsByOrderId结果应保持一致")
    void findAndExists_ShouldBeConsistent() {
        // Given: 数据库中存在医嘱记录
        Long existingOrderId = getExistingOrderId();
        Long nonExistingId = getNonExistingOrderId();
        
        // When & Then: 验证存在的ID一致性
        Optional<LongTermOrder> findResult = repository.findByOrderId(existingOrderId);
        boolean existsResult = repository.existsByOrderId(existingOrderId);
        assertEquals(findResult.isPresent(), existsResult, "findByOrderId和existsByOrderId对存在的ID结果应一致");
        
        // When & Then: 验证不存在的ID一致性
        Optional<LongTermOrder> findResultNonExisting = repository.findByOrderId(nonExistingId);
        boolean existsResultNonExisting = repository.existsByOrderId(nonExistingId);
        assertEquals(findResultNonExisting.isPresent(), existsResultNonExisting, "findByOrderId和existsByOrderId对不存在的ID结果应一致");
    }
}
