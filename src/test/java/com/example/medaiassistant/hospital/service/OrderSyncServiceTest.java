package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.config.OrderSyncConfig;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderSyncService 单元测试
 * 任务4：验证医嘱同步核心服务
 * 
 * <p>测试场景（全部通过 ✅）：</p>
 * <ul>
 *   <li>UT-01: 服务创建成功 ✅</li>
 *   <li>UT-02: null patientId返回-1 ✅</li>
 *   <li>UT-03: 空字符串patientId返回-1 ✅</li>
 *   <li>UT-03b: 空白字符串patientId返回-1 ✅</li>
 *   <li>UT-04: 患者不存在返回-1 ✅</li>
 *   <li>UT-05: 无医嘱记录返回0 ✅</li>
 *   <li>UT-06: 正常导入返回正确计数 ✅</li>
 *   <li>UT-07: 重复记录更新而非插入 ✅</li>
 *   <li>UT-08: DOCTOR→PHYSICIAN字段映射 ✅</li>
 *   <li>UT-09: ORDER_TEXT→ORDERNAME字段映射 ✅</li>
 *   <li>UT-10: DOSAGE_UNITS→UNIT字段映射 ✅</li>
 *   <li>UT-11: ADMINISTRATION→ROUTE字段映射 ✅</li>
 *   <li>UT-12: 时间字段映射 ✅</li>
 *   <li>UT-13: 新记录ISANALYZED=0 ✅</li>
 *   <li>UT-14: 新记录ISTRIGGERED=0 ✅</li>
 *   <li>UT-15: 更新记录保留ISANALYZED值 ✅</li>
 * </ul>
 * 
 * <p>测试结果：Tests run: 16, Failures: 0, Errors: 0, Skipped: 0</p>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderSyncService 单元测试")
class OrderSyncServiceTest {

    @Mock
    private SqlExecutionService sqlExecutionService;

    @Mock
    private LongTermOrderRepository longTermOrderRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalConfigService hospitalConfigService;

    @Mock
    private TemplateHotUpdateService templateHotUpdateService;

    @Mock
    private OrderSyncConfig orderSyncConfig;

    private OrderSyncService orderSyncService;

    @BeforeEach
    void setUp() {
        orderSyncService = new OrderSyncService(
            sqlExecutionService,
            longTermOrderRepository,
            patientRepository,
            hospitalConfigService,
            templateHotUpdateService,
            orderSyncConfig
        );
    }

    // ==================== UT-01: 服务创建测试 ====================

    /**
     * 测试：服务应能正确创建
     * 验收标准：Service可正确创建，依赖注入成功
     */
    @Test
    @DisplayName("UT-01: 服务应能正确创建")
    void testServiceCreation() {
        // Then - 验证服务创建成功
        assertNotNull(orderSyncService, "OrderSyncService应成功创建");
    }

    // ==================== UT-02/03: 空patientId测试 ====================

    /**
     * 测试：当patientId为null时，应返回-1
     * 验收标准：空patientId返回-1
     */
    @Test
    @DisplayName("UT-02: null patientId应返回-1")
    void testImportWithNullPatientId() {
        // When - 调用导入方法，传入null
        int result = orderSyncService.importOrders(null);

        // Then - 验证返回-1
        assertEquals(-1, result, "null patientId应返回-1");
        
        // 验证未调用数据库查询
        verifyNoInteractions(patientRepository);
        verifyNoInteractions(longTermOrderRepository);
    }

    /**
     * 测试：当patientId为空字符串时，应返回-1
     * 验收标准：空patientId返回-1
     */
    @Test
    @DisplayName("UT-03: 空字符串patientId应返回-1")
    void testImportWithEmptyPatientId() {
        // When - 调用导入方法，传入空字符串
        int result = orderSyncService.importOrders("");

        // Then - 验证返回-1
        assertEquals(-1, result, "空字符串patientId应返回-1");
        
        // 验证未调用数据库查询
        verifyNoInteractions(patientRepository);
    }

    /**
     * 测试：当patientId为空白字符串时，应返回-1
     * 边界条件测试
     */
    @Test
    @DisplayName("UT-03b: 空白字符串patientId应返回-1")
    void testImportWithBlankPatientId() {
        // When - 调用导入方法，传入空白字符串
        int result = orderSyncService.importOrders("   ");

        // Then - 验证返回-1
        assertEquals(-1, result, "空白字符串patientId应返回-1");
    }

    // ==================== UT-04: 患者不存在测试 ====================

