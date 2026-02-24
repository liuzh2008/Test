package com.example.medaiassistant.service;

import com.example.medaiassistant.model.Prompt;
import com.example.medaiassistant.model.PromptTemplate;
import com.example.medaiassistant.repository.PromptRepository;
import com.example.medaiassistant.repository.PromptTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DRGs AI分析服务
 * 负责生成DRGs分析相关的AI Prompt
 * 
 * 主要功能：
 * - 从数据库读取DRGs分析模板
 * - 生成个性化的AI分析Prompt
 * - 处理模板不存在等异常情况
 */
@Service
public class DrgAiAnalysisService {

    private static final String DRG_ANALYSIS_TEMPLATE_TYPE = "DRG_ANALYSIS";
    
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptRepository promptRepository;

    @Autowired
    public DrgAiAnalysisService(PromptTemplateRepository promptTemplateRepository, PromptRepository promptRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptRepository = promptRepository;
    }

    /**
     * 生成分析Prompt
     * 从PROMPTTEMPLATE表读取指定名称的模板
     * 
     * @param templateName 模板名称，不能为null或空字符串
     * @return 生成的Prompt内容
     * @throws TemplateNotFoundException 当模板不存在时抛出异常
     */
    public String generateAnalysisPrompt(String templateName) {
        validateTemplateName(templateName);
        
        PromptTemplate template = promptTemplateRepository.findByPromptTypeAndPromptName(
            DRG_ANALYSIS_TEMPLATE_TYPE, templateName);
        
        if (template == null) {
            throw new TemplateNotFoundException("未找到指定的Prompt模板: " + templateName);
        }
        
        return template.getPrompt();
    }
    
    /**
     * 生成分析Prompt并进行变量替换
     * 从PROMPTTEMPLATE表读取指定名称的模板，并进行变量替换
     * 
     * @param templateName 模板名称，不能为null或空字符串
     * @param variables 变量映射，包含模板中需要替换的变量
     * @return 生成的Prompt内容，变量已被替换
     * @throws TemplateNotFoundException 当模板不存在时抛出异常
     */
    public String generateAnalysisPrompt(String templateName, java.util.Map<String, String> variables) {
        validateTemplateName(templateName);
        
        PromptTemplate template = promptTemplateRepository.findByPromptTypeAndPromptName(
            DRG_ANALYSIS_TEMPLATE_TYPE, templateName);
        
        if (template == null) {
            throw new TemplateNotFoundException("未找到指定的Prompt模板: " + templateName);
        }
        
        String templateContent = template.getPrompt();
        
        // 如果提供了变量，进行变量替换
        if (variables != null && !variables.isEmpty()) {
            templateContent = processPromptTemplate(templateContent, variables);
        }
        
        return templateContent;
    }
    
    /**
     * 处理模板变量替换
     * 将模板中的{{variable}}替换为实际值
     * 
     * @param templateContent 模板内容
     * @param variables 变量映射
     * @return 替换后的模板内容
     */
    private String processPromptTemplate(String templateContent, java.util.Map<String, String> variables) {
        String result = templateContent;
        
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            String variable = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(variable, value);
        }
        
        return result;
    }
    
    /**
     * 验证模板名称参数
     * 
     * @param templateName 模板名称
     * @throws IllegalArgumentException 当模板名称为null或空字符串时抛出
     */
    private void validateTemplateName(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
    }
    
    /**
     * 保存Prompt并实现幂等性检查
     * 如果相同上下文的Prompt已存在，则返回已存在的prompt_id
     * 
     * @param patientId 患者ID
     * @param templateName 模板名称
     * @param objectiveContent 客观内容
     * @param dailyRecords 日常记录
     * @param templateContent 模板内容
     * @return 保存的Prompt ID
     */
    public Integer savePrompt(String patientId, String templateName, String objectiveContent, 
                             String dailyRecords, String templateContent) {
        // 参数验证
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        
        // 检查是否已存在相同上下文的Prompt
        Optional<Prompt> existingPrompt = promptRepository.findTopByPatientIdAndPromptTemplateNameOrderBySubmissionTimeDesc(
            patientId, templateName);
        
        if (existingPrompt.isPresent()) {
            Prompt prompt = existingPrompt.get();
            // 检查内容是否相同（简化检查，实际应该使用哈希值）
            if (isSameContext(prompt, objectiveContent, dailyRecords, templateContent)) {
                // 返回已存在的prompt_id
                return prompt.getPromptId();
            }
        }
        
        // 创建新的Prompt
        Prompt newPrompt = new Prompt();
        newPrompt.setPatientId(patientId);
        newPrompt.setPromptTemplateName(templateName);
        newPrompt.setObjectiveContent(objectiveContent);
        newPrompt.setDailyRecords(dailyRecords);
        newPrompt.setPromptTemplateContent(templateContent);
        newPrompt.setStatusName("待处理");
        newPrompt.setSubmissionTime(LocalDateTime.now());
        newPrompt.setPriority(1); // 默认优先级
        
        Prompt savedPrompt = promptRepository.save(newPrompt);
        return savedPrompt.getPromptId();
    }
    
    /**
     * 检查Prompt上下文是否相同
     * 
     * @param prompt 已存在的Prompt
     * @param objectiveContent 客观内容
     * @param dailyRecords 日常记录
     * @param templateContent 模板内容
     * @return 如果上下文相同返回true，否则返回false
     */
    private boolean isSameContext(Prompt prompt, String objectiveContent, String dailyRecords, String templateContent) {
        // 简化检查：比较关键字段内容
        boolean objectiveSame = (prompt.getObjectiveContent() == null && objectiveContent == null) ||
                               (prompt.getObjectiveContent() != null && prompt.getObjectiveContent().equals(objectiveContent));
        boolean dailyRecordsSame = (prompt.getDailyRecords() == null && dailyRecords == null) ||
                                  (prompt.getDailyRecords() != null && prompt.getDailyRecords().equals(dailyRecords));
        boolean templateSame = (prompt.getPromptTemplateContent() == null && templateContent == null) ||
                              (prompt.getPromptTemplateContent() != null && prompt.getPromptTemplateContent().equals(templateContent));
        
        return objectiveSame && dailyRecordsSame && templateSame;
    }
    

    /**
     * 模板未找到异常
     */
    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }
}
