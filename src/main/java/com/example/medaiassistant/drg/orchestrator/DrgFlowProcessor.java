package com.example.medaiassistant.drg.orchestrator;

/**
 * DRGs流程处理器接口
 * 定义流程处理器的基本契约
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-11
 */
public interface DrgFlowProcessor {

    /**
     * 获取处理器名称
     * 
     * @return 处理器名称
     */
    String getName();

    /**
     * 获取处理器执行顺序
     * 
     * @return 执行顺序（数值越小越先执行）
     */
    int getOrder();

    /**
     * 检查处理器是否启用
     * 
     * @return 如果启用则返回true
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 执行处理器逻辑
     * 
     * @param context 流程上下文
     */
    void process(DrgFlowContext context);
}
