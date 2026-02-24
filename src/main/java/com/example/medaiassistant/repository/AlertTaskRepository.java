package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.AlertTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 告警任务数据访问接口
 * 
 * 该接口提供了对告警任务实体的基本CRUD操作，以及一些自定义的查询方法。
 * 通过继承JpaRepository，自动获得了常用的数据库操作方法。
 */
@Repository
public interface AlertTaskRepository extends JpaRepository<AlertTask, Integer> {
    /**
     * 根据规则ID、患者ID和任务状态查找告警任务
     * 
     * 该方法用于查找具有特定规则ID、患者ID和任务状态的告警任务列表。
     * 主要用于避免创建重复的告警任务。
     * 
     * @param ruleId 告警规则ID
     * @param patientId 患者ID
     * @param taskStatus 任务状态
     * @return 符合条件的告警任务列表
     */
    List<AlertTask> findByRuleIdAndPatientIdAndTaskStatus(Integer ruleId, String patientId, AlertTask.TaskStatus taskStatus);
    
    /**
     * 根据患者ID和任务状态查找告警任务
     * 
     * 该方法用于查找特定患者ID且任务状态为"待处理"的告警任务列表。
     * 主要用于获取患者的待处理任务列表。
     * 
     * @param patientId 患者ID
     * @param taskStatus 任务状态
     * @return 符合条件的告警任务列表
     */
    List<AlertTask> findByPatientIdAndTaskStatus(String patientId, AlertTask.TaskStatus taskStatus);
    
    /**
     * 根据患者ID、任务类型和任务状态查找告警任务
     * 
     * 该方法用于查找特定患者ID、任务类型且任务状态为"待处理"的告警任务列表。
     * 主要用于获取患者的特定类型待处理任务列表。
     * 
     * @param patientId 患者ID
     * @param taskType 任务类型
     * @param taskStatus 任务状态
     * @return 符合条件的告警任务列表
     */
    List<AlertTask> findByPatientIdAndTaskTypeAndTaskStatus(String patientId, String taskType, AlertTask.TaskStatus taskStatus);
    
    /**
     * 根据任务ID更新任务状态
     * 
     * 该方法用于更新指定任务ID的任务状态。通过JPQL执行更新操作，直接修改数据库中的记录。
     * 该方法使用@Modifying注解标识为修改操作，使用@Transactional注解确保事务性，
     * 使用@Query注解定义JPQL更新语句，使用@Param注解绑定参数。
     * 
     * @param taskId 任务ID，用于标识要更新的告警任务，必须是数据库中已存在的任务ID
     * @param taskStatus 新的任务状态，必须是AlertTask.TaskStatus枚举中的有效值
     * @return int 更新的记录数，0表示没有记录被更新，大于0表示成功更新的记录数
     * @since 2025-08-07
     * @author Cline
     * @see AlertTask.TaskStatus
     * @see org.springframework.data.jpa.repository.Modifying
     * @see org.springframework.transaction.annotation.Transactional
     * @see org.springframework.data.jpa.repository.Query
     * @see org.springframework.data.repository.query.Param
     */
    @Modifying
    @Transactional
    @Query("UPDATE AlertTask t SET t.taskStatus = :taskStatus WHERE t.taskId = :taskId")
    int updateTaskStatusByTaskId(@Param("taskId") Integer taskId, @Param("taskStatus") AlertTask.TaskStatus taskStatus);
}
