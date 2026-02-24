package com.example.medaiassistant.dto;

import java.time.LocalDateTime;

public class SavePromptDTO {
    private Integer userId;
    private String patientId;
    private String promptTemplateName;
    private String objectiveContent;
    private String dailyRecords;
    private String promptTemplateContent;
    private Integer priority;
    private LocalDateTime submissionTime;
    private Integer sortNumber;
    private Integer estimatedWaitTime;
    private String statusName;
    private String generatedBy;
    private LocalDateTime executionTime;
    private Integer resultId;
    private Integer executionServerId;
    private String generateCostTime;
    private LocalDateTime processingStartTime;
    private LocalDateTime processingEndTime;
    private Integer retryCount;

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
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

    public String getPromptTemplateContent() {
        return promptTemplateContent;
    }

    public void setPromptTemplateContent(String promptTemplateContent) {
        this.promptTemplateContent = promptTemplateContent;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Integer getSortNumber() {
        return sortNumber;
    }

    public void setSortNumber(Integer sortNumber) {
        this.sortNumber = sortNumber;
    }

    public Integer getEstimatedWaitTime() {
        return estimatedWaitTime;
    }

    public void setEstimatedWaitTime(Integer estimatedWaitTime) {
        this.estimatedWaitTime = estimatedWaitTime;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public Integer getExecutionServerId() {
        return executionServerId;
    }

    public void setExecutionServerId(Integer executionServerId) {
        this.executionServerId = executionServerId;
    }

    public String getGenerateCostTime() {
        return generateCostTime;
    }

    public void setGenerateCostTime(String generateCostTime) {
        this.generateCostTime = generateCostTime;
    }

    public LocalDateTime getProcessingStartTime() {
        return processingStartTime;
    }

    public void setProcessingStartTime(LocalDateTime processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    public LocalDateTime getProcessingEndTime() {
        return processingEndTime;
    }

    public void setProcessingEndTime(LocalDateTime processingEndTime) {
        this.processingEndTime = processingEndTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}
