package com.example.medaiassistant.dto;

/**
 * 告警规则内容DTO
 * 用于封装告警规则的alert_content和required_actions字段
 */
public class AlertRuleContentDTO {
    private String alertContent;
    private String requiredActions;
    
    public AlertRuleContentDTO() {
    }
    
    public AlertRuleContentDTO(String alertContent, String requiredActions) {
        this.alertContent = alertContent;
        this.requiredActions = requiredActions;
    }
    
    // Getters and Setters
    public String getAlertContent() {
        return alertContent;
    }
    
    public void setAlertContent(String alertContent) {
        this.alertContent = alertContent;
    }
    
    public String getRequiredActions() {
        return requiredActions;
    }
    
    public void setRequiredActions(String requiredActions) {
        this.requiredActions = requiredActions;
    }
}
