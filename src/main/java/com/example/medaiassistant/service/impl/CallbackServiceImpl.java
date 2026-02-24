package com.example.medaiassistant.service.impl;

import com.example.medaiassistant.config.CallbackConfig;
import com.example.medaiassistant.dto.CallbackData;
import com.example.medaiassistant.model.CallbackStatus;
import com.example.medaiassistant.service.CallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 异步回调服务实现类
 * 使用RestTemplate进行HTTP回调，支持配置化的重试机制
 * 
 * 主要特性：
 * - 基于Spring的RestTemplate进行HTTP通信
 * - 支持可配置的重试次数和重试间隔
 * - 完善的错误处理和日志记录
 * - 线程安全的回调发送
 * 
 * 配置参数：
 * - callback.max-retries: 最大重试次数（默认3次）
 * - callback.retry-interval: 重试间隔毫秒（默认5000ms）
 * - callback.timeout: 超时时间毫秒（默认30000ms）
 * - callback.default-callback-url: 默认回调URL
 * 
 * 使用示例：
 * {@code
 * @Autowired
 * private CallbackService callbackService;
 * 
 * // 发送带默认重试的回调
 * CallbackData data = callbackService.createSuccessCallback("task-001", "成功");
 * boolean result = callbackService.sendCallbackToDefaultUrl(data);
 * }
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
@Service
public class CallbackServiceImpl implements CallbackService {

    private static final Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);

    private final RestTemplate restTemplate;
    private final CallbackConfig callbackConfig;

    public CallbackServiceImpl(RestTemplate restTemplate, CallbackConfig callbackConfig) {
        this.restTemplate = restTemplate;
        this.callbackConfig = callbackConfig;
    }

    @Override
    public boolean sendCallback(String callbackUrl, CallbackData callbackData) {
        try {
            logger.info("发送回调请求到: {}, 数据: {}", callbackUrl, callbackData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CallbackData> request = new HttpEntity<>(callbackData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("回调发送成功: {}, 响应: {}", callbackUrl, response.getBody());
                return true;
            } else {
                logger.warn("回调发送失败: {}, 状态码: {}", callbackUrl, response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            logger.error("回调发送异常: {}, 错误: {}", callbackUrl, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendCallbackWithRetry(String callbackUrl, CallbackData callbackData, int maxRetries,
            long retryInterval) {
        int attempt = 0;
        boolean success = false;

        while (attempt <= maxRetries && !success) {
            try {
                if (attempt > 0) {
                    logger.info("第 {} 次重试回调: {}", attempt, callbackUrl);
                    callbackData.setStatus(CallbackStatus.RETRYING);
                    callbackData.incrementRetryCount();

                    // 等待重试间隔
                    Thread.sleep(retryInterval);
                }

                success = sendCallback(callbackUrl, callbackData);

                if (success) {
                    logger.info("回调重试成功: {}, 重试次数: {}", callbackUrl, attempt);
                } else {
                    logger.warn("回调重试失败: {}, 重试次数: {}", callbackUrl, attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("回调重试被中断: {}", callbackUrl, e);
                break;
            } catch (Exception e) {
                logger.error("回调重试异常: {}, 重试次数: {}", callbackUrl, attempt, e);
            }

            attempt++;
        }

        if (!success) {
            logger.error("回调最终失败: {}, 最大重试次数: {}", callbackUrl, maxRetries);
        }

        return success;
    }

    /**
     * 使用默认配置发送回调（带重试机制）
     * 
     * @param callbackUrl  回调URL
     * @param callbackData 回调数据
     * @return 是否最终发送成功
     */
    @Override
    public boolean sendCallbackWithDefaultRetry(String callbackUrl, CallbackData callbackData) {
        return sendCallbackWithRetry(callbackUrl, callbackData,
                callbackConfig.getMaxRetries(), callbackConfig.getRetryInterval());
    }

    /**
     * 使用默认回调URL发送回调
     * 
     * @param callbackData 回调数据
     * @return 是否最终发送成功
     */
    @Override
    public boolean sendCallbackToDefaultUrl(CallbackData callbackData) {
        return sendCallbackWithDefaultRetry(callbackConfig.getDefaultCallbackUrl(), callbackData);
    }

    @Override
    public CallbackData createSuccessCallback(String dataId, String result) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.SUCCESS);
        callbackData.setResult(result);
        return callbackData;
    }

    @Override
    public CallbackData createFailedCallback(String dataId, String errorMessage) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.FAILED);
        callbackData.setErrorMessage(errorMessage);
        return callbackData;
    }

    @Override
    public CallbackData createProcessingCallback(String dataId) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.PROCESSING);
        return callbackData;
    }

    @Override
    public CallbackData createRetryingCallback(String dataId, int retryCount) {
        CallbackData callbackData = new CallbackData();
        callbackData.setDataId(dataId);
        callbackData.setStatus(CallbackStatus.RETRYING);
        callbackData.setRetryCount(retryCount);
        return callbackData;
    }
}
