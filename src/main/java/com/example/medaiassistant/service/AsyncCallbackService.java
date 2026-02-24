package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.CallbackData;
import com.example.medaiassistant.model.CallbackStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * 异步回调服务
 * 实现主应用(8081)和执行服务器(8082)之间的异步回调机制
 * 
 * 功能包括：
 * 1. 异步HTTP回调
 * 2. 基础重试机制
 * 3. 回调状态管理
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
@Service
public class AsyncCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCallbackService.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 主应用回调地址（支持多个地址，用逗号分隔）
     * 默认：http://localhost:8081/api/callback/receive
     */
    @Value("${callback.default-callback-url:http://localhost:8081/api/callback/receive}")
    private String mainAppCallbackUrls;

    /**
     * 最大重试次数
     * 默认：3次
     */
    @Value("${callback.max-retries:3}")
    private int maxRetryAttempts;

    /**
     * 重试间隔（毫秒）
     * 默认：5000毫秒（5秒）
     */
    @Value("${callback.retry-interval:5000}")
    private long retryDelay;

    /**
     * 异步执行回调
     * 
     * @param callbackData 回调数据
     * @return CompletableFuture表示异步操作结果
     */
    @Async
    public CompletableFuture<Boolean> executeCallbackAsync(CallbackData callbackData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeCallbackWithRetry(callbackData, 0);
            } catch (Exception e) {
                logger.error("异步回调执行失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 带重试机制的回调执行
     * 支持向多个主应用服务器发送回调请求，使用逗号分隔的URL配置
     * 
     * @param callbackData 回调数据对象，包含数据ID、状态、结果等信息
     * @param attempt      当前重试次数，从0开始计数
     * @return 回调是否成功，true表示所有配置的主应用服务器都回调成功
     * @throws RestClientException 当HTTP请求失败或服务器返回非2xx状态码时抛出
     * 
     * @example
     *          //
     *          配置示例：callback.default-callback-url=http://server1:8081/api/callback,http://server2:8081/api/callback
     *          CallbackData data = createSuccessCallback("data-001", "处理结果");
     *          boolean success = executeCallbackWithRetry(data, 0);
     * 
     * @since 2025-09-14
     * @version 1.1
     */
    public boolean executeCallbackWithRetry(CallbackData callbackData, int attempt) {
        try {
            logger.info("执行回调到主应用，attempt: {}, dataId: {}", attempt + 1, callbackData.getDataId());

            // 设置重试次数
            callbackData.setRetryCount(attempt + 1);

            // 准备HTTP请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CallbackData> request = new HttpEntity<>(callbackData, headers);

            // 执行HTTP POST请求到所有配置的主应用地址
            String[] callbackUrls = mainAppCallbackUrls.split(",");
            boolean allSuccess = true;

            for (String callbackUrl : callbackUrls) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            callbackUrl.trim(),
                            request,
                            String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("回调成功到 {}: dataId={}, status={}", callbackUrl, callbackData.getDataId(),
                                callbackData.getStatus());
                    } else {
                        logger.warn("回调失败到 {}: dataId={}, statusCode={}", callbackUrl, callbackData.getDataId(),
                                response.getStatusCode());
                        allSuccess = false;
                    }
                } catch (RestClientException e) {
                    logger.warn("回调执行失败到 {}: dataId={}, error={}", callbackUrl, callbackData.getDataId(),
                            e.getMessage());
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                logger.info("所有主应用回调成功: dataId={}", callbackData.getDataId());
                return true;
            } else {
                logger.warn("部分主应用回调失败: dataId={}", callbackData.getDataId());
                throw new RestClientException("部分主应用回调失败");
            }

        } catch (RestClientException e) {
            logger.warn("回调执行失败，准备重试: dataId={}, attempt={}, error={}",
                    callbackData.getDataId(), attempt + 1, e.getMessage());

            // 如果达到最大重试次数，记录最终失败
            if (attempt + 1 >= maxRetryAttempts) {
                logger.error("达到最大重试次数，回调最终失败: dataId={}, maxAttempts={}",
                        callbackData.getDataId(), maxRetryAttempts);
                return false;
            }

            // 等待一段时间后重试
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("重试等待被中断: {}", ie.getMessage());
                return false;
            }

            // 递归调用进行重试
            return executeCallbackWithRetry(callbackData, attempt + 1);
        }
    }

    /**
     * 创建成功回调数据
     * 
     * @param dataId 数据ID
     * @param result 处理结果
     * @return 回调数据
     */
    public CallbackData createSuccessCallback(String dataId, String result) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.SUCCESS);
        callbackData.setResult(result);
        return callbackData;
    }

    /**
     * 创建失败回调数据
     * 
     * @param dataId       数据ID
     * @param errorMessage 错误信息
     * @return 回调数据
     */
    public CallbackData createFailedCallback(String dataId, String errorMessage) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.FAILED);
        callbackData.setErrorMessage(errorMessage);
        return callbackData;
    }

    /**
     * 创建处理中回调数据
     * 
     * @param dataId 数据ID
     * @return 回调数据
     */
    public CallbackData createProcessingCallback(String dataId) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.PROCESSING);
        return callbackData;
    }

    /**
     * 创建重试中回调数据
     * 
     * @param dataId     数据ID
     * @param retryCount 重试次数
     * @return 回调数据
     */
    public CallbackData createRetryingCallback(String dataId, int retryCount) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.RETRYING);
        callbackData.setRetryCount(retryCount);
        return callbackData;
    }

    // Getter和Setter方法
    public String getMainAppCallbackUrls() {
        return mainAppCallbackUrls;
    }

    public void setMainAppCallbackUrls(String mainAppCallbackUrls) {
        this.mainAppCallbackUrls = mainAppCallbackUrls;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
    }
}
