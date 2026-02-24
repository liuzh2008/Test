package com.example.medaiassistant.hospital.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医院配置服务加载测试
 * 专门测试配置文件加载功能，按照TDD红-绿-重构流程
 * 
 * @author Cline
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
class HospitalConfigServiceLoadTest {

    /**
     * 绿阶段测试：测试加载不存在的配置文件
     * 服务应该记录警告但不抛出异常
     */
    @Test
    void testLoadNonExistentConfigFile_GreenPhase() {
        // 创建服务实例
        HospitalConfigService service = new HospitalConfigService();
        
        // 使用反射设置不存在的目录
        try {
            var configDirPathField = HospitalConfigService.class.getDeclaredField("configDirPath");
            configDirPathField.setAccessible(true);
            configDirPathField.set(service, "non/existent/directory");
            
            // 尝试调用私有方法loadAllConfigs
            var loadAllConfigsMethod = HospitalConfigService.class.getDeclaredMethod("loadAllConfigs");
            loadAllConfigsMethod.setAccessible(true);
            
            // 执行方法 - 应该记录警告但不抛出异常
            loadAllConfigsMethod.invoke(service);
            
            // 验证结果：服务应该继续运行，配置数量为0
            var getConfigCountMethod = HospitalConfigService.class.getDeclaredMethod("getConfigCount");
            int configCount = (int) getConfigCountMethod.invoke(service);
            
            assertEquals(0, configCount, "不存在的目录应该加载0个配置");
            
        } catch (Exception e) {
            // 不应该抛出异常
            fail("加载不存在的目录不应该抛出异常: " + e.getMessage());
        }
    }

    /**
     * ✅ 绿阶段测试：测试加载空配置文件目录
     * 这个测试现在应该通过，因为服务应该优雅处理空目录
     */
    @Test
    void testLoadEmptyConfigDirectory_GreenPhase() {
        // 创建服务实例
        HospitalConfigService service = new HospitalConfigService();
        
        // 使用反射设置空目录
        try {
            var configDirPathField = HospitalConfigService.class.getDeclaredField("configDirPath");
            configDirPathField.setAccessible(true);
            configDirPathField.set(service, "src/test/resources/empty-config-dir");
            
            // 创建空目录
            java.nio.file.Files.createDirectories(
                java.nio.file.Paths.get("src/test/resources/empty-config-dir"));
            
            // 尝试调用私有方法loadAllConfigs
            var loadAllConfigsMethod = HospitalConfigService.class.getDeclaredMethod("loadAllConfigs");
            loadAllConfigsMethod.setAccessible(true);
            
            // 执行方法
            loadAllConfigsMethod.invoke(service);
            
            // 验证结果（这个断言应该会失败，因为服务应该记录警告但继续运行）
            // 获取配置数量
            var getConfigCountMethod = HospitalConfigService.class.getDeclaredMethod("getConfigCount");
            int configCount = (int) getConfigCountMethod.invoke(service);
            
            // 断言应该失败：配置数量应该为0
            assertEquals(0, configCount, "空目录应该加载0个配置");
            
        } catch (Exception e) {
            // 如果抛出异常，测试失败（但根据实现，应该不会抛出异常）
            fail("加载空目录不应该抛出异常: " + e.getMessage());
        }
    }

    /**
     * ✅ 绿阶段测试：测试加载无效YAML文件
     * 这个测试现在应该通过，因为服务应该优雅处理无效YAML文件
     */
    @Test
    void testLoadInvalidYamlFile_GreenPhase() {
        // 创建服务实例
        HospitalConfigService service = new HospitalConfigService();
        
        // 使用反射设置测试目录
        try {
            var configDirPathField = HospitalConfigService.class.getDeclaredField("configDirPath");
            configDirPathField.setAccessible(true);
            configDirPathField.set(service, "src/test/resources/invalid-config-dir");
            
            // 创建测试目录和无效YAML文件
            java.nio.file.Path testDir = java.nio.file.Paths.get("src/test/resources/invalid-config-dir");
            java.nio.file.Files.createDirectories(testDir);
            java.nio.file.Files.write(testDir.resolve("invalid.yaml"), 
                "invalid: yaml: content: [".getBytes());
            
            // 尝试调用私有方法loadAllConfigs
            var loadAllConfigsMethod = HospitalConfigService.class.getDeclaredMethod("loadAllConfigs");
            loadAllConfigsMethod.setAccessible(true);
            
            // 执行方法（应该会失败或记录错误）
            loadAllConfigsMethod.invoke(service);
            
            // 验证结果（这个断言应该会失败）
            // 服务应该处理异常并继续运行，配置数量应该为0
            var getConfigCountMethod = HospitalConfigService.class.getDeclaredMethod("getConfigCount");
            int configCount = (int) getConfigCountMethod.invoke(service);
            
            assertEquals(0, configCount, "无效YAML文件应该加载0个配置");
            
        } catch (Exception e) {
            // 如果抛出异常，测试失败（但根据实现，应该不会抛出异常）
            fail("加载无效YAML不应该抛出异常: " + e.getMessage());
        }
    }
}
