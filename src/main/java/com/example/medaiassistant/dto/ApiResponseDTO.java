package com.example.medaiassistant.dto;

import lombok.Data;

/**
 * 统一API响应DTO
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Data
public class ApiResponseDTO<T> {
    
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间戳
     */
    private Long timestamp;
    
    /**
     * 默认构造函数
     */
    public ApiResponseDTO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 带参数构造函数
     */
    public ApiResponseDTO(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     * 
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（带自定义消息）
     * 
     * @param message 响应消息
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> ApiResponseDTO<T> success(String message, T data) {
        return new ApiResponseDTO<>(200, message, data);
    }
    
    /**
     * 失败响应
     * 
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(500, message, null);
    }
    
    /**
     * 失败响应（带状态码）
     * 
     * @param code 状态码
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> ApiResponseDTO<T> error(Integer code, String message) {
        return new ApiResponseDTO<>(code, message, null);
    }
    
    /**
     * 参数错误响应
     * 
     * @param message 错误消息
     * @return 参数错误响应对象
     */
    public static <T> ApiResponseDTO<T> badRequest(String message) {
        return new ApiResponseDTO<>(400, message, null);
    }
    
    /**
     * 未授权响应
     * 
     * @param message 错误消息
     * @return 未授权响应对象
     */
    public static <T> ApiResponseDTO<T> unauthorized(String message) {
        return new ApiResponseDTO<>(401, message, null);
    }
    
    /**
     * 未找到资源响应
     * 
     * @param message 错误消息
     * @return 未找到资源响应对象
     */
    public static <T> ApiResponseDTO<T> notFound(String message) {
        return new ApiResponseDTO<>(404, message, null);
    }
    
    /**
     * 业务逻辑错误响应
     * 
     * @param message 错误消息
     * @return 业务逻辑错误响应对象
     */
    public static <T> ApiResponseDTO<T> businessError(String message) {
        return new ApiResponseDTO<>(422, message, null);
    }
    
    /**
     * 判断响应是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    /**
     * 判断响应是否失败
     * 
     * @return 是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    /**
     * 获取状态描述
     * 
     * @return 状态描述
     */
    public String getStatusDescription() {
        if (isSuccess()) {
            return "成功";
        } else if (code != null) {
            switch (code) {
                case 400:
                    return "请求参数错误";
                case 401:
                    return "未授权";
                case 403:
                    return "禁止访问";
                case 404:
                    return "资源未找到";
                case 422:
                    return "业务逻辑错误";
                case 500:
                    return "服务器内部错误";
                default:
                    return "未知错误";
            }
        }
        return "未知状态";
    }
    
    /**
     * 获取响应时间（格式化）
     * 
     * @return 格式化后的响应时间
     */
    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
    
    @Override
    public String toString() {
        return "ApiResponseDTO{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + (data != null ? data.getClass().getSimpleName() : "null") +
                ", timestamp=" + getFormattedTimestamp() +
                '}';
    }
}
