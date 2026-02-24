package com.example.medaiassistant.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptedDataTemp简化设计测试
 * 验证ID与REQUEST_ID一致性的简化设计
 */
class EncryptedDataTempSimplifiedTest {

    private EncryptedDataTemp testData;
    private static final String TEST_ID = "cdwyy12345";

    @BeforeEach
    void setUp() {
        testData = new EncryptedDataTemp();
    }

    /**
     * 测试简化设计：ID和REQUEST_ID保持一致
     */
    @Test
    void testIdAndRequestIdConsistency() {
        // 设置ID
        testData.setId(TEST_ID);
        // 简化设计：REQUEST_ID与ID保持一致
        testData.setRequestId(TEST_ID);
        
        // 验证一致性
        assertEquals(TEST_ID, testData.getId());
        assertEquals(TEST_ID, testData.getRequestId());
        assertEquals(testData.getId(), testData.getRequestId());
    }

    /**
     * 测试简化设计的业务逻辑
     */
    @Test
    void testSimplifiedBusinessLogic() {
        String businessId = "cdwyy67890";
        
        // 模拟简化的业务逻辑：使用同一个值设置两个字段
        testData.setId(businessId);
        testData.setRequestId(businessId); // 简化设计：保持一致
        
        // 验证简化设计的效果
        assertTrue(testData.getId().equals(testData.getRequestId()));
        
        // 模拟查询场景：通过ID或REQUEST_ID都能找到同一条记录
        String queryByIdResult = testData.getId();
        String queryByRequestIdResult = testData.getRequestId();
        assertEquals(queryByIdResult, queryByRequestIdResult);
    }

    /**
     * 测试数据状态设置
     */
    @Test
    void testDataStatusSetting() {
        testData.setId(TEST_ID);
        testData.setRequestId(TEST_ID);
        testData.setStatus(DataStatus.RECEIVED);
        testData.setSource("EXECUTION_SERVER");
        
        assertEquals(DataStatus.RECEIVED, testData.getStatus());
        assertEquals("EXECUTION_SERVER", testData.getSource());
        // 验证ID一致性仍然保持
        assertEquals(testData.getId(), testData.getRequestId());
    }
}