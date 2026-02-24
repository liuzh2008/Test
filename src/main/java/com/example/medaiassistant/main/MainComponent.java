package com.example.medaiassistant.main;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 主服务器组件 - 用于测试包扫描
 * 这个组件应该只在main profile下被加载
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@Component
@Profile("main")
public class MainComponent {
    
    /**
     * 获取组件类型
     * @return 组件类型描述
     */
    public String getComponentType() {
        return "Main Component - Should be loaded only in main profile";
    }
}
