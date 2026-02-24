package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警规则实体类，映射alert_rules表
 * 
 * @author Cline
 * @since 2025-08-05
 */
@Entity
@Table(name = "alert_rules")
public class AlertRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;
    
    @Column(name = "rule_name", length = 100)
    private String ruleName;
    
    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column(name = "is_active")
    private Integer isActive;
    
    @Column(name = "priority")
    private Integer priority;
    
    @Column(name = "trigger_conditions", columnDefinition = "json")
    private String triggerConditions;
    
    @Column(name = "alert_content", columnDefinition = "text")
    private String alertContent;
    
    @Column(name = "required_actions", columnDefinition = "json")
    private String requiredActions;
    
    @Column(name = "target_roles", columnDefinition = "json")
    private String targetRoles;
    
    @Column(name = "alert_method")
    @Enumerated(EnumType.STRING)
    private AlertMethod alertMethod;
    
    @Column(name = "escalation_rules", columnDefinition = "json")
    private String escalationRules;
    
    @Column(name = "creator_id", length = 32)
    private String creatorId;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    // 枚举类型定义
    public enum RuleType {
        医嘱, 状态, 时间, 质控
    }
    
    public enum AlertMethod {
        弹窗, 短信, 站内信
    }
    
    // Getters and Setters
    public Integer getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public RuleType getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    
    public Integer getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getTriggerConditions() {
        return triggerConditions;
    }
    
    public void setTriggerConditions(String triggerConditions) {
        this.triggerConditions = triggerConditions;
    }
    
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
    
    public String getTargetRoles() {
        return targetRoles;
    }
    
    public void setTargetRoles(String targetRoles) {
        this.targetRoles = targetRoles;
    }
    
    public AlertMethod getAlertMethod() {
        return alertMethod;
    }
    
    public void setAlertMethod(AlertMethod alertMethod) {
        this.alertMethod = alertMethod;
    }
    
    public String getEscalationRules() {
        return escalationRules;
    }
    
    public void setEscalationRules(String escalationRules) {
        this.escalationRules = escalationRules;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}
