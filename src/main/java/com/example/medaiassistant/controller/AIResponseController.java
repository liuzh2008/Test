package com.example.medaiassistant.controller;

import com.example.medaiassistant.config.AIModelConfig;
import com.example.medaiassistant.constant.AIDisclaimerConstants;
import com.example.medaiassistant.dto.AIRequest;
import com.example.medaiassistant.util.RetryUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import java.time.Duration;

@RestController
@RequestMapping("/api/ai")
public class AIResponseController {
    private static final Logger logger = LoggerFactory.getLogger(AIResponseController.class);

    // 超时配置 - 增加超时时间以支持长时间的LLM处理
    private static final int READ_TIMEOUT = 300000; // 300秒读取超时（5分钟）
    private static final int CONNECT_TIMEOUT = 30000; // 30秒连接超时
    private static final int MAX_RETRIES = 3; // 最大重试次数

    private final WebClient webClient;
    private final AIModelConfig aiModelConfig;

    /**
     * AI响应控制器构造函数
     * 
     * @param webClientBuilder WebClient构建器，用于创建HTTP客户端
     * @param aiModelConfig AI模型配置管理器，包含所有AI模型的配置信息
     * @param httpClient 优化的HttpClient实例，包含连接池和DNS优化配置
     * 
     * @description
     * 初始化AI响应控制器，使用优化的HttpClient配置解决网络中断后连接失败问题。
     * 通过连接池管理、DNS优化和响应式重试机制，显著提升系统在网络波动下的可用性。
     * 
     * @configuration
     * - 响应超时: 300秒 (5分钟)
     * - 连接超时: 30秒
     * - DNS查询超时: 30秒
     * - 最大重试次数: 3次
     * - 连接池: 最大100连接，5分钟空闲超时
     * - DNS缓存: 1-5分钟TTL，30秒负缓存
     * 
     * @problem
     * 解决网络中断后偶发连接失败问题，避免必须重启才能恢复连接
     * 
     * @solution
     * 1. 响应式重试机制：在响应式链路中集成retryWhen
     * 2. DNS与连接池优化：配置连接池和DNS缓存策略
     * 3. 自动恢复机制：基于失败计数的客户端自动重建
     * 
     * @example
     * 配置示例：
     * HttpClient.create(connectionProvider)
     *   .responseTimeout(Duration.ofSeconds(300))
     *   .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
     *   .resolver(spec -> spec.queryTimeout(Duration.ofSeconds(30)))
     */
    public AIResponseController(WebClient.Builder webClientBuilder, AIModelConfig aiModelConfig, 
                               reactor.netty.http.client.HttpClient httpClient) {
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.aiModelConfig = aiModelConfig;
        
        logger.info("AIResponseController initialized with optimized HttpClient configuration");
        logger.info("  - Response timeout: {}ms", READ_TIMEOUT);
        logger.info("  - Connect timeout: {}ms", CONNECT_TIMEOUT);
        logger.info("  - Max retries: {}", MAX_RETRIES);
        logger.info("  - Response retry enabled with exponential backoff");
    }

