package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.VoiceRecognitionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealtimeVoiceRecognitionHandler 识别结果回调单元测试
 * 测试识别结果处理和推送功能
 * 
 * TDD红阶段：编写失败测试用例 - 已完成
 * TDD绿阶段：实现最小代码使测试通过 - 已完成 (6/6 测试通过)
 * TDD重构阶段：优化代码结构 - 已完成 (使用Jackson进行JSON序列化)
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("实时语音识别Handler - 识别结果回调测试")
class RealtimeVoiceRecognitionHandlerResultTest {

    @Mock
    private WebSocketSession mockSession;

    @Mock
    private VoiceRecognitionProperties voiceRecognitionProperties;

    private RealtimeVoiceRecognitionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RealtimeVoiceRecognitionHandler(voiceRecognitionProperties);
        objectMapper = new ObjectMapper();
        
        // 设置有效的API Key并建立连接
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-valid-test-api-key");
        when(mockSession.getId()).thenReturn("test-result-session");
        when(mockSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(mockSession);
    }

    /**
     * 测试临时结果处理
     * 验收标准：
     * - Given 用户正在进行语音录入
     * - When 阿里云ASR返回临时识别结果
     * - Then 后端将临时结果推送到前端
     */
    @Test
    @DisplayName("testOnEventPartialResult - 验证临时结果处理")
    void testOnEventPartialResult() throws Exception {
        // Given: 临时识别结果
        String partialText = "患者主诉";

        // When: 推送临时结果
        handler.sendPartialResult("test-result-session", partialText);

        // Then: 验证消息被推送
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeast(2)).sendMessage(captor.capture());
        
        // 找到包含partial的消息
        boolean foundPartial = captor.getAllValues().stream()
            .anyMatch(msg -> msg.getPayload().contains("\"type\":\"partial\""));
        assertTrue(foundPartial, "应该发送partial类型的消息");
    }

    /**
     * 测试最终结果处理
     * 验收标准：
     * - Given 用户正在进行语音录入
     * - When 阿里云ASR返回最终识别结果
     * - Then 后端将最终结果推送到前端，前端累加到病历内容
     */
    @Test
    @DisplayName("testOnEventFinalResult - 验证最终结果处理")
    void testOnEventFinalResult() throws Exception {
        // Given: 最终识别结果
        String finalText = "患者主诉头痛三天";

        // When: 推送最终结果
        handler.sendFinalResult("test-result-session", finalText);

        // Then: 验证消息被推送
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeast(2)).sendMessage(captor.capture());
        
        // 找到包含final的消息
        boolean foundFinal = captor.getAllValues().stream()
            .anyMatch(msg -> msg.getPayload().contains("\"type\":\"final\""));
        assertTrue(foundFinal, "应该发送final类型的消息");
    }

    /**
     * 测试识别完成回调
     * 验收标准：
     * - Given 用户停止语音识别
     * - When 阿里云ASR识别完成
     * - Then 后端发送完成消息到前端
     */
    @Test
    @DisplayName("testOnComplete - 验证识别完成回调")
    void testOnComplete() throws Exception {
        // Given: 连接已建立

        // When: 发送完成消息
        handler.sendComplete("test-result-session", 128);

        // Then: 验证消息被推送
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeast(2)).sendMessage(captor.capture());
        
        // 找到包含complete的消息
        boolean foundComplete = captor.getAllValues().stream()
            .anyMatch(msg -> msg.getPayload().contains("\"type\":\"complete\""));
        assertTrue(foundComplete, "应该发送complete类型的消息");
    }

    /**
     * 测试错误回调
     * 验收标准：
     * - Given WebSocket连接正常
     * - When 阿里云ASR服务返回错误
     * - Then 后端捕获异常并推送错误消息到前端
     */
    @Test
    @DisplayName("testOnError - 验证错误回调")
    void testOnError() throws Exception {
        // Given: 连接已建立

        // When: 发送错误消息
        handler.sendRecognitionError("test-result-session", "ASR服务异常");

        // Then: 验证错误消息被推送
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeast(2)).sendMessage(captor.capture());
        
        // 找到包含recognition_error的消息
        boolean foundError = captor.getAllValues().stream()
            .anyMatch(msg -> msg.getPayload().contains("\"type\":\"recognition_error\""));
        assertTrue(foundError, "应该发送recognition_error类型的消息");
    }

    /**
     * 测试消息格式符合JSON规范
     * 验收标准：
     * - Given 识别结果包含特殊字符
     * - When 构造JSON消息
     * - Then 消息格式正确，可以被解析
     */
    @Test
    @DisplayName("testResultMessageFormat - 验证消息格式符合JSON规范")
    void testResultMessageFormat() throws Exception {
        // Given: 包含特殊字符的识别结果
        String textWithSpecialChars = "患者说\"头痛\"，\n换行测试";

        // When: 推送结果
        handler.sendFinalResult("test-result-session", textWithSpecialChars);

        // Then: 验证JSON格式正确
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeast(2)).sendMessage(captor.capture());
        
        // 找到final消息并验证JSON格式
        for (TextMessage msg : captor.getAllValues()) {
            String payload = msg.getPayload();
            if (payload.contains("\"type\":\"final\"")) {
                // 验证可以被解析为JSON
                assertDoesNotThrow(() -> objectMapper.readTree(payload), 
                    "消息应该是有效的JSON格式");
                
                JsonNode root = objectMapper.readTree(payload);
                assertTrue(root.has("type"), "应该包含type字段");
                assertTrue(root.has("text"), "应该包含text字段");
                assertEquals("final", root.get("type").asText());
            }
        }
    }

    /**
     * 测试向无效Session发送结果
     */
    @Test
    @DisplayName("testSendResultToInvalidSession - 验证向无效Session发送结果的处理")
    void testSendResultToInvalidSession() throws Exception {
        // Given: 不存在的Session ID
        String invalidSessionId = "non-existent-session";

        // When & Then: 不应抛出异常
        assertDoesNotThrow(() -> handler.sendFinalResult(invalidSessionId, "测试文本"));
        assertDoesNotThrow(() -> handler.sendPartialResult(invalidSessionId, "测试文本"));
        assertDoesNotThrow(() -> handler.sendComplete(invalidSessionId, 0));
        assertDoesNotThrow(() -> handler.sendRecognitionError(invalidSessionId, "错误"));
    }
}
