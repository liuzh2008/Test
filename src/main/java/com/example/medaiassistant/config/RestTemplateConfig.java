package com.example.medaiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 配置RestTemplate bean，用于HTTP请求
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate bean
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}