package com.example.medaiassistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 结果接收数据传输对象（DTO）
 * 
 * 用于接收执行服务器(8082)异步处理完成后回调的AI处理结果。
 * 包含处理结果的所有相关信息，包括状态、内容、错误信息等。
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2025-09-13
 */
public class ReceiveResultDTO {
    
    /**
     * 数据唯一标识符
     * 用于追踪处理请求，必填字段
     */
    @NotBlank(message = "dataId不能为空")
    private String dataId;
    
    /**
     * AI处理的结果内容
     * 当处理成功时包含具体的处理结果
     */
    private String content;
    
    /**
     * 结果标题
     * 用于描述结果的简短标题
     */
    private String title;
    
    /**
     * 时间戳
     * 结果生成的时间，格式为字符串
     */
    private String timestamp;
    
    /**
     * 关联的Prompt ID
     * 标识处理请求对应的Prompt模板
     */
    private Integer promptId;
    
    /**
     * 患者ID
     * 结果关联的患者标识
     */
    private String patientId;
    
    /**
     * 原始内容
     * 处理前的原始数据内容
     */
    private String originalContent;
    
    /**
     * 最后修改人ID
     * 标识最后修改结果的用户
     */
    private Integer lastModifiedBy;
    
    /**
     * 是否已读标记
     * 0-未读，1-已读
     */
    private Integer isRead;
    
    /**
     * 处理状态
     * 标识处理结果的状态：SUCCESS-成功，ERROR-失败
     */
    private String status;
    
    /**
     * 错误信息
     * 当处理失败时包含具体的错误描述
     */
    private String errorMessage;

    // Getters and Setters
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

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

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
