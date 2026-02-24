package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.ExaminationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 检查结果数据访问层接口
 * 提供检查结果的CRUD操作和自定义查询方法
 * 
 * @author System
 * @version 1.1
 * @since 2025-12-30
 */
public interface ExaminationResultRepository extends JpaRepository<ExaminationResult, String> {
    
    /**
     * 根据患者ID查询检查结果列表
     * 
     * @param patientId 患者ID
     * @return 该患者的所有检查结果
     */
    List<ExaminationResult> findByPatientId(String patientId);
    
    /**
     * 根据检查申请号查询检查结果
     * 用于重复记录检测和更新策略
     * 
     * @param examinationId 检查申请号（主键）
     * @return 检查结果Optional，不存在时返回empty
     */
    Optional<ExaminationResult> findByExaminationId(String examinationId);
    
    /**
     * 判断指定检查申请号的记录是否存在
     * 用于快速判断重复，避免不必要的数据加载
     * 
     * @param examinationId 检查申请号（主键）
     * @return 存在返回true，否则返回false
     */
    boolean existsByExaminationId(String examinationId);
    
    /**
     * 统计指定患者的检查结果记录数
     * 用于同步诊断和统计
     * 
     * @param patientId 患者ID
     * @return 该患者的检查结果记录数
     */
    long countByPatientId(String patientId);
}
