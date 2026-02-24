package com.example.medaiassistant.drg.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 手术条目数据模型
 * 
 * 表示从DRGs表CLOB字段解析出的手术条目。
 * 包含手术编码、手术名称和别名列表。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-22
 */
public class ProcedureEntry {
    private final String procedureCode;
    private final String procedureName;
    private final List<String> aliases;

    public ProcedureEntry(String procedureCode, String procedureName) {
        this.procedureCode = procedureCode;
        this.procedureName = procedureName;
        this.aliases = new ArrayList<>();
    }

    public String getProcedureCode() {
        return procedureCode;
    }

    public String getProcedureName() {
        return procedureName;
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
        ProcedureEntry that = (ProcedureEntry) o;
        return Objects.equals(procedureCode, that.procedureCode) && 
               Objects.equals(procedureName, that.procedureName) && 
               Objects.equals(aliases, that.aliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(procedureCode, procedureName, aliases);
    }

    @Override
    public String toString() {
        return "ProcedureEntry{" +
                "procedureCode='" + procedureCode + '\'' +
                ", procedureName='" + procedureName + '\'' +
                ", aliases=" + aliases +
                '}';
    }
}
