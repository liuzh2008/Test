package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.AlertRuleContentDTO;
import com.example.medaiassistant.model.AlertRule;
import com.example.medaiassistant.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 告警规则控制器
 * 处理与告警规则相关的HTTP请求
 * 
 * @author Cline
 * @since 2025-08-05
 */
@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {
    
    @Autowired
    private AlertRuleService alertRuleService;
    
    /**
     * 获取rule_type为'时间'且is_active为1的记录，按priority排序
     * 
     * @return 符合条件的AlertRule列表，按priority升序排列
     */
    @GetMapping("/active-time-rules")
    public ResponseEntity<List<AlertRule>> getActiveTimeRulesOrderByPriority() {
        List<AlertRule> alertRules = alertRuleService.getActiveTimeRulesOrderByPriority();
        return ResponseEntity.ok(alertRules);
    }
    
    /**
     * 根据规则名称获取激活的告警规则内容信息
     * 
     * @param ruleName 规则名称
     * @return 符合条件的告警规则内容信息列表
     */
    @GetMapping("/active-rule-content")
    public ResponseEntity<List<AlertRuleContentDTO>> getActiveRuleContentByName(@RequestParam("rule_name") String ruleName) {
        List<AlertRuleContentDTO> ruleContentList = alertRuleService.getActiveRuleContentByName(ruleName);
        return ResponseEntity.ok(ruleContentList);
    }
}
