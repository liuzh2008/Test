package com.example.medaiassistant.hospital.model;

/**
 * 同步任务优先级枚举
 * 定义医院数据同步任务的优先级
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-03
 */
public enum SyncTaskPriority {
    
    /**
     * 高优先级 - 紧急数据同步任务
     */
    HIGH(1, "高优先级"),
    
    /**
     * 中优先级 - 常规数据同步任务
     */
    MEDIUM(2, "中优先级"),
    
    /**
     * 低优先级 - 后台数据同步任务
     */
    LOW(3, "低优先级");
    
    private final int value;
    private final String description;
    
    SyncTaskPriority(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * 获取优先级数值
     * 数值越小优先级越高
     * 
     * @return 优先级数值
     */
    public int getValue() {
        return value;
    }
    
    /**
     * 获取优先级描述
     * 
     * @return 优先级描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据数值获取优先级枚举
     * 
     * @param value 优先级数值
     * @return 对应的优先级枚举，如果数值无效返回MEDIUM
     */
    public static SyncTaskPriority fromValue(int value) {
        for (SyncTaskPriority priority : values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        return MEDIUM; // 默认返回中优先级
    }
    
    /**
     * 检查当前优先级是否高于指定优先级
     * 
     * @param other 要比较的优先级
     * @return 如果当前优先级高于指定优先级返回true
     */
    public boolean isHigherThan(SyncTaskPriority other) {
        return this.value < other.value;
    }
    
    /**
     * 检查当前优先级是否低于指定优先级
     * 
     * @param other 要比较的优先级
     * @return 如果当前优先级低于指定优先级返回true
     */
    public boolean isLowerThan(SyncTaskPriority other) {
        return this.value > other.value;
    }
    
    @Override
    public String toString() {
        return description + "(" + value + ")";
    }
}
