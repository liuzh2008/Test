package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 状态转换记录实体
 * 
 * 用于记录Prompt状态变更的详细历史信息
 * 支持状态变更审计和问题追踪
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@Entity
@Table(name = "status_transition_history")
public class StatusTransitionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * 关联的Prompt ID
     */
    @Column(name = "prompt_id", nullable = false)
    private Integer promptId;
    
    /**
     * 转换前状态
     */
    @Column(name = "from_status", length = 50)
    private String fromStatus;
    
    /**
     * 转换后状态
     */
    @Column(name = "to_status", length = 50, nullable = false)
    private String toStatus;
    
    /**
     * 状态转换原因
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    /**
     * 操作者信息
     */
    @Column(name = "operator_info", length = 200)
    private String operatorInfo;
    
    /**
     * 转换时间
     */
    @Column(name = "transition_time", nullable = false)
    private LocalDateTime transitionTime;
    
    /**
     * 操作耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;
    
    /**
     * 是否成功
     */
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 操作时的版本号
     */
    @Column(name = "version_at_time")
    private Integer versionAtTime;
    
    /**
     * 额外的上下文信息（JSON格式）
     */
    @Column(name = "context_info", columnDefinition = "TEXT")
    private String contextInfo;
    
    // 默认构造函数
    public StatusTransitionRecord() {}
    
    // 构造函数
    public StatusTransitionRecord(Integer promptId, String fromStatus, String toStatus, 
                                 String reason, String operatorInfo, Boolean success) {
        this.promptId = promptId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.operatorInfo = operatorInfo;
        this.success = success;
        this.transitionTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getPromptId() {
        return promptId;
    }
    
    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
    }
    
    public String getFromStatus() {
        return fromStatus;
    }
    
    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }
    
    public String getToStatus() {
        return toStatus;
    }
    
    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getOperatorInfo() {
        return operatorInfo;
    }
    
    public void setOperatorInfo(String operatorInfo) {
        this.operatorInfo = operatorInfo;
    }
    
    public LocalDateTime getTransitionTime() {
        return transitionTime;
    }
    
    public void setTransitionTime(LocalDateTime transitionTime) {
        this.transitionTime = transitionTime;
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getVersionAtTime() {
        return versionAtTime;
    }
    
    public void setVersionAtTime(Integer versionAtTime) {
        this.versionAtTime = versionAtTime;
    }
    
    public String getContextInfo() {
        return contextInfo;
    }
    
    public void setContextInfo(String contextInfo) {
        this.contextInfo = contextInfo;
    }
    
    @Override
    public String toString() {
        return String.format("StatusTransitionRecord{id=%d, promptId=%d, %s->%s, time=%s, success=%s}", 
                           id, promptId, fromStatus, toStatus, transitionTime, success);
    }
}