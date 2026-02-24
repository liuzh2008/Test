package com.example.medaiassistant.dto;

import com.example.medaiassistant.model.CallbackStatus;
import java.time.LocalDateTime;

/**
 * 异步回调数据结构
 * 用于主应用(8081)和执行服务器(8082)之间的异步回调通信
 * 
 * 包含字段：
 * - dataId: 数据唯一标识
 * - status: 处理状态
 * - result: 处理结果
 * - timestamp: 时间戳
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
public class CallbackData {

    /**
     * 数据唯一标识
     */
    private String dataId;

    /**
     * 处理状态
     */
    private CallbackStatus status;

    /**
     * 处理结果
     */
    private String result;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 默认构造函数
     */
    public CallbackData() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 带参数的构造函数
     * 
     * @param dataId 数据唯一标识
     * @param status 处理状态
     * @param result 处理结果
     */
    public CallbackData(String dataId, CallbackStatus status, String result) {
        this.dataId = dataId;
        this.status = status;
        this.result = result;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 带重试次数的构造函数
     * 
     * @param dataId     数据唯一标识
     * @param status     处理状态
     * @param result     处理结果
     * @param retryCount 重试次数
     */
    public CallbackData(String dataId, CallbackStatus status, String result, Integer retryCount) {
        this.dataId = dataId;
        this.status = status;
        this.result = result;
        this.retryCount = retryCount;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public CallbackStatus getStatus() {
        return status;
    }

    public void setStatus(CallbackStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public String toString() {
        return "CallbackData{" +
                "dataId='" + dataId + '\'' +
                ", status=" + status +
                ", result='" + result + '\'' +
                ", timestamp=" + timestamp +
                ", retryCount=" + retryCount +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
