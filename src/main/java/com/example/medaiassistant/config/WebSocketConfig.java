package com.example.medaiassistant.config;

import com.example.medaiassistant.controller.RealtimeVoiceRecognitionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置实时语音识别WebSocket端点
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeVoiceRecognitionHandler realtimeVoiceRecognitionHandler;

    public WebSocketConfig(RealtimeVoiceRecognitionHandler realtimeVoiceRecognitionHandler) {
        this.realtimeVoiceRecognitionHandler = realtimeVoiceRecognitionHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeVoiceRecognitionHandler, "/api/voice/realtime")
                .setAllowedOrigins("*");
    }
}
