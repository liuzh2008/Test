package com.example.medaiassistant.interceptor;

import com.example.medaiassistant.service.RateLimitService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 限流拦截器
 * 
 * 该拦截器在HTTP请求执行前进行限流控制，提供以下功能：
 * 1. 在请求执行前尝试获取限流许可
 * 2. 如果获取许可失败，抛出RateLimitException
 * 3. 在请求完成后释放限流许可
 * 4. 支持异常处理和资源清理
 * 
 * 使用示例：
 * 该拦截器会自动集成到RestTemplate中，无需手动调用。
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@Component
public class RateLimitInterceptor implements ClientHttpRequestInterceptor {

    private final RateLimitService rateLimitService;

    /**
     * 构造函数
     * 
     * 初始化限流拦截器，注入限流服务。
     * 
     * @param rateLimitService 限流服务
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * 拦截HTTP请求
     * 
     * 该方法在HTTP请求执行前进行限流控制，如果限流服务启用，
     * 则尝试获取许可，获取成功则执行请求，否则抛出异常。
     * 
     * @param request   HTTP请求
     * @param body      请求体
     * @param execution 请求执行器
     * @return HTTP响应
     * @throws IOException        如果IO操作失败
     * @throws RateLimitException 如果请求被限流
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution)
            throws IOException {

        boolean acquired = false;
        try {
            // 尝试获取限流许可
            acquired = rateLimitService.tryAcquire();

            if (!acquired) {
                throw new RateLimitException("请求被限流，当前并发请求数已达到上限");
            }

            // 执行HTTP请求
            return execution.execute(request, body);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLimitException("限流许可获取被中断", e);
        } finally {
            // 如果成功获取许可，则释放
            if (acquired) {
                rateLimitService.release();
            }
        }
    }
}

/**
 * 限流异常类
 * 
 * 该异常表示请求被限流服务拒绝。
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
class RateLimitException extends RuntimeException {

    /**
     * 构造函数
     * 
     * 创建限流异常实例。
     * 
     * @param message 异常消息
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * 
     * 创建限流异常实例，包含原因异常。
     * 
     * @param message 异常消息
     * @param cause   原因异常
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