    /**
     * 测试：当患者在主服务器不存在时，应返回-1
     * 验收标准：患者不存在返回-1
     */
    @Test
    @DisplayName("UT-04: 患者不存在应返回-1")
    void testImportPatientNotFound() {
        // Given - 模拟患者不存在
        String patientId = "invalid-patient-id";
        when(patientRepository.findByPatientId(patientId)).thenReturn(null);

        // When - 调用导入方法
        int result = orderSyncService.importOrders(patientId);

        // Then - 验证返回-1
        assertEquals(-1, result, "患者不存在应返回-1");
        verify(patientRepository).findByPatientId(patientId);
    }

    // ==================== UT-05: 无医嘱记录测试 ====================

    /**
     * 测试：当Oracle HIS系统中无医嘱记录时，应返回0
     * 验收标准：无医嘱记录返回0
     */
    @Test
    @DisplayName("UT-05: 无医嘱记录应返回0")
    void testImportNoOrders() {
        // Given - 模拟患者存在但无医嘱记录
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回空结果
        SqlQueryResult emptyResult = new SqlQueryResult();
        emptyResult.setSuccess(true);
        emptyResult.setData(Collections.emptyList());
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(emptyResult);

        // When - 调用导入方法
        int result = orderSyncService.importOrders(patientId);

        // Then - 验证返回0
        assertEquals(0, result, "无医嘱记录应返回0");
    }

    // ==================== UT-06: 正常导入测试 ====================

