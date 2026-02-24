package com.example.medaiassistant.dto;

/**
 * 创建Prompt模板数据传输对象
 * 
 * 该类用于在前端创建新的Prompt模板时传输数据，包含Prompt模板的所有必要属性。
 * 主要用于AIController中的createPromptTemplate方法，用于接收前端传来的Prompt模板创建请求。
 * 
 * @author MedAI Assistant Team
 * @version 1.0.0
 * @since 2024
 */
public class CreatePromptTemplateDTO {
    /** Prompt模板类型，如"诊断"、"医嘱"、"病历"等 */
    private String promptType;
    
    /** Prompt模板名称，用于标识具体的Prompt模板 */
    private String promptName;
    
    /** Prompt模板内容，包含实际的Prompt文本 */
    private String prompt;
    
    /** 过滤规则，用于筛选适用的患者或场景 */
    private String filterRules;
    
    /** 特殊内容，包含额外的配置或特殊处理逻辑 */
    private String specialContent;
    
    /** 需要的数据类型，指定生成该Prompt所需的数据类型 */
    private String requiredDataTypes;
    
    /** 作用范围，定义Prompt模板的适用范围 */
    private String scope;
    
    /** 科室ID，指定该Prompt模板所属的科室 */
    private Integer departmentId;
    
    /** 是否激活，控制该Prompt模板是否可用 */
    private Boolean isActive;

    /**
     * 默认构造函数
     * 创建一个空的CreatePromptTemplateDTO实例
     */
    public CreatePromptTemplateDTO() {
    }

    /**
     * 带参构造函数
     * 使用指定的参数创建CreatePromptTemplateDTO实例
     * 
     * @param promptType Prompt模板类型
     * @param promptName Prompt模板名称
     * @param prompt Prompt模板内容
     * @param filterRules 过滤规则
     * @param specialContent 特殊内容
     * @param requiredDataTypes 需要的数据类型
     * @param scope 作用范围
     * @param departmentId 科室ID
     * @param isActive 是否激活
     */
    public CreatePromptTemplateDTO(String promptType, String promptName, String prompt, 
                                  String filterRules, String specialContent, String requiredDataTypes,
                                  String scope, Integer departmentId, Boolean isActive) {
        this.promptType = promptType;
        this.promptName = promptName;
        this.prompt = prompt;
        this.filterRules = filterRules;
        this.specialContent = specialContent;
        this.requiredDataTypes = requiredDataTypes;
        this.scope = scope;
        this.departmentId = departmentId;
        this.isActive = isActive;
    }

    /**
     * 获取Prompt模板类型
     * 
     * @return Prompt模板类型字符串
     */
    public String getPromptType() {
        return promptType;
    }

    /**
     * 设置Prompt模板类型
     * 
     * @param promptType Prompt模板类型字符串
     */
    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    /**
     * 获取Prompt模板名称
     * 
     * @return Prompt模板名称字符串
     */
    public String getPromptName() {
        return promptName;
    }

    /**
     * 设置Prompt模板名称
     * 
     * @param promptName Prompt模板名称字符串
     */
    public void setPromptName(String promptName) {
        this.promptName = promptName;
    }

    /**
     * 获取Prompt模板内容
     * 
     * @return Prompt模板内容字符串
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * 设置Prompt模板内容
     * 
     * @param prompt Prompt模板内容字符串
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 获取过滤规则
     * 
     * @return 过滤规则字符串
     */
    public String getFilterRules() {
        return filterRules;
    }

    /**
     * 设置过滤规则
     * 
     * @param filterRules 过滤规则字符串
     */
    public void setFilterRules(String filterRules) {
        this.filterRules = filterRules;
    }

    /**
     * 获取特殊内容
     * 
     * @return 特殊内容字符串
     */
    public String getSpecialContent() {
        return specialContent;
    }

    /**
     * 设置特殊内容
     * 
     * @param specialContent 特殊内容字符串
     */
    public void setSpecialContent(String specialContent) {
        this.specialContent = specialContent;
    }

    /**
     * 获取需要的数据类型
     * 
     * @return 需要的数据类型字符串
     */
    public String getRequiredDataTypes() {
        return requiredDataTypes;
    }

    /**
     * 设置需要的数据类型
     * 
     * @param requiredDataTypes 需要的数据类型字符串
     */
    public void setRequiredDataTypes(String requiredDataTypes) {
        this.requiredDataTypes = requiredDataTypes;
    }

    /**
     * 获取作用范围
     * 
     * @return 作用范围字符串
     */
    public String getScope() {
        return scope;
    }

    /**
     * 设置作用范围
     * 
     * @param scope 作用范围字符串
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * 获取科室ID
     * 
     * @return 科室ID整数
     */
    public Integer getDepartmentId() {
        return departmentId;
    }

    /**
     * 设置科室ID
     * 
     * @param departmentId 科室ID整数
     */
    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * 获取是否激活状态
     * 
     * @return 是否激活的布尔值
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * 设置是否激活状态
     * 
     * @param isActive 是否激活的布尔值
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
