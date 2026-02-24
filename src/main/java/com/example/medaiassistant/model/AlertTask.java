package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警任务实体类，映射alert_tasks表
 * 
 * 该实体类表示系统中的告警任务，用于跟踪需要处理的告警事项。
 * 每个告警任务都与特定的告警规则和患者相关联。
 */
@Entity
@Table(name = "alert_tasks")
public class AlertTask {
    
    /**
     * 任务ID，主键，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;
    
    /**
     * 关联的告警规则ID
     */
    @Column(name = "rule_id")
    private Integer ruleId;
    
    /**
     * 关联的患者ID
     */
    @Column(name = "patient_id", length = 32)
    private String patientId;
    
    /**
     * 任务类型
     */
    @Column(name = "task_type", length = 50)
    private String taskType;
    
    /**
     * 任务内容，JSON格式
     */
    @Column(name = "task_content", columnDefinition = "json")
    private String taskContent;
    
    /**
     * 任务状态
     */
    @Column(name = "task_status")
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;
    
    /**
     * 截止时间
     */
    @Column(name = "deadline")
    private LocalDateTime deadline;
    
    /**
     * 指派角色，JSON格式
     */
    @Column(name = "assignee_roles", columnDefinition = "json")
    private String assigneeRoles;
    
    /**
     * 创建时间
     */
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    /**
     * 下次任务时间
     */
    @Column(name = "next_task_time")
    private LocalDateTime nextTaskTime;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_time")
    private LocalDateTime completedTime;
    
    /**
     * 任务状态枚举
     * 
     * 待处理: 任务已创建但尚未开始处理
     * 进行中: 任务正在被处理
     * 已完成: 任务已处理完成
     */
    public enum TaskStatus {
        待处理, 进行中, 已完成
    }
    
    // Getters and Setters
    public Integer getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }
    
    public Integer getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public String getTaskContent() {
        return taskContent;
    }
    
    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }
    
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }
    
    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
    
    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
    
    public String getAssigneeRoles() {
        return assigneeRoles;
    }
    
    public void setAssigneeRoles(String assigneeRoles) {
        this.assigneeRoles = assigneeRoles;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
    
    public LocalDateTime getNextTaskTime() {
        return nextTaskTime;
    }
    
    public void setNextTaskTime(LocalDateTime nextTaskTime) {
        this.nextTaskTime = nextTaskTime;
    }
}
