package com.example.medaiassistant.execution;

import org.springframework.stereotype.Component;

/**
 * 执行服务器组件 - 用于测试包扫描
 * 这个组件应该只在execution profile下被加载
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@Component
public class ExecutionComponent {
    
    /**
     * 获取组件类型
     * @return 组件类型描述
     */
    public String getComponentType() {
        return "Execution Component - Should be loaded only in execution profile";
    }
}
