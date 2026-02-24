package com.example.medaiassistant.model;

import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmrContent 实体类单元测试
 * 验证EMR病历内容实体的字段映射、去重字段、ID生成策略和CLOB字段映射
 * 
 * @author System
 * @version 1.0
 * @since 2026-01-11
 */
@DisplayName("EmrContent 实体类测试")
class EmrContentTest {

    // ==================== 字段映射测试 ====================

    @Test
    @DisplayName("应包含所有必要字段")
    void testEmrContentFieldMapping() throws NoSuchFieldException {
        // 验证所有15个字段存在
        String[] expectedFields = {
            "id", "patientId", "patiId", "visitId", "patiName",
            "deptCode", "deptName", "docTypeName", "recordDate", "content",
            "createUserId", "createBy", "docTitleTime", "modifiedOn", "deleteMark",
            "sourceTable", "sourceId"
        };
        
        for (String fieldName : expectedFields) {
            Field field = EmrContent.class.getDeclaredField(fieldName);
            assertNotNull(field, fieldName + " 字段应该存在");
        }
    }

    @Test
    @DisplayName("字段类型应正确映射")
    void testFieldTypesMapping() throws NoSuchFieldException {
        // ID字段应为Long类型
        assertEquals(Long.class, EmrContent.class.getDeclaredField("id").getType(), 
            "id字段类型应为Long");
        
        // visitId字段应为Integer类型
        assertEquals(Integer.class, EmrContent.class.getDeclaredField("visitId").getType(), 
            "visitId字段类型应为Integer");
        
        // deleteMark字段应为Integer类型
        assertEquals(Integer.class, EmrContent.class.getDeclaredField("deleteMark").getType(), 
            "deleteMark字段类型应为Integer");
        
        // 时间字段应为Timestamp类型
        assertEquals(Timestamp.class, EmrContent.class.getDeclaredField("recordDate").getType(), 
            "recordDate字段类型应为Timestamp");
        assertEquals(Timestamp.class, EmrContent.class.getDeclaredField("docTitleTime").getType(), 
            "docTitleTime字段类型应为Timestamp");
        assertEquals(Timestamp.class, EmrContent.class.getDeclaredField("modifiedOn").getType(), 
            "modifiedOn字段类型应为Timestamp");
    }

    // ==================== 去重字段测试 ====================

    @Test
    @DisplayName("应包含SOURCE_TABLE和SOURCE_ID去重字段")
    void testSourceTableAndSourceIdFields() throws NoSuchFieldException {
        // 验证sourceTable字段存在
        Field sourceTableField = EmrContent.class.getDeclaredField("sourceTable");
        assertNotNull(sourceTableField, "sourceTable字段应该存在");
        assertEquals(String.class, sourceTableField.getType(), "sourceTable字段类型应为String");
        
        // 验证sourceId字段存在
        Field sourceIdField = EmrContent.class.getDeclaredField("sourceId");
        assertNotNull(sourceIdField, "sourceId字段应该存在");
        assertEquals(String.class, sourceIdField.getType(), "sourceId字段类型应为String");
    }

    @Test
    @DisplayName("去重字段应有正确的@Column注解")
    void testSourceFieldsColumnAnnotation() throws NoSuchFieldException {
        // 验证sourceTable字段的Column注解
        Field sourceTableField = EmrContent.class.getDeclaredField("sourceTable");
        Column sourceTableColumn = sourceTableField.getAnnotation(Column.class);
        assertNotNull(sourceTableColumn, "sourceTable字段应有@Column注解");
        assertEquals("SOURCE_TABLE", sourceTableColumn.name(), "sourceTable的列名应为SOURCE_TABLE");
        
        // 验证sourceId字段的Column注解
        Field sourceIdField = EmrContent.class.getDeclaredField("sourceId");
        Column sourceIdColumn = sourceIdField.getAnnotation(Column.class);
        assertNotNull(sourceIdColumn, "sourceId字段应有@Column注解");
        assertEquals("SOURCE_ID", sourceIdColumn.name(), "sourceId的列名应为SOURCE_ID");
    }

    // ==================== ID生成策略测试 ====================

    @Test
    @DisplayName("ID字段应使用IDENTITY生成策略")
    void testIdGenerationStrategy() throws NoSuchFieldException {
        Field idField = EmrContent.class.getDeclaredField("id");
        
        // 验证@Id注解存在
        Id idAnnotation = idField.getAnnotation(Id.class);
        assertNotNull(idAnnotation, "id字段应有@Id注解");
        
        // 验证@GeneratedValue注解使用IDENTITY策略
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(generatedValue, "id字段应有@GeneratedValue注解");
        assertEquals(GenerationType.IDENTITY, generatedValue.strategy(), 
            "ID生成策略应为IDENTITY");
    }

    @Test
    @DisplayName("ID字段应映射到ID列")
    void testIdColumnMapping() throws NoSuchFieldException {
        Field idField = EmrContent.class.getDeclaredField("id");
        Column column = idField.getAnnotation(Column.class);
        assertNotNull(column, "id字段应有@Column注解");
        assertEquals("ID", column.name(), "id字段的列名应为ID");
    }

    // ==================== CLOB字段映射测试 ====================

