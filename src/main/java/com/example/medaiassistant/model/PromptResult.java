package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Prompt结果实体类
 * 存储AI生成的Prompt执行结果信息
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-23
 */
@Entity
@Table(name = "promptresult")
public class PromptResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ResultId")
    private Integer resultId;

    @Column(name = "PromptId")
    private Integer promptId;

    @Column(name = "OriginalResultContent", columnDefinition = "TEXT")
    private String originalResultContent;

    @Column(name = "ModifiedResultContent", columnDefinition = "TEXT")
    private String modifiedResultContent;

    @Column(name = "Status")
    private String status;

    @Column(name = "ExecutionTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executionTime;

    @Column(name = "CreatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "LastModifiedBy")
    private Integer lastModifiedBy;

    @Column(name = "IsRead")
    private Integer isRead;

    @Column(name = "deleted")
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
