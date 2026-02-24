package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Prompt实体类
 * 
 * 表示AI分析任务的Prompt记录，包含任务的所有相关信息。
 * 注意：由于Oracle数据库PROMPTS表没有VERSION列，已移除@Version字段。
 * 
 * @author MedAI Assistant Team
 * @version 2.0.0
 * @since 2025-09-14
 */
@Entity
@Table(name = "prompts")
public class Prompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromptId")
    private Integer promptId;

    @Column(name = "UserId")
    private Integer userId;

    @Column(name = "PatientId")
    private String patientId;

    @Column(name = "PromptTemplateName")
    private String promptTemplateName;

    @Column(name = "ObjectiveContent", columnDefinition = "LONGTEXT")
    private String objectiveContent;

    @Column(name = "DailyRecords", columnDefinition = "TEXT")
    private String dailyRecords;

    @Column(name = "StatusName")
    private String statusName;

    @Column(name = "Priority")
    private Integer priority;

    /**
     * 提交时间
     * 记录Prompt的提交时间，使用标准日期时间格式序列化
     * 
     * @see JsonFormat
     * @see LocalDateTime
     */
    @Column(name = "SubmissionTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submissionTime;

    @Column(name = "PromptTemplateContent", columnDefinition = "TEXT")
    private String promptTemplateContent;

    @Column(name = "GeneratedBy", columnDefinition = "TEXT")
    private String generatedBy;

    @Column(name = "SortNumber")
    private Integer sortNumber;

    @Column(name = "EstimatedWaitTime")
    private Integer estimatedWaitTime;

    @Column(name = "ExecutionTime")
    private LocalDateTime executionTime;

    @Column(name = "ResultId")
    private Integer resultId;

    @Column(name = "ExecutionServerId")
    private Integer executionServerId;

    @Column(name = "GenerateCostTime", columnDefinition = "TEXT")
    private String generateCostTime;

    @Column(name = "ProcessingStartTime")
    private LocalDateTime processingStartTime;

    @Column(name = "ProcessingEndTime")
    private LocalDateTime processingEndTime;

    @Column(name = "RetryCount")
    private Integer retryCount;

    @Column(name = "ExecutionResult", columnDefinition = "LONGTEXT")
    private String executionResult;

    // Getters and Setters

    // Getters and Setters
    public Integer getPromptId() {
        return promptId;
    }

    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
    }

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

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
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

    public String getPromptTemplateContent() {
        return promptTemplateContent;
    }

    public void setPromptTemplateContent(String promptTemplateContent) {
        this.promptTemplateContent = promptTemplateContent;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
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

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }
}
