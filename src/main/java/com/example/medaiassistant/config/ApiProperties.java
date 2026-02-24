package com.example.medaiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API配置属性类
 * 用于统一管理API相关的配置
 */
@Component
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    /**
     * API基础URL
     */
    private Base base = new Base();

    // Getters and Setters

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
    }

    /**
     * Base配置内部类
     */
    public static class Base {
        /**
         * API基础URL
         */
        private String url = "http://localhost:8081";

        /**
         * 连接超时时间（毫秒）
         */
        private Integer timeoutConnect = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private Integer timeoutRead = 30000;

        /**
         * 重试配置
         */
        private Retry retry = new Retry();

        // Getters and Setters

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getTimeoutConnect() {
            return timeoutConnect;
        }

        public void setTimeoutConnect(Integer timeoutConnect) {
            this.timeoutConnect = timeoutConnect;
        }

        public Integer getTimeoutRead() {
            return timeoutRead;
        }

        public void setTimeoutRead(Integer timeoutRead) {
            this.timeoutRead = timeoutRead;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }
    }

    /**
     * 重试配置内部类
     */
    public static class Retry {
        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 2;

        // Getters and Setters

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
