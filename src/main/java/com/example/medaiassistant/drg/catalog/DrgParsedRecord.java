package com.example.medaiassistant.drg.catalog;

import java.util.List;
import java.util.Objects;

/**
 * DRG解析记录数据模型
 * 
 * 表示从DRGs表解析出的完整记录，包含诊断和手术信息。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-23
 */
public class DrgParsedRecord {
    private final String drgId;
    private final List<DiagnosisEntry> diagnoses;
    private final List<ProcedureEntry> procedures;

    public DrgParsedRecord(String drgId, List<DiagnosisEntry> diagnoses, List<ProcedureEntry> procedures) {
        this.drgId = drgId;
        this.diagnoses = List.copyOf(diagnoses);
        this.procedures = List.copyOf(procedures);
    }

    public String getDrgId() {
        return drgId;
    }

    public List<DiagnosisEntry> getDiagnoses() {
        return diagnoses;
    }

    public List<ProcedureEntry> getProcedures() {
        return procedures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrgParsedRecord that = (DrgParsedRecord) o;
        return Objects.equals(drgId, that.drgId) && 
               Objects.equals(diagnoses, that.diagnoses) && 
               Objects.equals(procedures, that.procedures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(drgId, diagnoses, procedures);
    }

    @Override
    public String toString() {
        return "DrgParsedRecord{" +
                "drgId='" + drgId + '\'' +
                ", diagnoses=" + diagnoses +
                ", procedures=" + procedures +
                '}';
    }
}
