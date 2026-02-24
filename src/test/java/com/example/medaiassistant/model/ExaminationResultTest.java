package com.example.medaiassistant.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExaminationResult 实体类单元测试
 * 验证时间字段类型更新和updateDt字段添加
 */
@DisplayName("ExaminationResult 实体类测试")
class ExaminationResultTest {

    @Test
    @DisplayName("updateDt字段应该存在")
    void updateDtFieldShouldExist() throws NoSuchFieldException {
        Field field = ExaminationResult.class.getDeclaredField("updateDt");
        assertNotNull(field, "updateDt字段应该存在");
    }

    @Test
    @DisplayName("updateDt字段类型应为Timestamp")
    void updateDtFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = ExaminationResult.class.getDeclaredField("updateDt");
        assertEquals(Timestamp.class, field.getType(), "updateDt字段类型应为Timestamp");
    }

    @Test
    @DisplayName("checkIssueTime字段类型应为Timestamp")
    void checkIssueTimeFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = ExaminationResult.class.getDeclaredField("checkIssueTime");
        assertEquals(Timestamp.class, field.getType(), "checkIssueTime字段类型应为Timestamp");
    }

    @Test
    @DisplayName("checkExecuteTime字段类型应为Timestamp")
    void checkExecuteTimeFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = ExaminationResult.class.getDeclaredField("checkExecuteTime");
        assertEquals(Timestamp.class, field.getType(), "checkExecuteTime字段类型应为Timestamp");
    }

    @Test
    @DisplayName("checkReportTime字段类型应为Timestamp")
    void checkReportTimeFieldTypeShouldBeTimestamp() throws NoSuchFieldException {
        Field field = ExaminationResult.class.getDeclaredField("checkReportTime");
        assertEquals(Timestamp.class, field.getType(), "checkReportTime字段类型应为Timestamp");
    }

    @Test
    @DisplayName("updateDt的Getter方法应该存在")
    void getUpdateDtMethodShouldExist() throws NoSuchMethodException {
        Method getter = ExaminationResult.class.getMethod("getUpdateDt");
        assertNotNull(getter, "getUpdateDt方法应该存在");
        assertEquals(Timestamp.class, getter.getReturnType(), "getUpdateDt返回类型应为Timestamp");
    }

    @Test
    @DisplayName("updateDt的Setter方法应该存在")
    void setUpdateDtMethodShouldExist() throws NoSuchMethodException {
        Method setter = ExaminationResult.class.getMethod("setUpdateDt", Timestamp.class);
        assertNotNull(setter, "setUpdateDt方法应该存在");
    }

    @Test
    @DisplayName("updateDt的Getter和Setter应正常工作")
    void updateDtGetterSetterShouldWork() {
        ExaminationResult entity = new ExaminationResult();
        Timestamp testTime = new Timestamp(System.currentTimeMillis());
        
        entity.setUpdateDt(testTime);
        assertEquals(testTime, entity.getUpdateDt(), "updateDt的Getter/Setter应正常工作");
    }

    @Test
    @DisplayName("时间字段的Getter和Setter应正常工作")
    void timeFieldsGetterSetterShouldWork() {
        ExaminationResult entity = new ExaminationResult();
        Timestamp issueTime = new Timestamp(System.currentTimeMillis());
        Timestamp executeTime = new Timestamp(System.currentTimeMillis() + 1000);
        Timestamp reportTime = new Timestamp(System.currentTimeMillis() + 2000);
        
        entity.setCheckIssueTime(issueTime);
        entity.setCheckExecuteTime(executeTime);
        entity.setCheckReportTime(reportTime);
        
        assertEquals(issueTime, entity.getCheckIssueTime(), "checkIssueTime应正确设置和获取");
        assertEquals(executeTime, entity.getCheckExecuteTime(), "checkExecuteTime应正确设置和获取");
        assertEquals(reportTime, entity.getCheckReportTime(), "checkReportTime应正确设置和获取");
    }

    @Test
    @DisplayName("时间字段应支持null值")
    void timeFieldsShouldSupportNullValue() {
        ExaminationResult entity = new ExaminationResult();
        
        entity.setCheckIssueTime(null);
        entity.setCheckExecuteTime(null);
        entity.setCheckReportTime(null);
        entity.setUpdateDt(null);
        
        assertNull(entity.getCheckIssueTime(), "checkIssueTime应支持null值");
        assertNull(entity.getCheckExecuteTime(), "checkExecuteTime应支持null值");
        assertNull(entity.getCheckReportTime(), "checkReportTime应支持null值");
        assertNull(entity.getUpdateDt(), "updateDt应支持null值");
    }

    @Test
    @DisplayName("实体类应能正确创建实例")
    void entityShouldBeCreatable() {
        ExaminationResult entity = new ExaminationResult();
        assertNotNull(entity, "ExaminationResult实例应能正确创建");
    }
}
