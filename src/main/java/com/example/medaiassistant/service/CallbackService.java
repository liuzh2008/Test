package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.CallbackData;

/**
 * 异步回调服务接口
 * 提供异步回调的核心功能，包括回调发送、重试机制和回调数据创建
 * 
 * 主要功能：
 * - 发送单次HTTP回调
 * - 支持带重试机制的回调发送
 * - 创建各种状态的回调数据对象
 * - 使用配置化的默认参数进行回调
 * 
 * 使用示例：
 * {@code
 * @Autowired
 * private CallbackService callbackService;
 * 
 * // 创建并发送成功回调
 * CallbackData successData = callbackService.createSuccessCallback("task-001",
 * "处理成功");
 * boolean sent = callbackService.sendCallbackToDefaultUrl(successData);
 * }
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
public interface CallbackService {

    /**
     * 发送异步回调
     * 
     * @param callbackUrl  回调URL
     * @param callbackData 回调数据
     * @return 是否发送成功
     */
    boolean sendCallback(String callbackUrl, CallbackData callbackData);

    /**
     * 发送异步回调（带重试机制）
     * 
     * @param callbackUrl   回调URL
     * @param callbackData  回调数据
     * @param maxRetries    最大重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return 是否最终发送成功
     */
    boolean sendCallbackWithRetry(String callbackUrl, CallbackData callbackData, int maxRetries, long retryInterval);

    /**
     * 创建成功的回调数据
     * 
     * @param dataId 数据ID
     * @param result 处理结果
     * @return 回调数据对象
     */
    CallbackData createSuccessCallback(String dataId, String result);

    /**
     * 创建失败的回调数据
     * 
     * @param dataId       数据ID
     * @param errorMessage 错误信息
     * @return 回调数据对象
     */
    CallbackData createFailedCallback(String dataId, String errorMessage);

    /**
     * 创建处理中的回调数据
     * 
     * @param dataId 数据ID
     * @return 回调数据对象
     */
    CallbackData createProcessingCallback(String dataId);

    /**
     * 创建重试中的回调数据
     * 
     * @param dataId     数据ID
     * @param retryCount 当前重试次数
     * @return 回调数据对象
     */
    CallbackData createRetryingCallback(String dataId, int retryCount);

    /**
     * 使用默认配置发送回调（带重试机制）
     * 
     * @param callbackUrl  回调URL
     * @param callbackData 回调数据
     * @return 是否最终发送成功
     */
    boolean sendCallbackWithDefaultRetry(String callbackUrl, CallbackData callbackData);

    /**
     * 使用默认回调URL发送回调
     * 
     * @param callbackData 回调数据
     * @return 是否最终发送成功
     */
    boolean sendCallbackToDefaultUrl(CallbackData callbackData);
}
