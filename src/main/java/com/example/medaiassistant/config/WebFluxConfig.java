package com.example.medaiassistant.config;

import com.example.medaiassistant.converter.StringToAlertTaskStatusConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * 转换服务配置类
 * 注册自定义类型转换器
 * 
 * 注意: 本项目为Spring MVC应用，不能启用@EnableWebFlux
 * WebFlux依赖仅用于WebClient响应式HTTP客户端
 * 
 * @author Your Name
 * @version 1.1
 * @since 2025-08-06
 */
@Configuration
public class WebFluxConfig {
    
    /**
     * 创建并配置转换服务，注册自定义转换器
     * 
     * @return 配置好的ConversionService实例
     */
    @Bean
    public ConversionService conversionService() {
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(new StringToAlertTaskStatusConverter());
        return conversionService;
    }
}
