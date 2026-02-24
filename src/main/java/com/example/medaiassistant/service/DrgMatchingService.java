package com.example.medaiassistant.service;

import com.example.medaiassistant.drg.matching.PrimaryDiagnosisProcedureMatcher;
import com.example.medaiassistant.dto.drg.DrgCatalog;
import com.example.medaiassistant.dto.drg.MatchingResult;
import com.example.medaiassistant.dto.drg.PatientData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * DRG匹配服务
 * 
 * 提供主要诊断与手术匹配的服务层接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
@Service
public class DrgMatchingService {

    private final PrimaryDiagnosisProcedureMatcher primaryMatcher;

    @Autowired
    public DrgMatchingService(PrimaryDiagnosisProcedureMatcher primaryMatcher) {
        this.primaryMatcher = primaryMatcher;
    }

    /**
     * 匹配患者的主要诊断和手术
     * 
     * @param patientData 患者数据
     * @param catalog DRG目录
     * @return 匹配结果，包含主要诊断和手术列表
     */
    public MatchingResult matchPrimaryDiagnosisAndProcedure(PatientData patientData, DrgCatalog catalog) {
        return primaryMatcher.match(patientData, catalog);
    }
}
