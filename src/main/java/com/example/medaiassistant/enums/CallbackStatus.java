package com.example.medaiassistant.enums;

/**
 * 异步回调状态枚举
 * 定义异步回调处理过程中的各种状态
 * 
 * 状态说明：
 * - PENDING: 等待处理
 * - PROCESSING: 处理中
 * - SUCCESS: 处理成功
 * - FAILED: 处理失败
 * - RETRYING: 重试中
 * 
 * @since 2025-09-12
 * @author Cline
 * @version 1.0
 */
public enum CallbackStatus {

    /**
     * 等待处理
     */
    PENDING("PENDING", "等待处理"),

    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 处理成功
     */
    SUCCESS("SUCCESS", "处理成功"),

    /**
     * 处理失败
     */
    FAILED("FAILED", "处理失败"),

    /**
     * 重试中
     */
    RETRYING("RETRYING", "重试中");

    private final String code;
    private final String description;

    CallbackStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举
     * 
     * @param code 状态码
     * @return 对应的枚举，如果找不到返回null
     */
    public static CallbackStatus fromCode(String code) {
        for (CallbackStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为最终状态（不再需要重试的状态）
     * 
     * @return 如果是最终状态返回true，否则返回false
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED;
    }

    /**
     * 判断是否为可重试状态
     * 
     * @return 如果是可重试状态返回true，否则返回false
     */
    public boolean isRetryable() {
        return this == FAILED || this == RETRYING;
    }

    @Override
    public String toString() {
        return code;
    }
}
