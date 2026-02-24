package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.VoiceRecognitionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealtimeVoiceRecognitionHandler 单元测试
 * 测试WebSocket连接管理功能
 * 
 * TDD红阶段：编写失败测试用例 - 已完成
 * TDD绿阶段：实现最小代码使测试通过 - 已完成 (4/4 测试通过)
 * TDD重构阶段：优化代码结构 - 已完成
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("实时语音识别Handler - WebSocket连接管理测试")
class RealtimeVoiceRecognitionHandlerTest {

    @Mock
    private WebSocketSession mockSession;

    @Mock
    private VoiceRecognitionProperties voiceRecognitionProperties;

    private RealtimeVoiceRecognitionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RealtimeVoiceRecognitionHandler(voiceRecognitionProperties);
    }

    /**
     * 测试WebSocket连接建立成功
     * 验收标准：
     * - Given 用户已登录系统并进入病历记录页面
     * - When 用户点击"语音识别"按钮
     * - Then 系统在1秒内建立WebSocket连接
     */
    @Test
    @DisplayName("testWebSocketConnectionEstablished - 验证连接成功建立")
    void testWebSocketConnectionEstablished() throws Exception {
        // Given: 有效的API Key配置
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-valid-test-api-key");
        when(mockSession.getId()).thenReturn("test-session-123");

        // When: 建立WebSocket连接
        handler.afterConnectionEstablished(mockSession);

        // Then: 验证连接成功
        assertTrue(handler.hasActiveSession("test-session-123"), 
            "应该存在活跃的Session");
        verify(mockSession).sendMessage(argThat(msg -> 
            msg.getPayload().toString().contains("\"type\":\"ready\"")));
    }

    /**
     * 测试API Key无效时拒绝连接
     * 验收标准：
     * - Given 后端配置的API Key为空或无效
     * - When 前端发起WebSocket连接
     * - Then 连接被拒绝，状态码为SERVER_ERROR
     */
    @Test
    @DisplayName("testWebSocketConnectionWithInvalidApiKey - 验证API Key无效时拒绝连接")
    void testWebSocketConnectionWithInvalidApiKey() throws Exception {
        // Given: 无效的API Key配置
        when(voiceRecognitionProperties.getApiKey()).thenReturn(null);
        when(mockSession.getId()).thenReturn("test-session-456");

        // When: 尝试建立WebSocket连接
        handler.afterConnectionEstablished(mockSession);

        // Then: 验证连接被拒绝
        assertFalse(handler.hasActiveSession("test-session-456"), 
            "无效API Key时不应存在活跃Session");
        verify(mockSession).sendMessage(argThat(msg -> 
            msg.getPayload().toString().contains("\"type\":\"error\"")));
        verify(mockSession).close(CloseStatus.SERVER_ERROR);
    }

    /**
     * 测试WebSocket连接正常关闭
     * 验收标准：
     * - Given WebSocket连接已建立
     * - When 用户点击停止录音或关闭页面
     * - Then WebSocket连接正常关闭，状态码为1000
     */
    @Test
    @DisplayName("testWebSocketConnectionClosed - 验证连接正常关闭")
    void testWebSocketConnectionClosed() throws Exception {
        // Given: 已建立的WebSocket连接
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-valid-test-api-key");
        when(mockSession.getId()).thenReturn("test-session-789");
        handler.afterConnectionEstablished(mockSession);
        assertTrue(handler.hasActiveSession("test-session-789"));

        // When: 关闭连接
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Then: 验证资源已清理
        assertFalse(handler.hasActiveSession("test-session-789"), 
            "关闭后不应存在活跃Session");
    }

    /**
     * 测试异常时资源清理
     * 验收标准：
     * - Given 用户正在使用语音识别
     * - When WebSocket连接发生异常
     * - Then 自动清理相关资源
     */
    @Test
    @DisplayName("testWebSocketConnectionCleanupOnError - 验证异常时资源清理")
    void testWebSocketConnectionCleanupOnError() throws Exception {
        // Given: 已建立的WebSocket连接
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-valid-test-api-key");
        when(mockSession.getId()).thenReturn("test-session-error");
        handler.afterConnectionEstablished(mockSession);
        assertTrue(handler.hasActiveSession("test-session-error"));

        // When: 发生传输错误
        Exception testException = new RuntimeException("模拟传输错误");
        handler.handleTransportError(mockSession, testException);

        // Then: 验证资源已清理
        assertFalse(handler.hasActiveSession("test-session-error"), 
            "错误后不应存在活跃Session");
    }
}
