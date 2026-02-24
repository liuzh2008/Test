package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.CallbackData;
import com.example.medaiassistant.model.CallbackStatus;
import com.example.medaiassistant.service.CallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回调测试控制器
 * 用于测试异步回调功能
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
@RestController
@RequestMapping("/api/callback-test")
public class CallbackTestController {

    private static final Logger logger = LoggerFactory.getLogger(CallbackTestController.class);

    private final CallbackService callbackService;

    public CallbackTestController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    /**
     * 测试发送成功回调
     * 
     * @param dataId      数据ID
     * @param result      处理结果
     * @param callbackUrl 回调URL（可选，默认使用配置的URL）
     * @return 测试结果
     */
    @GetMapping("/test-success")
    public ResponseEntity<String> testSuccessCallback(
            @RequestParam String dataId,
            @RequestParam String result,
            @RequestParam(required = false) String callbackUrl) {

        try {
            CallbackData callbackData = callbackService.createSuccessCallback(dataId, result);

            boolean success;
            if (callbackUrl != null && !callbackUrl.isEmpty()) {
                success = callbackService.sendCallbackWithDefaultRetry(callbackUrl, callbackData);
            } else {
                success = callbackService.sendCallbackToDefaultUrl(callbackData);
            }

            if (success) {
                return ResponseEntity.ok("成功回调测试完成，数据ID: " + dataId);
            } else {
                return ResponseEntity.internalServerError().body("成功回调测试失败，数据ID: " + dataId);
            }
        } catch (Exception e) {
            logger.error("成功回调测试异常", e);
            return ResponseEntity.internalServerError().body("成功回调测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试发送失败回调
     * 
     * @param dataId       数据ID
     * @param errorMessage 错误信息
     * @param callbackUrl  回调URL（可选，默认使用配置的URL）
     * @return 测试结果
     */
    @GetMapping("/test-failed")
    public ResponseEntity<String> testFailedCallback(
            @RequestParam String dataId,
            @RequestParam String errorMessage,
            @RequestParam(required = false) String callbackUrl) {

        try {
            CallbackData callbackData = callbackService.createFailedCallback(dataId, errorMessage);

            boolean success;
            if (callbackUrl != null && !callbackUrl.isEmpty()) {
                success = callbackService.sendCallbackWithDefaultRetry(callbackUrl, callbackData);
            } else {
                success = callbackService.sendCallbackToDefaultUrl(callbackData);
            }

            if (success) {
                return ResponseEntity.ok("失败回调测试完成，数据ID: " + dataId);
            } else {
                return ResponseEntity.internalServerError().body("失败回调测试失败，数据ID: " + dataId);
            }
        } catch (Exception e) {
            logger.error("失败回调测试异常", e);
            return ResponseEntity.internalServerError().body("失败回调测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试回调配置
     * 
     * @return 配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<String> testConfig() {
        try {
            // 测试创建各种状态的回调数据
            CallbackData successData = callbackService.createSuccessCallback("test-config-1", "配置测试成功");
            CallbackData failedData = callbackService.createFailedCallback("test-config-2", "配置测试失败");
            CallbackData processingData = callbackService.createProcessingCallback("test-config-3");
            CallbackData retryingData = callbackService.createRetryingCallback("test-config-4", 2);

            StringBuilder result = new StringBuilder();
            result.append("回调服务配置测试:\n");
            result.append("1. 成功回调: ").append(successData).append("\n");
            result.append("2. 失败回调: ").append(failedData).append("\n");
            result.append("3. 处理中回调: ").append(processingData).append("\n");
            result.append("4. 重试中回调: ").append(retryingData).append("\n");
            result.append("5. 状态枚举: ");
            for (CallbackStatus status : CallbackStatus.values()) {
                result.append(status.getCode()).append("(").append(status.getDescription()).append(") ");
            }

            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            logger.error("配置测试异常", e);
            return ResponseEntity.internalServerError().body("配置测试异常: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Callback test service is healthy");
    }
}
