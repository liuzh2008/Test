package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 患者过滤策略工厂测试
 * 测试PatientFilterStrategyFactory的功能和行为
 */
@ExtendWith(MockitoExtension.class)
public class PatientFilterStrategyFactoryTest {

    /**
     * 测试策略工厂的基本功能
     * 验证工厂能正确创建和管理策略实例
     */
    @Test
    void testStrategyFactoryBasicFunctions() {
        // 创建所有策略实例
        NoFilterStrategy noFilterStrategy = new NoFilterStrategy();
        DepartmentOnlyFilterStrategy departmentOnlyFilterStrategy = new DepartmentOnlyFilterStrategy();

        // 创建策略工厂
        PatientFilterStrategyFactory factory = new PatientFilterStrategyFactory(
                List.of(noFilterStrategy, departmentOnlyFilterStrategy)
        );

        // 测试获取无过滤策略
        PatientFilterStrategy strategy1 = factory.getStrategy(SchedulingProperties.TimerConfig.FilterMode.NONE);
        assertEquals(NoFilterStrategy.class, strategy1.getClass());
        assertEquals("无过滤", strategy1.getStrategyName());

        // 测试获取仅科室过滤策略
        PatientFilterStrategy strategy2 = factory.getStrategy(SchedulingProperties.TimerConfig.FilterMode.DEPARTMENT_ONLY);
        assertEquals(DepartmentOnlyFilterStrategy.class, strategy2.getClass());
        assertEquals("仅科室过滤", strategy2.getStrategyName());
    }

    /**
     * 测试策略工厂处理null过滤模式
     * 验证当传入null过滤模式时，工厂会抛出异常
     */
    @Test
    void testStrategyFactoryWithNullFilterMode() {
        // 创建所有策略实例
        NoFilterStrategy noFilterStrategy = new NoFilterStrategy();
        DepartmentOnlyFilterStrategy departmentOnlyFilterStrategy = new DepartmentOnlyFilterStrategy();

        // 创建策略工厂
        PatientFilterStrategyFactory factory = new PatientFilterStrategyFactory(
                List.of(noFilterStrategy, departmentOnlyFilterStrategy)
        );

        // 测试获取null策略，应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getStrategy(null);
        });
    }

    /**
     * 测试策略工厂处理空策略列表
     * 验证当策略列表为空时，工厂会抛出异常
     */
    @Test
    void testStrategyFactoryWithEmptyStrategyList() {
        // 创建一个空策略列表的工厂
        PatientFilterStrategyFactory factory = new PatientFilterStrategyFactory(List.of());

        // 测试获取策略，应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getStrategy(SchedulingProperties.TimerConfig.FilterMode.NONE);
        });
    }

    /**
     * 测试策略工厂处理重复策略
     * 验证当存在多个适用策略时，工厂会返回第一个匹配的策略
     */
    @Test
    void testStrategyFactoryWithDuplicateStrategies() {
        // 创建两个适用相同过滤模式的策略
        PatientFilterStrategy duplicateStrategy1 = new TestPatientFilterStrategy();
        PatientFilterStrategy duplicateStrategy2 = new TestPatientFilterStrategy();

        // 创建策略工厂
        PatientFilterStrategyFactory factory = new PatientFilterStrategyFactory(
                List.of(duplicateStrategy1, duplicateStrategy2)
        );

        // 测试获取策略，应该返回第一个匹配的策略
        PatientFilterStrategy strategy = factory.getStrategy(SchedulingProperties.TimerConfig.FilterMode.NONE);
        assertEquals(duplicateStrategy1, strategy);
    }

    /**
     * 测试策略实现类
     * 用于测试PatientFilterStrategy接口的功能
     */
    private static class TestPatientFilterStrategy implements PatientFilterStrategy {

        @Override
        public String getStrategyName() {
            return "Test Strategy";
        }

        @Override
        public boolean isApplicable(SchedulingProperties.TimerConfig.FilterMode filterMode) {
            return filterMode == SchedulingProperties.TimerConfig.FilterMode.NONE;
        }

        @Override
        public java.util.List<com.example.medaiassistant.model.Patient> filterPatients(
                com.example.medaiassistant.repository.PatientRepository repository,
                com.example.medaiassistant.config.SchedulingProperties.TimerConfig config,
                org.springframework.data.domain.Pageable pageable) {
            return java.util.List.of();
        }
    }
}
