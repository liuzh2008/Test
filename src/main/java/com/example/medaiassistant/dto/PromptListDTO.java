package com.example.medaiassistant.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class PromptListDTO {
    private Integer promptId;
    private String patientId;
    private String promptTemplateName;
    private String objectiveContent;
    private String dailyRecords;
    private Integer priority;
    /**
     * 提交时间
     * 记录Prompt的提交时间，使用标准日期时间格式序列化
     * 
     * @see JsonFormat
     * @see LocalDateTime
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submissionTime;
    private String statusName;

    // 构造函数
    public PromptListDTO(Integer promptId, String patientId, String promptTemplateName, 
                        String objectiveContent, String dailyRecords, Integer priority,
                        LocalDateTime submissionTime, String statusName) {
        this.promptId = promptId;
        this.patientId = patientId;
        this.promptTemplateName = promptTemplateName;
        this.objectiveContent = objectiveContent;
        this.dailyRecords = dailyRecords;
        this.priority = priority;
        this.submissionTime = submissionTime;
        this.statusName = statusName;
    }

    // Getter和Setter方法
    public Integer getPromptId() {
        return promptId;
    }

    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
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

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}
