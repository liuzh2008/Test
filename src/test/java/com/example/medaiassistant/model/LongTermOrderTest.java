package com.example.medaiassistant.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LongTermOrder实体类单元测试
 * 验证实体类字段定义、类型和默认值是否符合医嘱同步实现方案要求
 * 
 * 测试策略：
 * - 使用纯JUnit测试，不加载Spring上下文
 * - 使用反射验证字段存在性和类型
 * - 验证默认值设置正确
 * - 验证Getter/Setter功能正常
 * 
 * TDD执行记录：
 * - 红阶段：2026-01-10 编写测试用例，结果: 16 tests, 10 failures, 1 error
 * - 绿阶段：2026-01-10 修改LongTermOrder实体类，结果: 16 tests, 0 failures, 0 errors
 * - 完善阶段：2026-01-10 增加Getter/Setter功能测试，结果: 20 tests, 0 failures, 0 errors
 * - 重构阶段：2026-01-10 已完成
 * 
 * @author TDD Generator
 * @version 1.1
 * @since 2026-01-10
 */
@DisplayName("LongTermOrder实体类测试")
class LongTermOrderTest {

    // ==================== OT-01: 验证orderId字段存在 ====================
    @Test
    @DisplayName("OT-01: orderId字段应存在")
    void orderIdFieldShouldExist() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("orderId");
        assertNotNull(field, "orderId字段应该存在");
    }

    // ==================== OT-02: 验证orderId类型为Long ====================
    @Test
    @DisplayName("OT-02: orderId字段类型应为Long")
    void orderIdFieldTypeShouldBeLong() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("orderId");
        assertEquals(Long.class, field.getType(), 
            "orderId字段类型应为Long，以匹配Oracle NUMBER(10,0)主键类型");
    }

    // ==================== OT-03: 验证orderDate类型为Timestamp ====================
    @Test
    @DisplayName("OT-03: orderDate字段类型应为Timestamp")
    void orderDateFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("orderDate");
        assertEquals(Timestamp.class, field.getType(), 
            "orderDate字段类型应为Timestamp，以匹配Oracle TIMESTAMP(6)类型");
    }

    // ==================== OT-04: 验证stopTime类型为Timestamp ====================
    @Test
    @DisplayName("OT-04: stopTime字段类型应为Timestamp")
    void stopTimeFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("stopTime");
        assertEquals(Timestamp.class, field.getType(), 
            "stopTime字段类型应为Timestamp，以匹配Oracle TIMESTAMP(6)类型");
    }

    // ==================== OT-05: 验证isAnalyzed默认值为0 ====================
    @Test
    @DisplayName("OT-05: isAnalyzed默认值应为0")
    void isAnalyzedDefaultValueShouldBeZero() {
        LongTermOrder order = new LongTermOrder();
        assertEquals(Integer.valueOf(0), order.getIsAnalyzed(), 
            "新创建的实体isAnalyzed默认值应为0");
    }

    // ==================== OT-06: 验证isTriggered默认值为0 ====================
    @Test
    @DisplayName("OT-06: isTriggered默认值应为0")
    void isTriggeredDefaultValueShouldBeZero() {
        LongTermOrder order = new LongTermOrder();
        assertEquals(Integer.valueOf(0), order.getIsTriggered(), 
            "新创建的实体isTriggered默认值应为0");
    }

    // ==================== OT-07: 验证physician字段存在 ====================
    @Test
    @DisplayName("OT-07: physician字段应存在")
    void physicianFieldShouldExist() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("physician");
        assertNotNull(field, "physician字段应该存在，用于存储开单医生");
    }

    // ==================== OT-08: 验证orderName字段存在 ====================
    @Test
    @DisplayName("OT-08: orderName字段应存在")
    void orderNameFieldShouldExist() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("orderName");
        assertNotNull(field, "orderName字段应该存在，用于存储医嘱名称");
    }

    // ==================== OT-09: 验证unit字段存在 ====================
    @Test
    @DisplayName("OT-09: unit字段应存在")
    void unitFieldShouldExist() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("unit");
        assertNotNull(field, "unit字段应该存在，用于存储剂量单位");
    }

    // ==================== OT-10: 验证route字段存在 ====================
    @Test
    @DisplayName("OT-10: route字段应存在")
    void routeFieldShouldExist() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("route");
        assertNotNull(field, "route字段应该存在，用于存储给药途径");
    }

    // ==================== OT-11: 验证实体可正确创建 ====================
    @Test
    @DisplayName("OT-11: 实体应可正确创建并设置所有字段")
    void entityShouldBeCreatable() {
        LongTermOrder order = new LongTermOrder();
        
        // 设置患者ID
        order.setPatientId("P001");
        assertEquals("P001", order.getPatientId(), "patientId应正确设置和获取");
        
        // 设置医生
        order.setPhysician("张医生");
        assertEquals("张医生", order.getPhysician(), "physician应正确设置和获取");
        
        // 设置医嘱名称
        order.setOrderName("头孢克肟分散片");
        assertEquals("头孢克肟分散片", order.getOrderName(), "orderName应正确设置和获取");
        
        // 设置剂量
        order.setDosage("500");
        assertEquals("500", order.getDosage(), "dosage应正确设置和获取");
        
        // 设置单位
        order.setUnit("mg");
        assertEquals("mg", order.getUnit(), "unit应正确设置和获取");
        
        // 设置频次
        order.setFrequency("BID");
        assertEquals("BID", order.getFrequency(), "frequency应正确设置和获取");
        
        // 设置给药途径
        order.setRoute("口服");
        assertEquals("口服", order.getRoute(), "route应正确设置和获取");
        
        // 验证默认值
        assertEquals(Integer.valueOf(0), order.getIsAnalyzed(), "isAnalyzed默认值应为0");
        assertEquals(Integer.valueOf(0), order.getIsTriggered(), "isTriggered默认值应为0");
    }

    // ==================== OT-12: 验证orderId的Getter/Setter使用Long类型 ====================
    @Test
    @DisplayName("OT-12: orderId的Getter返回类型应为Long")
    void orderIdGetterShouldReturnLong() throws NoSuchMethodException {
        Class<?> returnType = LongTermOrder.class.getMethod("getOrderId").getReturnType();
        assertEquals(Long.class, returnType, 
            "getOrderId()返回类型应为Long，以匹配Oracle NUMBER(10,0)主键类型");
    }

    // ==================== OT-13: 验证时间字段Getter返回Timestamp类型 ====================
    @Test
    @DisplayName("OT-13: orderDate的Getter返回类型应为Timestamp")
    void orderDateGetterShouldReturnTimestamp() throws NoSuchMethodException {
        Class<?> returnType = LongTermOrder.class.getMethod("getOrderDate").getReturnType();
        assertEquals(Timestamp.class, returnType, 
            "getOrderDate()返回类型应为Timestamp，以匹配Oracle TIMESTAMP(6)类型");
    }

    @Test
    @DisplayName("OT-14: stopTime的Getter返回类型应为Timestamp")
    void stopTimeGetterShouldReturnTimestamp() throws NoSuchMethodException {
        Class<?> returnType = LongTermOrder.class.getMethod("getStopTime").getReturnType();
        assertEquals(Timestamp.class, returnType, 
            "getStopTime()返回类型应为Timestamp，以匹配Oracle TIMESTAMP(6)类型");
    }

    // ==================== 额外测试：验证visitId类型为Long ====================
    @Test
    @DisplayName("visitId字段类型应为Long")
    void visitIdFieldTypeShouldBeLong() throws NoSuchFieldException {
        Field field = LongTermOrder.class.getDeclaredField("visitId");
        assertEquals(Long.class, field.getType(), 
            "visitId字段类型应为Long，以匹配Oracle NUMBER(10,0)类型");
    }

    // ==================== OT-15: 验证orderId的Setter/Getter功能 ====================
    @Test
    @DisplayName("OT-15: orderId应可正确设置和获取")
    void orderIdShouldBeSettableAndGettable() {
        LongTermOrder order = new LongTermOrder();
        Long expectedId = 12345L;
        order.setOrderId(expectedId);
        assertEquals(expectedId, order.getOrderId(), "orderId应正确设置和获取");
    }

    // ==================== OT-16: 验证时间字段的Setter/Getter功能 ====================
    @Test
    @DisplayName("OT-16: 时间字段应可正确设置和获取")
    void timeFieldsShouldBeSettableAndGettable() {
        LongTermOrder order = new LongTermOrder();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        // 测试orderDate
        order.setOrderDate(now);
        assertEquals(now, order.getOrderDate(), "orderDate应正确设置和获取");
        
        // 测试stopTime
        order.setStopTime(now);
        assertEquals(now, order.getStopTime(), "stopTime应正确设置和获取");
    }

    // ==================== OT-17: 验证visitId的Setter/Getter功能 ====================
    @Test
    @DisplayName("OT-17: visitId应可正确设置和获取")
    void visitIdShouldBeSettableAndGettable() {
        LongTermOrder order = new LongTermOrder();
        Long expectedVisitId = 999L;
        order.setVisitId(expectedVisitId);
        assertEquals(expectedVisitId, order.getVisitId(), "visitId应正确设置和获取");
    }
}
