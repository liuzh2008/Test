package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.UserDecisionRequest;
import com.example.medaiassistant.model.DrgAnalysisResult;

/**
 * 用户决策服务接口
 * 处理用户选择MCC类别的业务逻辑
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-10
 */
public interface UserDecisionService {
    
    /**
     * 用户选择MCC类别并保存分析结果
     * 
     * @param request 用户决策请求
     * @return 保存后的分析结果
     */
    DrgAnalysisResult saveUserDecision(UserDecisionRequest request);
    
    /**
     * 根据分析结果ID查询决策结果
     * 
     * @param resultId 分析结果ID
     * @return 分析结果
     */
    DrgAnalysisResult getDecisionResult(Long resultId);
    
    /**
     * 根据患者ID查询决策结果
     * 
     * @param patientId 患者ID
     * @return 分析结果列表
     */
    java.util.List<DrgAnalysisResult> getDecisionResultsByPatientId(String patientId);
}
