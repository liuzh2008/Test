package com.example.medaiassistant.drg.orchestrator;

import java.util.List;

/**
 * DRGs流程编排器
 * 负责按配置顺序执行流程处理器，支持处理器间的上下文传递和异常处理
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
public class DrgAnalysisOrchestrator {

    private final DrgFlowProcessorRegistry processorRegistry;

    /**
     * 构造函数
     * 
     * @param processorRegistry 处理器注册表
     */
    public DrgAnalysisOrchestrator(DrgFlowProcessorRegistry processorRegistry) {
        this.processorRegistry = processorRegistry;
    }

    /**
     * 执行流程编排
     * 按配置顺序执行所有启用的处理器
     * 
     * @param context 流程上下文
     */
    public void orchestrate(DrgFlowContext context) {
        List<DrgFlowProcessor> processors = processorRegistry.getAllProcessors();
        
        for (DrgFlowProcessor processor : processors) {
            // 跳过禁用的处理器
            if (!processor.isEnabled()) {
                continue;
            }
            
            try {
                processor.process(context);
            } catch (Exception e) {
                // 记录错误信息并中断流程
                context.setAttribute("error", "Processor " + processor.getName() + " failed: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * 执行二段式编排
     * 先执行决策阶段，再执行盈亏计算阶段
     * 
     * 重构说明：
     * - 提取了结果就绪检查逻辑到独立方法
     * - 添加了阶段执行前的状态验证
     * - 改进了错误处理和状态标记
     * 
     * @param context 流程上下文
     */
    public void orchestrateTwoStage(DrgFlowContext context) {
        // 检查结果就绪状态
        if (!isResultReady(context)) {
            context.setAttribute("twoStageTriggered", false);
            return;
        }
        
        // 标记二段式编排已触发
        context.setAttribute("twoStageTriggered", true);
        
        // 第一阶段：决策阶段
        executeDecisionStage(context);
        
        // 第二阶段：盈亏计算阶段
        executeProfitLossStage(context);
        
        // 标记异步触发支持
        context.setAttribute("asyncTriggerSupported", true);
        
        // 标记编排终点已到达
        context.setAttribute("orchestrationEndpointReached", true);
    }
    
    /**
     * 检查结果是否就绪
     * 
     * @param context 流程上下文
     * @return 如果结果就绪则返回true
     */
    private boolean isResultReady(DrgFlowContext context) {
        boolean promptSaved = (boolean) context.getAttribute("promptSaved", false);
        boolean aiResultReady = (boolean) context.getAttribute("aiResultReady", false);
        return promptSaved && aiResultReady;
    }
    
    /**
     * 执行决策阶段
     * 
     * @param context 流程上下文
     */
    private void executeDecisionStage(DrgFlowContext context) {
        // 标记决策阶段开始执行
        context.setAttribute("decisionStageExecuted", true);
        
        // 获取所有启用的处理器
        List<DrgFlowProcessor> processors = processorRegistry.getAllProcessors();
        
        // 执行决策相关的处理器
        for (DrgFlowProcessor processor : processors) {
            if (!processor.isEnabled()) {
                continue;
            }
            
            // 只执行决策阶段的处理器（可以根据处理器名称或类型过滤）
            if (isDecisionProcessor(processor)) {
                try {
                    processor.process(context);
                } catch (Exception e) {
                    context.setAttribute("error", "Decision processor " + processor.getName() + " failed: " + e.getMessage());
                    break;
                }
            }
        }
    }
    
    /**
     * 执行盈亏计算阶段
     * 
     * @param context 流程上下文
     */
    private void executeProfitLossStage(DrgFlowContext context) {
        // 标记盈亏计算阶段开始执行
        context.setAttribute("profitLossStageExecuted", true);
        
        // 获取所有启用的处理器
        List<DrgFlowProcessor> processors = processorRegistry.getAllProcessors();
        
        // 执行盈亏计算相关的处理器
        for (DrgFlowProcessor processor : processors) {
            if (!processor.isEnabled()) {
                continue;
            }
            
            // 只执行盈亏计算阶段的处理器（可以根据处理器名称或类型过滤）
            if (isProfitLossProcessor(processor)) {
                try {
                    processor.process(context);
                } catch (Exception e) {
                    context.setAttribute("error", "Profit loss processor " + processor.getName() + " failed: " + e.getMessage());
                    break;
                }
            }
        }
    }
    
    /**
     * 判断是否为决策阶段处理器
     * 
     * @param processor 处理器
     * @return 如果是决策阶段处理器则返回true
     */
    private boolean isDecisionProcessor(DrgFlowProcessor processor) {
        // 根据处理器名称判断是否为决策阶段处理器
        String name = processor.getName();
        return name != null && (name.contains("decision") || name.contains("Decision"));
    }
    
    /**
     * 判断是否为盈亏计算阶段处理器
     * 
     * @param processor 处理器
     * @return 如果是盈亏计算阶段处理器则返回true
     */
    private boolean isProfitLossProcessor(DrgFlowProcessor processor) {
        // 根据处理器名称判断是否为盈亏计算阶段处理器
        String name = processor.getName();
        return name != null && (name.contains("profit") || name.contains("Profit") || 
                               name.contains("loss") || name.contains("Loss"));
    }
}
