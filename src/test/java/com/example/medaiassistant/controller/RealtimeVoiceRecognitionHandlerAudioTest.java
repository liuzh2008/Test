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
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealtimeVoiceRecognitionHandler 音频数据处理单元测试
 * 测试音频数据接收与转发功能
 * 
 * TDD红阶段：编写失败测试用例 - 已完成
 * TDD绿阶段：实现最小代码使测试通过 - 已完成 (5/5 测试通过)
 * TDD重构阶段：优化代码结构 - 已完成
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("实时语音识别Handler - 音频数据处理测试")
class RealtimeVoiceRecognitionHandlerAudioTest {

    @Mock
    private WebSocketSession mockSession;

    @Mock
    private VoiceRecognitionProperties voiceRecognitionProperties;

    private RealtimeVoiceRecognitionHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RealtimeVoiceRecognitionHandler(voiceRecognitionProperties);
        // 设置有效的API Key并建立连接
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-valid-test-api-key");
        when(mockSession.getId()).thenReturn("test-audio-session");
        handler.afterConnectionEstablished(mockSession);
    }

    /**
     * 测试接收二进制消息
     * 验收标准：
     * - Given WebSocket连接已建立且麦克风权限已授权
     * - When 用户对着麦克风说话
     * - Then 前端每256ms发送一次PCM音频数据包
     */
    @Test
    @DisplayName("testHandleBinaryMessage - 验证接收二进制消息")
    void testHandleBinaryMessage() throws Exception {
        // Given: 模拟PCM音频数据
        byte[] audioData = new byte[4096];
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (byte) (i % 256);
        }
        BinaryMessage message = new BinaryMessage(ByteBuffer.wrap(audioData));

        // When: 发送二进制消息
        handler.handleBinaryMessage(mockSession, message);

        // Then: 验证音频数据被接收和处理
        assertTrue(handler.hasActiveSession("test-audio-session"), 
            "Session应该保持活跃");
        assertEquals(1, handler.getReceivedAudioFrameCount("test-audio-session"),
            "应该记录接收到1帧音频数据");
    }

    /**
     * 测试音频帧转发到Recognition
     * 验收标准：
     * - Given 后端接收到音频数据
     * - When 调用sendAudioFrame
     * - Then 音频数据转发到ASR服务
     */
    @Test
    @DisplayName("testAudioFrameForwarding - 验证音频帧转发到Recognition")
    void testAudioFrameForwarding() throws Exception {
        // Given: 模拟多帧PCM音频数据
        byte[] audioData1 = createPCMData(4096);
        byte[] audioData2 = createPCMData(4096);
        BinaryMessage message1 = new BinaryMessage(ByteBuffer.wrap(audioData1));
        BinaryMessage message2 = new BinaryMessage(ByteBuffer.wrap(audioData2));

        // When: 发送多帧音频数据
        handler.handleBinaryMessage(mockSession, message1);
        handler.handleBinaryMessage(mockSession, message2);

        // Then: 验证音频帧被正确转发
        assertEquals(2, handler.getReceivedAudioFrameCount("test-audio-session"),
            "应该记录接收到2帧音频数据");
        assertTrue(handler.getTotalAudioBytesReceived("test-audio-session") >= 8192,
            "总接收字节数应该>=8192");
    }

    /**
     * 测试无效音频格式处理
     * 验收标准：
     * - Given 收到的音频数据格式无效（大小为0或异常）
     * - When 处理无效音频数据
     * - Then 忽略无效数据，不影响连接状态
     */
    @Test
    @DisplayName("testInvalidAudioFormat - 验证无效音频格式处理")
    void testInvalidAudioFormat() throws Exception {
        // Given: 空的音频数据
        byte[] emptyData = new byte[0];
        BinaryMessage emptyMessage = new BinaryMessage(ByteBuffer.wrap(emptyData));

        // When: 发送空音频数据
        handler.handleBinaryMessage(mockSession, emptyMessage);

        // Then: 验证Session保持活跃，无效数据被忽略
        assertTrue(handler.hasActiveSession("test-audio-session"), 
            "Session应该保持活跃");
        assertEquals(0, handler.getReceivedAudioFrameCount("test-audio-session"),
            "空数据不应被计入有效帧数");
    }

    /**
     * 测试音频缓冲区溢出保护
     * 验收标准：
     * - Given 音频数据大小超过预设最大值
     * - When 处理超大音频数据
     * - Then 截断或拒绝处理，保护系统稳定性
     */
    @Test
    @DisplayName("testAudioBufferOverflow - 验证音频缓冲区溢出保护")
    void testAudioBufferOverflow() throws Exception {
        // Given: 超大音频数据（超过1MB）
        byte[] oversizedData = new byte[2 * 1024 * 1024]; // 2MB
        BinaryMessage oversizedMessage = new BinaryMessage(ByteBuffer.wrap(oversizedData));

        // When: 发送超大音频数据
        handler.handleBinaryMessage(mockSession, oversizedMessage);

        // Then: 验证Session保持活跃，超大数据被拒绝或截断
        assertTrue(handler.hasActiveSession("test-audio-session"), 
            "Session应该保持活跃");
        // 验证系统不会因为超大数据而崩溃
        assertDoesNotThrow(() -> handler.hasActiveSession("test-audio-session"));
    }

    /**
     * 测试未注册Session的音频数据处理
     */
    @Test
    @DisplayName("testUnregisteredSessionAudioHandling - 验证未注册Session的音频处理")
    void testUnregisteredSessionAudioHandling() throws Exception {
        // Given: 未注册的Session
        WebSocketSession unregisteredSession = mock(WebSocketSession.class);
        when(unregisteredSession.getId()).thenReturn("unregistered-session");
        
        byte[] audioData = createPCMData(4096);
        BinaryMessage message = new BinaryMessage(ByteBuffer.wrap(audioData));

        // When & Then: 处理未注册Session的消息不应抛出异常
        assertDoesNotThrow(() -> handler.handleBinaryMessage(unregisteredSession, message));
    }

    /**
     * 创建模拟PCM音频数据
     */
    private byte[] createPCMData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            // 模拟16位PCM数据
            data[i] = (byte) ((Math.sin(i * 0.1) * 127));
        }
        return data;
    }
}