    @PostMapping(value = "/response", produces = { MediaType.APPLICATION_NDJSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    public Flux<String> getAIResponse(@Valid @RequestBody AIRequest request) {
        String modelName = request.getModel();

        logger.info("Received AI request - model: {}, stream: {}, temperature: {}, maxTokens: {}",
                modelName, request.isStream(), request.getTemperature(), request.getMaxTokens());
        logger.debug("Full AI request details: {}", request.toString());

        // 前端处理要求：
        // 1. 流式响应(content-type=application/x-ndjson):
        // - 每行是一个完整的JSON对象
        // - 最后一行是[DONE]
        // - 错误时会返回错误JSON对象
        // 2. 非流式响应(content-type=application/json):
        // - 直接返回完整JSON响应
        // - 错误时status code不为200

        // 获取模型配置
        AIModelConfig.ModelConfig modelConfig = aiModelConfig.getModelConfig(modelName);
        logger.info("Requesting model: {}, found config: {}", modelName, modelConfig != null);
        if (modelConfig == null) {
            return Flux.just("{\"error\":\"Unsupported model: " + modelName + "\"}");
        }

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(modelConfig.getKey());

        // 构建完整的请求体，包含所有AI参数
        Map<String, Object> requestBody = new HashMap<>();
        /**
         * 模型名称映射逻辑
         * 对于inHospitalDeepseek模型，使用配置的URL和API密钥，但发送有效的模型名称到AI服务
         * 这样可以在保持独立配置的同时，使用兼容的模型名称
         * 
         * @param modelName 前端传入的模型名称
         * @return 有效的AI服务模型名称
         */
        String effectiveModelName = modelName.equals("inHospitalDeepseek") ? "deepseek-chat" : modelName;
        requestBody.put("model", effectiveModelName);
        requestBody.put("messages", request.getMessages());
        requestBody.put("stream", request.isStream());

        // 确保所有参数都有值，避免发送null
        if (request.getTemperature() != null) {
            requestBody.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            requestBody.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            requestBody.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            requestBody.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            requestBody.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getN() != null) {
            requestBody.put("n", request.getN());
        }

        // 可选参数
        if (request.getLogprobs() != null) {
            requestBody.put("logprobs", request.getLogprobs());
        }
        if (request.getLogprobsTopLogprobs() != null && request.getLogprobsTopLogprobs() > 0) {
            requestBody.put("logprobs_top_logprobs", request.getLogprobsTopLogprobs());
        }
        if (request.getEcho() != null) {
            requestBody.put("echo", request.getEcho());
        }
        // 移除stop参数，防止LLM提前停止
        // if (request.getStop() != null && !request.getStop().isEmpty()) {
        //     requestBody.put("stop", request.getStop());
        // }
        if (request.getUser() != null && !request.getUser().trim().isEmpty()) {
            requestBody.put("user", request.getUser().trim());
        }

        // 构建请求实体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // 带重试机制的请求处理
        try {
            return executeWithRetry(modelConfig, requestEntity, request.isStream());
        } catch (Exception e) {
            logger.error("Unexpected error in getAIResponse", e);
            return Flux.just("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private Flux<String> executeWithRetry(AIModelConfig.ModelConfig modelConfig,
            HttpEntity<Map<String, Object>> requestEntity,
            boolean isStream) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                logger.info("Sending request to AI service (attempt {}): url={}, stream={}",
                        retryCount + 1, modelConfig.getUrl(), isStream);

                if (isStream) {
                    return handleStreamResponse(modelConfig, requestEntity);
                } else {
                    return handleNonStreamResponse(modelConfig, requestEntity);
                }

            } catch (HttpClientErrorException e) {
                logger.error("HTTP client error (attempt {}): status={}, message={}",
                        retryCount + 1, e.getStatusCode(), e.getMessage());

                // 根据不同的HTTP状态码返回不同的错误信息
                String errorMessage;
                switch (e.getStatusCode().value()) {
                    case 400:
                        errorMessage = "Bad request - invalid parameters";
                        break;
                    case 401:
                        errorMessage = "Unauthorized - invalid API key";
                        break;
                    case 403:
                        errorMessage = "Forbidden - insufficient permissions";
                        break;
                    case 429:
                        errorMessage = "Rate limit exceeded - please try again later";
                        break;
                    default:
                        errorMessage = "AI service client error: " + e.getStatusText();
                }

                return Flux.just("{\"error\":\"" + errorMessage + "\"}");

            } catch (HttpServerErrorException e) {
                logger.error("HTTP server error (attempt {}): status={}, message={}",
                        retryCount + 1, e.getStatusCode(), e.getMessage());
                lastException = e;
                retryCount++;

                if (retryCount < MAX_RETRIES) {
                    try {
                        // 指数退避重试
                        long delay = (long) Math.pow(2, retryCount) * 1000;
                        logger.info("Retrying in {} ms...", delay);
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Flux.just("{\"error\":\"Request interrupted\"}");
                    }
                }

            } catch (ResourceAccessException e) {
                logger.error("Resource access error (attempt {}): {}", retryCount + 1, e.getMessage());
                lastException = e;
                retryCount++;

                if (retryCount < MAX_RETRIES) {
                    try {
                        long delay = (long) Math.pow(2, retryCount) * 1000;
                        logger.info("Retrying in {} ms...", delay);
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Flux.just("{\"error\":\"Request interrupted\"}");
                    }
                }

            } catch (RestClientException e) {
                logger.error("Rest client error (attempt {}): {}", retryCount + 1, e.getMessage());
                lastException = e;
                retryCount++;

                if (retryCount < MAX_RETRIES) {
                    try {
                        long delay = (long) Math.pow(2, retryCount) * 1000;
                        logger.info("Retrying in {} ms...", delay);
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Flux.just("{\"error\":\"Request interrupted\"}");
                    }
                }

            } catch (Exception e) {
                logger.error("Unexpected error (attempt {}). Request details - URL: {}, Headers: {}, Body: {}. Error: ",
                        retryCount + 1, modelConfig.getUrl(), requestEntity.getHeaders(),
                        requestEntity.getBody(), e);
                return Flux.just("{\"error\":\"Internal server error\",\"message\":\"" +
                        e.getMessage() + "\",\"code\":\"INTERNAL_ERROR\"}");
            }
        }

        // 所有重试都失败了
        String errorMessage = lastException != null ? lastException.getMessage() : "Unknown error";
        logger.error("All retry attempts failed for AI service request");
        return Flux.just(
                "{\"error\":\"AI service unavailable after " + MAX_RETRIES + " attempts: " + errorMessage + "\"}");
    }

    private Flux<String> handleStreamResponse(AIModelConfig.ModelConfig modelConfig,
            HttpEntity<Map<String, Object>> requestEntity) {
        try {
            // 创建StringBuilder来累积内容
            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder reasoningBuilder = new StringBuilder();

            Flux<String> responseFlux = webClient.post()
                    .uri(modelConfig.getUrl())
                    .headers(headers -> headers.putAll(requestEntity.getHeaders()))
                    .bodyValue(Objects.requireNonNullElse(requestEntity.getBody(), new HashMap<>()))
                    .exchangeToFlux(response -> {
                        if (!response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("No response body")
                                    .flatMapMany(body -> {
                                        logger.error("DeepSeek API returned error: Status={}, Body={}",
                                                response.statusCode(), body);
                                        return Flux.error(new RuntimeException(
                                                "DeepSeek API error: " + response.statusCode() + " - " + body));
                                    });
                        }
                        return response.bodyToFlux(String.class)
                                .doOnNext(data -> {
                                    if (data != null) {
                                        logger.debug("Raw DeepSeek response: {}", data);
                                    }
                                });
                    })
                    .concatMap(data -> {
                        // 处理结束标记
                        if (data.equals("[DONE]")) {
                            // 在流结束时，发送累积的完整内容
                            try {
                                // 按照用户要求的格式构建返回信息
                                Map<String, Object> finalResponse = new HashMap<>();
                                finalResponse.put("reasoning_content",
                                        reasoningBuilder.length() > 0 ? reasoningBuilder.toString() : null);
                                finalResponse.put("content", contentBuilder.toString());
                                finalResponse.put("error", null);
                                finalResponse.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);

                                String jsonResponse = new ObjectMapper().writeValueAsString(finalResponse) + "\n";
                                logger.info("Sending final response with complete content");
                                logger.info("Complete content: {}", contentBuilder.toString());
                                if (reasoningBuilder.length() > 0) {
                                    logger.info("Complete reasoning content: {}", reasoningBuilder.toString());
                                }
                                // 记录最终返回给前端的完整JSON数据
                                logger.info("Final response JSON: {}", jsonResponse.trim());

                                return Flux.just(jsonResponse, "[DONE]\n");
                            } catch (Exception e) {
                                logger.error("Error creating final response", e);
                                return Flux.just("[DONE]\n");
                            }
                        }
                        try {
                            // 原始数据可能是SSE格式或纯JSON
                            String jsonData = data.startsWith("data: ") ? data.substring(6) : data;
                            Map<String, Object> parsed = new ObjectMapper().readValue(
                                    jsonData,
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                    });

                            // 提取内容并累积
                            extractContentFromStreamData(parsed, contentBuilder, reasoningBuilder);

                            // 发送当前数据块（保持流式输出）
                            Map<String, Object> response = new HashMap<>();
                            StringBuilder content = new StringBuilder();
                            StringBuilder reasoning = new StringBuilder();
                            extractContentFromStreamData(parsed, content, reasoning);

                            response.put("content", content.toString());
                            if (reasoning.length() > 0) {
                                response.put("reasoning_content", reasoning.toString());
                            }
                            response.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);

                            // 返回NDJSON格式
                            String jsonResponse = new ObjectMapper().writeValueAsString(response) + "\n";
                            logger.debug("Sending response chunk: {}", jsonResponse.trim());
                            return Flux.just(jsonResponse);
                        } catch (Exception e) {
                            logger.error("SSE数据解析失败: {}", data, e);
                            String errorResponse = "{\"error\":\"Data parsing error\"}\n";
                            logger.info("Sending error response: {}", errorResponse.trim());
                            return Flux.just(errorResponse);
                        }
                    })
                    // 集成响应式重试机制
                    .retryWhen(RetryUtil.createAIRetrySpec())
                    .onErrorResume(e -> {
                        logger.error("Stream error after retries. Original request: {}. Error: ", requestEntity, e);
                        String errorResponse = "{\"error\":\"Stream Error\",\"message\":\"" +
                                e.getMessage() + "\",\"code\":\"STREAM_ERROR\"}\n";
                        logger.info("Sending stream error response after retries: {}", errorResponse.trim());
                        return Flux.just(errorResponse);
                    })
                    .doOnComplete(() -> logger.info("Stream processing completed"))
                    .timeout(Duration.ofMillis(READ_TIMEOUT));

