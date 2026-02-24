package com.example.medaiassistant.repository;

import com.example.medaiassistant.config.TestConfig;
import com.example.medaiassistant.model.AlertRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 告警规则数据访问层测试
 * 使用@TestConfig注解简化测试配置
 * 
 * @author Cline
 * @since 2025-12-04
 */
@TestConfig(description = "告警规则数据访问层测试")
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    /**
     * 测试查询所有激活的时间规则并按优先级排序
     * 红阶段：测试会失败，因为数据库中可能没有测试数据
     */
    @Test
    void findActiveTimeRulesOrderByPriority_ShouldReturnActiveTimeRulesOrderedByPriority() {
        // Arrange: 准备测试数据（目前数据库可能为空）
        
        // Act: 执行查询
        List<AlertRule> activeTimeRules = alertRuleRepository.findActiveTimeRulesOrderByPriority();
        
        // Assert: 验证结果
        // 红阶段：这个测试应该会失败，因为数据库中没有测试数据
        // 但我们先验证方法能正常执行，不抛出异常
        assertNotNull(activeTimeRules, "返回的列表不应为null");
        
        // 验证列表中的规则都是时间类型且激活状态
        for (AlertRule rule : activeTimeRules) {
            assertEquals(AlertRule.RuleType.时间, rule.getRuleType(), "规则类型应为'时间'");
            assertEquals(1, rule.getIsActive(), "规则应处于激活状态");
        }
        
        // 验证按优先级升序排序
        for (int i = 0; i < activeTimeRules.size() - 1; i++) {
            assertTrue(activeTimeRules.get(i).getPriority() <= activeTimeRules.get(i + 1).getPriority(),
                    "规则应按优先级升序排列");
        }
    }

    /**
     * 测试查询所有激活的状态规则并按优先级排序
     */
    @Test
    void findActiveStatusRulesOrderByPriority_ShouldReturnActiveStatusRulesOrderedByPriority() {
        // Arrange: 准备测试数据（目前数据库可能为空）
        
        // Act: 执行查询
        List<AlertRule> activeStatusRules = alertRuleRepository.findActiveStatusRulesOrderByPriority();
        
        // Assert: 验证结果
        assertNotNull(activeStatusRules, "返回的列表不应为null");
        
        // 验证列表中的规则都是状态类型且激活状态
        for (AlertRule rule : activeStatusRules) {
            assertEquals(AlertRule.RuleType.状态, rule.getRuleType(), "规则类型应为'状态'");
            assertEquals(1, rule.getIsActive(), "规则应处于激活状态");
        }
        
        // 验证按优先级升序排序
        for (int i = 0; i < activeStatusRules.size() - 1; i++) {
            assertTrue(activeStatusRules.get(i).getPriority() <= activeStatusRules.get(i + 1).getPriority(),
                    "规则应按优先级升序排列");
        }
    }

    /**
     * 测试根据规则名称查询激活的告警规则内容信息
     */
    @Test
    void findActiveRuleContentByName_ShouldReturnRuleContentForActiveRule() {
        // Arrange: 准备测试数据（目前数据库可能为空）
        String ruleName = "测试规则";
        
        // Act: 执行查询
        var ruleContentList = alertRuleRepository.findActiveRuleContentByName(ruleName);
        
        // Assert: 验证结果
        assertNotNull(ruleContentList, "返回的列表不应为null");
        
        // 如果查询到数据，验证DTO结构
        if (!ruleContentList.isEmpty()) {
            var ruleContent = ruleContentList.get(0);
            assertNotNull(ruleContent.getAlertContent(), "告警内容不应为null");
            assertNotNull(ruleContent.getRequiredActions(), "必需操作不应为null");
        }
    }
}
