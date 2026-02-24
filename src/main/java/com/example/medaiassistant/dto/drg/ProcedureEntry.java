package com.example.medaiassistant.dto.drg;

import lombok.Data;

/**
 * DRG手术条目数据传输对象
 * 
 * 用于表示DRG中的单个手术条目，包含手术编码和手术名称
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
@Data
public class ProcedureEntry {
    
    /**
     * 手术编码
     * 示例: "37.9000x001", "88.7201"
     */
    private final String procedureCode;
    
    /**
     * 手术名称
     * 示例: "经皮左心耳封堵术", "冠状动脉造影"
     */
    private final String procedureName;
    
    /**
     * 构造函数
     * 
     * @param procedureCode 手术编码
     * @param procedureName 手术名称
     */
    public ProcedureEntry(String procedureCode, String procedureName) {
        this.procedureCode = procedureCode;
        this.procedureName = procedureName;
    }
    
    /**
     * 获取手术编码，如果为空则返回空字符串
     */
    public String getProcedureCodeSafe() {
        return procedureCode != null ? procedureCode : "";
    }
    
    /**
     * 获取手术名称，如果为空则返回空字符串
     */
    public String getProcedureNameSafe() {
        return procedureName != null ? procedureName : "";
    }
    
    @Override
    public String toString() {
        return "ProcedureEntry{" +
                "procedureCode='" + procedureCode + '\'' +
                ", procedureName='" + procedureName + '\'' +
                '}';
    }
}
