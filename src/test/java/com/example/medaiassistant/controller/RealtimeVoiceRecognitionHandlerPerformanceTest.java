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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealtimeVoiceRecognitionHandler 轻量级性能测试
 * 验证核心性能指标符合要求
 * 
 * 性能基准：
 * - 连接建立: < 100ms
 * - 单帧音频处理: < 10ms
 * - 消息推送: < 50ms
 * - 并发连接支持: >= 10个
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("实时语音识别Handler - 轻量级性能测试")
class RealtimeVoiceRecognitionHandlerPerformanceTest {

    @Mock
    private VoiceRecognitionProperties voiceRecognitionProperties;

    private RealtimeVoiceRecognitionHandler handler;

    private static final int CONCURRENT_CONNECTIONS = 10;
    private static final int AUDIO_FRAMES_PER_SESSION = 100;
    private static final long CONNECTION_TIMEOUT_MS = 100;
    private static final long AUDIO_FRAME_TIMEOUT_MS = 10;
    private static final long MESSAGE_PUSH_TIMEOUT_MS = 50;

    @BeforeEach
    void setUp() {
        handler = new RealtimeVoiceRecognitionHandler(voiceRecognitionProperties);
        when(voiceRecognitionProperties.getApiKey()).thenReturn("sk-performance-test-api-key");
    }

    /**
     * 测试连接建立性能
     * 验收标准：连接建立时间 < 100ms
     */
    @Test
    @DisplayName("性能测试 - 连接建立时间 < 100ms")
    void testConnectionEstablishmentPerformance() throws Exception {
        WebSocketSession mockSession = createMockSession("perf-session-1");

        long startTime = System.nanoTime();
        handler.afterConnectionEstablished(mockSession);
        long endTime = System.nanoTime();

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        assertTrue(handler.hasActiveSession("perf-session-1"), "Session应该成功建立");
        assertTrue(elapsedMs < CONNECTION_TIMEOUT_MS, 
            String.format("连接建立时间应 < %dms, 实际: %dms", CONNECTION_TIMEOUT_MS, elapsedMs));
        
        System.out.printf("[性能] 连接建立时间: %dms (目标: <%dms) - PASS%n", elapsedMs, CONNECTION_TIMEOUT_MS);
    }

    /**
     * 测试单帧音频处理性能
     * 验收标准：单帧处理时间 < 10ms
     */
    @Test
    @DisplayName("性能测试 - 单帧音频处理 < 10ms")
    void testSingleAudioFrameProcessingPerformance() throws Exception {
        WebSocketSession mockSession = createMockSession("perf-session-2");
        handler.afterConnectionEstablished(mockSession);

        byte[] audioData = createPCMData(4096);
        BinaryMessage message = new BinaryMessage(ByteBuffer.wrap(audioData));

        long startTime = System.nanoTime();
        handler.handleBinaryMessage(mockSession, message);
        long endTime = System.nanoTime();

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        assertEquals(1, handler.getReceivedAudioFrameCount("perf-session-2"));
        assertTrue(elapsedMs < AUDIO_FRAME_TIMEOUT_MS, 
            String.format("单帧处理时间应 < %dms, 实际: %dms", AUDIO_FRAME_TIMEOUT_MS, elapsedMs));
        
        System.out.printf("[性能] 单帧音频处理时间: %dms (目标: <%dms) - PASS%n", elapsedMs, AUDIO_FRAME_TIMEOUT_MS);
    }

    /**
     * 测试批量音频帧处理吞吐量
     * 验收标准：100帧处理平均时间 < 10ms/帧
     */
    @Test
    @DisplayName("性能测试 - 批量音频处理吞吐量")
    void testBatchAudioFrameProcessingThroughput() throws Exception {
        WebSocketSession mockSession = createMockSession("perf-session-3");
        handler.afterConnectionEstablished(mockSession);

        byte[] audioData = createPCMData(4096);
        List<BinaryMessage> messages = new ArrayList<>();
        for (int i = 0; i < AUDIO_FRAMES_PER_SESSION; i++) {
            messages.add(new BinaryMessage(ByteBuffer.wrap(audioData)));
        }

        long startTime = System.nanoTime();
        for (BinaryMessage message : messages) {
            handler.handleBinaryMessage(mockSession, message);
        }
        long endTime = System.nanoTime();

        long totalMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        double avgMs = (double) totalMs / AUDIO_FRAMES_PER_SESSION;
        
        assertEquals(AUDIO_FRAMES_PER_SESSION, handler.getReceivedAudioFrameCount("perf-session-3"));
        assertTrue(avgMs < AUDIO_FRAME_TIMEOUT_MS, 
            String.format("平均帧处理时间应 < %dms, 实际: %.2fms", AUDIO_FRAME_TIMEOUT_MS, avgMs));
        
        System.out.printf("[性能] 批量处理 %d 帧, 总时间: %dms, 平均: %.2fms/帧 (目标: <%.0fms) - PASS%n", 
            AUDIO_FRAMES_PER_SESSION, totalMs, avgMs, (double) AUDIO_FRAME_TIMEOUT_MS);
    }

