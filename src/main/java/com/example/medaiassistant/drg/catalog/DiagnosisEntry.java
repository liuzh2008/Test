package com.example.medaiassistant.drg.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 诊断条目数据模型
 * 
 * 表示从DRGs表CLOB字段解析出的诊断条目。
 * 包含ICD编码、诊断名称和别名列表。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-22
 */
public class DiagnosisEntry {
    private final String icdCode;
    private final String diagnosisName;
    private final List<String> aliases;

    public DiagnosisEntry(String icdCode, String diagnosisName) {
        this.icdCode = icdCode;
        this.diagnosisName = diagnosisName;
        this.aliases = new ArrayList<>();
    }

    public String getIcdCode() {
        return icdCode;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public List<String> getAliases() {
        return new ArrayList<>(aliases);
    }

    public void addAlias(String alias) {
        this.aliases.add(alias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiagnosisEntry that = (DiagnosisEntry) o;
        return Objects.equals(icdCode, that.icdCode) && 
               Objects.equals(diagnosisName, that.diagnosisName) && 
               Objects.equals(aliases, that.aliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icdCode, diagnosisName, aliases);
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
