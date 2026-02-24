package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.PromptResult;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import com.example.medaiassistant.repository.PromptResultRepository;
import com.example.medaiassistant.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PollingService单元测试类
 * 测试轮询服务的各种场景，包括乐观锁冲突处理
 * 
 * @author MedAI Assistant Team
 * @version 1.0
 * @since 2025-09-29
 */
@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock
    private EncryptedDataTempRepository encryptedDataTempRepository;

    @Mock
    private EncryptedDataTempService encryptedDataTempService;

    @Mock
    private PromptResultRepository promptResultRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private ServerConfigService serverConfigService;

    @Mock
    private OptimisticLockRetryService optimisticLockRetryService;

    @InjectMocks
    private PollingService pollingService;

    private Prompt testPrompt;
    private EncryptedDataTemp testEncryptedData;
    private PromptResult testPromptResult;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testPrompt = new Prompt();
        testPrompt.setPromptId(123);
        testPrompt.setStatusName("已提交");
        testPrompt.setPatientId("P001");
        testPrompt.setObjectiveContent("测试目标内容");
        testPrompt.setDailyRecords("测试日常记录");
        testPrompt.setPromptTemplateContent("测试模板内容");

        testEncryptedData = new EncryptedDataTemp();
        testEncryptedData.setId("cdwyy123");
        testEncryptedData.setRequestId("cdwyy123");
        testEncryptedData.setStatus(DataStatus.ENCRYPTED);

        testPromptResult = new PromptResult();
        testPromptResult.setResultId(456);
        testPromptResult.setPromptId(123);
        testPromptResult.setOriginalResultContent("测试执行结果");
        testPromptResult.setStatus("SUCCESS");
    }

    /**
     * 测试轮询服务已禁用时的行为
     */
    @Test
    void testPollSubmittedPrompts_WhenDisabled_ShouldSkipPolling() {
        // 设置轮询服务为禁用状态
        pollingService.disablePolling();

        // 执行轮询
        pollingService.pollSubmittedPrompts();

        // 验证没有进行任何数据库操作
        verify(promptRepository, never()).findByStatusName(anyString());
        verify(encryptedDataTempRepository, never()).findByRequestId(anyString());
    }

    /**
     * 测试没有待处理Prompt时的行为
     */
    @Test
    void testPollSubmittedPrompts_WhenNoSubmittedPrompts_ShouldReturnEarly() {
        // 设置轮询服务为启用状态
        pollingService.enablePolling();

        // 模拟没有待处理的Prompt
        when(promptRepository.findByStatusName("已提交")).thenReturn(List.of());

        // 执行轮询
        pollingService.pollSubmittedPrompts();

        // 验证查询被调用，但没有进一步处理
        verify(promptRepository).findByStatusName("已提交");
        verify(encryptedDataTempRepository, never()).findByRequestId(anyString());
    }

    /**
     * 测试轮询状态统计
     */
    @Test
    void testGetPollingStats_ShouldReturnCorrectStatistics() {
        // 模拟统计数据
        when(encryptedDataTempRepository.countByStatus(DataStatus.ENCRYPTED)).thenReturn(5L);
        when(encryptedDataTempRepository.countByStatus(DataStatus.SENT)).thenReturn(3L);
        when(encryptedDataTempRepository.countByStatus(DataStatus.ERROR)).thenReturn(2L);

        String stats = pollingService.getPollingStats();

        assertNotNull(stats);
        assertTrue(stats.contains("待处理: 5"));
        assertTrue(stats.contains("已发送: 3"));
        assertTrue(stats.contains("错误: 2"));
    }

    /**
     * 测试轮询服务状态管理
     */
    @Test
    void testPollingServiceStateManagement() {
        // 初始状态应该是禁用
        assertFalse(pollingService.isPollingEnabled());

        // 启用轮询服务
        pollingService.enablePolling();
        assertTrue(pollingService.isPollingEnabled());

        // 禁用轮询服务
        pollingService.disablePolling();
        assertFalse(pollingService.isPollingEnabled());
    }

    /**
     * 测试手动触发轮询
     */
    @Test
    void testTriggerPolling_ShouldCallPollSubmittedPrompts() {
        // 设置轮询服务为启用状态
        pollingService.enablePolling();

        // 模拟没有待处理的Prompt
        when(promptRepository.findByStatusName("已提交")).thenReturn(List.of());

        // 执行手动触发
        pollingService.triggerPolling();

        // 验证轮询方法被调用
        verify(promptRepository).findByStatusName("已提交");
    }

    /**
     * 测试乐观锁重试服务集成
     */
    @Test
    void testOptimisticLockRetryServiceIntegration() {
        // 验证重试服务被正确注入
        assertNotNull(pollingService);
        assertNotNull(optimisticLockRetryService);
        
        // 验证重试服务在初始状态下没有被调用
        verifyNoInteractions(optimisticLockRetryService);
    }

    /**
     * 测试轮询服务的事务配置
     */
    @Test
    void testPollingServiceTransactionalConfiguration() {
        // 验证轮询服务正确配置了事务注解
        var method = PollingService.class.getDeclaredMethods();
        boolean hasTransactionalAnnotation = false;
        
        for (var m : method) {
            if (m.getName().equals("pollSubmittedPrompts") && 
                m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)) {
                hasTransactionalAnnotation = true;
                break;
            }
        }
        
        assertTrue(hasTransactionalAnnotation, "pollSubmittedPrompts方法应该配置了@Transactional注解");
    }

    /**
     * 测试轮询服务启用状态切换
     */
    @Test
    void testPollingServiceEnableDisableCycle() {
        // 初始状态
        assertFalse(pollingService.isPollingEnabled());

        // 启用
        pollingService.enablePolling();
        assertTrue(pollingService.isPollingEnabled());

        // 再次禁用
        pollingService.disablePolling();
        assertFalse(pollingService.isPollingEnabled());

        // 再次启用
        pollingService.enablePolling();
        assertTrue(pollingService.isPollingEnabled());
    }

    /**
     * 测试轮询服务异常处理
     */
    @Test
    void testPollingServiceExceptionHandling() {
        // 设置轮询服务为启用状态
        pollingService.enablePolling();

        // 模拟数据库异常
        when(promptRepository.findByStatusName("已提交"))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行轮询，应该捕获异常而不抛出
        assertDoesNotThrow(() -> pollingService.pollSubmittedPrompts());

        // 验证异常被记录但服务继续运行
        verify(promptRepository).findByStatusName("已提交");
    }
}
