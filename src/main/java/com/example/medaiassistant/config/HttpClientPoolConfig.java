package com.example.medaiassistant.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import com.example.medaiassistant.interceptor.RetryInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * HttpClient连接池配置类
 * 
 * 该配置类基于Apache HttpClient 5.x实现高性能HTTP连接池，提供以下功能：
 * 1. 连接池管理：支持最大连接数和每个路由连接数限制
 * 2. 连接复用：减少TCP连接建立和关闭的开销
 * 3. 连接保持：支持HTTP Keep-Alive机制
 * 4. 超时控制：连接超时、请求超时、Socket超时
 * 5. 线程安全：支持并发访问
 * 
 * 配置说明：
 * - 最大总连接数：200
 * - 每个路由最大连接数：50
 * - 连接请求超时：30秒
 * - 连接超时：30秒
 * - Socket超时：5分钟
 * - 连接保持时间：30秒
 * 
 * 使用示例：
 * @Autowired
 * private RestTemplate restTemplate;
 * 
 * String response = restTemplate.getForObject("http://example.com/api", String.class);
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@Configuration
public class HttpClientPoolConfig {

    @Autowired
    private ConnectionPoolConfig connectionPoolConfig;

    @Autowired
    private RetryInterceptor retryInterceptor;

    /**
     * 创建连接池管理器
     * 
     * 该方法创建并配置PoolingHttpClientConnectionManager，提供以下功能：
     * 1. 设置最大总连接数和每个路由最大连接数
     * 2. 配置连接超时和Socket超时
     * 3. 启用连接复用和连接保持
     * 4. 配置连接池策略
     * 
     * @return 配置好的连接池管理器
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Bean
    public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        // 注册HTTP和HTTPS连接工厂
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry,
                PoolConcurrencyPolicy.STRICT,
                PoolReusePolicy.LIFO,
                TimeValue.of(connectionPoolConfig.getKeepAliveTime(), TimeUnit.MILLISECONDS)
        );

        // 配置连接池参数
        connectionManager.setMaxTotal(connectionPoolConfig.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(connectionPoolConfig.getMaxConnectionsPerRoute());

        // 配置连接参数
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionPoolConfig.getConnectTimeout()))
                .setSocketTimeout(Timeout.ofMilliseconds(connectionPoolConfig.getSocketTimeout()))
                .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        // 配置Socket参数
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(connectionPoolConfig.getSocketTimeout()))
                .build();
        connectionManager.setDefaultSocketConfig(socketConfig);

        return connectionManager;
    }

    /**
     * 创建HTTP客户端
     * 
     * 该方法创建基于连接池的CloseableHttpClient实例，提供以下功能：
     * 1. 使用连接池管理器
     * 2. 配置请求超时参数
     * 3. 启用连接保持策略
     * 4. 禁用重定向处理
     * 
     * @param connectionManager 连接池管理器
     * @return 配置好的HTTP客户端
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        // 配置请求参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionPoolConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(connectionPoolConfig.getSocketTimeout()))
                .setRedirectsEnabled(false) // 禁用重定向
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .addRequestInterceptorFirst(retryInterceptor)
                .build();
    }

    /**
     * 创建HTTP请求工厂
     * 
     * 该方法创建HttpComponentsClientHttpRequestFactory，用于集成到Spring的RestTemplate中。
     * 提供基于Apache HttpClient 5.x的连接池支持。
     * 
     * @param httpClient HTTP客户端
     * @return 配置好的HTTP请求工厂
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
