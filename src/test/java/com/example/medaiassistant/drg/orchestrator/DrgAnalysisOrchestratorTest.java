package com.example.medaiassistant.drg.orchestrator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DRGs流程编排器测试
 * 按照TDD红-绿-重构流程实现故事2：流程编排器
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
@DisplayName("DRGs流程编排器测试")
class DrgAnalysisOrchestratorTest {

    private DrgFlowProcessorRegistry registry;
    private DrgAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        registry = new DrgFlowProcessorRegistry();
        orchestrator = new DrgAnalysisOrchestrator(registry);
    }

    @Test
    @DisplayName("应该按配置顺序执行处理器")
    void shouldExecuteProcessorsInConfiguredOrder() {
        // 给定：配置的处理顺序
        TestProcessor processor1 = new TestProcessor("processor-1", 1);
        TestProcessor processor2 = new TestProcessor("processor-2", 2);
        TestProcessor processor3 = new TestProcessor("processor-3", 3);
        
        registry.register(processor1);
        registry.register(processor2);
        registry.register(processor3);
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：处理器应该按配置顺序执行
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        assertEquals(3, executionOrder.size(), "应该执行3个处理器");
        assertEquals("processor-1", executionOrder.get(0), "第一个执行的应该是processor-1");
        assertEquals("processor-2", executionOrder.get(1), "第二个执行的应该是processor-2");
        assertEquals("processor-3", executionOrder.get(2), "第三个执行的应该是processor-3");
    }

    @Test
    @DisplayName("应该跳过禁用的处理器")
    void shouldSkipDisabledProcessors() {
        // 给定：包含禁用处理器的配置
        TestProcessor enabledProcessor = new TestProcessor("enabled-processor", 1);
        TestProcessor disabledProcessor = new TestProcessor("disabled-processor", 2);
        disabledProcessor.setEnabled(false);
        
        registry.register(enabledProcessor);
        registry.register(disabledProcessor);
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该跳过禁用的处理器
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        assertEquals(1, executionOrder.size(), "应该只执行1个启用的处理器");
        assertEquals("enabled-processor", executionOrder.get(0), "应该只执行启用的处理器");
        assertFalse(executionOrder.contains("disabled-processor"), "不应该执行禁用的处理器");
    }

    @Test
    @DisplayName("应该在处理器间正确传递上下文")
    void shouldPassContextBetweenProcessors() {
        // 给定：多个处理器
        TestProcessor processor1 = new TestProcessor("processor-1", 1);
        TestProcessor processor2 = new TestProcessor("processor-2", 2);
        
        registry.register(processor1);
        registry.register(processor2);
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：上下文应该在处理器间正确传递
        assertTrue(context.containsAttribute("processor-1-executed"), "processor-1应该设置属性");
        assertTrue(context.containsAttribute("processor-2-executed"), "processor-2应该设置属性");
        assertEquals("value-from-processor-1", context.getAttribute("shared-data"), "应该传递共享数据");
    }

    @Test
    @DisplayName("应该处理处理器执行异常")
    void shouldHandleProcessorExecutionException() {
        // 给定：包含会抛出异常的处理器
        TestProcessor normalProcessor = new TestProcessor("normal-processor", 1);
        TestProcessor failingProcessor = new TestProcessor("failing-processor", 2);
        failingProcessor.setShouldFail(true);
        TestProcessor subsequentProcessor = new TestProcessor("subsequent-processor", 3);
        
        registry.register(normalProcessor);
        registry.register(failingProcessor);
        registry.register(subsequentProcessor);
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该记录错误信息并跳过后续处理器
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        assertEquals(1, executionOrder.size(), "应该只执行异常处理器前的处理器");
        assertTrue(executionOrder.contains("normal-processor"), "应该执行正常处理器");
        assertFalse(executionOrder.contains("failing-processor"), "不应该执行异常处理器");
        assertFalse(executionOrder.contains("subsequent-processor"), "不应该执行异常后的处理器");
        
        assertTrue(context.containsAttribute("error"), "应该记录错误信息");
    }

    @Test
    @DisplayName("应该支持二段式编排")
    void shouldSupportTwoStageOrchestration() {
        // 给定：二段式编排配置
        TestProcessor decisionStageProcessor = new TestProcessor("decision-processor", 1);
        TestProcessor profitLossStageProcessor = new TestProcessor("profit-loss-processor", 2);
        
        registry.register(decisionStageProcessor);
        registry.register(profitLossStageProcessor);
        
        // 当：执行二段式编排
        DrgFlowContext context = new DrgFlowContext();
        // 设置必要的上下文属性以通过结果就绪检查
        context.setAttribute("promptSaved", true);
        context.setAttribute("aiResultReady", true);
        orchestrator.orchestrateTwoStage(context);
        
        // 则：应该先执行决策阶段，再执行盈亏计算阶段
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        assertEquals(2, executionOrder.size(), "应该执行2个处理器");
        assertEquals("decision-processor", executionOrder.get(0), "应该先执行决策处理器");
        assertEquals("profit-loss-processor", executionOrder.get(1), "应该后执行盈亏计算处理器");
    }

    @Test
    @DisplayName("空注册表应该正常执行")
    void shouldHandleEmptyRegistry() {
        // 给定：空的注册表
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该正常执行，不抛出异常
        assertNotNull(context, "上下文应该存在");
        assertFalse(context.containsAttribute("error"), "不应该有错误信息");
    }

    @Test
    @DisplayName("应该记录执行时间")
    void shouldRecordExecutionTime() {
        // 给定：多个处理器
        TestProcessor processor1 = new TestProcessor("processor-1", 1);
        TestProcessor processor2 = new TestProcessor("processor-2", 2);
        
        registry.register(processor1);
        registry.register(processor2);
        
        // 当：执行编排并记录时间
        long startTime = System.currentTimeMillis();
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        long endTime = System.currentTimeMillis();
        
        // 则：执行时间应该在合理范围内
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "执行时间应小于1秒: " + executionTime + "ms");
        
        // 并且：处理器应该正常执行
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        assertEquals(2, executionOrder.size(), "应该执行2个处理器");
    }

    @Test
    @DisplayName("应该处理所有处理器都禁用的场景")
    void shouldHandleAllProcessorsDisabled() {
        // 给定：所有处理器都禁用
        TestProcessor disabledProcessor1 = new TestProcessor("disabled-1", 1);
        TestProcessor disabledProcessor2 = new TestProcessor("disabled-2", 2);
        disabledProcessor1.setEnabled(false);
        disabledProcessor2.setEnabled(false);
        
        registry.register(disabledProcessor1);
        registry.register(disabledProcessor2);
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该正常执行，不执行任何处理器
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        if (executionOrder != null) {
            assertTrue(executionOrder.isEmpty(), "不应该执行任何处理器");
        }
        assertFalse(context.containsAttribute("error"), "不应该有错误信息");
    }

    /**
     * 测试处理器实现
     */
    private static class TestProcessor implements DrgFlowProcessor {
        private final String name;
        private final int order;
        private boolean enabled = true;
        private boolean shouldFail = false;

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
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void process(DrgFlowContext context) {
            if (shouldFail) {
                throw new RuntimeException("Processor execution failed: " + name);
            }
            
            // 记录执行顺序
            @SuppressWarnings("unchecked")
            List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
            if (executionOrder == null) {
                executionOrder = new java.util.ArrayList<>();
                context.setAttribute("executionOrder", executionOrder);
            }
            executionOrder.add(name);
            
            // 设置处理器执行标记
            context.setAttribute(name + "-executed", true);
            
            // 传递共享数据
            if (name.equals("processor-1")) {
                context.setAttribute("shared-data", "value-from-processor-1");
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
    }
}
