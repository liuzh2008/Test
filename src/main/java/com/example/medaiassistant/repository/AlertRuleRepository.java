package com.example.medaiassistant.repository;

import com.example.medaiassistant.dto.AlertRuleContentDTO;
import com.example.medaiassistant.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警规则数据访问接口
 * 
 * 该接口提供了对告警规则实体的基本CRUD操作，以及一些自定义的查询方法，
 * 用于支持基于时间或状态的告警规则处理功能。
 * 
 * @author Cline
 * @since 2025-08-06
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    /**
     * 查询所有激活的时间规则并按优先级排序
     * 
     * 该方法用于查询所有规则类型为"时间"且处于激活状态的告警规则，
     * 并按照优先级字段进行升序排序。主要用于定时任务中处理基于时间的告警规则。
     * 
     * 查询条件说明：
     * 1. ruleType = '时间'：筛选规则类型为时间的规则
     * 2. isActive = 1：筛选处于激活状态的规则
     * 3. ORDER BY r.priority ASC：按优先级升序排序
     * 
     * @return 激活的时间规则列表，按优先级升序排列
     */
    @Query("SELECT r FROM AlertRule r WHERE r.ruleType = '时间' AND r.isActive = 1 ORDER BY r.priority ASC")
    List<AlertRule> findActiveTimeRulesOrderByPriority();
    
    /**
     * 查询所有激活的状态规则并按优先级排序
     * 
     * 该方法用于查询所有规则类型为"状态"且处于激活状态的告警规则，
     * 并按照优先级字段进行升序排序。主要用于定时任务中处理基于患者状态的告警规则。
     * 
     * 查询条件说明：
     * 1. ruleType = '状态'：筛选规则类型为状态的规则
     * 2. isActive = 1：筛选处于激活状态的规则
     * 3. ORDER BY r.priority ASC：按优先级升序排序
     * 
     * @return 激活的状态规则列表，按优先级升序排列
     */
    @Query("SELECT r FROM AlertRule r WHERE r.ruleType = '状态' AND r.isActive = 1 ORDER BY r.priority ASC")
    List<AlertRule> findActiveStatusRulesOrderByPriority();
    
    /**
     * 根据规则名称查询激活的告警规则的内容信息
     * 
     * 该方法用于根据规则名称查询处于激活状态的告警规则的alert_content和required_actions字段，
     * 主要用于获取特定规则的告警内容和所需操作信息
     * 
     * 查询条件说明：
     * 1. ruleName = :ruleName：根据规则名称筛选
     * 2. isActive = 1：筛选处于激活状态的规则
     * 3. SELECT new com.example.medaiassistant.dto.AlertRuleContentDTO(r.alertContent, r.requiredActions)：只选择需要的字段
     * 
     * @param ruleName 规则名称
     * @return 符合条件的告警规则内容信息
     */
    @Query("SELECT new com.example.medaiassistant.dto.AlertRuleContentDTO(r.alertContent, r.requiredActions) FROM AlertRule r WHERE r.ruleName = :ruleName AND r.isActive = 1")
    List<AlertRuleContentDTO> findActiveRuleContentByName(@Param("ruleName") String ruleName);
}
