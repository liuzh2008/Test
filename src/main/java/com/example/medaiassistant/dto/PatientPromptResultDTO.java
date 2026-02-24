package com.example.medaiassistant.dto;

import java.time.LocalDateTime;

public class PatientPromptResultDTO {
    private Integer resultId;
    private Integer promptId;
    private String originalResultContent;
    private String modifiedResultContent;
    private String status;
    private LocalDateTime executionTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer lastModifiedBy;
    private Integer isRead;

    private String promptTemplateName;
    private String objectiveContent;
    private String dailyRecords;
    private String statusName;

    // 无参构造函数
    public PatientPromptResultDTO() {
    }

    // 带所有参数的构造函数
    public PatientPromptResultDTO(Integer resultId, Integer promptId, String originalResultContent,
            String modifiedResultContent, String status, LocalDateTime executionTime,
            LocalDateTime createdAt, LocalDateTime updatedAt, Integer lastModifiedBy,
            Integer isRead, String promptTemplateName, String objectiveContent,
            String dailyRecords, String statusName) {
        this.resultId = resultId;
        this.promptId = promptId;
        this.originalResultContent = originalResultContent;
        this.modifiedResultContent = modifiedResultContent;
        this.status = status;
        this.executionTime = executionTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastModifiedBy = lastModifiedBy;
        this.isRead = isRead;
        this.promptTemplateName = promptTemplateName;
        this.objectiveContent = objectiveContent;
        this.dailyRecords = dailyRecords;
        this.statusName = statusName;
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

    public String getOriginalResultContent() {
        return originalResultContent;
    }

    public void setOriginalResultContent(String originalResultContent) {
        this.originalResultContent = originalResultContent;
    }

    public String getModifiedResultContent() {
        return modifiedResultContent;
    }

    public void setModifiedResultContent(String modifiedResultContent) {
        this.modifiedResultContent = modifiedResultContent;
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

    public Integer getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Integer lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public String getPromptTemplateName() {
        return promptTemplateName;
    }

    public void setPromptTemplateName(String promptTemplateName) {
        this.promptTemplateName = promptTemplateName;
    }

    public String getObjectiveContent() {
        return objectiveContent;
    }

    public void setObjectiveContent(String objectiveContent) {
        this.objectiveContent = objectiveContent;
    }

    public String getDailyRecords() {
        return dailyRecords;
    }

    public void setDailyRecords(String dailyRecords) {
        this.dailyRecords = dailyRecords;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}
