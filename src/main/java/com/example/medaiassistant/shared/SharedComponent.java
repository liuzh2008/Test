package com.example.medaiassistant.shared;

import org.springframework.stereotype.Component;

/**
 * 共享组件 - 用于测试包扫描
 * 这个组件应该在所有Profile下都被加载
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@Component
public class SharedComponent {
    
    /**
     * 获取组件类型
     * @return 组件类型描述
     */
    public String getComponentType() {
        return "Shared Component - Should be loaded in all profiles";
    }
}
