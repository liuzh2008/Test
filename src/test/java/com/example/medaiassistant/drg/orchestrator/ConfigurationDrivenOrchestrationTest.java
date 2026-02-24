package com.example.medaiassistant.drg.orchestrator;

import com.example.medaiassistant.drg.config.DrgFlowConfiguration;
import com.example.medaiassistant.drg.config.ProcessorConfiguration;
import com.example.medaiassistant.drg.config.DrgFlowConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置驱动编排测试
 * 测试配置管理与编排器的集成
 * 
 * 测试评价：
 * - 验证配置驱动编排的核心功能
 * - 测试配置变更对编排行为的影响
 * - 确保配置与编排器的正确集成
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
@DisplayName("配置驱动编排测试")
class ConfigurationDrivenOrchestrationTest {

    private DrgFlowConfigLoader configLoader;
    private DrgFlowProcessorRegistry registry;
    private DrgAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        configLoader = new DrgFlowConfigLoader();
        registry = new DrgFlowProcessorRegistry();
        orchestrator = new DrgAnalysisOrchestrator(registry);
    }

    @Test
    @DisplayName("应该基于配置执行编排")
    void shouldExecuteBasedOnConfiguration() {
        // 给定：完整的流程配置
        String configPath = "config/drg-flow-config.yml";
        DrgFlowConfiguration config = configLoader.loadConfiguration(configPath);
        
        // 注册配置中启用的处理器
        config.getEnabledProcessors().forEach(processorConfig -> {
            TestProcessor processor = new TestProcessor(
                processorConfig.getName(), 
                processorConfig.getOrder(), 
                processorConfig.isEnabled()
            );
            registry.register(processor);
        });
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该严格按照配置执行
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        
        // 验证执行顺序与配置一致
        List<ProcessorConfiguration> enabledProcessors = config.getEnabledProcessors();
        assertEquals(enabledProcessors.size(), executionOrder.size(), "应该执行所有启用的处理器");
        
        for (int i = 0; i < enabledProcessors.size(); i++) {
            assertEquals(enabledProcessors.get(i).getName(), executionOrder.get(i), 
                "执行顺序应该与配置一致");
        }
    }

    @Test
    @DisplayName("应该跳过配置中禁用的处理器")
    void shouldSkipDisabledProcessorsInConfiguration() {
        // 给定：包含禁用处理器的配置
        String configPath = "config/drg-flow-config.yml";
        DrgFlowConfiguration config = configLoader.loadConfiguration(configPath);
        
        // 手动修改配置，禁用一些处理器
        config.getProcessors().forEach(processor -> {
            if (processor.getName().equals("mcc-screening")) {
                processor.setEnabled(false);
            }
        });
        
        // 注册所有处理器（包括禁用的）
        config.getProcessors().forEach(processorConfig -> {
            TestProcessor processor = new TestProcessor(
                processorConfig.getName(), 
                processorConfig.getOrder(), 
                processorConfig.isEnabled()
            );
            registry.register(processor);
        });
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：应该跳过禁用的处理器
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        
        // 验证禁用的处理器没有被执行
        assertFalse(executionOrder.contains("mcc-screening"), "不应该执行禁用的处理器");
        
        // 验证启用的处理器正常执行
        assertTrue(executionOrder.contains("data-validation"), "应该执行启用的处理器");
        assertTrue(executionOrder.contains("drg-calculation"), "应该执行启用的处理器");
    }

    @Test
    @DisplayName("应该按配置顺序执行处理器")
    void shouldExecuteProcessorsInConfiguredOrder() {
        // 给定：配置的处理顺序
        String configPath = "config/drg-flow-config.yml";
        DrgFlowConfiguration config = configLoader.loadConfiguration(configPath);
        
        // 注册处理器
        config.getEnabledProcessors().forEach(processorConfig -> {
            TestProcessor processor = new TestProcessor(
                processorConfig.getName(), 
                processorConfig.getOrder(), 
                processorConfig.isEnabled()
            );
            registry.register(processor);
        });
        
        // 当：执行编排
        DrgFlowContext context = new DrgFlowContext();
        orchestrator.orchestrate(context);
        
        // 则：处理器应该按配置顺序执行
        @SuppressWarnings("unchecked")
        List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
        assertNotNull(executionOrder, "应该记录执行顺序");
        
        // 验证执行顺序
        assertEquals("data-validation", executionOrder.get(0), "第一个执行的应该是data-validation");
        assertEquals("mcc-screening", executionOrder.get(1), "第二个执行的应该是mcc-screening");
        assertEquals("drg-calculation", executionOrder.get(2), "第三个执行的应该是drg-calculation");
    }

    /**
     * 测试处理器实现
     */
    private static class TestProcessor implements DrgFlowProcessor {
        private final String name;
        private final int order;
        private final boolean enabled;

        public TestProcessor(String name, int order, boolean enabled) {
            this.name = name;
            this.order = order;
            this.enabled = enabled;
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
            // 记录执行顺序
            @SuppressWarnings("unchecked")
            List<String> executionOrder = (List<String>) context.getAttribute("executionOrder");
            if (executionOrder == null) {
                executionOrder = new java.util.ArrayList<>();
                context.setAttribute("executionOrder", executionOrder);
            }
            executionOrder.add(name);
        }
    }
}
