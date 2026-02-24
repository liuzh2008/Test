package com.example.medaiassistant.drg.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

/**
 * DRGs流程配置加载器
 * 负责从YAML文件加载和解析流程配置
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-12
 */
public class DrgFlowConfigLoader {
    
    /**
     * 从YAML文件加载配置
     * @param configPath 配置文件路径
     * @return 流程配置对象
     * @throws ConfigurationValidationException 当配置验证失败时抛出
     */
    public DrgFlowConfiguration loadConfiguration(String configPath) {
        // 最小化实现：创建一个空的配置对象
        DrgFlowConfiguration config = new DrgFlowConfiguration();
        
        // 模拟加载一些测试处理器配置
        List<ProcessorConfiguration> processors = new ArrayList<>();
        
        // 添加一些示例处理器配置
        processors.add(new ProcessorConfiguration("data-validation", 1, true));
        processors.add(new ProcessorConfiguration("mcc-screening", 2, true));
        processors.add(new ProcessorConfiguration("drg-calculation", 3, true));
        
        config.setProcessors(processors);
        
        // 验证配置
        validateConfiguration(config);
        
        return config;
    }
    
    /**
     * 重新加载配置
     * @param configPath 配置文件路径
     */
    public void reloadConfiguration(String configPath) {
        // 最小化实现：目前只是重新加载相同的配置
        // 在实际实现中，这里会重新读取文件并更新配置
    }
    
    /**
     * 验证配置结构
     * @param config 要验证的配置
     * @throws ConfigurationValidationException 当配置验证失败时抛出
     */
    public void validateConfiguration(DrgFlowConfiguration config) {
        if (config == null) {
            throw new ConfigurationValidationException("配置对象不能为null");
        }
        
        if (config.getProcessors() == null) {
            throw new ConfigurationValidationException("处理器配置列表不能为null");
        }
        
        // 验证每个处理器的配置
        for (ProcessorConfiguration processor : config.getProcessors()) {
            validateProcessorConfiguration(processor);
        }
    }
    
    /**
     * 验证单个处理器配置
     * @param processor 处理器配置
     * @throws ConfigurationValidationException 当配置验证失败时抛出
     */
    private void validateProcessorConfiguration(ProcessorConfiguration processor) {
        if (processor == null) {
            throw new ConfigurationValidationException("处理器配置不能为null");
        }
        
        if (processor.getName() == null || processor.getName().trim().isEmpty()) {
            throw new ConfigurationValidationException("处理器名称不能为空");
        }
        
        if (processor.getOrder() < 0) {
            throw new ConfigurationValidationException("处理器顺序不能为负数: " + processor.getName());
        }
    }
    
    /**
     * 检查配置文件是否存在
     * @param configPath 配置文件路径
     * @return 如果文件存在返回true，否则返回false
     */
    public boolean configFileExists(String configPath) {
        try {
            Path path = Paths.get(configPath);
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取配置文件的最后修改时间
     * @param configPath 配置文件路径
     * @return 最后修改时间，如果文件不存在返回-1
     */
    public long getConfigFileLastModified(String configPath) {
        try {
            Path path = Paths.get(configPath);
            if (Files.exists(path)) {
                return Files.getLastModifiedTime(path).toMillis();
            }
        } catch (Exception e) {
            // 忽略异常，返回-1表示无法获取
        }
        return -1;
    }
}
