package com.example.medaiassistant.model;

/**
 * 数据状态枚举
 * 定义加密数据临时表的状态流转
 * 
 * @author System
 * @version 1.0
 * @since 2025-09-11
 */
public enum DataStatus {
    /**
     * 已接收 - 数据已被接收但尚未解密
     */
    RECEIVED("RECEIVED", "已接收"),
    
    /**
     * 已解密 - 数据已成功解密
     */
    DECRYPTED("DECRYPTED", "已解密"),
    
    /**
     * 处理中 - 数据正在处理中
     */
    PROCESSING("PROCESSING", "处理中"),
    
    /**
     * 已处理 - 数据处理完成
     */
    PROCESSED("PROCESSED", "已处理"),
    
    /**
     * 已加密 - 处理结果已加密
     */
    ENCRYPTED("ENCRYPTED", "已加密"),
    
    /**
     * 已发送 - 加密结果已发送
     */
    SENT("SENT", "已发送"),
    
    /**
     * 错误 - 处理过程中发生错误
     */
    ERROR("ERROR", "错误");

    private final String code;
    private final String description;

    DataStatus(String code, String description) {
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
     * 根据状态码获取枚举值
     * 
     * @param code 状态码字符串
     * @return 对应的DataStatus枚举值
     * @throws IllegalArgumentException 如果状态码不存在
     */
    public static DataStatus fromCode(String code) {
        for (DataStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }

    /**
     * 检查是否允许从当前状态转换到目标状态
     * 
     * @param targetStatus 目标状态
     * @return true如果允许转换，false否则
     */
    public boolean canTransitionTo(DataStatus targetStatus) {
        switch (this) {
            case RECEIVED:
                return targetStatus == DECRYPTED || targetStatus == ERROR;
            case DECRYPTED:
                return targetStatus == PROCESSING || targetStatus == ERROR;
            case PROCESSING:
                return targetStatus == PROCESSED || targetStatus == ERROR;
            case PROCESSED:
                return targetStatus == ENCRYPTED || targetStatus == ERROR;
            case ENCRYPTED:
                return targetStatus == SENT || targetStatus == ERROR;
            case SENT:
                return false; // 最终状态，不允许再转换
            case ERROR:
                return false; // 错误状态，不允许再转换
            default:
                return false;
        }
    }

    /**
     * 检查是否为最终状态
     * 
     * @return true如果是最终状态（SENT或ERROR），false否则
     */
    public boolean isFinal() {
        return this == SENT || this == ERROR;
    }

    @Override
    public String toString() {
        return code;
    }
}
