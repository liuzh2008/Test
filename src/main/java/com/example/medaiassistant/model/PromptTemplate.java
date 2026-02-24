package com.example.medaiassistant.model;

import jakarta.persistence.*;

@Entity
@Table(name = "prompttemplate")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromptID")
    private Integer promptId;

    @Column(name = "PromptType", nullable = false)
    private String promptType;

    @Column(name = "PromptName", nullable = false)
    private String promptName;

    @Column(name = "Prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(name = "FilterRules", columnDefinition = "TEXT")
    private String filterRules;

    @Column(name = "SPECIAL_CONTENT", columnDefinition = "LONGTEXT")
    private String specialContent;

    @Column(name = "REQUIRED_DATA_TYPES", length = 500)
    private String requiredDataTypes;

    @Column(name = "SCOPE", length = 20)
    private String scope;

    @Column(name = "DEPARTMENT_ID")
    private Integer departmentId;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    // Getters and setters
    public Integer getPromptId() {
        return promptId;
    }

    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
    }

    public String getPromptType() {
        return promptType;
    }

    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    public String getPromptName() {
        return promptName;
    }

    public void setPromptName(String promptName) {
        this.promptName = promptName;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getFilterRules() {
        return filterRules;
    }

    public void setFilterRules(String filterRules) {
        this.filterRules = filterRules;
    }

    public String getSpecialContent() {
        return specialContent;
    }

    public void setSpecialContent(String specialContent) {
        this.specialContent = specialContent;
    }

    public String getRequiredDataTypes() {
        return requiredDataTypes;
    }

    public void setRequiredDataTypes(String requiredDataTypes) {
        this.requiredDataTypes = requiredDataTypes;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
