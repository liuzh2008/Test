package com.example.medaiassistant.util;

import com.example.medaiassistant.dto.drg.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 测试数据工厂类
 * 
 * 提供常用的测试数据创建方法，用于DRG匹配相关的单元测试
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-21
 */
public class TestDataFactory {
    
    private TestDataFactory() {
        // 工具类，防止实例化
    }
    
    /**
     * 创建有心房颤动和左心耳封堵术的患者数据
     */
    public static PatientData createPatientWithAtrialFibrillation() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术")
        );
        return new PatientData(diagnoses, procedures);
    }
    
    /**
     * 创建有原发性高血压的患者数据（无手术）
     */
    public static PatientData createPatientWithHypertension() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I10", "原发性高血压")
        );
        List<PatientProcedure> procedures = Collections.emptyList();
        return new PatientData(diagnoses, procedures);
    }
    
    /**
     * 创建有2型糖尿病的患者数据（无手术）
     */
    public static PatientData createPatientWithDiabetes() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("E11.900", "2型糖尿病")
        );
        List<PatientProcedure> procedures = Collections.emptyList();
        return new PatientData(diagnoses, procedures);
    }
    
    /**
     * 创建有复杂诊断的患者数据
     */
    public static PatientData createPatientWithComplexDiagnoses() {
        List<PatientDiagnosis> diagnoses = Arrays.asList(
            new PatientDiagnosis("I48.000", "心房颤动"),
            new PatientDiagnosis("I10", "原发性高血压"),
            new PatientDiagnosis("E11.900", "2型糖尿病")
        );
        List<PatientProcedure> procedures = Arrays.asList(
            new PatientProcedure("37.9000x001", "经皮左心耳封堵术"),
            new PatientProcedure("88.7201", "冠状动脉造影")
        );
        return new PatientData(diagnoses, procedures);
    }
    
    /**
     * 创建有心脏手术的DRG记录
     */
    public static DrgParsedRecord createDrgWithCardiacProcedures() {
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术")
        );
        return new DrgParsedRecord(1L, "DRG001", "心脏相关DRG", 
            new BigDecimal("15000.00"), diagnoses, procedures);
    }
    
    /**
     * 创建有高血压诊断的DRG记录（无手术）
     */
    public static DrgParsedRecord createDrgWithHypertension() {
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压"))
        );
        List<ProcedureEntry> procedures = Collections.emptyList();
        return new DrgParsedRecord(2L, "DRG002", "高血压相关DRG", 
            new BigDecimal("8000.00"), diagnoses, procedures);
    }
    
    /**
     * 创建有糖尿病诊断的DRG记录（无手术）
     */
    public static DrgParsedRecord createDrgWithDiabetes() {
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("E11.900", "2型糖尿病", Arrays.asList("糖尿病"))
        );
        List<ProcedureEntry> procedures = Collections.emptyList();
        return new DrgParsedRecord(3L, "DRG003", "糖尿病相关DRG", 
            new BigDecimal("6000.00"), diagnoses, procedures);
    }
    
    /**
     * 创建有复杂诊断和手术的DRG记录
     */
    public static DrgParsedRecord createDrgWithComplexDiagnosesAndProcedures() {
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I48.000", "心房颤动", Arrays.asList("心房纤颤")),
            new DiagnosisEntry("I10", "原发性高血压", Arrays.asList("高血压")),
            new DiagnosisEntry("E11.900", "2型糖尿病", Arrays.asList("糖尿病"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("37.9000x001", "经皮左心耳封堵术"),
            new ProcedureEntry("88.7201", "冠状动脉造影")
        );
        return new DrgParsedRecord(4L, "DRG004", "复杂心脏DRG", 
            new BigDecimal("18000.00"), diagnoses, procedures);
    }
    
    /**
     * 创建有造影手术的DRG记录
     */
    public static DrgParsedRecord createDrgWithAngiography() {
        List<DiagnosisEntry> diagnoses = Arrays.asList(
            new DiagnosisEntry("I25.100", "冠状动脉粥样硬化性心脏病", 
                Arrays.asList("冠心病", "冠状动脉硬化"))
        );
        List<ProcedureEntry> procedures = Arrays.asList(
            new ProcedureEntry("88.7201", "冠状动脉造影")
        );
        return new DrgParsedRecord(5L, "DRG005", "造影相关DRG", 
            new BigDecimal("10000.00"), diagnoses, procedures);
    }

    /**
     * 创建混合DRG记录列表（包含有手术和无手术的DRG）
     */
    public static List<DrgParsedRecord> createMixedDrgList() {
        return Arrays.asList(
            createDrgWithCardiacProcedures(),      // 有手术
            createDrgWithHypertension(),           // 无手术
            createDrgWithDiabetes(),               // 无手术
            createDrgWithComplexDiagnosesAndProcedures(), // 有手术
            createDrgWithAngiography()             // 有手术
        );
    }

    /**
     * 创建只有手术的DRG记录列表
     */
    public static List<DrgParsedRecord> createDrgListWithProceduresOnly() {
        return Arrays.asList(
            createDrgWithCardiacProcedures(),
            createDrgWithComplexDiagnosesAndProcedures(),
            createDrgWithAngiography()
        );
    }

    /**
     * 创建只有无手术的DRG记录列表
     */
    public static List<DrgParsedRecord> createDrgListWithoutProceduresOnly() {
        return Arrays.asList(
            createDrgWithHypertension(),
            createDrgWithDiabetes()
        );
    }
    
    /**
     * 创建空的匹配结果
     */
    public static MatchingResult createEmptyMatchingResult() {
        return new MatchingResult(Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * 创建有诊断的匹配结果
     */
    public static MatchingResult createMatchingResultWithDiagnoses() {
        List<String> diagnoses = Arrays.asList("心房颤动", "原发性高血压");
        return new MatchingResult(diagnoses, Collections.emptyList());
    }
    
    /**
     * 创建有手术的匹配结果
     */
    public static MatchingResult createMatchingResultWithProcedures() {
        List<String> procedures = Arrays.asList("经皮左心耳封堵术");
        return new MatchingResult(Collections.emptyList(), procedures);
    }
    
    /**
     * 创建有诊断和手术的匹配结果
     */
    public static MatchingResult createMatchingResultWithDiagnosesAndProcedures() {
        List<String> diagnoses = Arrays.asList("心房颤动", "原发性高血压", "2型糖尿病");
        List<String> procedures = Arrays.asList("经皮左心耳封堵术", "冠状动脉造影");
        return new MatchingResult(diagnoses, procedures);
    }
}
