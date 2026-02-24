package com.example.medaiassistant.dto;

import lombok.Data;

/**
 * 快照生成响应DTO
 * 
 * 用于返回快照生成的结果
 * 
 * @author Cline
 * @since 2025-10-24
 */
@Data
public class SnapshotResponseDTO {
    
    /**
     * 快照ID
     * 如果为null，表示输入集合未变化，跳过快照创建
     */
    private Long snapshotId;
    
    /**
     * 状态
     * CREATED - 成功创建快照
     * SKIPPED - 跳过快照创建
     * ERROR - 快照生成失败
     */
    private String status;
    
    /**
     * 消息描述
     */
    private String message;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 默认构造函数
     */
    public SnapshotResponseDTO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 带参数的构造函数
     */
    public SnapshotResponseDTO(Long snapshotId, String status, String message) {
        this();
        this.snapshotId = snapshotId;
        this.status = status;
        this.message = message;
    }
}