    @Test
    @DisplayName("CONTENT字段应有@Lob注解")
    void testClobFieldMapping() throws NoSuchFieldException {
        Field contentField = EmrContent.class.getDeclaredField("content");
        
        // 验证@Lob注解存在
        Lob lobAnnotation = contentField.getAnnotation(Lob.class);
        assertNotNull(lobAnnotation, "content字段应有@Lob注解表示CLOB类型");
        
        // 验证字段类型为String
        assertEquals(String.class, contentField.getType(), "content字段类型应为String");
    }

    @Test
    @DisplayName("CONTENT字段应映射到CONTENT列")
    void testContentColumnMapping() throws NoSuchFieldException {
        Field contentField = EmrContent.class.getDeclaredField("content");
        Column column = contentField.getAnnotation(Column.class);
        assertNotNull(column, "content字段应有@Column注解");
        assertEquals("CONTENT", column.name(), "content字段的列名应为CONTENT");
    }

    // ==================== 实体注解测试 ====================

    @Test
    @DisplayName("类应有@Entity注解")
    void testEntityAnnotation() {
        Entity entityAnnotation = EmrContent.class.getAnnotation(Entity.class);
        assertNotNull(entityAnnotation, "EmrContent类应有@Entity注解");
    }

    @Test
    @DisplayName("类应映射到EMR_CONTENT表")
    void testTableAnnotation() {
        Table tableAnnotation = EmrContent.class.getAnnotation(Table.class);
        assertNotNull(tableAnnotation, "EmrContent类应有@Table注解");
        assertEquals("EMR_CONTENT", tableAnnotation.name(), "表名应为EMR_CONTENT");
    }

    // ==================== Getter/Setter测试 ====================

    @Test
    @DisplayName("所有字段的Getter和Setter应正常工作")
    void testGettersAndSetters() {
        EmrContent entity = new EmrContent();
        
        // 测试ID
        entity.setId(1L);
        assertEquals(1L, entity.getId());
        
        // 测试字符串字段
        entity.setPatientId("patient-001");
        assertEquals("patient-001", entity.getPatientId());
        
        entity.setPatiId("pati-001");
        assertEquals("pati-001", entity.getPatiId());
        
        entity.setPatiName("张三");
        assertEquals("张三", entity.getPatiName());
        
        entity.setDeptCode("DEPT001");
        assertEquals("DEPT001", entity.getDeptCode());
        
        entity.setDeptName("心内科");
        assertEquals("心内科", entity.getDeptName());
        
        entity.setDocTypeName("入院记录");
        assertEquals("入院记录", entity.getDocTypeName());
        
        entity.setContent("病历内容CLOB");
        assertEquals("病历内容CLOB", entity.getContent());
        
        entity.setCreateUserId("user001");
        assertEquals("user001", entity.getCreateUserId());
        
        entity.setCreateBy("医生A");
        assertEquals("医生A", entity.getCreateBy());
        
        // 测试去重字段
        entity.setSourceTable("emr.emr_content");
        assertEquals("emr.emr_content", entity.getSourceTable());
        
        entity.setSourceId("source-001");
        assertEquals("source-001", entity.getSourceId());
        
        // 测试整数字段
        entity.setVisitId(1);
        assertEquals(1, entity.getVisitId());
        
        entity.setDeleteMark(0);
        assertEquals(0, entity.getDeleteMark());
        
        // 测试时间字段
        Timestamp now = new Timestamp(System.currentTimeMillis());
        entity.setRecordDate(now);
        assertEquals(now, entity.getRecordDate());
        
        entity.setDocTitleTime(now);
        assertEquals(now, entity.getDocTitleTime());
        
        entity.setModifiedOn(now);
        assertEquals(now, entity.getModifiedOn());
    }

    @Test
    @DisplayName("字段应支持null值")
    void testNullValueSupport() {
        EmrContent entity = new EmrContent();
        
        // 所有字段默认应为null
        assertNull(entity.getId());
        assertNull(entity.getPatientId());
        assertNull(entity.getPatiId());
        assertNull(entity.getVisitId());
        assertNull(entity.getPatiName());
        assertNull(entity.getDeptCode());
        assertNull(entity.getDeptName());
        assertNull(entity.getDocTypeName());
        assertNull(entity.getRecordDate());
        assertNull(entity.getContent());
        assertNull(entity.getCreateUserId());
        assertNull(entity.getCreateBy());
        assertNull(entity.getDocTitleTime());
        assertNull(entity.getModifiedOn());
        assertNull(entity.getDeleteMark());
        assertNull(entity.getSourceTable());
        assertNull(entity.getSourceId());
    }

    @Test
    @DisplayName("实体类应能正确创建实例")
    void testEntityCreation() {
        EmrContent entity = new EmrContent();
        assertNotNull(entity, "EmrContent实例应能正确创建");
    }

    // ==================== toString测试 ====================

    @Test
    @DisplayName("toString方法应返回非空字符串")
    void testToStringNotNull() {
        EmrContent entity = new EmrContent();
        String result = entity.toString();
        assertNotNull(result, "toString()方法应返回非空字符串");
    }

    @Test
    @DisplayName("toString方法应包含类名信息")
    void testToStringContainsClassName() {
        EmrContent entity = new EmrContent();
        entity.setId(1L);
        entity.setPatientId("patient-001");
        entity.setSourceTable("emr.emr_content");
        entity.setSourceId("source-001");
        
        String result = entity.toString();
        // 验证toString包含类相关信息（即使是默认实现也会包含类名）
        assertTrue(result.contains("EmrContent") || result.contains("@"), 
            "toString()应包含类名或对象标识信息");
    }
}
