package com.example.medaiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchedulingProperties科室过滤配置测试类
 * 按照TDD方法实现配置层开发
 */
@SpringBootTest(
    classes = SchedulingProperties.class,
    properties = {
        "scheduling.timer.department-filter-enabled=true",
        "scheduling.timer.target-departments=心血管一病区,心血管二病区",
        "scheduling.timer.department-filter-default-enabled=false",
        // 禁用不必要的组件
        "spring.main.web-application-type=none",
        "spring.task.scheduling.enabled=false",
        "scheduling.auto-execute.enabled=false",
        "prompt.submission.enabled=false",
        "prompt.polling.enabled=false",
        "monitoring.metrics.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none"
    }
)
@EnableConfigurationProperties(SchedulingProperties.class)
class SchedulingPropertiesDepartmentFilterTest {

    private SchedulingProperties schedulingProperties;

    @BeforeEach
    void setUp() {
        schedulingProperties = new SchedulingProperties();
    }

    /**
     * 测试目标科室列表配置正确加载
     */
    @Test
    void testTargetDepartmentsConfiguration() {
        // 给定：配置了目标科室列表
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setTargetDepartments(Arrays.asList("心血管一病区", "心血管二病区"));
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：目标科室列表应该正确加载
        List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
        assertNotNull(targetDepartments);
        assertEquals(2, targetDepartments.size());
        assertTrue(targetDepartments.contains("心血管一病区"));
        assertTrue(targetDepartments.contains("心血管二病区"));
    }

    /**
     * 测试科室过滤开关配置
     */
    @Test
    void testDepartmentFilterEnabledConfiguration() {
        // 给定：启用了科室过滤
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDepartmentFilterEnabled(true);
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：科室过滤应该启用
        assertTrue(schedulingProperties.getTimer().isDepartmentFilterEnabled());
    }

    /**
     * 测试科室过滤默认值
     */
    @Test
    void testDepartmentFilterDefaultValue() {
        // 给定：使用默认配置
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        
        // 当：创建新的定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：科室过滤应该默认禁用
        assertFalse(schedulingProperties.getTimer().isDepartmentFilterEnabled());
    }

    /**
     * 测试空科室列表处理
     */
    @Test
    void testEmptyTargetDepartments() {
        // 给定：空的目标科室列表
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setTargetDepartments(Arrays.asList());
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：目标科室列表应该为空
        List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
        assertNotNull(targetDepartments);
        assertTrue(targetDepartments.isEmpty());
    }

    /**
     * 测试单个科室配置
     */
    @Test
    void testSingleDepartmentConfiguration() {
        // 给定：配置单个目标科室
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setTargetDepartments(Arrays.asList("心血管一病区"));
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：目标科室列表应该包含单个科室
        List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
        assertEquals(1, targetDepartments.size());
        assertEquals("心血管一病区", targetDepartments.get(0));
    }

    /**
     * 测试科室名称包含空格的处理
     */
    @Test
    void testDepartmentNameWithSpaces() {
        // 给定：科室名称包含空格
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setTargetDepartments(Arrays.asList("心血管 一病区", "心血管 二病区"));
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：科室名称应该正确保存
        List<String> targetDepartments = schedulingProperties.getTimer().getTargetDepartments();
        assertTrue(targetDepartments.contains("心血管 一病区"));
        assertTrue(targetDepartments.contains("心血管 二病区"));
    }

    /**
     * 测试配置验证逻辑 - 启用过滤但无目标科室
     */
    @Test
    void testValidationEnabledFilterButNoTargetDepartments() {
        // 给定：启用了科室过滤但没有配置目标科室
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDepartmentFilterEnabled(true);
        timerConfig.setTargetDepartments(Arrays.asList());
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：配置应该有效，但实际过滤时可能没有效果
        assertTrue(schedulingProperties.getTimer().isDepartmentFilterEnabled());
        assertTrue(schedulingProperties.getTimer().getTargetDepartments().isEmpty());
    }

    /**
     * 测试配置验证逻辑 - 禁用过滤但有目标科室
     */
    @Test
    void testValidationDisabledFilterWithTargetDepartments() {
        // 给定：禁用了科室过滤但配置了目标科室
        SchedulingProperties.TimerConfig timerConfig = new SchedulingProperties.TimerConfig();
        timerConfig.setDepartmentFilterEnabled(false);
        timerConfig.setTargetDepartments(Arrays.asList("心血管一病区", "心血管二病区"));
        
        // 当：设置定时任务配置
        schedulingProperties.setTimer(timerConfig);
        
        // 那么：配置应该有效，但过滤功能不会启用
        assertFalse(schedulingProperties.getTimer().isDepartmentFilterEnabled());
        assertEquals(2, schedulingProperties.getTimer().getTargetDepartments().size());
    }
}
