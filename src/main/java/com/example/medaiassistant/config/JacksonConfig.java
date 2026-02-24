package com.example.medaiassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson配置类，用于配置全局的ObjectMapper
 * 主要配置Java 8时间类型(LocalDateTime等)的序列化和反序列化
 * 解决LocalDateTime等时间类型序列化为数组格式的问题
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-23
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置全局的ObjectMapper实例
     * 注册JavaTimeModule以支持Java 8时间类型的序列化和反序列化
     * 设置默认日期格式为"yyyy-MM-dd HH:mm:ss"
     * 
     * @return ObjectMapper 配置好的ObjectMapper实例，支持Java 8时间类型
     * @see JavaTimeModule
     * @see Jackson2ObjectMapperBuilder
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .build();
    }
}
