package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DrgCatalog;
import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import com.example.medaiassistant.dto.drg.DiagnosisEntry;
import com.example.medaiassistant.dto.drg.ProcedureEntry;
import com.example.medaiassistant.dto.drg.PatientDiagnosis;
import com.example.medaiassistant.dto.drg.PatientProcedure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 主要诊断与手术匹配器
 * 
 * 实现完整的DRG匹配流程，集成所有匹配组件：
 * 1. 分流过滤
 * 2. ICD精确匹配
 * 3. 名称相似度匹配
 * 4. 名称收集聚合
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@Component
public class PrimaryDiagnosisProcedureMatcher {

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    
    /**
     * 执行完整的DRG匹配流程
     * 
     * @param patientData 患者数据
     * @param catalog DRG目录
     * @return 匹配结果，包含主要诊断和手术列表
     */
    public MatchingResult match(PatientData patientData, DrgCatalog catalog) {
        // 处理空值情况
        if (patientData == null || catalog == null || catalog.isEmpty()) {
            return new MatchingResult(List.of(), List.of());
        }
        
        // 1. 分流过滤：根据患者是否有手术过滤DRG记录
        List<DrgParsedRecord> filteredDrgs = DrgFilter.filterByProcedurePresence(patientData, catalog.getDrgRecords());
        
        if (filteredDrgs.isEmpty()) {
            return new MatchingResult(List.of(), List.of());
        }
        
        // 2. 创建名称收集器
        NameCollector nameCollector = new NameCollector();
        
        // 3. 对每个过滤后的DRG记录进行匹配
        for (DrgParsedRecord drg : filteredDrgs) {
            boolean diagnosisMatched = matchDiagnoses(patientData, drg);
            boolean procedureMatched = matchProcedures(patientData, drg);
            
            // 4. 收集匹配成功的名称
            if (diagnosisMatched || procedureMatched) {
                nameCollector.collectDrgNames(drg, diagnosisMatched, procedureMatched);
            }
        }
        
        // 5. 返回匹配结果
        return new MatchingResult(
            new ArrayList<>(nameCollector.getPrimaryDiagnoses()),
            new ArrayList<>(nameCollector.getPrimaryProcedures())
        );
    }
    
    /**
     * 匹配诊断
     * 
     * @param patientData 患者数据
     * @param drg DRG记录
     * @return 是否匹配成功
     */
    private boolean matchDiagnoses(PatientData patientData, DrgParsedRecord drg) {
        if (!patientData.hasDiagnoses() || drg.getDiagnoses().isEmpty()) {
            return false;
        }
        
        // 对每个患者诊断进行匹配
        for (PatientDiagnosis patientDiagnosis : patientData.getDiagnoses()) {
            for (DiagnosisEntry drgDiagnosis : drg.getDiagnoses()) {
                // 尝试ICD精确匹配
                if (IcdMatcher.exactMatch(patientDiagnosis.getIcdCode(), drgDiagnosis.getIcdCode())) {
                    return true;
                }
                
                // 尝试名称相似度匹配
                if (NameMatcher.similarityMatch(patientDiagnosis.getDiagnosisName(), drgDiagnosis.getDiagnosisName(), DEFAULT_SIMILARITY_THRESHOLD)) {
                    return true;
                }
                
                // 尝试别名匹配
                if (drgDiagnosis.hasAliases()) {
                    for (String alias : drgDiagnosis.getAliases()) {
                        if (NameMatcher.similarityMatch(patientDiagnosis.getDiagnosisName(), alias, DEFAULT_SIMILARITY_THRESHOLD)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 匹配手术
     * 
     * @param patientData 患者数据
     * @param drg DRG记录
     * @return 是否匹配成功
     */
    private boolean matchProcedures(PatientData patientData, DrgParsedRecord drg) {
        if (!patientData.hasProcedures() || drg.getProcedures().isEmpty()) {
            return false;
        }
        
        // 对每个患者手术进行匹配
        for (PatientProcedure patientProcedure : patientData.getProcedures()) {
            for (ProcedureEntry drgProcedure : drg.getProcedures()) {
                // 尝试ICD精确匹配
                if (IcdMatcher.exactMatch(patientProcedure.getProcedureCode(), drgProcedure.getProcedureCode())) {
                    return true;
                }
                
                // 尝试名称相似度匹配
                if (NameMatcher.similarityMatch(patientProcedure.getProcedureName(), drgProcedure.getProcedureName(), DEFAULT_SIMILARITY_THRESHOLD)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