            return responseFlux;
        } catch (Exception e) {
            logger.error("Error creating stream response", e);
            String errorResponse = "{\"error\":\"Error creating stream: " + e.getMessage() + "\"}";
            logger.info("Sending stream creation error response: {}", errorResponse);
            return Flux.just(errorResponse);
        }
    }

    private void extractContentFromStreamData(Map<String, Object> data,
            StringBuilder contentBuilder,
            StringBuilder reasoningBuilder) {
        // 提取content
        if (data.containsKey("choices")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);

                // 处理流式响应中的delta
                if (firstChoice.containsKey("delta")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
                    if (delta.containsKey("content")) {
                        String content = (String) delta.get("content");
                        if (content != null) {
                            contentBuilder.append(content);
                        }
                    }
                    // 尝试提取思维链内容
                    if (delta.containsKey("reasoning_content")) {
                        String reasoningContent = (String) delta.get("reasoning_content");
                        if (reasoningContent != null) {
                            reasoningBuilder.append(reasoningContent);
                        }
                    }
                }

                // 处理非流式响应中的message
                if (firstChoice.containsKey("message")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message.containsKey("content")) {
                        String content = (String) message.get("content");
                        if (content != null) {
                            contentBuilder.append(content);
                        }
                    }
                    // 尝试提取思维链内容
                    if (message.containsKey("reasoning_content")) {
                        String reasoningContent = (String) message.get("reasoning_content");
                        if (reasoningContent != null) {
                            reasoningBuilder.append(reasoningContent);
                        }
                    }
                }
            }
        }
    }

    private Flux<String> handleNonStreamResponse(AIModelConfig.ModelConfig modelConfig,
            HttpEntity<Map<String, Object>> requestEntity) {
        return webClient.post()
                .uri(modelConfig.getUrl())
                .headers(headers -> headers.putAll(requestEntity.getHeaders()))
                .bodyValue(Objects.requireNonNullElse(requestEntity.getBody(), new HashMap<>()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                // 集成响应式重试机制
                .retryWhen(RetryUtil.createAIRetrySpec())
                .flatMapMany(responseBody -> {
                    try {
                        if (responseBody != null) {
                            // 提取思维链内容（reasoning_content）
                            String reasoningContent = null;
                            // 提取content
                            String content = null;
                            if (responseBody.containsKey("choices")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> firstChoice = choices.get(0);
                                    if (firstChoice != null && firstChoice.containsKey("message")) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                                        if (message != null) {
                                            content = (String) message.get("content");

                                            // 尝试提取思维链内容
                                            if (message.containsKey("reasoning_content")) {
                                                reasoningContent = (String) message.get("reasoning_content");
                                            }
                                        }
                                    }
                                }
                            }

                            // 提取错误信息
                            String error = null;
                            if (responseBody.containsKey("error")) {
                                error = (String) responseBody.get("error");
                            }

                            // 构建新响应体
                            Map<String, Object> newResponse = new HashMap<>();
                            newResponse.put("reasoning_content", reasoningContent);
                            newResponse.put("content", content);
                            newResponse.put("error", error);
                            newResponse.put(AIDisclaimerConstants.AI_DISCLAIMER_FIELD, AIDisclaimerConstants.AI_DISCLAIMER);
                            try {
                                return Flux.just(new ObjectMapper().writeValueAsString(newResponse));
                            } catch (Exception e) {
                                return Flux.just("{\"error\":\"JSON serialization error\"}");
                            }
                        }

                        try {
                            return Flux.just(new ObjectMapper().writeValueAsString(responseBody));
                        } catch (Exception e) {
                            return Flux.just("{\"error\":\"JSON serialization error\"}");
                        }
                    } catch (Exception e) {
                        logger.error("Error processing non-stream response", e);
                        return Flux.just("{\"error\":\"Error processing response: " + e.getMessage() + "\"}");
                    }
                })
                .onErrorResume(e -> {
                    logger.error("Error handling non-stream response after retries", e);
                    return Flux.just("{\"error\":\"Error processing response: " + e.getMessage() + "\"}");
                });
    }
}
