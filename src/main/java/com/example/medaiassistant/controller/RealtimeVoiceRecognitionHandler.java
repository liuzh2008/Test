package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.VoiceRecognitionProperties;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实时语音识别WebSocket处理器
 * 
 * <p>处理WebSocket连接建立、音频数据接收和识别结果推送。
 * 该处理器作为前端与阿里云ASR服务之间的桥梁，实现医疗病历的语音实时转文字功能。</p>
 * 
 * <h3>功能特性</h3>
 * <ul>
 *   <li>WebSocket连接管理 - 建立、关闭、错误处理</li>
 *   <li>音频数据接收 - 支持PCM格式，最大1MB/帧</li>
 *   <li>识别结果推送 - partial/final/complete/error消息类型</li>
 *   <li>并发连接支持 - 使用ConcurrentHashMap管理多个Session</li>
 *   <li>缓冲区溢出保护 - 超大音频帧自动截断</li>
 * </ul>
 * 
 * <h3>WebSocket端点</h3>
 * <pre>
 * ws://localhost:8081/api/voice/realtime
 * wss://your-domain.com/api/voice/realtime (生产环境)
 * </pre>
 * 
 * <h3>消息类型</h3>
 * <ul>
 *   <li>{@code ready} - 连接就绪，可以开始发送音频</li>
 *   <li>{@code partial} - 临时识别结果（实时更新）</li>
 *   <li>{@code final} - 最终识别结果（句子结束）</li>
 *   <li>{@code complete} - 识别完成</li>
 *   <li>{@code error} - 连接错误</li>
 *   <li>{@code recognition_error} - 识别服务错误</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 前端建立连接
 * const ws = new WebSocket('ws://localhost:8081/api/voice/realtime');
 * 
 * // 发送PCM音频数据
 * ws.send(pcmData.buffer);
 * 
 * // 接收识别结果
 * ws.onmessage = (event) => {
 *   const data = JSON.parse(event.data);
 *   if (data.type === 'final') {
 *     recordContent += data.text;
 *   }
 * };
 * }</pre>
 * 
 * @author System
 * @version 1.1
 * @since 2026-02-22
 * @see com.example.medaiassistant.config.WebSocketConfig
 * @see com.example.medaiassistant.config.VoiceRecognitionProperties
 */
