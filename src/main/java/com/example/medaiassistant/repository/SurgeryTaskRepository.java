package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.SurgeryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 手术任务数据访问接口
 * 提供手术任务数据的CRUD操作
 */
@Repository
public interface SurgeryTaskRepository extends JpaRepository<SurgeryTask, Integer> {
    
    /**
     * 根据患者ID查询手术任务
     * @param patientId 患者ID
     * @return 手术任务列表
     */
    List<SurgeryTask> findByPatientId(String patientId);
    
    /**
     * 根据患者ID和任务状态查询手术任务
     * @param patientId 患者ID
     * @param taskStatus 任务状态
     * @return 手术任务列表
     */
    List<SurgeryTask> findByPatientIdAndTaskStatus(String patientId, SurgeryTask.TaskStatus taskStatus);
}
