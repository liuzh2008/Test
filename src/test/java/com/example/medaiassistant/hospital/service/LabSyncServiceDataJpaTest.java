package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.LabResultRepository;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LIS检验结果同步服务数据层集成测试
 * 使用@DataJpaTest真正插入数据到数据库中，验证LabID生成和重复检查功能
 * 
 * 测试目标：
 * 1. 验证LabID是否正确生成并插入数据库
 * 2. 验证重复检查逻辑（四字段检查）是否正常工作
 * 3. 验证数据转换和保存功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-15
 */
@DataJpaTest
@ActiveProfiles("datajpa-test")
@DisplayName("LIS检验结果同步服务数据层集成测试")
class LabSyncServiceDataJpaTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private LabResultRepository labResultRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    /**
     * 测试前准备：创建测试病人数据
     */
    @BeforeEach
    void setUp() {
        // 创建测试病人
        Patient testPatient = new Patient();
        testPatient.setPatientId("TEST-990500000640090-1");
        testPatient.setName("测试病人");
        testPatient.setGender("男");
        testPatient.setDateOfBirth(new Date());
        testPatient.setAdmissionTime(new Date());
        testPatient.setIsInHospital(true);
        testPatient.setDepartment("测试科室");
        testPatient.setBedNumber("TEST-001");
        testPatient.setStatus("在院");
        
        entityManager.persist(testPatient);
        entityManager.flush();
    }
    
    /**
     * 测试1：验证LabID生成和插入功能
     * 真正插入数据到数据库，验证数据是否正确保存
     */
    @Test
    @DisplayName("测试LabID生成和插入功能 - 应能生成LabID并插入数据库")
    void testLabIdGenerationAndInsertion() {
        // 准备测试数据
        String mainServerPatientId = "TEST-990500000640090-1";
        
        // 首先验证病人是否存在
        Patient patient = patientRepository.findByPatientId(mainServerPatientId);
        assertNotNull(patient, "测试病人应存在");
        
        // 创建一个模拟的LabResult并保存（LabID由数据库自动生成）
        LabResult testLabResult = new LabResult();
        testLabResult.setPatientId(mainServerPatientId);
        testLabResult.setLabName("白细胞计数");
        testLabResult.setLabType("血常规");
        testLabResult.setLabResult("6.5");
        testLabResult.setReferenceRange("4.0-10.0");
        testLabResult.setUnit("10^9/L");
        testLabResult.setAbnormalIndicator("N");
        testLabResult.setLabReportTime(Timestamp.valueOf("2025-12-15 10:30:00"));
        testLabResult.setLabIssueTime(Timestamp.valueOf("2025-12-15 10:30:00"));
        testLabResult.setIsAnalyzed(0);
        
        // 保存到数据库
        LabResult savedResult = labResultRepository.save(testLabResult);
        entityManager.flush();
        
        // 验证数据是否正确保存
        assertNotNull(savedResult, "LabResult应成功保存");
        assertNotNull(savedResult.getId(), "ID不应为空");
        assertEquals(mainServerPatientId, savedResult.getPatientId(), "PatientID应匹配");
        assertEquals("白细胞计数", savedResult.getLabName(), "LabName应匹配");
        
        // 从数据库查询验证
        Optional<LabResult> retrievedResult = labResultRepository.findById(savedResult.getId());
        assertTrue(retrievedResult.isPresent(), "应能从数据库查询到保存的记录");
        assertEquals(savedResult.getId(), retrievedResult.get().getId(), "ID应一致");
        
        System.out.println("测试通过：ID生成和插入功能正常");
        System.out.println("生成的ID: " + savedResult.getId());
        System.out.println("保存的记录数: " + labResultRepository.count());
    }
    
    /**
     * 测试2：验证重复检查功能（四字段检查）
     * 插入相同记录，验证重复检查是否生效
     * 注意：H2数据库不支持DBMS_LOB函数，跳过此测试
     */
    @Test
    @DisplayName("测试重复检查功能 - 应能检测重复记录（跳过：H2不支持DBMS_LOB）")
    @org.junit.jupiter.api.Disabled("H2数据库不支持Oracle DBMS_LOB函数")
    void testDuplicateRecordCheck() {
        // 准备测试数据
        String mainServerPatientId = "TEST-990500000640090-1";
        String labName = "血红蛋白";
        String labResultValue = "135";
        Timestamp reportTime = Timestamp.valueOf("2025-12-15 11:00:00");
        
        // 第一次插入（LabID由数据库自动生成）
        LabResult firstResult = new LabResult();
        firstResult.setPatientId(mainServerPatientId);
        firstResult.setLabName(labName);
        firstResult.setLabType("血常规");
        firstResult.setLabResult(labResultValue);
        firstResult.setReferenceRange("120-160");
        firstResult.setUnit("g/L");
        firstResult.setAbnormalIndicator("N");
        firstResult.setLabReportTime(reportTime);
        firstResult.setLabIssueTime(reportTime);
        firstResult.setIsAnalyzed(0);
        
        LabResult savedFirst = labResultRepository.save(firstResult);
        entityManager.flush();
        assertNotNull(savedFirst, "第一次插入应成功");
        Long savedId = savedFirst.getId();
        
        // 验证数据库中有记录
        List<LabResult> existingResults = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                mainServerPatientId, labName, reportTime, labResultValue);
        
        assertEquals(1, existingResults.size(), "应能找到一条记录");
        assertEquals(savedId, existingResults.get(0).getId(), "ID应匹配");
        
        // 验证四字段查询功能
        List<LabResult> duplicateCheck = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                mainServerPatientId, labName, reportTime, labResultValue);
        
        // 验证重复检查查询功能正常
        assertFalse(duplicateCheck.isEmpty(), "重复检查应能找到记录");
        assertEquals(savedId, duplicateCheck.get(0).getId(), "应找到第一次插入的记录");
        
        System.out.println("测试通过：重复检查功能正常");
        System.out.println("找到记录数: " + duplicateCheck.size());
        System.out.println("数据库总记录数: " + labResultRepository.count());
    }
    
    /**
     * 测试3：验证不同结果值的记录不被视为重复
     * 相同患者、相同项目、相同时间，但不同结果值，应视为不同记录
     * 注意：H2数据库不支持DBMS_LOB函数，跳过此测试
     */
    @Test
    @DisplayName("测试不同结果值记录 - 应视为不同记录（跳过：H2不支持DBMS_LOB）")
    @org.junit.jupiter.api.Disabled("H2数据库不支持Oracle DBMS_LOB函数")
    void testDifferentResultValueRecords() {
        // 准备测试数据
        String mainServerPatientId = "TEST-990500000640090-1";
        String labName = "血糖";
        Timestamp reportTime = Timestamp.valueOf("2025-12-15 12:00:00");
        
        // 插入第一条记录（LabID由数据库自动生成）
        LabResult firstResult = new LabResult();
        firstResult.setPatientId(mainServerPatientId);
        firstResult.setLabName(labName);
        firstResult.setLabType("生化");
        firstResult.setLabResult("5.5"); // 结果值1
        firstResult.setReferenceRange("3.9-6.1");
        firstResult.setUnit("mmol/L");
        firstResult.setAbnormalIndicator("N");
        firstResult.setLabReportTime(reportTime);
        firstResult.setLabIssueTime(reportTime);
        firstResult.setIsAnalyzed(0);
        
        labResultRepository.save(firstResult);
        
        // 插入第二条记录（相同患者、项目、时间，但不同结果值）
        LabResult secondResult = new LabResult();
        secondResult.setPatientId(mainServerPatientId);
        secondResult.setLabName(labName);
        secondResult.setLabType("生化");
        secondResult.setLabResult("6.5"); // 不同的结果值
        secondResult.setReferenceRange("3.9-6.1");
        secondResult.setUnit("mmol/L");
        secondResult.setAbnormalIndicator("H"); // 异常标志也不同
        secondResult.setLabReportTime(reportTime);
        secondResult.setLabIssueTime(reportTime);
        secondResult.setIsAnalyzed(0);
        
        labResultRepository.save(secondResult);
        entityManager.flush();
        
        // 查询所有记录
        List<LabResult> allResults = labResultRepository.findByPatientId(mainServerPatientId);
        assertEquals(2, allResults.size(), "应有2条不同结果值的记录");
        
        // 分别查询不同结果值的记录
        List<LabResult> result55 = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                mainServerPatientId, labName, reportTime, "5.5");
        
        List<LabResult> result65 = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTimeAndLabResult(
                mainServerPatientId, labName, reportTime, "6.5");
        
        assertEquals(1, result55.size(), "应找到结果值为5.5的记录");
        assertEquals(1, result65.size(), "应找到结果值为6.5的记录");
        
        System.out.println("测试通过：不同结果值记录正确处理");
        System.out.println("找到5.5记录: " + result55.size() + "条");
        System.out.println("找到6.5记录: " + result65.size() + "条");
        System.out.println("数据库总记录数: " + labResultRepository.count());
    }
    
    /**
     * 测试4：验证LabID唯一性
     * 确保生成的LabID是唯一的
     */
    @Test
    @DisplayName("测试LabID唯一性 - 应生成唯一LabID")
    void testLabIdUniqueness() {
        // 准备测试数据
        String mainServerPatientId = "TEST-990500000640090-1";
        
        // 创建多个测试记录（LabID由数据库自动生成）
        for (int i = 0; i < 5; i++) {
            LabResult result = new LabResult();
            result.setPatientId(mainServerPatientId);
            result.setLabName("测试项目_" + i);
            result.setLabType("测试类型");
            result.setLabResult("值_" + i);
            result.setReferenceRange("参考范围");
            result.setUnit("单位");
            result.setAbnormalIndicator("N");
            result.setLabReportTime(Timestamp.valueOf("2025-12-15 14:00:00"));
            result.setLabIssueTime(Timestamp.valueOf("2025-12-15 14:00:00"));
            result.setIsAnalyzed(0);
            
            labResultRepository.save(result);
        }
        
        entityManager.flush();
        
        // 查询所有记录
        List<LabResult> allResults = labResultRepository.findByPatientId(mainServerPatientId);
        assertEquals(5, allResults.size(), "应插入5条记录");
        
        // 验证ID唯一性
        long uniqueIdCount = allResults.stream()
            .map(LabResult::getId)
            .distinct()
            .count();
        
        assertEquals(5, uniqueIdCount, "所有ID应是唯一的");
        
        System.out.println("测试通过：ID唯一性验证成功");
        System.out.println("插入记录数: " + allResults.size());
        System.out.println("唯一ID数: " + uniqueIdCount);
    }
    
    /**
     * 测试5：验证三字段查询方法（向后兼容）
     * 确保原有的三字段查询方法仍然可用
     */
    @Test
    @DisplayName("测试三字段查询方法 - 应保持向后兼容")
    void testThreeFieldQueryMethod() {
        // 准备测试数据
        String mainServerPatientId = "TEST-990500000640090-1";
        String labName = "C反应蛋白";
        Timestamp reportTime = Timestamp.valueOf("2025-12-15 15:00:00");
        
        // 插入记录（LabID由数据库自动生成）
        LabResult result = new LabResult();
        result.setPatientId(mainServerPatientId);
        result.setLabName(labName);
        result.setLabType("炎症指标");
        result.setLabResult("8.2");
        result.setReferenceRange("0-5");
        result.setUnit("mg/L");
        result.setAbnormalIndicator("H");
        result.setLabReportTime(reportTime);
        result.setLabIssueTime(reportTime);
        result.setIsAnalyzed(0);
        
        LabResult savedResult = labResultRepository.save(result);
        entityManager.flush();
        
        // 使用三字段查询方法
        List<LabResult> threeFieldResults = labResultRepository
            .findByPatientIdAndLabNameAndLabReportTime(mainServerPatientId, labName, reportTime);
        
        assertEquals(1, threeFieldResults.size(), "三字段查询应找到记录");
        assertEquals(savedResult.getId(), threeFieldResults.get(0).getId(), "ID应匹配");
        
        System.out.println("测试通过：三字段查询方法向后兼容");
        System.out.println("三字段查询结果数: " + threeFieldResults.size());
    }
}
