package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 科室过滤配置验证器测试类
 * 
 * ✅ P2修订：已限定classes和禁用无关组件
 * 
 * @version 1.1
 * @since 2025-11-07
 */
@SpringBootTest(
    classes = DepartmentFilterConfigValidator.class,
    properties = {
        // 禁用无关组件
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        "monitoring.metrics.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
class DepartmentFilterConfigValidatorTest {

    @Autowired
    private DepartmentFilterConfigValidator validator;

    private List<String> validDepartments;
    private List<String> emptyDepartments;

    @BeforeEach
    void setUp() {
        validDepartments = Arrays.asList("心血管一病区", "心血管二病区", "神经内科");
        emptyDepartments = Collections.emptyList();
    }

    /**
     * 测试有效的科室过滤配置 - 启用过滤且有目标科室
     */
    @Test
    void testValidDepartmentFilterConfigEnabledWithTargets() {
        // 给定：启用了科室过滤且有目标科室
        boolean departmentFilterEnabled = true;
        List<String> targetDepartments = validDepartments;

        // 当：验证配置
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentFilterConfig(departmentFilterEnabled, targetDepartments);

        // 那么：配置应该有效
        assertTrue(result.isValid());
        assertTrue(result.getMessage().contains("科室过滤配置有效"));
        assertTrue(result.getMessage().contains("心血管一病区"));
    }

    /**
     * 测试有效的科室过滤配置 - 禁用过滤
     */
    @Test
    void testValidDepartmentFilterConfigDisabled() {
        // 给定：禁用了科室过滤
        boolean departmentFilterEnabled = false;
        List<String> targetDepartments = validDepartments;

        // 当：验证配置
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentFilterConfig(departmentFilterEnabled, targetDepartments);

        // 那么：配置应该有效
        assertTrue(result.isValid());
        assertTrue(result.getMessage().contains("科室过滤已禁用"));
    }

    /**
     * 测试无效的科室过滤配置 - 启用过滤但无目标科室
     */
    @Test
    void testInvalidDepartmentFilterConfigEnabledWithoutTargets() {
        // 给定：启用了科室过滤但无目标科室
        boolean departmentFilterEnabled = true;
        List<String> targetDepartments = emptyDepartments;

        // 当：验证配置
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentFilterConfig(departmentFilterEnabled, targetDepartments);

        // 那么：配置应该无效
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("目标科室列表为空"));
    }

    /**
     * 测试无效的科室过滤配置 - 启用过滤但目标科室为null
     */
    @Test
    void testInvalidDepartmentFilterConfigEnabledWithNullTargets() {
        // 给定：启用了科室过滤但目标科室为null
        boolean departmentFilterEnabled = true;
        List<String> targetDepartments = null;

        // 当：验证配置
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentFilterConfig(departmentFilterEnabled, targetDepartments);

        // 那么：配置应该无效
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("目标科室列表为空"));
    }

    /**
     * 测试科室是否在目标列表中 - 存在的情况
     */
    @Test
    void testIsDepartmentInTargetListExists() {
        // 给定：目标科室列表和存在的科室
        String department = "心血管一病区";

        // 当：检查科室是否在目标列表中
        boolean exists = validator.isDepartmentInTargetList(validDepartments, department);

        // 那么：应该返回true
        assertTrue(exists);
    }

    /**
     * 测试科室是否在目标列表中 - 不存在的情况
     */
    @Test
    void testIsDepartmentInTargetListNotExists() {
        // 给定：目标科室列表和不存在的科室
        String department = "骨科";

        // 当：检查科室是否在目标列表中
        boolean exists = validator.isDepartmentInTargetList(validDepartments, department);

        // 那么：应该返回false
        assertFalse(exists);
    }

    /**
     * 测试科室是否在目标列表中 - 空列表的情况
     */
    @Test
    void testIsDepartmentInTargetListEmptyList() {
        // 给定：空的目标科室列表
        String department = "心血管一病区";

        // 当：检查科室是否在目标列表中
        boolean exists = validator.isDepartmentInTargetList(emptyDepartments, department);

        // 那么：应该返回false
        assertFalse(exists);
    }

    /**
     * 测试科室是否在目标列表中 - null列表的情况
     */
    @Test
    void testIsDepartmentInTargetListNullList() {
        // 给定：null的目标科室列表
        String department = "心血管一病区";

        // 当：检查科室是否在目标列表中
        boolean exists = validator.isDepartmentInTargetList(null, department);

        // 那么：应该返回false
        assertFalse(exists);
    }

    /**
     * 测试科室是否在目标列表中 - null科室的情况
     */
    @Test
    void testIsDepartmentInTargetListNullDepartment() {
        // 给定：目标科室列表和null科室
        String department = null;

        // 当：检查科室是否在目标列表中
        boolean exists = validator.isDepartmentInTargetList(validDepartments, department);

        // 那么：应该返回false
        assertFalse(exists);
    }

    /**
     * 测试科室名称验证 - 有效名称
     */
    @Test
    void testValidateDepartmentNameValid() {
        // 给定：有效的科室名称
        String department = "心血管一病区";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该通过
        assertTrue(result.isValid());
        assertEquals("科室名称验证通过", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 空名称
     */
    @Test
    void testValidateDepartmentNameEmpty() {
        // 给定：空的科室名称
        String department = "";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称不能为空", result.getMessage());
    }

    /**
     * 测试科室名称验证 - null名称
     */
    @Test
    void testValidateDepartmentNameNull() {
        // 给定：null的科室名称
        String department = null;

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称不能为空", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 名称过长
     */
    @Test
    void testValidateDepartmentNameTooLong() {
        // 给定：过长的科室名称
        String department = "这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的科室名称";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称长度不能超过40个字符", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 包含非法字符
     */
    @Test
    void testValidateDepartmentNameWithInvalidCharacters() {
        // 给定：包含非法字符的科室名称
        String department = "心血管;一病区";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称包含非法字符", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 包含管道符
     */
    @Test
    void testValidateDepartmentNameWithPipeCharacter() {
        // 给定：包含管道符的科室名称
        String department = "心血管|一病区";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称包含非法字符", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 包含引号
     */
    @Test
    void testValidateDepartmentNameWithQuoteCharacter() {
        // 给定：包含引号的科室名称
        String department = "心血管\"一病区";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该失败
        assertFalse(result.isValid());
        assertEquals("科室名称包含非法字符", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 名称带空格
     */
    @Test
    void testValidateDepartmentNameWithSpaces() {
        // 给定：带空格的科室名称
        String department = "心血管 一病区";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该通过
        assertTrue(result.isValid());
        assertEquals("科室名称验证通过", result.getMessage());
    }

    /**
     * 测试科室名称验证 - 名称带前后空格
     */
    @Test
    void testValidateDepartmentNameWithLeadingTrailingSpaces() {
        // 给定：带前后空格的科室名称
        String department = "  心血管一病区  ";

        // 当：验证科室名称
        DepartmentFilterConfigValidator.ValidationResult result = 
            validator.validateDepartmentName(department);

        // 那么：验证应该通过
        assertTrue(result.isValid());
        assertEquals("科室名称验证通过", result.getMessage());
    }
}