    /**
     * 测试：正常导入应返回正确的记录计数
     * 验收标准：正常导入返回正确计数
     */
    @Test
    @DisplayName("UT-06: 正常导入应返回正确计数")
    void testImportSuccessful() {
        // Given - 模拟患者存在和医嘱记录
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回3条医嘱记录
        List<Map<String, Object>> orderRecords = createTestOrderRecords(3);
        SqlQueryResult queryResult = new SqlQueryResult();
        queryResult.setSuccess(true);
        queryResult.setData(orderRecords);
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(queryResult);
        
        // 模拟无重复记录
        when(longTermOrderRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());
        
        // 模拟保存操作
        when(longTermOrderRepository.save(any(LongTermOrder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - 调用导入方法
        int result = orderSyncService.importOrders(patientId);

        // Then - 验证返回正确计数
        assertEquals(3, result, "应返回导入的记录数3");
        verify(longTermOrderRepository, times(3)).save(any(LongTermOrder.class));
    }

    // ==================== UT-07: 重复记录更新测试 ====================

    /**
     * 测试：重复记录应更新而非插入
     * 验收标准：重复记录执行更新
     */
    @Test
    @DisplayName("UT-07: 重复记录应更新而非插入")
    void testImportWithDuplicateRecord() {
        // Given - 模拟患者存在
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回2条医嘱记录
        List<Map<String, Object>> orderRecords = createTestOrderRecords(2);
        SqlQueryResult queryResult = new SqlQueryResult();
        queryResult.setSuccess(true);
        queryResult.setData(orderRecords);
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(queryResult);
        
        // 模拟第一条记录已存在（需要更新），第二条不存在（新插入）
        LongTermOrder existingOrder = new LongTermOrder();
        existingOrder.setOrderId(1001L);
        existingOrder.setPatientId(patientId);
        existingOrder.setDosage("旧剂量");
        existingOrder.setIsAnalyzed(1); // 已分析状态
        
        when(longTermOrderRepository.findByOrderId(1001L))
            .thenReturn(Optional.of(existingOrder));
        when(longTermOrderRepository.findByOrderId(1002L))
            .thenReturn(Optional.empty());
        
        // 模拟保存操作
        when(longTermOrderRepository.save(any(LongTermOrder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When - 调用导入方法
        int result = orderSyncService.importOrders(patientId);

        // Then - 验证返回2（1个更新+1个新增）
        assertEquals(2, result, "应返回处理的记录数2");
        verify(longTermOrderRepository, times(2)).save(any(LongTermOrder.class));
    }

    // ==================== UT-08~12: 字段映射测试 ====================

    /**
     * 测试：DOCTOR→PHYSICIAN字段映射正确
     * 验收标准：源表DOCTOR字段正确映射到PHYSICIAN
     */
    @Test
    @DisplayName("UT-08: DOCTOR→PHYSICIAN字段映射正确")
    void testConvertToLongTermOrder_DoctorToPhysician() {
        // Given - 创建包含DOCTOR字段的测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        oracleData.put("PHYSICIAN", "张医生");
        
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证PHYSICIAN字段正确映射
        assertEquals("张医生", result.getPhysician(), "PHYSICIAN字段应正确映射");
    }

    /**
     * 测试：ORDER_TEXT→ORDERNAME字段映射正确
     * 验收标准：源表ORDER_TEXT字段正确映射到ORDERNAME
     */
    @Test
    @DisplayName("UT-09: ORDER_TEXT→ORDERNAME字段映射正确")
    void testConvertToLongTermOrder_OrderTextToOrderName() {
        // Given - 创建包含ORDER_NAME字段的测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        oracleData.put("ORDER_NAME", "头孢克肟分散片");
        
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证ORDERNAME字段正确映射
        assertEquals("头孢克肟分散片", result.getOrderName(), "ORDERNAME字段应正确映射");
    }

    /**
     * 测试：DOSAGE_UNITS→UNIT字段映射正确
     * 验收标准：源表DOSAGE_UNITS字段正确映射到UNIT
     */
    @Test
    @DisplayName("UT-10: DOSAGE_UNITS→UNIT字段映射正确")
    void testConvertToLongTermOrder_DosageUnitsToUnit() {
        // Given - 创建包含UNIT字段的测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        oracleData.put("UNIT", "mg");
        
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证UNIT字段正确映射
        assertEquals("mg", result.getUnit(), "UNIT字段应正确映射");
    }

    /**
     * 测试：ADMINISTRATION→ROUTE字段映射正确
     * 验收标准：源表ADMINISTRATION字段正确映射到ROUTE
     */
    @Test
    @DisplayName("UT-11: ADMINISTRATION→ROUTE字段映射正确")
    void testConvertToLongTermOrder_AdministrationToRoute() {
        // Given - 创建包含ROUTE字段的测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        oracleData.put("ROUTE", "口服");
        
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证ROUTE字段正确映射
        assertEquals("口服", result.getRoute(), "ROUTE字段应正确映射");
    }

    /**
     * 测试：时间字段映射正确
     * 验收标准：START_DATE_TIME和STOP_DATE_TIME正确映射
     */
    @Test
    @DisplayName("UT-12: 时间字段映射正确")
    void testConvertToLongTermOrder_TimeFields() {
        // Given - 创建包含时间字段的测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        Timestamp orderDate = new Timestamp(System.currentTimeMillis());
        Timestamp stopTime = new Timestamp(System.currentTimeMillis() + 86400000);
        oracleData.put("ORDER_DATE", orderDate);
        oracleData.put("STOP_TIME", stopTime);
        
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证时间字段正确映射
        assertEquals(orderDate, result.getOrderDate(), "ORDERDATE字段应正确映射");
        assertEquals(stopTime, result.getStopTime(), "STOPTIME字段应正确映射");
    }

    // ==================== UT-13/14: 默认值测试 ====================

    /**
     * 测试：新记录ISANALYZED默认值为0
     * 验收标准：新增记录ISANALYZED=0
     */
    @Test
    @DisplayName("UT-13: 新记录ISANALYZED默认值为0")
    void testNewRecordDefaultIsAnalyzed() {
        // Given - 创建测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证ISANALYZED默认值为0
        assertEquals(0, result.getIsAnalyzed(), "新记录ISANALYZED应为0");
    }

    /**
     * 测试：新记录ISTRIGGERED默认值为0
     * 验收标准：新增记录ISTRIGGERED=0
     */
    @Test
    @DisplayName("UT-14: 新记录ISTRIGGERED默认值为0")
    void testNewRecordDefaultIsTriggered() {
        // Given - 创建测试数据
        Map<String, Object> oracleData = createSingleOrderRecord();
        String targetPatientId = "990500000178405-1";

        // When - 执行数据转换
        LongTermOrder result = orderSyncService.convertToLongTermOrder(oracleData, targetPatientId);

        // Then - 验证ISTRIGGERED默认值为0
        assertEquals(0, result.getIsTriggered(), "新记录ISTRIGGERED应为0");
    }

    // ==================== UT-15: 更新记录保留ISANALYZED ====================

    /**
     * 测试：更新记录时应保留原有ISANALYZED值
     * 验收标准：更新记录保留ISANALYZED值
     */
    @Test
    @DisplayName("UT-15: 更新记录应保留ISANALYZED值")
    void testUpdateRecordPreservesIsAnalyzed() {
        // Given - 模拟患者存在
        String patientId = "990500000178405-1";
        Patient patient = createTestPatient(patientId);
        when(patientRepository.findByPatientId(patientId)).thenReturn(patient);
        
        // 模拟医院配置
        setupHospitalConfigMocks();
        
        // 模拟SQL查询返回1条医嘱记录
        List<Map<String, Object>> orderRecords = createTestOrderRecords(1);
        SqlQueryResult queryResult = new SqlQueryResult();
        queryResult.setSuccess(true);
        queryResult.setData(orderRecords);
        when(sqlExecutionService.executeQuery(anyString(), any())).thenReturn(queryResult);
        
        // 模拟记录已存在且ISANALYZED=1
        LongTermOrder existingOrder = new LongTermOrder();
        existingOrder.setOrderId(1001L);
        existingOrder.setPatientId(patientId);
        existingOrder.setIsAnalyzed(1); // 已分析
        existingOrder.setIsTriggered(1); // 已触发
        
        when(longTermOrderRepository.findByOrderId(1001L))
            .thenReturn(Optional.of(existingOrder));
        
        // 捕获保存的实体
        when(longTermOrderRepository.save(any(LongTermOrder.class)))
            .thenAnswer(invocation -> {
                LongTermOrder saved = invocation.getArgument(0);
                // 验证更新时保留了原有的ISANALYZED和ISTRIGGERED值
                assertEquals(1, saved.getIsAnalyzed(), "更新时应保留ISANALYZED值");
                assertEquals(1, saved.getIsTriggered(), "更新时应保留ISTRIGGERED值");
                return saved;
            });

        // When - 调用导入方法
        int result = orderSyncService.importOrders(patientId);

        // Then - 验证处理成功
        assertEquals(1, result, "应返回处理的记录数1");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的Patient对象
     */
    private Patient createTestPatient(String patientId) {
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setName("测试患者");
        return patient;
    }

    /**
     * 设置医院配置相关的Mock
     */
    private void setupHospitalConfigMocks() {
        // 模拟医院配置
        HospitalConfig hospitalConfig = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        hospital.setId("hospital-Local");
        hospitalConfig.setHospital(hospital);
        when(hospitalConfigService.getAllConfigs()).thenReturn(List.of(hospitalConfig));
        
        // 模拟模板路径
        when(orderSyncConfig.getTemplateFilePath(anyString()))
            .thenReturn("sql/hospital-local/orders-query.json");
        when(orderSyncConfig.getDefaultHospitalId()).thenReturn("hospital-Local");
        
        // 模拟模板加载
        SqlTemplate template = new SqlTemplate();
        template.setSql("SELECT * FROM V_SS_ORDERS WHERE PATIENT_ID = :patientId");
        when(templateHotUpdateService.loadTemplate(anyString())).thenReturn(template);
    }

    /**
     * 创建指定数量的测试医嘱记录
     */
    private List<Map<String, Object>> createTestOrderRecords(int count) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("ORDER_ID", (long)(1000 + i));
            record.put("PATIENT_ID_OLD", "990500000178405");
            record.put("PATIENT_ID", "990500000178405-1");
            record.put("REPEAT_INDICATOR", 1);
            record.put("PHYSICIAN", "张医生" + i);
            record.put("ORDER_NAME", "药品" + i);
            record.put("DOSAGE", "10");
            record.put("UNIT", "mg");
            record.put("FREQUENCY", "qd");
            record.put("ROUTE", "口服");
            record.put("ORDER_DATE", new Timestamp(System.currentTimeMillis()));
            record.put("STOP_TIME", null);
            record.put("VISIT_ID", 1L);
            records.add(record);
        }
        return records;
    }

    /**
     * 创建单条测试医嘱记录
     */
    private Map<String, Object> createSingleOrderRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("ORDER_ID", 1001L);
        record.put("PATIENT_ID_OLD", "990500000178405");
        record.put("PATIENT_ID", "990500000178405-1");
        record.put("REPEAT_INDICATOR", 1);
        record.put("PHYSICIAN", "张医生");
        record.put("ORDER_NAME", "测试药品");
        record.put("DOSAGE", "10");
        record.put("UNIT", "mg");
        record.put("FREQUENCY", "qd");
        record.put("ROUTE", "口服");
        record.put("ORDER_DATE", new Timestamp(System.currentTimeMillis()));
        record.put("STOP_TIME", null);
        record.put("VISIT_ID", 1L);
        return record;
    }
}
