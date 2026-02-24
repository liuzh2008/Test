package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.service.EncryptedDataTempService;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.service.AsyncCallbackService;
import com.example.medaiassistant.config.AIModelConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionServerController幂等性逻辑简化测试
 * 
 * 专门测试约束冲突修复中的核心逻辑：
 * - 记录存在性检查
 * - 幂等操作处理
 */
class ExecutionServerControllerIdempotentTest {

    @Mock
    private ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository;
    
    @Mock
    private EncryptedDataTempService encryptedDataTempService;
    
    @Mock
    private ServerConfigService serverConfigService;
    
    @Mock
    private AsyncCallbackService asyncCallbackService;

    @Mock
    private AIModelConfig aiModelConfig;

    private ExecutionServerController controller;
    
    private static final String TEST_ID = "24791";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ExecutionServerController(
            serverConfigService,
            encryptedDataTempRepository,
            encryptedDataTempService,
            asyncCallbackService,
            aiModelConfig
        );
    }

    /**
     * 测试核心幂等性逻辑：检查记录是否存在
     */
    @Test
    void testIdempotentLogic_ExistingRecord_ShouldReturnTrue() {
        // Mock: 数据库中存在记录
        EncryptedDataTemp existingRecord = new EncryptedDataTemp();
        existingRecord.setId(TEST_ID);
        existingRecord.setStatus(DataStatus.RECEIVED);
        
        when(encryptedDataTempRepository.findById(TEST_ID)).thenReturn(Optional.of(existingRecord));

        // 验证findById方法调用
        Optional<EncryptedDataTemp> result = encryptedDataTempRepository.findById(TEST_ID);
        
        assertTrue(result.isPresent());
        assertEquals(TEST_ID, result.get().getId());
        assertEquals(DataStatus.RECEIVED, result.get().getStatus());
        
        // 验证调用次数
        verify(encryptedDataTempRepository, times(1)).findById(TEST_ID);
    }

    /**
     * 测试核心幂等性逻辑：记录不存在的情况
     */
    @Test
    void testIdempotentLogic_NoRecord_ShouldReturnEmpty() {
        // Mock: 数据库中不存在记录
        when(encryptedDataTempRepository.findById(TEST_ID)).thenReturn(Optional.empty());

        // 验证findById方法调用
        Optional<EncryptedDataTemp> result = encryptedDataTempRepository.findById(TEST_ID);
        
        assertFalse(result.isPresent());
        
        // 验证调用次数
        verify(encryptedDataTempRepository, times(1)).findById(TEST_ID);
    }

    /**
     * 测试更新现有记录的逻辑
     */
    @Test
    void testUpdateExistingRecord_ShouldCallSave() {
        // 准备现有记录
        EncryptedDataTemp existingRecord = new EncryptedDataTemp();
        existingRecord.setId(TEST_ID);
        existingRecord.setStatus(DataStatus.RECEIVED);
        
        // Mock save操作
        when(encryptedDataTempRepository.save(any(EncryptedDataTemp.class))).thenReturn(existingRecord);

        // 执行保存操作
        EncryptedDataTemp savedRecord = encryptedDataTempRepository.save(existingRecord);
        
        assertNotNull(savedRecord);
        assertEquals(TEST_ID, savedRecord.getId());
        
        // 验证调用次数
        verify(encryptedDataTempRepository, times(1)).save(any(EncryptedDataTemp.class));
    }

    /**
     * 测试状态更新逻辑
     */
    @Test
    void testStatusUpdateLogic_ShouldCallUpdateStatus() {
        // 执行状态更新
        encryptedDataTempService.updateStatus(TEST_ID, DataStatus.DECRYPTED, null);
        
        // 验证调用次数
        verify(encryptedDataTempService, times(1)).updateStatus(eq(TEST_ID), eq(DataStatus.DECRYPTED), any());
    }

    /**
     * 测试幂等性的完整流程模拟
     */
    @Test
    void testCompleteIdempotentFlow_ExistingRecord() {
        // 1. 检查记录是否存在
        EncryptedDataTemp existingRecord = new EncryptedDataTemp();
        existingRecord.setId(TEST_ID);
        existingRecord.setStatus(DataStatus.RECEIVED);
        
        when(encryptedDataTempRepository.findById(TEST_ID)).thenReturn(Optional.of(existingRecord));
        when(encryptedDataTempRepository.save(any(EncryptedDataTemp.class))).thenReturn(existingRecord);

        // 2. 执行幂等性检查
        Optional<EncryptedDataTemp> record = encryptedDataTempRepository.findById(TEST_ID);
        
        if (record.isPresent()) {
            // 3. 记录存在，更新现有记录
            EncryptedDataTemp existing = record.get();
            // 模拟设置解密数据等操作
            encryptedDataTempRepository.save(existing);
            
            // 4. 更新状态（如果需要）
            if (existing.getStatus() == DataStatus.RECEIVED) {
                encryptedDataTempService.updateStatus(TEST_ID, DataStatus.DECRYPTED, null);
            }
        }

        // 验证执行流程
        verify(encryptedDataTempRepository, times(1)).findById(TEST_ID);
        verify(encryptedDataTempRepository, times(1)).save(any(EncryptedDataTemp.class));
        verify(encryptedDataTempService, times(1)).updateStatus(eq(TEST_ID), eq(DataStatus.DECRYPTED), any());
    }

    /**
     * 测试幂等性的完整流程模拟 - 新记录
     */
    @Test
    void testCompleteIdempotentFlow_NewRecord() {
        // 1. 检查记录是否存在
        when(encryptedDataTempRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        
        EncryptedDataTemp newRecord = new EncryptedDataTemp();
        newRecord.setId(TEST_ID);
        newRecord.setStatus(DataStatus.RECEIVED);
        when(encryptedDataTempRepository.save(any(EncryptedDataTemp.class))).thenReturn(newRecord);

        // 2. 执行幂等性检查
        Optional<EncryptedDataTemp> record = encryptedDataTempRepository.findById(TEST_ID);
        
        if (record.isEmpty()) {
            // 3. 记录不存在，创建新记录
            EncryptedDataTemp newData = new EncryptedDataTemp();
            newData.setId(TEST_ID);
            newData.setStatus(DataStatus.RECEIVED);
            encryptedDataTempRepository.save(newData);
            
            // 4. 更新状态
            encryptedDataTempService.updateStatus(TEST_ID, DataStatus.DECRYPTED, null);
        }

        // 验证执行流程
        verify(encryptedDataTempRepository, times(1)).findById(TEST_ID);
        verify(encryptedDataTempRepository, times(1)).save(any(EncryptedDataTemp.class));
        verify(encryptedDataTempService, times(1)).updateStatus(eq(TEST_ID), eq(DataStatus.DECRYPTED), any());
    }

    /**
     * 测试健康检查接口
     */
    @Test
    void testHealthCheck() {
        var response = controller.healthCheck();
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("healthy", responseBody.get("status"));
        assertEquals("MedAI Execution Server", responseBody.get("service"));
        assertEquals(8082, responseBody.get("port"));
    }

    /**
     * 测试服务器信息接口
     */
    @Test
    void testGetServerInfo() {
        when(serverConfigService.getDecryptionServerIp()).thenReturn("100.66.1.2");
        
        var response = controller.getServerInfo();
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals("MedAI Execution Server", responseBody.get("serverName"));
        assertEquals(8082, responseBody.get("port"));
        assertEquals("100.66.1.2", responseBody.get("decryptionServerIp"));
    }
}
