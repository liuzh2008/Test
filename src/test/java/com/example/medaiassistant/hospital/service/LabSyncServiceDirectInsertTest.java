package com.example.medaiassistant.hospital.service;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直接数据插入测试 - 使用JdbcTemplate直接插入数据到开发环境数据库
 * 绕过JPA类型转换问题，直接测试数据插入功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-15
 */
@SpringBootTest
@ActiveProfiles("dev-db-test")
@DisplayName("直接数据插入测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabSyncServiceDirectInsertTest {
    
    private static final Logger log = LoggerFactory.getLogger(LabSyncServiceDirectInsertTest.class);
    private static final String TARGET_PATIENT_ID = "990500001204401_1";
    private static final String TEST_PREFIX = "DIRECT_TEST_";
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 测试前准备
     */
    @BeforeEach
    void setUp() {
        cleanupTestData();
        log.info("开始直接数据插入测试 - 病人ID: {}", TARGET_PATIENT_ID);
    }
    
    /**
     * 测试后清理
     */
    @AfterEach
    void tearDown() {
        cleanupTestData();
    }
    
    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        try {
            // 删除测试LabResult记录 - 按照病人ID和测试项目名称前缀清理
            int deletedLabResults = jdbcTemplate.update(
                "DELETE FROM LABRESULTS WHERE PATIENTID = ? AND LABNAME LIKE ?",
                TARGET_PATIENT_ID, TEST_PREFIX + "%"
            );
            
            if (deletedLabResults > 0) {
                log.info("清理测试数据 - 删除LabResults: {}条", deletedLabResults);
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 测试1：直接插入白细胞计数检验结果
     */
    @Test
    @Order(1)
    @DisplayName("直接插入白细胞计数检验结果")
    void testDirectInsertWhiteBloodCellCount() {
        log.info("开始直接插入白细胞计数检验结果");
        
        String labName = TEST_PREFIX + "白细胞计数";
        String labResultValue = "6.8";
        String reportTime = "2025-12-15 14:30:00";
        
        try {
            // 直接使用SQL插入数据（不含LABID字段，由数据库自动生成ID）
            int rowsInserted = jdbcTemplate.update(
                "INSERT INTO LABRESULTS (LABNAME, LABTYPE, LABRESULT, REFERENCERANGE, UNIT, " +
                "ABNORMAL_INDICATOR, LABISSUETIME, LABREPORTTIME, PATIENTID, ISANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                labName, "血常规", labResultValue, "4.0-10.0", "10^9/L", "N",
                reportTime, reportTime, TARGET_PATIENT_ID, 0
            );
            
            assertEquals(1, rowsInserted, "应插入1条记录");
            log.info("白细胞计数检验结果直接插入成功 - 结果值: {}", labResultValue);
            
            // 验证数据是否插入成功
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                Integer.class, labName, TARGET_PATIENT_ID
            );
            
            assertEquals(Integer.valueOf(1), count, "数据库中应有1条记录");
            log.info("白细胞计数检验结果验证成功 - 找到记录数: {}", count);
            
        } catch (Exception e) {
            log.error("直接插入白细胞计数检验结果失败: {}", e.getMessage());
            fail("直接插入失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试2：直接插入血红蛋白检验结果
     */
    @Test
    @Order(2)
    @DisplayName("直接插入血红蛋白检验结果")
    void testDirectInsertHemoglobin() {
        log.info("开始直接插入血红蛋白检验结果");
        
        String labName = TEST_PREFIX + "血红蛋白";
        String labResultValue = "135";
        String reportTime = "2025-12-15 14:35:00";
        
        try {
            // 直接使用SQL插入数据（不含LABID字段）
            int rowsInserted = jdbcTemplate.update(
                "INSERT INTO LABRESULTS (LABNAME, LABTYPE, LABRESULT, REFERENCERANGE, UNIT, " +
                "ABNORMAL_INDICATOR, LABISSUETIME, LABREPORTTIME, PATIENTID, ISANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                labName, "血常规", labResultValue, "120-160", "g/L", "N",
                reportTime, reportTime, TARGET_PATIENT_ID, 0
            );
            
            assertEquals(1, rowsInserted, "应插入1条记录");
            log.info("血红蛋白检验结果直接插入成功 - 结果值: {}", labResultValue);
            
            // 查询插入的数据
            String retrievedResult = jdbcTemplate.queryForObject(
                "SELECT LABRESULT FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                String.class, labName, TARGET_PATIENT_ID
            );
            
            assertEquals(labResultValue, retrievedResult, "检验结果值应匹配");
            log.info("血红蛋白检验结果验证成功 - 查询结果值: {}", retrievedResult);
            
        } catch (Exception e) {
            log.error("直接插入血红蛋白检验结果失败: {}", e.getMessage());
            fail("直接插入失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试3：直接插入血糖检验结果（正常值）
     */
    @Test
    @Order(3)
    @DisplayName("直接插入血糖检验结果（正常值）")
    void testDirectInsertGlucoseNormal() {
        log.info("开始直接插入血糖检验结果（正常值）");
        
        String labName = TEST_PREFIX + "血糖_正常";
        String labResultValue = "5.2";
        String reportTime = "2025-12-15 14:40:00";
        
        try {
            // 直接使用SQL插入数据（不含LABID字段）
            int rowsInserted = jdbcTemplate.update(
                "INSERT INTO LABRESULTS (LABNAME, LABTYPE, LABRESULT, REFERENCERANGE, UNIT, " +
                "ABNORMAL_INDICATOR, LABISSUETIME, LABREPORTTIME, PATIENTID, ISANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                labName, "生化", labResultValue, "3.9-6.1", "mmol/L", "N",
                reportTime, reportTime, TARGET_PATIENT_ID, 0
            );
            
            assertEquals(1, rowsInserted, "应插入1条记录");
            log.info("血糖检验结果（正常值）直接插入成功 - 结果值: {}", labResultValue);
            
            // 验证异常标志
            String abnormalIndicator = jdbcTemplate.queryForObject(
                "SELECT ABNORMAL_INDICATOR FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                String.class, labName, TARGET_PATIENT_ID
            );
            
            assertEquals("N", abnormalIndicator, "异常标志应匹配");
            log.info("血糖检验结果（正常值）验证成功 - 异常标志: {}", abnormalIndicator);
            
        } catch (Exception e) {
            log.error("直接插入血糖检验结果（正常值）失败: {}", e.getMessage());
            fail("直接插入失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试4：直接插入血糖检验结果（异常值）
     */
    @Test
    @Order(4)
    @DisplayName("直接插入血糖检验结果（异常值）")
    void testDirectInsertGlucoseAbnormal() {
        log.info("开始直接插入血糖检验结果（异常值）");
        
        String labName = TEST_PREFIX + "血糖_异常";
        String labResultValue = "7.5"; // 异常值
        String reportTime = "2025-12-15 14:40:00";
        
        try {
            // 直接使用SQL插入数据（不含LABID字段）
            int rowsInserted = jdbcTemplate.update(
                "INSERT INTO LABRESULTS (LABNAME, LABTYPE, LABRESULT, REFERENCERANGE, UNIT, " +
                "ABNORMAL_INDICATOR, LABISSUETIME, LABREPORTTIME, PATIENTID, ISANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                labName, "生化", labResultValue, "3.9-6.1", "mmol/L", "H",
                reportTime, reportTime, TARGET_PATIENT_ID, 0
            );
            
            assertEquals(1, rowsInserted, "应插入1条记录");
            log.info("血糖检验结果（异常值）直接插入成功 - 结果值: {}, 异常标志: H", labResultValue);
            
            // 验证记录已插入
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                Integer.class, labName, TARGET_PATIENT_ID
            );
            
            assertNotNull(count, "查询结果不应为null");
            assertTrue(count >= 1, "应有1条血糖记录");
            log.info("血糖检验结果（异常值）验证成功 - 记录数: {}", count);
            
        } catch (Exception e) {
            log.error("直接插入血糖检验结果（异常值）失败: {}", e.getMessage());
            fail("直接插入失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试5：验证数据插入功能
     * 这个测试独立运行，不依赖其他测试的数据
     */
    @Test
    @Order(5)
    @DisplayName("验证数据插入功能")
    void testVerifyDataInsertion() {
        log.info("开始验证数据插入功能");
        
        try {
            // 先插入一条测试数据
            String labName = TEST_PREFIX + "验证测试项目";
            String labResultValue = "100";
            String reportTime = "2025-12-15 15:00:00";
            
            // 直接使用SQL插入数据（不含LABID字段）
            int rowsInserted = jdbcTemplate.update(
                "INSERT INTO LABRESULTS (LABNAME, LABTYPE, LABRESULT, REFERENCERANGE, UNIT, " +
                "ABNORMAL_INDICATOR, LABISSUETIME, LABREPORTTIME, PATIENTID, ISANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                labName, "验证测试", labResultValue, "0-200", "U/L", "N",
                reportTime, reportTime, TARGET_PATIENT_ID, 0
            );
            
            assertEquals(1, rowsInserted, "应插入1条记录");
            log.info("验证测试数据插入成功 - 项目: {}, 结果值: {}", labName, labResultValue);
            
            // 验证数据是否插入成功
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                Integer.class, labName, TARGET_PATIENT_ID
            );
            
            assertEquals(Integer.valueOf(1), count, "数据库中应有1条记录");
            log.info("验证测试数据验证成功 - 找到记录数: {}", count);
            
            // 查询插入的数据详情
            String retrievedResult = jdbcTemplate.queryForObject(
                "SELECT LABRESULT FROM LABRESULTS WHERE LABNAME = ? AND PATIENTID = ?",
                String.class, labName, TARGET_PATIENT_ID
            );
            
            assertEquals(labResultValue, retrievedResult, "检验结果值应匹配");
            log.info("验证测试数据详情验证成功 - 查询结果值: {}", retrievedResult);
            
            log.info("数据插入功能验证通过");
            
        } catch (Exception e) {
            log.error("验证数据插入功能失败: {}", e.getMessage());
            fail("验证失败: " + e.getMessage());
        }
    }
}
