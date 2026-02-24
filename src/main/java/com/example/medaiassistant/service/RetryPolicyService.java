package com.example.medaiassistant.service;

import com.example.medaiassistant.config.RetryPolicyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * 重试策略服务类
 * 
 * 该类实现基于指数退避的重试策略，提供以下功能：
 * 1. 指数退避重试：重试间隔按指数增长（1秒、2秒、4秒）
 * 2. 状态码检查：只对特定状态码进行重试（5xx状态码和连接超时）
 * 3. 最大重试次数限制：最多重试3次
 * 4. 异常处理：支持连接超时和IO异常的重试
 * 
 * 重试策略说明：
 * - 最大重试次数：3次
 * - 重试间隔：1秒、2秒、4秒（指数退避）
 * - 可重试状态码：429, 500, 502, 503, 504
 * - 可重试异常：SocketTimeoutException, IOException
 * 
 * 使用示例：
 * 
 * @Autowired
 *            private RetryPolicyService retryPolicyService;
 * 
 *            String result = retryPolicyService.executeWithRetry(() -> {
 *            return httpClient.execute(request);
 *            });
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@Service
public class RetryPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicyService.class);

    private final RetryPolicyConfig retryPolicyConfig;

    /**
     * 构造函数
     * 
     * @param retryPolicyConfig 重试策略配置
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RetryPolicyService(RetryPolicyConfig retryPolicyConfig) {
        this.retryPolicyConfig = retryPolicyConfig;
    }

    /**
     * 执行带重试策略的操作
     * 
     * 该方法执行指定的操作，并在遇到可重试的异常或状态码时进行重试。
     * 重试策略基于指数退避算法，最大重试次数由配置决定。
     * 
     * @param operation 要执行的操作
     * @param <T>       操作返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败，抛出最后一次的异常
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        return executeWithRetry(operation, null);
    }

    /**
     * 执行带重试策略的操作（支持状态码检查）
     * 
     * 该方法执行指定的操作，并在遇到可重试的异常或状态码时进行重试。
     * 如果提供了状态码检查器，会根据状态码决定是否重试。
     * 
     * @param operation         要执行的操作
     * @param statusCodeChecker 状态码检查器（可选）
     * @param <T>               操作返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败，抛出最后一次的异常
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation, StatusCodeChecker statusCodeChecker)
            throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= retryPolicyConfig.getMaxRetries()) {
            try {
                T result = operation.execute();

                // 如果提供了状态码检查器，检查状态码是否需要重试
                if (statusCodeChecker != null && statusCodeChecker.shouldRetry(statusCodeChecker.getStatusCode())) {
                    if (retryCount < retryPolicyConfig.getMaxRetries()) {
                        logger.warn("请求返回可重试状态码: {}, 进行第{}次重试",
                                statusCodeChecker.getStatusCode(), retryCount + 1);
                        waitForRetry(retryCount);
                        retryCount++;
                        continue;
                    } else {
                        logger.error("达到最大重试次数({})，状态码: {}",
                                retryPolicyConfig.getMaxRetries(), statusCodeChecker.getStatusCode());
                        throw new RuntimeException("达到最大重试次数，状态码: " + statusCodeChecker.getStatusCode());
                    }
                }

                return result;

            } catch (Exception e) {
                lastException = e;

                // 检查是否为可重试的异常
                if (isRetryableException(e)) {
                    if (retryCount < retryPolicyConfig.getMaxRetries()) {
                        logger.warn("请求遇到可重试异常: {}, 进行第{}次重试",
                                e.getClass().getSimpleName(), retryCount + 1);
                        waitForRetry(retryCount);
                        retryCount++;
                    } else {
                        logger.error("达到最大重试次数({})，最后一次异常: {}",
                                retryPolicyConfig.getMaxRetries(), e.getMessage());
                        throw e;
                    }
                } else {
                    // 不可重试的异常，直接抛出
                    throw e;
                }
            }
        }

        // 理论上不会执行到这里，但为了编译安全
        throw lastException != null ? lastException : new RuntimeException("重试策略执行失败");
    }

    /**
     * 等待重试间隔
     * 
     * 根据当前重试次数计算等待时间，使用指数退避算法。
     * 重试间隔：1秒、2秒、4秒
     * 
     * @param retryCount 当前重试次数
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    private void waitForRetry(int retryCount) {
        try {
            long waitTime = calculateWaitTime(retryCount);
            logger.debug("等待重试间隔: {}ms", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("重试等待被中断");
        }
    }

    /**
     * 计算等待时间
     * 
     * 使用指数退避算法计算等待时间：
     * 第0次重试：initialIntervalMs (1000ms)
     * 第1次重试：initialIntervalMs * multiplier (2000ms)
     * 第2次重试：initialIntervalMs * multiplier^2 (4000ms)
     * 
     * @param retryCount 当前重试次数
     * @return 等待时间（毫秒）
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    private long calculateWaitTime(int retryCount) {
        long waitTime = (long) (retryPolicyConfig.getInitialIntervalMs() *
                Math.pow(retryPolicyConfig.getMultiplier(), retryCount));

        // 确保不超过最大间隔
        return Math.min(waitTime, retryPolicyConfig.getMaxIntervalMs());
    }

    /**
     * 检查异常是否为可重试的异常
     * 
     * 可重试的异常包括：
     * - SocketTimeoutException: 连接超时
     * - IOException: IO异常（网络问题）
     * 
     * @param e 异常
     * @return 如果异常可重试返回true，否则返回false
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    private boolean isRetryableException(Exception e) {
        return e instanceof SocketTimeoutException ||
                e instanceof IOException;
    }

    /**
     * 检查状态码是否为可重试的状态码
     * 
     * @param statusCode HTTP状态码
     * @return 如果状态码可重试返回true，否则返回false
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public boolean isRetryableStatusCode(int statusCode) {
        return retryPolicyConfig.getRetryableStatusCodes().contains(statusCode);
    }

    /**
     * 可重试操作接口
     * 
     * @param <T> 操作返回类型
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        /**
         * 执行操作
         * 
         * @return 操作结果
         * @throws Exception 操作过程中可能抛出的异常
         * @since 2025-09-28
         * @author Cline
         * @version 1.0
         */
        T execute() throws Exception;
    }

    /**
     * 状态码检查器接口
     * 
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public interface StatusCodeChecker {
        /**
         * 获取状态码
         * 
         * @return HTTP状态码
         * @since 2025-09-28
         * @author Cline
         * @version 1.0
         */
        int getStatusCode();

        /**
         * 检查是否应该重试
         * 
         * @param statusCode HTTP状态码
         * @return 如果应该重试返回true，否则返回false
         * @since 2025-09-28
         * @author Cline
         * @version 1.0
         */
        boolean shouldRetry(int statusCode);
    }

    /**
     * 获取重试策略配置
     * 
     * @return 重试策略配置
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RetryPolicyConfig getRetryPolicyConfig() {
        return retryPolicyConfig;
    }
}
