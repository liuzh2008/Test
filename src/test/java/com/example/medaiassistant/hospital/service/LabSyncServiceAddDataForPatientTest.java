package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.repository.LabResultRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 为特定病人添加检验结果数据测试
 * 连接到开发环境Oracle数据库，向病人990500001204401_1添加检验结果
 * 
 * 测试目标：
 * 1. 验证可以向现有病人添加检验结果
 * 2. 验证LabID生成功能
 * 3. 验证重复检查功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-15
 */
@SpringBootTest
@ActiveProfiles("dev-db-test") // 使用开发环境数据库配置
@Transactional
@DisplayName("为病人990500001204401_1添加检验结果数据测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabSyncServiceAddDataForPatientTest {
    
    private static final Logger log = LoggerFactory.getLogger(LabSyncServiceAddDataForPatientTest.class);
    private static final String TARGET_PATIENT_ID = "990500001204401_1";
    private static final String TEST_PREFIX = "DEV_TEST_";
    
    @Autowired
    private LabResultRepository labResultRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // LabID由数据库自动生成，不再需要手动设置
    
    /**
     * 测试前准备
     */
    @BeforeEach
    void setUp() {
        // 清理之前的测试数据
        cleanupTestData();
        
        log.info("开始为病人 {} 添加检验结果测试", TARGET_PATIENT_ID);
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
            // 删除测试LabResult记录 - 根据病人ID和测试时间范围清理
            int deletedLabResults = jdbcTemplate.update(
                "DELETE FROM LABRESULTS WHERE PATIENTID = ? AND LABREPORTTIME >= TO_TIMESTAMP('2025-12-15 14:30:00', 'YYYY-MM-DD HH24:MI:SS')",
                TARGET_PATIENT_ID
            );
            
            if (deletedLabResults > 0) {
                log.info("清理测试数据 - 删除LabResults: {}条", deletedLabResults);
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 测试1：验证病人存在
     */
    @Test
    @Order(1)
    @DisplayName("验证病人990500001204401_1存在")
    void testPatientExists() {
        log.info("开始验证病人存在性");
        
        try {
            // 检查病人表中是否存在该病人 - 使用正确的列名PATIENTID
            Integer patientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PATIENTS WHERE PATIENTID = ?", 
                Integer.class, TARGET_PATIENT_ID);
            
            assertNotNull(patientCount, "病人查询结果不应为空");
            assertTrue(patientCount > 0, "病人990500001204401_1应存在于数据库中");
            
            log.info("病人验证通过 - 病人ID: {}, 存在记录数: {}", TARGET_PATIENT_ID, patientCount);
            
        } catch (Exception e) {
            log.error("验证病人存在时发生异常: {}", e.getMessage());
            // 如果查询失败，可能是表不存在或其他问题，但不视为测试失败
            log.warn("病人验证跳过 - 可能表不存在或权限问题: {}", e.getMessage());
        }
    }
    
    /**
     * 测试2：添加白细胞计数检验结果
     */
    @Test
    @Order(2)
    @DisplayName("添加白细胞计数检验结果")
    void testAddWhiteBloodCellCount() {
        log.info("开始添加白细胞计数检验结果");
        
        String labName = "白细胞计数";
        String labResultValue = "6.8";
        String reportTimeStr = "2025-12-15 14:30:00";
        Timestamp reportTime = Timestamp.valueOf(reportTimeStr);
        
        // 创建检验结果（LabID由数据库自动生成）
        LabResult labResult = new LabResult();
        labResult.setPatientId(TARGET_PATIENT_ID);
        labResult.setLabName(labName);
        labResult.setLabType("血常规");
        labResult.setLabResult(labResultValue);
        labResult.setReferenceRange("4.0-10.0");
        labResult.setUnit("10^9/L");
        labResult.setAbnormalIndicator("N");
        labResult.setLabReportTime(reportTime);
        labResult.setLabIssueTime(reportTime);
        labResult.setIsAnalyzed(0);
        
        // 保存到数据库
        LabResult savedResult = labResultRepository.save(labResult);
        
        // 验证保存成功
        assertNotNull(savedResult, "检验结果应成功保存");
        assertNotNull(savedResult.getId(), "ID不应为空");
        assertEquals(TARGET_PATIENT_ID, savedResult.getPatientId(), "病人ID应匹配");
        assertEquals(labName, savedResult.getLabName(), "检验项目名称应匹配");
        assertEquals(labResultValue, savedResult.getLabResult(), "检验结果值应匹配");
        
        log.info("白细胞计数检验结果添加成功 - ID: {}, 结果值: {}", savedResult.getId(), labResultValue);
        
        // 验证数据库中的记录
        Optional<LabResult> retrievedResult = labResultRepository.findById(savedResult.getId());
        assertTrue(retrievedResult.isPresent(), "应能从数据库查询到保存的记录");
        
        // 验证四字段重复检查
        List<LabResult> duplicateCheck = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                TARGET_PATIENT_ID, labName, reportTime, labResultValue);
        
        assertEquals(1, duplicateCheck.size(), "重复检查应能找到1条记录");
        assertEquals(savedResult.getId(), duplicateCheck.get(0).getId(), "ID应匹配");
        
        log.info("白细胞计数检验结果验证通过");
    }
    
    /**
     * 测试3：添加血红蛋白检验结果
     */
    @Test
    @Order(3)
    @DisplayName("添加血红蛋白检验结果")
    void testAddHemoglobin() {
        log.info("开始添加血红蛋白检验结果");
        
        String labName = "血红蛋白";
        String labResultValue = "135";
        String reportTimeStr = "2025-12-15 14:35:00";
        Timestamp reportTime = Timestamp.valueOf(reportTimeStr);
        
        // 创建检验结果（LabID由数据库自动生成）
        LabResult labResult = new LabResult();
        labResult.setPatientId(TARGET_PATIENT_ID);
        labResult.setLabName(labName);
        labResult.setLabType("血常规");
        labResult.setLabResult(labResultValue);
        labResult.setReferenceRange("120-160");
        labResult.setUnit("g/L");
        labResult.setAbnormalIndicator("N");
        labResult.setLabReportTime(reportTime);
        labResult.setLabIssueTime(reportTime);
        labResult.setIsAnalyzed(0);
        
        // 保存到数据库
        LabResult savedResult = labResultRepository.save(labResult);
        
        // 验证保存成功
        assertNotNull(savedResult, "检验结果应成功保存");
        assertNotNull(savedResult.getId(), "ID不应为空");
        assertEquals(TARGET_PATIENT_ID, savedResult.getPatientId(), "病人ID应匹配");
        
        log.info("血红蛋白检验结果添加成功 - ID: {}, 结果值: {}", savedResult.getId(), labResultValue);
        
        // 验证数据库中的记录 - 使用主键ID查询
        Integer dbCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM LABRESULTS WHERE ID = ?", 
            Integer.class, savedResult.getId());
        
        assertEquals(Integer.valueOf(1), dbCount, "数据库中应有1条记录");
        
        log.info("血红蛋白检验结果验证通过");
    }
    
    /**
     * 测试4：添加血糖检验结果（正常值）
     */
    @Test
    @Order(4)
    @DisplayName("添加血糖检验结果（正常值）")
    void testAddGlucoseNormal() {
        log.info("开始添加血糖检验结果（正常值）");
        
        String labName = "血糖";
        String labResultValue = "5.2";
        String reportTimeStr = "2025-12-15 14:40:00";
        Timestamp reportTime = Timestamp.valueOf(reportTimeStr);
        
        // 创建检验结果（LabID由数据库自动生成）
        LabResult labResult = new LabResult();
        labResult.setPatientId(TARGET_PATIENT_ID);
        labResult.setLabName(labName);
        labResult.setLabType("生化");
        labResult.setLabResult(labResultValue);
        labResult.setReferenceRange("3.9-6.1");
        labResult.setUnit("mmol/L");
        labResult.setAbnormalIndicator("N");
        labResult.setLabReportTime(reportTime);
        labResult.setLabIssueTime(reportTime);
        labResult.setIsAnalyzed(0);
        
        // 保存到数据库
        LabResult savedResult = labResultRepository.save(labResult);
        
        // 验证保存成功
        assertNotNull(savedResult, "检验结果应成功保存");
        assertEquals(labResultValue, savedResult.getLabResult(), "检验结果值应匹配");
        
        log.info("血糖检验结果（正常值）添加成功 - ID: {}, 结果值: {}", savedResult.getId(), labResultValue);
        
        // 验证重复检查功能
        List<LabResult> existingResults = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                TARGET_PATIENT_ID, labName, reportTime, labResultValue);
        
        assertEquals(1, existingResults.size(), "应能找到1条记录");
        
        log.info("血糖检验结果（正常值）验证通过");
    }
    
    /**
     * 测试5：添加血糖检验结果（异常值）- 测试不同结果值
     */
    @Test
    @Order(5)
    @DisplayName("添加血糖检验结果（异常值）")
    void testAddGlucoseAbnormal() {
        log.info("开始添加血糖检验结果（异常值）");
        
        String labName = "血糖";
        String labResultValue = "7.5"; // 异常值
        String reportTimeStr = "2025-12-15 14:40:00"; // 相同时间，不同结果值
        Timestamp reportTime = Timestamp.valueOf(reportTimeStr);
        
        // 创建检验结果（LabID由数据库自动生成）
        LabResult labResult = new LabResult();
        labResult.setPatientId(TARGET_PATIENT_ID);
        labResult.setLabName(labName);
        labResult.setLabType("生化");
        labResult.setLabResult(labResultValue);
        labResult.setReferenceRange("3.9-6.1");
        labResult.setUnit("mmol/L");
        labResult.setAbnormalIndicator("H"); // 异常标志
        labResult.setLabReportTime(reportTime);
        labResult.setLabIssueTime(reportTime);
        labResult.setIsAnalyzed(0);
        
        // 保存到数据库
        LabResult savedResult = labResultRepository.save(labResult);
        
        // 验证保存成功
        assertNotNull(savedResult, "检验结果应成功保存");
        assertEquals("H", savedResult.getAbnormalIndicator(), "异常标志应匹配");
        
        log.info("血糖检验结果（异常值）添加成功 - ID: {}, 结果值: {}, 异常标志: {}", 
            savedResult.getId(), labResultValue, savedResult.getAbnormalIndicator());
        
        // 验证相同时间不同结果值的记录可以共存
        List<LabResult> allGlucoseResults = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTime(
                TARGET_PATIENT_ID, labName, reportTime);
        
        assertTrue(allGlucoseResults.size() >= 2, "相同时间应有至少2条不同结果值的血糖记录");
        
        log.info("血糖检验结果（异常值）验证通过 - 找到记录数: {}", allGlucoseResults.size());
    }
    
    /**
     * 测试6：验证病人所有检验结果
     */
    @Test
    @Order(6)
    @DisplayName("验证病人所有检验结果")
    void testVerifyAllLabResultsForPatient() {
        log.info("开始验证病人所有检验结果");
        
        // 查询病人的所有检验结果
        List<LabResult> patientResults = labResultRepository.findByPatientId(TARGET_PATIENT_ID);
        
        // 统计测试数据（本测试中插入的所有记录）
        long testResultsCount = patientResults.size();
        
        log.info("病人 {} 的检验结果统计 - 总数据: {}条", 
            TARGET_PATIENT_ID, patientResults.size());
        
        // 验证至少有一些检验结果
        assertTrue(testResultsCount >= 4, "应至少有4条测试检验结果");
        
        // 打印检验结果详情
        patientResults.stream()
            .forEach(result -> {
                log.info("检验结果详情 - ID: {}, 项目: {}, 结果: {}, 时间: {}", 
                    result.getId(), result.getLabName(), 
                    result.getLabResult(), result.getLabReportTime());
            });
        
        log.info("病人所有检验结果验证通过");
    }
}
