package com.example.medaiassistant.config;

import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * HttpClient配置类
 * 
 * 该类提供了优化的HttpClient配置，包括：
 * - 连接池管理
 * - DNS解析器优化
 * - 超时配置
 * - 连接逐出策略
 * 
 * 通过优化配置，解决网络中断后连接失败问题，提升系统在网络波动下的可用性。
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-10-14
 */
@Configuration
public class HttpClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientConfig.class);

    // 连接池配置
    private static final int MAX_CONNECTIONS = 100;
    private static final int PENDING_ACQUIRE_MAX_COUNT = 100;
    private static final Duration PENDING_ACQUIRE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration MAX_IDLE_TIME = Duration.ofMinutes(5);
    private static final Duration MAX_LIFE_TIME = Duration.ofMinutes(10);
    private static final Duration EVICT_IN_BACKGROUND = Duration.ofMinutes(2);

    // 超时配置
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(300); // 5分钟
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30); // 30秒

    // DNS配置
    private static final Duration DNS_QUERY_TIMEOUT = Duration.ofSeconds(30); // 30秒
    private static final Duration DNS_CACHE_MIN_TTL = Duration.ofMinutes(1); // 1分钟
    private static final Duration DNS_CACHE_MAX_TTL = Duration.ofMinutes(5); // 5分钟
    private static final Duration DNS_CACHE_NEGATIVE_TTL = Duration.ofSeconds(30); // 30秒

    /**
     * 创建优化的HttpClient实例
     * 
     * 该HttpClient配置了：
     * - 连接池管理：避免连接资源泄露与不健康状态长时间滞留
     * - DNS解析器优化：提升DNS解析成功率和缓存效率
     * - 超时配置：合理的连接和响应超时时间
     * - IPv4优先：减少IPv6网络环境异常带来的解析问题
     * 
     * @return 配置好的HttpClient实例
     */
    @Bean
    public HttpClient createOptimizedHttpClient() {
        // 设置系统属性，优先使用IPv4
        System.setProperty("java.net.preferIPv4Stack", "true");
        
        // 创建连接池
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-http-client")
                .maxConnections(MAX_CONNECTIONS)
                .pendingAcquireMaxCount(PENDING_ACQUIRE_MAX_COUNT)
                .pendingAcquireTimeout(PENDING_ACQUIRE_TIMEOUT)
                .maxIdleTime(MAX_IDLE_TIME)
                .maxLifeTime(MAX_LIFE_TIME)
                .evictInBackground(EVICT_IN_BACKGROUND)
                .build();

        // 创建并配置HttpClient
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(RESPONSE_TIMEOUT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .resolver(spec -> spec
                        .queryTimeout(DNS_QUERY_TIMEOUT)
                        .cacheMinTimeToLive(DNS_CACHE_MIN_TTL)
                        .cacheMaxTimeToLive(DNS_CACHE_MAX_TTL)
                        .cacheNegativeTimeToLive(DNS_CACHE_NEGATIVE_TTL));
                // 注意：wiretap配置已移除，生产环境不需要详细网络日志

        logger.info("Optimized HttpClient configured with:");
        logger.info("  - Connection pool: maxConnections={}, maxIdleTime={}, maxLifeTime={}", 
                MAX_CONNECTIONS, MAX_IDLE_TIME, MAX_LIFE_TIME);
        logger.info("  - Timeouts: response={}, connect={}", RESPONSE_TIMEOUT, CONNECT_TIMEOUT);
        logger.info("  - DNS: queryTimeout={}, cacheTTL=[{}-{}], negativeTTL={}", 
                DNS_QUERY_TIMEOUT, DNS_CACHE_MIN_TTL, DNS_CACHE_MAX_TTL, DNS_CACHE_NEGATIVE_TTL);
        logger.info("  - IPv4 preference enabled");

        return httpClient;
    }

    /**
     * 获取连接池配置信息
     * 
     * @return 连接池配置信息字符串
     */
    public static String getConnectionPoolInfo() {
        return String.format(
                "ConnectionPool[connections=%d, pendingAcquire=%d/%s, idleTime=%s, lifeTime=%s, evict=%s]",
                MAX_CONNECTIONS, PENDING_ACQUIRE_MAX_COUNT, PENDING_ACQUIRE_TIMEOUT,
                MAX_IDLE_TIME, MAX_LIFE_TIME, EVICT_IN_BACKGROUND);
    }

    /**
     * 获取DNS配置信息
     * 
     * @return DNS配置信息字符串
     */
    public static String getDNSConfigInfo() {
        return String.format(
                "DNS[queryTimeout=%s, cacheTTL=[%s-%s], negativeTTL=%s]",
                DNS_QUERY_TIMEOUT, DNS_CACHE_MIN_TTL, DNS_CACHE_MAX_TTL, DNS_CACHE_NEGATIVE_TTL);
    }

    /**
     * 获取超时配置信息
     * 
     * @return 超时配置信息字符串
     */
    public static String getTimeoutConfigInfo() {
        return String.format("Timeouts[response=%s, connect=%s]", RESPONSE_TIMEOUT, CONNECT_TIMEOUT);
    }

    /**
     * 获取最大连接数
     * 
     * @return 最大连接数
     */
    public static int getMaxConnections() {
        return MAX_CONNECTIONS;
    }

    /**
     * 获取响应超时时间
     * 
     * @return 响应超时时间
     */
    public static Duration getResponseTimeout() {
        return RESPONSE_TIMEOUT;
    }

    /**
     * 获取连接超时时间
     * 
     * @return 连接超时时间
     */
    public static Duration getConnectTimeout() {
        return CONNECT_TIMEOUT;
    }

    /**
     * 获取DNS查询超时时间
     * 
     * @return DNS查询超时时间
     */
    public static Duration getDnsQueryTimeout() {
        return DNS_QUERY_TIMEOUT;
    }

    /**
     * 获取DNS缓存最小TTL
     * 
     * @return DNS缓存最小TTL
     */
    public static Duration getDnsCacheMinTtl() {
        return DNS_CACHE_MIN_TTL;
    }

    /**
     * 获取DNS缓存最大TTL
     * 
     * @return DNS缓存最大TTL
     */
    public static Duration getDnsCacheMaxTtl() {
        return DNS_CACHE_MAX_TTL;
    }

    /**
     * 获取DNS负缓存TTL
     * 
     * @return DNS负缓存TTL
     */
    public static Duration getDnsCacheNegativeTtl() {
        return DNS_CACHE_NEGATIVE_TTL;
    }
}
