package com.example.medaiassistant.config;

import com.example.medaiassistant.drg.catalog.ClobParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DRG相关配置类
 * 负责配置DRG模块的Spring Bean
 */
@Configuration
public class DrgConfig {

    /**
     * 配置ClobParser Bean
     * @return ClobParser实例
     */
    @Bean
    public ClobParser clobParser() {
        return new ClobParser();
    }
}
