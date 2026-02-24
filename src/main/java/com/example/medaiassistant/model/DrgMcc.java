package com.example.medaiassistant.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 并发症字典实体类
 * 对应数据库表: DRGMCC
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-16
 */
@Entity
@Table(name = "DRGMCC")
@Data
public class DrgMcc {
    
    /**
     * 主键
     */
    @Id
    @Column(name = "IDDRGMCC")
    private Long id;
    
    /**
     * 并发症ICD编码
     */
    @Column(name = "MCC_CODE", length = 45)
    private String mccCode;
    
    /**
     * 并发症名称
     */
    @Column(name = "MCC_NAME", length = 45)
    private String mccName;
    
    /**
     * 排除条件（ICD编码列表）
     */
    @Column(name = "MCC_EXCEPT", length = 45)
    private String mccExcept;
    
    /**
     * MCC类型："MCC"严重/"CC"一般
     */
    @Column(name = "MCC_TYPE", length = 45)
    private String mccType;
    
    /**
     * 默认构造函数
     */
    public DrgMcc() {
    }
    
    /**
     * 带参数构造函数
     */
    public DrgMcc(Long id, String mccCode, String mccName, String mccExcept, String mccType) {
        this.id = id;
        this.mccCode = mccCode;
        this.mccName = mccName;
        this.mccExcept = mccExcept;
        this.mccType = mccType;
    }
    
    /**
     * 判断是否为严重并发症
     */
    public boolean isSevere() {
        return "MCC".equalsIgnoreCase(mccType);
    }
    
    /**
     * 判断是否为一般并发症
     */
    public boolean isCommon() {
        return "CC".equalsIgnoreCase(mccType);
    }
    
    /**
     * 获取MCC类型描述
     */
    public String getMccTypeDescription() {
        if (isSevere()) {
            return "严重并发症";
        } else if (isCommon()) {
            return "一般并发症";
        } else {
            return "未知类型";
        }
    }
    
    /**
     * 判断是否有排除条件
     */
    public boolean hasExclusion() {
        return mccExcept != null && !mccExcept.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "DrgMcc{" +
                "id=" + id +
                ", mccCode='" + mccCode + '\'' +
                ", mccName='" + mccName + '\'' +
                ", mccType='" + mccType + '\'' +
                '}';
    }
}
