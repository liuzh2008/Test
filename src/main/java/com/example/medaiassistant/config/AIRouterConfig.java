package com.example.medaiassistant.config;

import com.example.medaiassistant.controller.AIResponseController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * AI路由配置，专门处理AI相关的WebFlux路由
 * 这样可以在WebMVC应用中单独使用WebFlux功能
 */
@Configuration
public class AIRouterConfig {

    private final AIResponseController aiResponseController;

    public AIRouterConfig(AIResponseController aiResponseController) {
        this.aiResponseController = aiResponseController;
    }

    @Bean
    public RouterFunction<ServerResponse> aiRouterFunction() {
        return RouterFunctions.route()
            .POST("/api/ai/response", request -> 
                request.bodyToMono(com.example.medaiassistant.dto.AIRequest.class)
                    .flatMap(aiRequest -> 
                        ServerResponse.ok()
                            .body(aiResponseController.getAIResponse(aiRequest), String.class)
                    )
            )
            .build();
    }
}