@Component
public class RealtimeVoiceRecognitionHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeVoiceRecognitionHandler.class);

    /**
     * 最大允许的音频帧大小（1MB）
     */
    private static final int MAX_AUDIO_FRAME_SIZE = 1024 * 1024;

    private final VoiceRecognitionProperties voiceRecognitionProperties;
    private final ObjectMapper objectMapper;

    /**
     * 存储活跃的WebSocket Session映射
     */
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 存储每个Session对应的ASR识别器实例
     */
    private final Map<String, Recognition> activeRecognizers = new ConcurrentHashMap<>();

    /**
     * 音频帧计数器（每个Session）
     */
    private final Map<String, AtomicInteger> audioFrameCounters = new ConcurrentHashMap<>();

    /**
     * 音频字节计数器（每个Session）
     */
    private final Map<String, AtomicLong> audioBytesCounters = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * 
     * @param voiceRecognitionProperties 语音识别配置属性，包含API Key等配置
     */
    public RealtimeVoiceRecognitionHandler(VoiceRecognitionProperties voiceRecognitionProperties) {
        this.voiceRecognitionProperties = voiceRecognitionProperties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * WebSocket连接建立后的处理
     * 
     * <p>执行以下操作：</p>
     * <ol>
     *   <li>验证API Key配置是否有效</li>
     *   <li>注册活跃Session到映射表</li>
     *   <li>初始化音频帧和字节计数器</li>
     *   <li>启动阿里云ASR识别器</li>
     *   <li>发送就绪消息通知前端</li>
     * </ol>
     * 
     * @param session WebSocket会话对象
     * @throws Exception 如果发送消息失败
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket连接已建立 - Session ID: {}", sessionId);

        // 验证API Key
        String apiKey = voiceRecognitionProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("API Key未配置，拒绝连接 - Session ID: {}", sessionId);
            sendErrorMessage(session, "API Key未配置");
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        // 注册活跃Session
        activeSessions.put(sessionId, session);
        audioFrameCounters.put(sessionId, new AtomicInteger(0));
        audioBytesCounters.put(sessionId, new AtomicLong(0));

        // 启动ASR识别器
        try {
            startAsrRecognizer(sessionId, apiKey);
        } catch (Exception e) {
            logger.error("启动ASR识别器失败 - Session ID: {}, Error: {}", sessionId, e.getMessage(), e);
            sendErrorMessage(session, "语音识别服务启动失败: " + e.getMessage());
            cleanupSession(sessionId);
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        
        // 发送就绪消息
        sendReadyMessage(session);
        logger.info("实时语音识别服务就绪 - Session ID: {}", sessionId);
    }

    /**
     * 启动阿里云ASR识别器
     * 
     * @param sessionId WebSocket会话ID
     * @param apiKey 阿里云API Key
     */
    private void startAsrRecognizer(String sessionId, String apiKey) {
        RecognitionParam param = RecognitionParam.builder()
                .model(voiceRecognitionProperties.getModel())
                .format(voiceRecognitionProperties.getFormat())
                .sampleRate(voiceRecognitionProperties.getSampleRate())
                .apiKey(apiKey)
                .build();

        Recognition recognizer = new Recognition();
        activeRecognizers.put(sessionId, recognizer);

        ResultCallback<RecognitionResult> callback = new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                try {
                    if (result.getSentence() == null) {
                        return;
                    }
                    String text = result.getSentence().getText();
                    if (text == null || text.isEmpty()) {
                        return;
                    }
                    if (result.isSentenceEnd()) {
                        logger.info("ASR最终结果 - Session ID: {}, Text: {}", sessionId, text);
                        sendFinalResult(sessionId, text);
                    } else {
                        logger.debug("ASR临时结果 - Session ID: {}, Text: {}", sessionId, text);
                        sendPartialResult(sessionId, text);
                    }
                } catch (Exception e) {
                    logger.error("处理ASR结果异常 - Session ID: {}, Error: {}", sessionId, e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                logger.info("ASR识别完成 - Session ID: {}", sessionId);
                AtomicLong bytesCounter = audioBytesCounters.get(sessionId);
                long totalBytes = bytesCounter != null ? bytesCounter.get() : 0;
                sendComplete(sessionId, (int) (totalBytes / 32));
            }

            @Override
            public void onError(Exception e) {
                logger.error("ASR识别错误 - Session ID: {}, Error: {}", sessionId, e.getMessage(), e);
                sendRecognitionError(sessionId, e.getMessage());
            }
        };

        recognizer.call(param, callback);
        logger.info("ASR识别器已启动 - Session ID: {}, Model: {}, SampleRate: {}, Format: {}",
                sessionId, voiceRecognitionProperties.getModel(),
                voiceRecognitionProperties.getSampleRate(),
                voiceRecognitionProperties.getFormat());
    }

    /**
     * WebSocket连接关闭后的处理
     * 
     * <p>清理Session相关的所有资源，包括从活跃映射表中移除、
     * 清理计数器等。</p>
     * 
     * @param session WebSocket会话对象
     * @param status 关闭状态码，1000表示正常关闭
     * @throws Exception 如果清理过程中发生错误
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket连接已关闭 - Session ID: {}, Status: {}", sessionId, status);
        
        // 清理资源
        cleanupSession(sessionId);
    }

    /**
     * 处理传输错误
     * 
     * <p>当WebSocket传输过程中发生异常时调用，自动清理相关资源。</p>
     * 
     * @param session WebSocket会话对象
     * @param exception 发生的异常
     * @throws Exception 如果清理过程中发生错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket传输错误 - Session ID: {}, Error: {}", sessionId, exception.getMessage());
        
        // 清理资源
        cleanupSession(sessionId);
    }

    /**
     * 处理二进制消息（音频数据）
     * 
     * <p>接收前端发送的PCM音频数据，执行以下处理：</p>
     * <ol>
     *   <li>验证Session是否已注册</li>
     *   <li>验证音频数据有效性（非空）</li>
     *   <li>检查并处理超大音频帧（超过1MB截断）</li>
     *   <li>更新音频帧和字节统计计数器</li>
     *   <li>转发音频数据到阿里云ASR服务</li>
     * </ol>
     * 
     * @param session WebSocket会话对象
     * @param message 包含PCM音频数据的二进制消息
     * @throws Exception 如果处理过程中发生错误
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String sessionId = session.getId();
        
        if (!activeSessions.containsKey(sessionId)) {
            logger.warn("收到来自未注册Session的消息 - Session ID: {}", sessionId);
            return;
        }

        byte[] audioData = message.getPayload().array();
        int dataSize = audioData.length;

        // 验证音频数据有效性
        if (dataSize == 0) {
            logger.warn("收到空音频数据，忽略 - Session ID: {}", sessionId);
            return;
        }

        // 检查音频帧大小，防止缓冲区溢出
        if (dataSize > MAX_AUDIO_FRAME_SIZE) {
            logger.warn("音频帧过大，截断处理 - Session ID: {}, 原始大小: {} bytes, 最大允许: {} bytes", 
                sessionId, dataSize, MAX_AUDIO_FRAME_SIZE);
            byte[] truncated = new byte[MAX_AUDIO_FRAME_SIZE];
            System.arraycopy(audioData, 0, truncated, 0, MAX_AUDIO_FRAME_SIZE);
            audioData = truncated;
            dataSize = MAX_AUDIO_FRAME_SIZE;
        }

        // 更新统计计数器
        AtomicInteger frameCounter = audioFrameCounters.get(sessionId);
        AtomicLong bytesCounter = audioBytesCounters.get(sessionId);
        if (frameCounter != null) {
            frameCounter.incrementAndGet();
        }
        if (bytesCounter != null) {
            bytesCounter.addAndGet(dataSize);
        }

        logger.debug("收到音频数据 - Session ID: {}, 大小: {} bytes, 总帧数: {}", 
            sessionId, dataSize, frameCounter != null ? frameCounter.get() : 0);
        
        // 转发音频数据到阿里云ASR服务
        Recognition recognizer = activeRecognizers.get(sessionId);
        if (recognizer != null) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(audioData, 0, dataSize);
                recognizer.sendAudioFrame(buffer);
            } catch (Exception e) {
                logger.error("发送音频帧到ASR失败 - Session ID: {}, Error: {}", sessionId, e.getMessage());
                sendRecognitionError(sessionId, "音频数据发送失败: " + e.getMessage());
            }
        } else {
            logger.warn("ASR识别器不存在 - Session ID: {}", sessionId);
        }
    }

    /**
     * 检查是否存在活跃Session
     * 
     * @param sessionId WebSocket会话ID
     * @return 如果Session存在且活跃返回true，否则返回false
     */
    public boolean hasActiveSession(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * 获取活跃Session数量
     * 
     * @return 当前活跃的WebSocket连接数
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 获取指定Session接收到的音频帧数量
     * 
     * @param sessionId WebSocket会话ID
     * @return 接收到的音频帧数量，Session不存在时返回0
     */
    public int getReceivedAudioFrameCount(String sessionId) {
        AtomicInteger counter = audioFrameCounters.get(sessionId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指定Session接收到的音频总字节数
     * 
     * @param sessionId WebSocket会话ID
     * @return 接收到的音频总字节数，Session不存在时返回0
     */
    public long getTotalAudioBytesReceived(String sessionId) {
        AtomicLong counter = audioBytesCounters.get(sessionId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 发送临时识别结果到指定Session
     * 
     * <p>用于推送ASR服务返回的实时临时识别结果，前端可用于实时显示。</p>
     * 
     * @param sessionId WebSocket会话ID
     * @param text 临时识别的文本内容
     */
    public void sendPartialResult(String sessionId, String text) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("无法发送临时结果，Session不存在或已关闭 - Session ID: {}", sessionId);
            return;
        }
        
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "partial");
            node.put("text", text);
            String message = objectMapper.writeValueAsString(node);
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
            logger.debug("已发送临时结果 - Session ID: {}, Text: {}", sessionId, text);
        } catch (IOException e) {
            logger.error("发送临时结果失败 - Session ID: {}, Error: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 发送最终识别结果到指定Session
     * 
     * <p>用于推送ASR服务返回的最终识别结果（句子结束），
     * 前端应将此结果累加到病历内容。</p>
     * 
     * @param sessionId WebSocket会话ID
     * @param text 最终识别的文本内容
     */
    public void sendFinalResult(String sessionId, String text) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("无法发送最终结果，Session不存在或已关闭 - Session ID: {}", sessionId);
            return;
        }
        
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "final");
            node.put("text", text);
            String message = objectMapper.writeValueAsString(node);
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
            logger.debug("已发送最终结果 - Session ID: {}, Text: {}", sessionId, text);
        } catch (IOException e) {
            logger.error("发送最终结果失败 - Session ID: {}, Error: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 发送识别完成消息到指定Session
     * 
     * <p>用于通知前端识别过程已完成，包含本次识别的总字符数统计。</p>
     * 
     * @param sessionId WebSocket会话ID
     * @param totalCharacters 本次识别的总字符数
     */
    public void sendComplete(String sessionId, int totalCharacters) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("无法发送完成消息，Session不存在或已关闭 - Session ID: {}", sessionId);
            return;
        }
        
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "complete");
            node.put("message", String.format("识别完成，共识别 %d 个字符", totalCharacters));
            node.put("totalCharacters", totalCharacters);
            String message = objectMapper.writeValueAsString(node);
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
            logger.info("已发送完成消息 - Session ID: {}, 总字符数: {}", sessionId, totalCharacters);
        } catch (IOException e) {
            logger.error("发送完成消息失败 - Session ID: {}, Error: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 发送识别错误消息到指定Session
     * 
     * <p>用于通知前端ASR服务发生错误，前端应显示错误提示。</p>
     * 
     * @param sessionId WebSocket会话ID
     * @param errorMessage 错误描述信息
     */
    public void sendRecognitionError(String sessionId, String errorMessage) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("无法发送错误消息，Session不存在或已关闭 - Session ID: {}", sessionId);
            return;
        }
        
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "recognition_error");
            node.put("message", errorMessage);
            String message = objectMapper.writeValueAsString(node);
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
            logger.error("已发送识别错误消息 - Session ID: {}, Error: {}", sessionId, errorMessage);
        } catch (IOException e) {
            logger.error("发送识别错误消息失败 - Session ID: {}, Error: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 清理指定Session的资源
     * 
     * <p>从活跃映射表中移除Session，清理音频帧和字节计数器，
     * 停止对应的ASR识别器。</p>
     * 
     * @param sessionId 要清理的WebSocket会话ID
     */
    private void cleanupSession(String sessionId) {
        WebSocketSession removed = activeSessions.remove(sessionId);
        audioFrameCounters.remove(sessionId);
        audioBytesCounters.remove(sessionId);

        // 停止ASR识别器
        Recognition recognizer = activeRecognizers.remove(sessionId);
        if (recognizer != null) {
            try {
                recognizer.stop();
                logger.info("ASR识别器已停止 - Session ID: {}", sessionId);
            } catch (Exception e) {
                logger.warn("停止ASR识别器异常 - Session ID: {}, Error: {}", sessionId, e.getMessage());
            }
        }

        if (removed != null) {
            logger.info("已清理Session资源 - Session ID: {}", sessionId);
        }
    }

    /**
     * 发送就绪消息
     * 
     * <p>通知前端WebSocket连接已就绪，可以开始发送音频数据。</p>
     * 
     * @param session WebSocket会话对象
     * @throws IOException 如果发送消息失败
     */
    private void sendReadyMessage(WebSocketSession session) throws IOException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "ready");
        node.put("message", "实时语音识别服务已就绪");
        String message = objectMapper.writeValueAsString(node);
        session.sendMessage(new TextMessage(message));
    }

    /**
     * 发送错误消息
     * 
     * <p>通知前端连接级别的错误（如API Key未配置）。</p>
     * 
     * @param session WebSocket会话对象
     * @param errorMessage 错误描述信息
     * @throws IOException 如果发送消息失败
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "error");
        node.put("message", errorMessage);
        String message = objectMapper.writeValueAsString(node);
        session.sendMessage(new TextMessage(message));
    }
}