    /**
     * 测试消息推送性能
     * 验收标准：消息推送时间 < 50ms
     */
    @Test
    @DisplayName("性能测试 - 消息推送时间 < 50ms")
    void testMessagePushPerformance() throws Exception {
        WebSocketSession mockSession = createMockSession("perf-session-4");
        when(mockSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(mockSession);

        long startTime = System.nanoTime();
        handler.sendFinalResult("perf-session-4", "性能测试文本");
        long endTime = System.nanoTime();

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        assertTrue(elapsedMs < MESSAGE_PUSH_TIMEOUT_MS, 
            String.format("消息推送时间应 < %dms, 实际: %dms", MESSAGE_PUSH_TIMEOUT_MS, elapsedMs));
        
        System.out.printf("[性能] 消息推送时间: %dms (目标: <%dms) - PASS%n", elapsedMs, MESSAGE_PUSH_TIMEOUT_MS);
    }

    /**
     * 测试并发连接处理能力
     * 验收标准：支持至少10个并发连接
     */
    @Test
    @DisplayName("性能测试 - 并发连接支持 >= 10个")
    void testConcurrentConnectionCapacity() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CONNECTIONS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_CONNECTIONS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.nanoTime();
        
        for (int i = 0; i < CONCURRENT_CONNECTIONS; i++) {
            final int sessionIndex = i;
            executor.submit(() -> {
                try {
                    WebSocketSession mockSession = createMockSession("concurrent-session-" + sessionIndex);
                    handler.afterConnectionEstablished(mockSession);
                    
                    // 发送几帧音频数据
                    byte[] audioData = createPCMData(4096);
                    for (int j = 0; j < 10; j++) {
                        handler.handleBinaryMessage(mockSession, new BinaryMessage(ByteBuffer.wrap(audioData)));
                    }
                    
                    if (handler.hasActiveSession("concurrent-session-" + sessionIndex)) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        long endTime = System.nanoTime();

        long totalMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        assertTrue(completed, "并发测试应在5秒内完成");
        assertEquals(CONCURRENT_CONNECTIONS, successCount.get(), 
            String.format("应支持 %d 个并发连接, 成功: %d, 失败: %d", 
                CONCURRENT_CONNECTIONS, successCount.get(), failCount.get()));
        assertEquals(CONCURRENT_CONNECTIONS, handler.getActiveSessionCount(), 
            "活跃Session数量应等于并发连接数");
        
        System.out.printf("[性能] 并发连接: %d/%d 成功, 总时间: %dms - PASS%n", 
            successCount.get(), CONCURRENT_CONNECTIONS, totalMs);
    }

    /**
     * 测试内存效率 - 大量音频数据不导致内存泄漏
     * 验收标准：处理1000帧后内存增长 < 50MB
     */
    @Test
    @DisplayName("性能测试 - 内存效率检查")
    void testMemoryEfficiency() throws Exception {
        WebSocketSession mockSession = createMockSession("memory-test-session");
        handler.afterConnectionEstablished(mockSession);

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        byte[] audioData = createPCMData(4096);
        int totalFrames = 1000;
        
        for (int i = 0; i < totalFrames; i++) {
            handler.handleBinaryMessage(mockSession, new BinaryMessage(ByteBuffer.wrap(audioData)));
        }

        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncreaseMB = (memoryAfter - memoryBefore) / (1024 * 1024);

        assertEquals(totalFrames, handler.getReceivedAudioFrameCount("memory-test-session"));
        assertTrue(memoryIncreaseMB < 50, 
            String.format("内存增长应 < 50MB, 实际: %dMB", memoryIncreaseMB));
        
        System.out.printf("[性能] 处理 %d 帧后内存增长: %dMB (目标: <50MB) - PASS%n", totalFrames, memoryIncreaseMB);
    }

    private WebSocketSession createMockSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private byte[] createPCMData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((Math.sin(i * 0.1) * 127));
        }
        return data;
    }
}
