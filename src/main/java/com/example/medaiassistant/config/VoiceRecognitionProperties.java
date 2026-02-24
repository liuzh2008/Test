package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语音识别配置属性
 * 
 * @author System
 * @version 1.0
 * @since 2026-02-22
 */
@Component
@ConfigurationProperties(prefix = "voice.recognition")
public class VoiceRecognitionProperties {

    /**
     * 阿里云百炼ASR API Key
     */
    private String apiKey;

    /**
     * ASR模型名称
     */
    private String model = "paraformer-realtime-v2";

    /**
     * 采样率
     */
    private int sampleRate = 16000;

    /**
     * 音频格式
     */
    private String format = "pcm";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
