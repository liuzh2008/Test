package com.example.medaiassistant.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 患者Prompt详情轻量级数据传输对象
 * 用于快速列表显示，不包含大文本内容以提高性能
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-23
 */
public class PromptDetailSimpleDTO {
    private Integer resultId;
    private Integer promptId;
    private String promptTemplateName;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executionTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String statusName;
    private String objectiveContentPreview;
    private String dailyRecordsPreview;
    private Integer isRead;

    public PromptDetailSimpleDTO() {
    }

    public PromptDetailSimpleDTO(Integer resultId, Integer promptId, String promptTemplateName, 
                               String status, LocalDateTime executionTime, LocalDateTime createdAt, 
                               LocalDateTime updatedAt, String statusName, Integer isRead) {
        this.resultId = resultId;
        this.promptId = promptId;
        this.promptTemplateName = promptTemplateName;
        this.status = status;
        this.executionTime = executionTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusName = statusName;
        this.isRead = isRead;
    }

    // Getters and Setters
    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public Integer getPromptId() {
        return promptId;
    }

    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
    }

    public String getPromptTemplateName() {
        return promptTemplateName;
    }

    public void setPromptTemplateName(String promptTemplateName) {
        this.promptTemplateName = promptTemplateName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getObjectiveContentPreview() {
        return objectiveContentPreview;
    }

    public void setObjectiveContentPreview(String objectiveContentPreview) {
        this.objectiveContentPreview = objectiveContentPreview;
    }

    public String getDailyRecordsPreview() {
        return dailyRecordsPreview;
    }

    public void setDailyRecordsPreview(String dailyRecordsPreview) {
        this.dailyRecordsPreview = dailyRecordsPreview;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }
}
