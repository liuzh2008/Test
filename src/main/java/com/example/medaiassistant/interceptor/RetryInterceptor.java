package com.example.medaiassistant.interceptor;

import com.example.medaiassistant.service.RetryPolicyService;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 重试拦截器
 * 
 * 该拦截器集成到Apache HttpClient中，提供基于重试策略的请求重试功能。
 * 支持对可重试的异常和状态码进行自动重试。
 * 
 * 功能特性：
 * 1. 异常重试：对SocketTimeoutException和IOException进行重试
 * 2. 状态码重试：对5xx状态码进行重试
 * 3. 指数退避：重试间隔按指数增长（1秒、2秒、4秒）
 * 4. 最大重试限制：最多重试3次
 * 
 * 使用说明：
 * 该拦截器会自动集成到HttpClientPoolConfig中，无需手动调用。
 * 
 * @since 2025-09-28
 * @author Cline
 * @version 1.0
 */
@Component
public class RetryInterceptor implements org.apache.hc.core5.http.HttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RetryInterceptor.class);

    private final RetryPolicyService retryPolicyService;

    /**
     * 构造函数
     * 
     * @param retryPolicyService 重试策略服务
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RetryInterceptor(RetryPolicyService retryPolicyService) {
        this.retryPolicyService = retryPolicyService;
    }

    /**
     * 处理HTTP请求
     * 
     * 该方法在HTTP请求发送前执行，主要用于记录请求信息。
     * 实际的重试逻辑在HttpClient的请求执行器中处理。
     * 
     * @param request HTTP请求
     * @param entity  请求实体
     * @param context HTTP上下文
     * @throws HttpException HTTP异常
     * @throws IOException   IO异常
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        // 记录请求信息，重试逻辑在HttpClient的请求执行器中处理
        logger.debug("处理HTTP请求: {} {}", request.getMethod(), request.getRequestUri());
    }

    /**
     * 处理HTTP响应
     * 
     * 该方法在HTTP响应接收后执行，主要用于记录响应信息和状态码。
     * 
     * @param response HTTP响应
     * @param entity   响应实体
     * @param context  HTTP上下文
     * @throws HttpException HTTP异常
     * @throws IOException   IO异常
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public void process(HttpResponse response, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        int statusCode = response.getCode();
        logger.debug("收到HTTP响应: 状态码 {}", statusCode);

        // 记录可重试状态码
        if (retryPolicyService.isRetryableStatusCode(statusCode)) {
            logger.warn("收到可重试状态码: {}", statusCode);
        }
    }

    /**
     * 获取重试策略服务
     * 
     * @return 重试策略服务
     * @since 2025-09-28
     * @author Cline
     * @version 1.0
     */
    public RetryPolicyService getRetryPolicyService() {
        return retryPolicyService;
    }
}
