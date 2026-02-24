package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * 手术任务实体类
 * 对应数据库表 surgery_tasks
 */
@Entity
@Table(name = "surgery_tasks")
@Data
public class SurgeryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @Column(name = "surgery_date")
    private Date surgeryDate;

    @Column(name = "surgery_name")
    private String surgeryName;

    @Column(name = "anesthesia_type")
    private String anesthesiaType;

    @Column(name = "preop_discussion_host")
    private String preopDiscussionHost;

    @Column(name = "preop_discussion_participants", columnDefinition = "TEXT")
    private String preopDiscussionParticipants;

    @Column(name = "surgery_risk_assessment", columnDefinition = "TEXT")
    private String surgeryRiskAssessment;

    @Column(name = "completed_tasks", columnDefinition = "JSON")
    private String completedTasks;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "surgeon_id")
    private Integer surgeonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status")
    private TaskStatus taskStatus;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 手术任务状态枚举
     */
    public enum TaskStatus {
        /** 计划中 */
        计划中, 
        /** 进行中 */
        进行中, 
        /** 已完成 */
        已完成, 
        /** 已取消 */
        已取消
    }
}
