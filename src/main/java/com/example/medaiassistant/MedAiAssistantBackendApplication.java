package com.example.medaiassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MedAI助手后端应用程序主类
 * 
 * <p>这是Spring Boot应用程序的入口点，配置了应用程序的基本设置。</p>
 * 
 * <p>注解说明：</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} - 启用Spring Boot自动配置和组件扫描</li>
 *   <li>{@code @EnableScheduling} - 启用Spring的定时任务功能</li>
 *   <li>{@code @EnableJpaRepositories} - 显式启用JPA Repository扫描，排除执行服务器专用包</li>
 * </ul>
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-04
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(
    basePackages = {
        "com.example.medaiassistant.repository",
        "com.example.medaiassistant.hospital.repository"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.example\\.medaiassistant\\.repository\\.executionserver\\..*"
    )
)
public class MedAiAssistantBackendApplication {

	/**
	 * 应用程序主入口方法
	 * 
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(MedAiAssistantBackendApplication.class, args);
	}

}
