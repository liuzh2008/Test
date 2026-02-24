package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.StatusTransitionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态转换记录数据访问层
 * 
 * 提供状态转换历史的数据访问功能
 * 
 * @author MedAI Assistant Team
 * @version 3.0.0
 * @since 2025-10-01 (迭代3)
 */
@Repository
public interface StatusTransitionRecordRepository extends JpaRepository<StatusTransitionRecord, Long> {
    
    /**
     * 根据Prompt ID查询状态转换历史
     * 按转换时间倒序排列
     * 
     * @param promptId Prompt ID
     * @return 状态转换记录列表
     */
    List<StatusTransitionRecord> findByPromptIdOrderByTransitionTimeDesc(Integer promptId);
    
    /**
     * 根据Prompt ID和时间范围查询状态转换历史
     * 
     * @param promptId Prompt ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态转换记录列表
     */
    List<StatusTransitionRecord> findByPromptIdAndTransitionTimeBetweenOrderByTransitionTimeDesc(
            Integer promptId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询指定Prompt的最新状态转换记录
     * 
     * @param promptId Prompt ID
     * @return 最新的状态转换记录
     */
    @Query("SELECT s FROM StatusTransitionRecord s WHERE s.promptId = :promptId ORDER BY s.transitionTime DESC")
    List<StatusTransitionRecord> findLatestByPromptId(@Param("promptId") Integer promptId);
    
    /**
     * 根据状态查询转换记录
     * 
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @return 状态转换记录列表
     */
    List<StatusTransitionRecord> findByFromStatusAndToStatusOrderByTransitionTimeDesc(String fromStatus, String toStatus);
    
    /**
     * 查询失败的状态转换
     * 
     * @param success 是否成功
     * @return 失败的状态转换记录列表
     */
    List<StatusTransitionRecord> findBySuccessOrderByTransitionTimeDesc(Boolean success);
    
    /**
     * 根据操作者查询状态转换记录
     * 
     * @param operatorInfo 操作者信息
     * @return 状态转换记录列表
     */
    List<StatusTransitionRecord> findByOperatorInfoOrderByTransitionTimeDesc(String operatorInfo);
    
    /**
     * 统计指定时间范围内的状态转换次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 转换次数
     */
    @Query("SELECT COUNT(s) FROM StatusTransitionRecord s WHERE s.transitionTime BETWEEN :startTime AND :endTime")
    Long countTransitionsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计成功和失败的转换次数
     * 
     * @param success 是否成功
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 转换次数
     */
    @Query("SELECT COUNT(s) FROM StatusTransitionRecord s WHERE s.success = :success AND s.transitionTime BETWEEN :startTime AND :endTime")
    Long countTransitionsBySuccessAndTimeBetween(@Param("success") Boolean success, 
                                               @Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询平均转换耗时
     * 
     * @param fromStatus 源状态
     * @param toStatus 目标状态
     * @return 平均耗时（毫秒）
     */
    @Query("SELECT AVG(s.durationMs) FROM StatusTransitionRecord s WHERE s.fromStatus = :fromStatus AND s.toStatus = :toStatus AND s.success = true")
    Double getAverageTransitionDuration(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
    
    /**
     * 删除指定时间之前的历史记录
     * 用于清理过期的历史数据
     * 
     * @param beforeTime 时间界限
     * @return 删除的记录数
     */
    @Query("DELETE FROM StatusTransitionRecord s WHERE s.transitionTime < :beforeTime")
    Long deleteByTransitionTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);
}