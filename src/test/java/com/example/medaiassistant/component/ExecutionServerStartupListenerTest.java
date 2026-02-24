package com.example.medaiassistant.component;

import com.example.medaiassistant.model.ServerConfiguration;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.repository.ServerConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionServerStartupListener测试类
 */
public class ExecutionServerStartupListenerTest {

    @Mock
    private ServerConfigService serverConfigService;

    @Mock
    private ServerConfigurationRepository serverConfigurationRepository;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private ExecutionServerStartupListener executionServerStartupListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试当存在执行服务器配置时，能够正确读取并存储IP地址
     */
    @Test
    public void testOnApplicationReadyWithExecutionServerConfig() {
        // 准备测试数据，模拟数据库中的实际记录
        ServerConfiguration config = new ServerConfiguration();
        config.setConfigName("execution_server");
        // 根据实际数据库记录设置configData
        config.setConfigData("{\"ip\": \"100.66.1.2\"}");
        
        // 设置模拟行为 - 模拟从数据库中读取真正的数据
        when(serverConfigurationRepository.findByConfigName("execution_server")).thenReturn(config);
        
        // 直接调用方法进行测试
        executionServerStartupListener.onApplicationReady();
        
        // 验证结果
        verify(serverConfigurationRepository).findByConfigName("execution_server");
        // 验证是否正确解析并存储了IP地址
        verify(serverConfigService).setDecryptionServerIp("100.66.1.2");
    }

    /**
     * 测试当不存在执行服务器配置时，不会抛出异常
     */
    @Test
    public void testOnApplicationReadyWithoutExecutionServerConfig() {
        // 设置模拟行为 - 模拟数据库中没有相关配置
        when(serverConfigurationRepository.findByConfigName("execution_server")).thenReturn(null);
        
        // 直接调用方法进行测试
        assertDoesNotThrow(() -> {
            executionServerStartupListener.onApplicationReady();
        });
        
        // 验证结果
        verify(serverConfigurationRepository).findByConfigName("execution_server");
        verify(serverConfigService, never()).setDecryptionServerIp(anyString());
    }
}
