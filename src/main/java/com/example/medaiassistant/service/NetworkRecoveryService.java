package com.example.medaiassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络恢复服务类
 * 
 * 该类负责管理网络失败计数和自动重建客户端逻辑，解决网络中断后连接失败问题。
 * 通过维护连续网络失败计数，在达到阈值时自动重建HttpClient/WebClient实例。
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-10-14
 */
@Service
public class NetworkRecoveryService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkRecoveryService.class);

    // 网络失败配置
    private static final int FAILURE_THRESHOLD = 3; // 连续失败阈值
    private static final long RECOVERY_WINDOW_MS = 5 * 60 * 1000; // 5分钟恢复窗口

    // 网络失败计数器
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(System.currentTimeMillis());

    // 客户端重建锁
    private final Object rebuildLock = new Object();

    /**
     * 增加网络失败计数
     * 
     * 当发生网络异常时调用此方法，增加失败计数并记录失败时间。
     * 如果达到失败阈值，会触发自动重建客户端的逻辑。
     * 
     * @param exception 网络异常
     * @param currentWebClient 当前的WebClient实例
     * @param httpClient 当前的HttpClient实例
     * @return 是否需要重建客户端
     */
    public boolean incrementFailureCount(Exception exception, WebClient currentWebClient, HttpClient httpClient) {
        long currentTime = System.currentTimeMillis();
        int currentCount = failureCount.incrementAndGet();
        lastFailureTime.set(currentTime);

        logger.warn("Network failure detected - count: {}, exception: {}", 
                currentCount, exception.getClass().getSimpleName());
        logger.debug("Network failure details: {}", exception.getMessage());

        // 检查是否需要重建客户端
        if (shouldRebuildClient()) {
            logger.info("Network failure threshold reached ({} failures), triggering client rebuild", currentCount);
            rebuildWebClient(currentWebClient, httpClient);
            return true;
        }

        return false;
    }

    /**
     * 重置网络失败计数
     * 
     * 当网络请求成功时调用此方法，重置失败计数并记录成功时间。
     * 这表示网络连接已恢复正常。
     */
    public void resetFailureCount() {
        int previousCount = failureCount.getAndSet(0);
        lastSuccessTime.set(System.currentTimeMillis());

        if (previousCount > 0) {
            logger.info("Network recovery detected - reset failure count from {} to 0", previousCount);
        }
    }

    /**
     * 判断是否需要重建客户端
     * 
     * 基于以下条件判断是否需要重建客户端：
     * 1. 连续失败次数达到阈值
     * 2. 在恢复窗口内没有成功请求
     * 
     * @return 如果需要重建客户端返回true，否则返回false
     */
    public boolean shouldRebuildClient() {
        int currentCount = failureCount.get();
        long currentTime = System.currentTimeMillis();
        long lastSuccess = lastSuccessTime.get();

        // 检查是否达到失败阈值
        if (currentCount >= FAILURE_THRESHOLD) {
            // 检查是否在恢复窗口内没有成功请求
            if (currentTime - lastSuccess > RECOVERY_WINDOW_MS) {
                logger.debug("Should rebuild client - failures: {}, last success: {}ms ago", 
                        currentCount, currentTime - lastSuccess);
                return true;
            } else {
                logger.debug("Not rebuilding client - recent success within recovery window");
                return false;
            }
        }

        return false;
    }

    /**
     * 重建WebClient实例
     * 
     * 当网络连接持续失败时，重建WebClient实例以恢复连接。
     * 使用线程安全的方式确保不会并发重建客户端。
     * 
     * @param currentWebClient 当前的WebClient实例
     * @param httpClient 当前的HttpClient实例
     * @return 重建后的WebClient实例，如果不需要重建则返回原实例
     */
    public WebClient rebuildWebClient(WebClient currentWebClient, HttpClient httpClient) {
        synchronized (rebuildLock) {
            // 双重检查，避免不必要的重建
            if (!shouldRebuildClient()) {
                logger.debug("Skipping client rebuild - condition no longer met");
                return currentWebClient;
            }

            try {
                logger.info("Starting WebClient rebuild process...");

                // 重置失败计数
                int previousCount = failureCount.getAndSet(0);
                lastSuccessTime.set(System.currentTimeMillis());

                // 创建新的WebClient实例
                WebClient.Builder webClientBuilder = WebClient.builder();
                WebClient newWebClient = webClientBuilder
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();

                logger.info("WebClient rebuild completed - previous failure count: {}", previousCount);
                return newWebClient;

            } catch (Exception e) {
                logger.error("Failed to rebuild WebClient", e);
                // 重建失败时恢复原计数
                failureCount.incrementAndGet();
                return currentWebClient;
            }
        }
    }

    /**
     * 获取当前失败计数
     * 
     * @return 当前网络失败计数
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 获取最后失败时间
     * 
     * @return 最后失败时间（毫秒时间戳）
     */
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }

    /**
     * 获取最后成功时间
     * 
     * @return 最后成功时间（毫秒时间戳）
     */
    public long getLastSuccessTime() {
        return lastSuccessTime.get();
    }

    /**
     * 获取失败阈值
     * 
     * @return 失败阈值
     */
    public int getFailureThreshold() {
        return FAILURE_THRESHOLD;
    }

    /**
     * 获取恢复窗口时间
     * 
     * @return 恢复窗口时间（毫秒）
     */
    public long getRecoveryWindowMs() {
        return RECOVERY_WINDOW_MS;
    }

    /**
     * 获取网络状态信息
     * 
     * @return 网络状态信息字符串
     */
    public String getNetworkStatus() {
        int currentCount = failureCount.get();
        long currentTime = System.currentTimeMillis();
        long lastFailure = lastFailureTime.get();
        long lastSuccess = lastSuccessTime.get();

        return String.format(
                "NetworkStatus[failures=%d/%d, lastFailure=%dms ago, lastSuccess=%dms ago, shouldRebuild=%s]",
                currentCount, FAILURE_THRESHOLD,
                currentTime - lastFailure,
                currentTime - lastSuccess,
                shouldRebuildClient());
    }

    /**
     * 检查网络健康状况
     * 
     * @return 如果网络健康返回true，否则返回false
     */
    public boolean isNetworkHealthy() {
        int currentCount = failureCount.get();
        long currentTime = System.currentTimeMillis();
        long lastSuccess = lastSuccessTime.get();

        // 如果最近有成功请求，则认为网络健康
        if (currentTime - lastSuccess < RECOVERY_WINDOW_MS) {
            return true;
        }

        // 如果失败次数较少，也认为网络健康
        return currentCount < FAILURE_THRESHOLD;
    }

    /**
     * 强制重置网络状态
     * 
     * 用于手动恢复网络状态，例如在健康检查中调用。
     */
    public void forceReset() {
        int previousCount = failureCount.getAndSet(0);
        lastSuccessTime.set(System.currentTimeMillis());
        lastFailureTime.set(0);

        if (previousCount > 0) {
            logger.info("Network state force reset - previous failure count: {}", previousCount);
        }
    }
}
