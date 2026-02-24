package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DataStatus;
import com.example.medaiassistant.model.EncryptedDataTemp;
import com.example.medaiassistant.repository.EncryptedDataTempRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据提取与预处理服务测试类
 * 测试DataExtractionService的各种功能，包括数据提取、验证、预处理和状态更新
 * 
 * @module 模块3：数据提取与预处理模块测试
 * @author System
 * @version 1.0
 * @since 2025-09-11
 */
@ExtendWith(MockitoExtension.class)
class DataExtractionServiceTest {

    @Mock
    private EncryptedDataTempRepository encryptedDataTempRepository;

    @Mock
    private EncryptedDataTempService encryptedDataTempService;

    @InjectMocks
    private DataExtractionService dataExtractionService;

    private EncryptedDataTemp testData;
    private String testId = "test-id-123";
    private String testRequestId = "req-123";
    private String testSource = "test-source";

    @BeforeEach
    void setUp() {
        testData = new EncryptedDataTemp();
        testData.setId(testId);
        testData.setRequestId(testRequestId);
        testData.setSource(testSource);
        testData.setStatus(DataStatus.DECRYPTED);
    }

    @Test
    void testExtractAllDecryptedData() {
        // 准备
        when(encryptedDataTempRepository.findByStatus(DataStatus.DECRYPTED))
                .thenReturn(List.of(testData));

        // 执行
        List<EncryptedDataTemp> result = dataExtractionService.extractAllDecryptedData();

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).getId());
        verify(encryptedDataTempRepository).findByStatus(DataStatus.DECRYPTED);
    }

    @Test
    void testExtractAndPreprocessData_ValidData() throws SQLException {
        // 准备
        String testContent = "{\"patient\": \"张三\", \"diagnosis\": \"感冒\"}";
        Clob mockClob = mock(Clob.class);
        
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));
        when(mockClob.length()).thenReturn((long) testContent.length());
        when(mockClob.getSubString(1, testContent.length())).thenReturn(testContent);
        
        testData.setDecryptedData(mockClob);
        
        // 执行
        String result = dataExtractionService.extractAndPreprocessData(testId);

        // 验证
        assertNotNull(result);
        assertTrue(result.contains("patient"));
        assertTrue(result.contains("diagnosis"));
        verify(encryptedDataTempService).startProcessing(testId);
    }

    @Test
    void testExtractAndPreprocessData_DataNotFound() {
        // 准备
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.empty());

        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            dataExtractionService.extractAndPreprocessData(testId);
        });
    }

    @Test
    void testExtractAndPreprocessData_WrongStatus() {
        // 准备
        testData.setStatus(DataStatus.RECEIVED);
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));

        // 执行和验证
        assertThrows(IllegalStateException.class, () -> {
            dataExtractionService.extractAndPreprocessData(testId);
        });
    }

    @Test
    void testBatchExtractAndPreprocessData() throws SQLException {
        // 准备
        String testContent = "{\"patient\": \"测试患者\", \"diagnosis\": \"测试诊断\"}";
        Clob mockClob = mock(Clob.class);
        
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));
        when(mockClob.length()).thenReturn((long) testContent.length());
        when(mockClob.getSubString(1, testContent.length())).thenReturn(testContent);
        
        testData.setDecryptedData(mockClob);

        // 执行
        List<String> results = dataExtractionService.batchExtractAndPreprocessData(List.of(testId));

        // 验证
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("patient"));
        assertTrue(results.get(0).contains("diagnosis"));
    }

    @Test
    void testGetPendingProcessingCount() {
        // 准备
        when(encryptedDataTempRepository.countByStatus(DataStatus.DECRYPTED)).thenReturn(5L);

        // 执行
        long count = dataExtractionService.getPendingProcessingCount();

        // 验证
        assertEquals(5L, count);
        verify(encryptedDataTempRepository).countByStatus(DataStatus.DECRYPTED);
    }

    @Test
    void testHasPendingData_True() {
        // 准备
        when(encryptedDataTempRepository.countByStatus(DataStatus.DECRYPTED)).thenReturn(1L);

        // 执行
        boolean hasData = dataExtractionService.hasPendingData();

        // 验证
        assertTrue(hasData);
    }

    @Test
    void testHasPendingData_False() {
        // 准备
        when(encryptedDataTempRepository.countByStatus(DataStatus.DECRYPTED)).thenReturn(0L);

        // 执行
        boolean hasData = dataExtractionService.hasPendingData();

        // 验证
        assertFalse(hasData);
    }

    @Test
    void testPreprocessData_Integration() throws SQLException {
        // 准备 - 通过公共方法间接测试预处理功能
        String testContent = "  {\"patient\": \"张三\"}  \r\n";
        Clob mockClob = mock(Clob.class);
        
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));
        when(mockClob.length()).thenReturn((long) testContent.length());
        when(mockClob.getSubString(1, testContent.length())).thenReturn(testContent);
        
        testData.setDecryptedData(mockClob);

        // 执行
        String result = dataExtractionService.extractAndPreprocessData(testId);

        // 验证 - 预处理应该去除空白和标准化换行符
        assertNotNull(result);
        assertTrue(result.startsWith("{")); // 应该去除首部空白
        assertTrue(result.endsWith("}")); // 应该去除尾部空白
        assertFalse(result.contains("\r\n")); // 应该标准化换行符
        verify(encryptedDataTempService).startProcessing(testId);
    }

    @Test
    void testContentValidation_Integration() throws SQLException {
        // 准备 - 测试内容验证功能
        String validContent = "{\"patient\": \"李四\", \"diagnosis\": \"肺炎\", \"medicalRecord\": \"详细病历\"}";
        Clob mockClob = mock(Clob.class);
        
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));
        when(mockClob.length()).thenReturn((long) validContent.length());
        when(mockClob.getSubString(1, validContent.length())).thenReturn(validContent);
        
        testData.setDecryptedData(mockClob);

        // 执行 - 应该成功通过验证
        String result = dataExtractionService.extractAndPreprocessData(testId);

        // 验证
        assertNotNull(result);
        assertTrue(result.contains("patient"));
        assertTrue(result.contains("diagnosis"));
        verify(encryptedDataTempService).startProcessing(testId);
    }

    @Test
    void testContentValidation_Failure() throws SQLException {
        // 准备 - 测试无效内容
        String invalidContent = "short";
        Clob mockClob = mock(Clob.class);
        
        when(encryptedDataTempRepository.findById(testId)).thenReturn(Optional.of(testData));
        when(mockClob.length()).thenReturn((long) invalidContent.length());
        when(mockClob.getSubString(1, invalidContent.length())).thenReturn(invalidContent);
        
        testData.setDecryptedData(mockClob);

        // 执行和验证 - 应该抛出异常
        assertThrows(IllegalStateException.class, () -> {
            dataExtractionService.extractAndPreprocessData(testId);
        });
    }
}
