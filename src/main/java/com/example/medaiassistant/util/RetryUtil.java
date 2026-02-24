package com.example.medaiassistant.util;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.springframework.web.client.ResourceAccessException;

/**
 * 重试工具类，封装响应式重试逻辑
 * 
 * 该类提供了AI服务调用时的重试机制，包括：
 * - 指数退避策略
 * - 随机抖动避免惊群效应
 * - 可重试异常判断
 * - 最大重试次数限制
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-10-14
 */
public class RetryUtil {
    private static final Logger logger = LoggerFactory.getLogger(RetryUtil.class);
    
    // 重试配置常量
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    private static final double JITTER_FACTOR = 0.5; // 50%随机抖动
    
    private static final Random random = new Random();

    /**
     * 创建AI服务调用的重试规范
     * 
     * 使用指数退避策略，每次重试间隔时间翻倍，并加入随机抖动避免多个客户端同时重试。
     * 仅对可重试的网络异常进行重试。
     * 
     * @return 配置好的重试规范
     */
    public static RetryBackoffSpec createAIRetrySpec() {
        return Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .jitter(JITTER_FACTOR)
                .filter(RetryUtil::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    logger.info("AI服务调用重试 - 第{}次重试，异常: {}", 
                            retrySignal.totalRetries() + 1,
                            retrySignal.failure().getClass().getSimpleName());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.error("AI服务调用重试耗尽 - 最大重试次数: {}, 最终异常: {}", 
                            MAX_RETRIES, retrySignal.failure().getMessage());
                    return retrySignal.failure();
                });
    }

    /**
     * 判断异常是否可重试
     * 
     * 可重试的异常包括：
     * - UnknownHostException: DNS解析失败
     * - ConnectException: 连接失败
     * - SocketTimeoutException: 套接字超时
     * - TimeoutException: 超时异常
     * - ResourceAccessException: Spring资源访问异常（通常包装了网络异常）
     * - 5xx服务端错误（通过异常消息判断）
     * 
     * @param throwable 需要判断的异常
     * @return 如果异常可重试返回true，否则返回false
     */
    public static boolean isRetryableException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // 检查直接异常类型
        if (throwable instanceof UnknownHostException ||
            throwable instanceof ConnectException ||
            throwable instanceof SocketTimeoutException ||
            throwable instanceof TimeoutException ||
            throwable instanceof ResourceAccessException) {
            logger.debug("检测到可重试异常: {}", throwable.getClass().getSimpleName());
            return true;
        }

        // 检查异常链中的可重试异常
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof UnknownHostException ||
                cause instanceof ConnectException ||
                cause instanceof SocketTimeoutException ||
                cause instanceof TimeoutException) {
                logger.debug("检测到可重试异常链: {} -> {}", 
                        throwable.getClass().getSimpleName(), cause.getClass().getSimpleName());
                return true;
            }
            
            // 检查5xx服务端错误（通过异常消息判断）
            if (is5xxServerError(cause)) {
                logger.debug("检测到5xx服务端错误: {}", cause.getMessage());
                return true;
            }
            
            cause = cause.getCause();
        }

        logger.debug("不可重试异常: {}", throwable.getClass().getSimpleName());
        return false;
    }

    /**
     * 判断异常是否表示5xx服务端错误
     * 
     * 通过检查异常消息中是否包含5xx状态码来判断是否为服务端错误。
     * 常见的5xx错误包括：500, 502, 503, 504等。
     * 
     * @param throwable 需要判断的异常
     * @return 如果是5xx服务端错误返回true，否则返回false
     */
    private static boolean is5xxServerError(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return false;
        }
        
        String message = throwable.getMessage().toLowerCase();
        
        // 检查常见的5xx错误模式
        return message.contains("500") || 
               message.contains("502") || 
               message.contains("503") || 
               message.contains("504") ||
               message.contains("internal server error") ||
               message.contains("service unavailable") ||
               message.contains("bad gateway") ||
               message.contains("gateway timeout");
    }

    /**
     * 计算带随机抖动的重试延迟时间
     * 
     * 使用指数退避策略，并在基础延迟上加入随机抖动，避免多个客户端同时重试。
     * 
     * @param retryCount 当前重试次数（从0开始）
     * @return 计算后的重试延迟时间（毫秒）
     */
    public static long calculateRetryDelay(int retryCount) {
        // 指数退避：1s, 2s, 4s, 8s...
        long baseDelay = (long) Math.pow(2, retryCount) * 1000;
        
        // 随机抖动：±50%
        double jitter = (random.nextDouble() - 0.5) * JITTER_FACTOR * 2;
        long jitteredDelay = (long) (baseDelay * (1 + jitter));
        
        // 确保延迟时间在合理范围内
        return Math.max(INITIAL_BACKOFF.toMillis(), 
                       Math.min(jitteredDelay, MAX_BACKOFF.toMillis()));
    }

    /**
     * 获取最大重试次数
     * 
     * @return 最大重试次数
     */
    public static int getMaxRetries() {
        return MAX_RETRIES;
    }

    /**
     * 获取初始退避时间
     * 
     * @return 初始退避时间
     */
    public static Duration getInitialBackoff() {
        return INITIAL_BACKOFF;
    }

    /**
     * 获取最大退避时间
     * 
     * @return 最大退避时间
     */
    public static Duration getMaxBackoff() {
        return MAX_BACKOFF;
    }

    /**
     * 获取随机抖动因子
     * 
     * @return 随机抖动因子
     */
    public static double getJitterFactor() {
        return JITTER_FACTOR;
    }
}
