package com.example.medaiassistant.integration.packagestructure;

import com.example.medaiassistant.common.CommonComponent;
import com.example.medaiassistant.execution.ExecutionComponent;
import com.example.medaiassistant.main.MainComponent;
import com.example.medaiassistant.shared.SharedComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 包结构扫描测试 - 绿阶段
 * 验证包结构重构后的组件扫描功能
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-22
 */
@SpringBootTest
@ActiveProfiles("execution")
class PackageStructureScanTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 🟢 绿阶段测试：验证执行服务器模式下main包组件不应加载
     * 执行服务器模式下应该无法找到main包下的组件
     */
    @Test
    void mainPackageComponentsShouldNotBeLoadedInExecutionProfile() {
        // 验证main包组件在execution profile下不应被加载
        assertThrows(NoSuchBeanDefinitionException.class, () -> {
            applicationContext.getBean(MainComponent.class);
        }, "Main包组件在execution profile下不应被加载");
    }

    /**
     * 🟢 绿阶段测试：验证common包组件应该被加载
     * common包组件应该在所有profile下都被加载
     */
    @Test
    void commonPackageComponentsShouldBeLoaded() {
        // 验证common包组件在execution profile下应该被加载
        CommonComponent commonComponent = applicationContext.getBean(CommonComponent.class);
        assertNotNull(commonComponent, "Common包组件在execution profile下应该被加载");
        assertEquals("Common Component - Should be loaded in all profiles", 
                     commonComponent.getComponentType());
    }

    /**
     * 🟢 绿阶段测试：验证execution包组件应该被加载
     * execution包组件应该在execution profile下被加载
     */
    @Test
    void executionPackageComponentsShouldBeLoaded() {
        // 验证execution包组件在execution profile下应该被加载
        ExecutionComponent executionComponent = applicationContext.getBean(ExecutionComponent.class);
        assertNotNull(executionComponent, "Execution包组件在execution profile下应该被加载");
        assertEquals("Execution Component - Should be loaded only in execution profile", 
                     executionComponent.getComponentType());
    }

    /**
     * 🟢 绿阶段测试：验证shared包组件应该被加载
     * shared包组件应该在所有profile下都被加载
     */
    @Test
    void sharedPackageComponentsShouldBeLoaded() {
        // 验证shared包组件在execution profile下应该被加载
        SharedComponent sharedComponent = applicationContext.getBean(SharedComponent.class);
        assertNotNull(sharedComponent, "Shared包组件在execution profile下应该被加载");
        assertEquals("Shared Component - Should be loaded in all profiles", 
                     sharedComponent.getComponentType());
    }
}
