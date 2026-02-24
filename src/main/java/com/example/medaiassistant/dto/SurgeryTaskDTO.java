package com.example.medaiassistant.dto;

import com.example.medaiassistant.model.SurgeryTask;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 手术任务数据传输对象
 * 用于前端与后端之间的数据传输，封装手术任务的完整信息
 * 
 * @author MedAI System
 * @version 0.3.004
 * @since 2026-02-24
 */
@Data
public class SurgeryTaskDTO {
    /**
     * 任务ID（主键）
     * 数据库自增生成，新建任务时为null
     */
    private Integer taskId;
    
    /**
     * 手术日期
     * 格式：yyyy-MM-dd（如：2026-02-24）
     * 使用@JsonFormat注解支持前端纯日期字符串反序列化
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date surgeryDate;
    
    /**
     * 手术名称
     * 如：冠状动脉造影术、阑尾切除术等
     */
    private String surgeryName;
    
    /**
     * 麻醉类型
     * 如：全身麻醉、局部麻醉、椎管内麻醉等
     */
    private String anesthesiaType;
    
    /**
     * 术前讨论主持人
     * 通常为主刀医生或科室主任
     */
    private String preopDiscussionHost;
    
    /**
     * 术前讨论参与者
     * 多个参与者使用逗号分隔
     * 如："张医生,王医生,赵护士"
     */
    private String preopDiscussionParticipants;
    
    /**
     * 手术风险评估
     * 详细记录手术的风险级别、可能并发症、注意事项等
     * 支持多行文本
     */
    private String surgeryRiskAssessment;
    
    /**
     * 已完成任务列表
     * JSON格式字符串，记录手术前后各项任务的完成情况
     * 如："{\"术前检查\":true,\"麻醉评估\":true}"
     */
    private String completedTasks;
    
    /**
     * 患者ID
     * 关联到PATIENTS表的patient_id字段
     */
    private String patientId;
    
    /**
     * 科室ID
     * 关联到科室表的department_id字段
     */
    private Integer departmentId;
    
    /**
     * 医生ID（主刀医生）
     * 关联到用户表的user_id字段
     */
    private Integer surgeonId;
    
    /**
     * 任务状态
     * 枚举值：计划中、进行中、已完成、已取消
     * @see SurgeryTask.TaskStatus
     */
    private SurgeryTask.TaskStatus taskStatus;
    
    /**
     * 任务创建时间
     * 格式：yyyy-MM-dd HH:mm:ss（如：2026-02-24 10:30:00）
     * 由数据库自动生成
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
    
    /**
     * 任务更新时间
     * 格式：yyyy-MM-dd HH:mm:ss（如：2026-02-24 15:30:00）
     * 每次更新时自动刷新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

    /**
     * 从实体对象转换为DTO对象
     * @param surgeryTask 手术任务实体对象
     * @return 手术任务DTO对象
     */
    public static SurgeryTaskDTO fromEntity(SurgeryTask surgeryTask) {
        SurgeryTaskDTO dto = new SurgeryTaskDTO();
        dto.setTaskId(surgeryTask.getTaskId());
        dto.setSurgeryDate(surgeryTask.getSurgeryDate());
        dto.setSurgeryName(surgeryTask.getSurgeryName());
        dto.setAnesthesiaType(surgeryTask.getAnesthesiaType());
        dto.setPreopDiscussionHost(surgeryTask.getPreopDiscussionHost());
        dto.setPreopDiscussionParticipants(surgeryTask.getPreopDiscussionParticipants());
        dto.setSurgeryRiskAssessment(surgeryTask.getSurgeryRiskAssessment());
        dto.setCompletedTasks(surgeryTask.getCompletedTasks());
        dto.setPatientId(surgeryTask.getPatientId());
        dto.setDepartmentId(surgeryTask.getDepartmentId());
        dto.setSurgeonId(surgeryTask.getSurgeonId());
        dto.setTaskStatus(surgeryTask.getTaskStatus());
        dto.setCreatedAt(surgeryTask.getCreatedAt());
        dto.setUpdatedAt(surgeryTask.getUpdatedAt());
        return dto;
    }

    /**
     * 从DTO对象转换为实体对象
     * @return 手术任务实体对象
     */
    public SurgeryTask toEntity() {
        SurgeryTask surgeryTask = new SurgeryTask();
        surgeryTask.setTaskId(this.taskId);
        surgeryTask.setSurgeryDate(this.surgeryDate);
        surgeryTask.setSurgeryName(this.surgeryName);
        surgeryTask.setAnesthesiaType(this.anesthesiaType);
        surgeryTask.setPreopDiscussionHost(this.preopDiscussionHost);
        surgeryTask.setPreopDiscussionParticipants(this.preopDiscussionParticipants);
        surgeryTask.setSurgeryRiskAssessment(this.surgeryRiskAssessment);
        surgeryTask.setCompletedTasks(this.completedTasks);
        surgeryTask.setPatientId(this.patientId);
        surgeryTask.setDepartmentId(this.departmentId);
        surgeryTask.setSurgeonId(this.surgeonId);
        surgeryTask.setTaskStatus(this.taskStatus);
        surgeryTask.setCreatedAt(this.createdAt);
        surgeryTask.setUpdatedAt(this.updatedAt);
        return surgeryTask;
    }
}
