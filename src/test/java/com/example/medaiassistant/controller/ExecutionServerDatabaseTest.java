package com.example.medaiassistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.medaiassistant.repository.executionserver.ExecutionServerEncryptedDataTempRepository;
import com.example.medaiassistant.service.EncryptedDataTempService;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.service.AsyncCallbackService;
import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.util.ResponseCacheUtil;

import static org.mockito.Mockito.when;

/**
 * 执行服务器数据库连接和ENCRYPTED_DATA_TEMP表访问测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-11-28
 */
@SpringBootTest
@AutoConfigureWebTestClient
class ExecutionServerDatabaseTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ExecutionServerEncryptedDataTempRepository encryptedDataTempRepository;

    @MockitoBean
    private EncryptedDataTempService encryptedDataTempService;

    @MockitoBean
    private ServerConfigService serverConfigService;

    @MockitoBean
    private AsyncCallbackService asyncCallbackService;

    @MockitoBean
    private AIModelConfig aiModelConfig;
    
    @MockitoBean
    private ResponseCacheUtil responseCacheUtil;

    /**
     * 测试执行服务器数据库连接
     * 预期：返回200 OK，状态为UP
     */
    @Test
    void testExecutionServerDatabaseConnection() {
        // 模拟repository.count()方法返回0
        when(encryptedDataTempRepository.count()).thenReturn(0L);
        
        webTestClient.get().uri("/api/execute/database/connection")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.message").isEqualTo("执行服务器数据库连接成功")
                .jsonPath("$.recordCount").isEqualTo(0)
                .jsonPath("$.timestamp").exists();
    }

    /**
     * 测试ENCRYPTED_DATA_TEMP表访问
     * 预期：返回200 OK，状态为UP，包含表信息
     */
    @Test
    void testEncryptedDataTempTableAccess() {
        // 模拟repository.count()方法返回5
        when(encryptedDataTempRepository.count()).thenReturn(5L);
        
        webTestClient.get().uri("/api/execute/database/encrypted-data-temp/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.tableExists").isEqualTo(true)
                .jsonPath("$.tableName").isEqualTo("ENCRYPTED_DATA_TEMP")
                .jsonPath("$.recordCount").isEqualTo(5)
                .jsonPath("$.message").isEqualTo("ENCRYPTED_DATA_TEMP表访问正常")
                .jsonPath("$.timestamp").exists();
    }
}
