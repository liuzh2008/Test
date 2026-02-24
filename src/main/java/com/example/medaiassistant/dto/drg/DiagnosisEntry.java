package com.example.medaiassistant.dto.drg;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DRG诊断条目数据传输对象
 * 
 * 用于表示DRG中的单个诊断条目，包含ICD编码、诊断名称和别名列表
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@Data
public class DiagnosisEntry {
    
    /**
     * ICD-10诊断编码
     * 示例: "I48.000", "I10", "E11.900"
     */
    private final String icdCode;
    
    /**
     * 诊断名称
     * 示例: "心房颤动", "原发性高血压", "2型糖尿病"
     */
    private final String diagnosisName;
    
    /**
     * 诊断别名列表
     * 示例: ["心房纤颤", "房颤"]
     */
    private final List<String> aliases;
    
    /**
     * 构造函数
     * 
     * @param icdCode ICD-10诊断编码
     * @param diagnosisName 诊断名称
     * @param aliases 诊断别名列表，如果为null则创建空列表
     */
    public DiagnosisEntry(String icdCode, String diagnosisName, List<String> aliases) {
        this.icdCode = icdCode;
        this.diagnosisName = diagnosisName;
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
    }
    
    /**
     * 获取ICD编码，如果为空则返回空字符串
     */
    public String getIcdCodeSafe() {
        return icdCode != null ? icdCode : "";
    }
    
    /**
     * 获取诊断名称，如果为空则返回空字符串
     */
    public String getDiagnosisNameSafe() {
        return diagnosisName != null ? diagnosisName : "";
    }
    
    /**
     * 获取不可修改的别名列表
     */
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }
    
    /**
     * 判断是否有别名
     */
    public boolean hasAliases() {
        return !aliases.isEmpty();
    }
    
    /**
     * 获取别名数量
     */
    public int getAliasCount() {
        return aliases.size();
    }
    
    @Override
    public String toString() {
        return "DiagnosisEntry{" +
                "icdCode='" + icdCode + '\'' +
                ", diagnosisName='" + diagnosisName + '\'' +
                ", aliases=" + aliases +
                '}';
    }
}
