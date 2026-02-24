package com.example.medaiassistant.model;

import lombok.Builder;
import lombok.Data;

/**
 * MCC候选结果实体类
 * 用于存储MCC预筛选的候选结果
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@Data
@Builder
public class MccCandidate {
    
    /**
     * 匹配类型常量 - 编码精确匹配
     */
    public static final String MATCH_TYPE_CODE_MATCH = "CODE_MATCH";
    
    /**
     * 匹配类型常量 - 名称相似度匹配
     */
    public static final String MATCH_TYPE_NAME_MATCH = "NAME_MATCH";
    
    /**
     * MCC编码
     */
    private String mccCode;
    
    /**
     * MCC名称
     */
    private String mccName;
    
    /**
     * MCC类型："MCC"严重/"CC"一般
     */
    private String mccType;
    
    /**
     * 相似度值（0.0-1.0）
     */
    private Double similarity;
    
    /**
     * 匹配类型："CODE_MATCH"编码精确匹配/"NAME_MATCH"名称相似度匹配
     */
    private String matchType;
    
    /**
     * 是否被排除
     */
    private Boolean excluded;
    
    /**
     * 来源诊断名称
     */
    private String sourceDiagnosis;
    
    /**
     * 来源诊断ICD编码
     */
    private String sourceIcdCode;
    
    /**
     * 默认构造函数
     */
    public MccCandidate() {
    }
    
    /**
     * 带参数构造函数
     */
    public MccCandidate(String mccCode, String mccName, String mccType, Double similarity, 
                       String matchType, Boolean excluded, String sourceDiagnosis, String sourceIcdCode) {
        this.mccCode = mccCode;
        this.mccName = mccName;
        this.mccType = mccType;
        this.similarity = similarity;
        this.matchType = matchType;
        this.excluded = excluded;
        this.sourceDiagnosis = sourceDiagnosis;
        this.sourceIcdCode = sourceIcdCode;
    }
    
    /**
     * 判断是否为精确匹配
     */
    public boolean isExactMatch() {
        return MATCH_TYPE_CODE_MATCH.equals(matchType);
    }
    
    /**
     * 判断是否为相似度匹配
     */
    public boolean isSimilarityMatch() {
        return MATCH_TYPE_NAME_MATCH.equals(matchType);
    }
    
    /**
     * 获取相似度百分比
     */
    public int getSimilarityPercentage() {
        return similarity != null ? (int) (similarity * 100) : 0;
    }
    
    /**
     * 获取匹配类型描述
     */
    public String getMatchTypeDescription() {
        if (isExactMatch()) {
            return "编码精确匹配";
        } else if (isSimilarityMatch()) {
            return "名称相似度匹配";
        } else {
            return "未知匹配类型";
        }
    }
    
    @Override
    public String toString() {
        return "MccCandidate{" +
                "mccCode='" + mccCode + '\'' +
                ", mccName='" + mccName + '\'' +
                ", mccType='" + mccType + '\'' +
                ", similarity=" + similarity +
                ", matchType='" + matchType + '\'' +
                ", excluded=" + excluded +
                ", sourceDiagnosis='" + sourceDiagnosis + '\'' +
                '}';
    }
}
