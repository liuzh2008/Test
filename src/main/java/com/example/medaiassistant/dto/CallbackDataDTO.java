package com.example.medaiassistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
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
public class CallbackDataDTO {

    /**
     * 数据唯一标识
     */
    @NotNull(message = "dataId不能为空")
    private String dataId;

    /**
     * 处理状态
     * 可能的值：PENDING, PROCESSING, SUCCESS, FAILED, RETRYING
     */
    @NotNull(message = "状态不能为空")
    private String status;

    /**
     * 处理结果
     */
    private Object result;

    /**
     * 时间戳
     */
    @NotNull(message = "时间戳不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 错误信息（可选）
     */
    private String errorMessage;

    /**
     * 重试次数（可选）
     */
    private Integer retryCount;

    // 构造函数
    public CallbackDataDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public CallbackDataDTO(String dataId, String status, Object result) {
        this.dataId = dataId;
        this.status = status;
        this.result = result;
        this.timestamp = LocalDateTime.now();
    }

    public CallbackDataDTO(String dataId, String status, Object result, String errorMessage) {
        this.dataId = dataId;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "CallbackDataDTO{" +
                "dataId='" + dataId + '\'' +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", timestamp=" + timestamp +
                ", errorMessage='" + errorMessage + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
