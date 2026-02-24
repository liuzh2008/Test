package com.example.medaiassistant.drg.orchestrator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DRGs流程处理器注册表测试
 * 按照TDD红-绿-重构流程实现故事1：处理器注册表管理
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-11
 */
@DisplayName("DRGs流程处理器注册表测试")
class DrgFlowProcessorRegistryTest {

    private DrgFlowProcessorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DrgFlowProcessorRegistry();
    }

    @Test
    @DisplayName("应该成功注册处理器")
    void shouldRegisterProcessorSuccessfully() {
        // 给定：处理器注册表
        DrgFlowProcessor processor = new TestProcessor("test-processor", 1);
        
        // 当：注册新的处理器
        registry.register(processor);
        
        // 则：处理器应该被成功注册
        DrgFlowProcessor retrieved = registry.getProcessor("test-processor");
        assertNotNull(retrieved, "应该能够获取已注册的处理器");
        assertEquals("test-processor", retrieved.getName(), "处理器名称应该匹配");
    }

    @Test
    @DisplayName("应该按名称获取处理器")
    void shouldGetProcessorByName() {
        // 给定：已注册的处理器
        DrgFlowProcessor processor = new TestProcessor("data-processor", 2);
        registry.register(processor);
        
        // 当：按名称获取处理器
        DrgFlowProcessor retrieved = registry.getProcessor("data-processor");
        
        // 则：应该返回对应的处理器
        assertNotNull(retrieved, "应该返回已注册的处理器");
        assertEquals("data-processor", retrieved.getName(), "处理器名称应该匹配");
        assertEquals(2, retrieved.getOrder(), "处理器顺序应该匹配");
    }

    @Test
    @DisplayName("应该按执行顺序返回所有处理器")
    void shouldReturnAllProcessorsInOrder() {
        // 给定：多个不同顺序的处理器
        DrgFlowProcessor processor1 = new TestProcessor("processor-1", 3);
        DrgFlowProcessor processor2 = new TestProcessor("processor-2", 1);
        DrgFlowProcessor processor3 = new TestProcessor("processor-3", 2);
        
        registry.register(processor1);
        registry.register(processor2);
        registry.register(processor3);
        
        // 当：获取所有处理器
        List<DrgFlowProcessor> processors = registry.getAllProcessors();
        
        // 则：应该按执行顺序返回
        assertNotNull(processors, "应该返回处理器列表");
        assertEquals(3, processors.size(), "应该返回所有处理器");
        assertEquals("processor-2", processors.get(0).getName(), "第一个处理器应该是顺序最小的");
        assertEquals("processor-3", processors.get(1).getName(), "第二个处理器应该是顺序中间的");
        assertEquals("processor-1", processors.get(2).getName(), "第三个处理器应该是顺序最大的");
    }

    @Test
    @DisplayName("注册同名处理器时应该抛出异常")
    void shouldThrowExceptionWhenRegisterDuplicateProcessor() {
        // 给定：已存在的处理器名称
        DrgFlowProcessor processor1 = new TestProcessor("duplicate-processor", 1);
        registry.register(processor1);
        
        // 当：注册同名处理器
        DrgFlowProcessor processor2 = new TestProcessor("duplicate-processor", 2);
        
        // 则：应该抛出异常
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> registry.register(processor2),
            "注册同名处理器应该抛出IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("duplicate-processor"), 
            "异常消息应该包含处理器名称");
    }

    @Test
    @DisplayName("获取不存在的处理器应该返回null")
    void shouldReturnNullWhenGettingNonExistentProcessor() {
        // 给定：空的注册表
        
        // 当：获取不存在的处理器
        DrgFlowProcessor processor = registry.getProcessor("non-existent");
        
        // 则：应该返回null
        assertNull(processor, "获取不存在的处理器应该返回null");
    }

    @Test
    @DisplayName("应该正确检查处理器是否存在")
    void shouldCheckProcessorExistenceCorrectly() {
        // 给定：已注册的处理器
        DrgFlowProcessor processor = new TestProcessor("check-processor", 1);
        registry.register(processor);
        
        // 当：检查处理器是否存在
        boolean exists = registry.containsProcessor("check-processor");
        boolean notExists = registry.containsProcessor("non-existent");
        
        // 则：应该正确返回存在状态
        assertTrue(exists, "应该返回处理器存在");
        assertFalse(notExists, "应该返回处理器不存在");
    }

    @Test
    @DisplayName("应该正确返回处理器数量")
    void shouldReturnCorrectProcessorCount() {
        // 给定：多个处理器
        registry.register(new TestProcessor("processor-1", 1));
        registry.register(new TestProcessor("processor-2", 2));
        
        // 当：获取处理器数量
        int count = registry.size();
        
        // 则：应该返回正确的数量
        assertEquals(2, count, "应该返回正确的处理器数量");
    }

    @Test
    @DisplayName("应该正确检查注册表是否为空")
    void shouldCheckIfRegistryIsEmpty() {
        // 给定：空的注册表
        
        // 当：检查是否为空
        boolean isEmpty = registry.isEmpty();
        
        // 则：应该返回true
        assertTrue(isEmpty, "空的注册表应该返回true");
        
        // 当：注册处理器后
        registry.register(new TestProcessor("processor", 1));
        
        // 则：应该返回false
        assertFalse(registry.isEmpty(), "非空注册表应该返回false");
    }

    @Test
    @DisplayName("应该成功移除处理器")
    void shouldRemoveProcessorSuccessfully() {
        // 给定：已注册的处理器
        DrgFlowProcessor processor = new TestProcessor("remove-processor", 1);
        registry.register(processor);
        
        // 当：移除处理器
        DrgFlowProcessor removed = registry.removeProcessor("remove-processor");
        
        // 则：应该返回被移除的处理器
        assertNotNull(removed, "应该返回被移除的处理器");
        assertEquals("remove-processor", removed.getName(), "处理器名称应该匹配");
        
        // 并且：处理器应该不再存在
        assertNull(registry.getProcessor("remove-processor"), "处理器应该不再存在");
        assertFalse(registry.containsProcessor("remove-processor"), "应该返回处理器不存在");
    }

    @Test
    @DisplayName("移除不存在的处理器应该返回null")
    void shouldReturnNullWhenRemovingNonExistentProcessor() {
        // 给定：空的注册表
        
        // 当：移除不存在的处理器
        DrgFlowProcessor removed = registry.removeProcessor("non-existent");
        
        // 则：应该返回null
        assertNull(removed, "移除不存在的处理器应该返回null");
    }

    @Test
    @DisplayName("应该成功清空所有处理器")
    void shouldClearAllProcessors() {
        // 给定：多个处理器
        registry.register(new TestProcessor("processor-1", 1));
        registry.register(new TestProcessor("processor-2", 2));
        registry.register(new TestProcessor("processor-3", 3));
        
        // 当：清空注册表
        registry.clear();
        
        // 则：注册表应该为空
        assertTrue(registry.isEmpty(), "注册表应该为空");
        assertEquals(0, registry.size(), "处理器数量应该为0");
        assertTrue(registry.getAllProcessors().isEmpty(), "处理器列表应该为空");
    }

    @Test
    @DisplayName("应该返回所有处理器名称")
    void shouldReturnAllProcessorNames() {
        // 给定：多个处理器
        registry.register(new TestProcessor("processor-1", 1));
        registry.register(new TestProcessor("processor-2", 2));
        
        // 当：获取所有处理器名称
        Set<String> names = registry.getProcessorNames();
        
        // 则：应该返回所有处理器名称
        assertNotNull(names, "应该返回处理器名称集合");
        assertEquals(2, names.size(), "应该返回2个处理器名称");
        assertTrue(names.contains("processor-1"), "应该包含processor-1");
        assertTrue(names.contains("processor-2"), "应该包含processor-2");
    }

    @Test
    @DisplayName("注册null处理器应该抛出异常")
    void shouldThrowExceptionWhenRegisteringNullProcessor() {
        // 给定：null处理器
        
        // 当：注册null处理器
        // 则：应该抛出异常
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> registry.register(null),
            "注册null处理器应该抛出IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("不能为null"), 
            "异常消息应该包含'不能为null'");
    }

    @Test
    @DisplayName("注册空名称处理器应该抛出异常")
    void shouldThrowExceptionWhenRegisteringEmptyNameProcessor() {
        // 给定：空名称处理器
        DrgFlowProcessor processor = new TestProcessor("", 1);
        
        // 当：注册空名称处理器
        // 则：应该抛出异常
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> registry.register(processor),
            "注册空名称处理器应该抛出IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("不能为空"), 
            "异常消息应该包含'不能为空'");
    }

    /**
     * 测试处理器实现
     */
    private static class TestProcessor implements DrgFlowProcessor {
        private final String name;
        private final int order;

        public TestProcessor(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void process(DrgFlowContext context) {
            // 测试实现，不做任何操作
        }
    }
}
