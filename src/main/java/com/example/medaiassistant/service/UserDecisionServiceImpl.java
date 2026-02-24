package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.UserDecisionRequest;
import com.example.medaiassistant.enums.MccType;
import com.example.medaiassistant.model.DrgAnalysisResult;
import com.example.medaiassistant.repository.DrgAnalysisResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserDecisionService实现类
 * 处理用户选择MCC类别的业务逻辑实现
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-10
 */
@Service
@Transactional
public class UserDecisionServiceImpl implements UserDecisionService {

    private final DrgAnalysisResultRepository repository;

    /**
     * 构造函数注入
     * 
     * @param repository DRG分析结果仓库
     */
    @Autowired
    public UserDecisionServiceImpl(DrgAnalysisResultRepository repository) {
        this.repository = repository;
    }

    /**
     * 用户选择MCC类别并保存分析结果
     * 
     * @param request 用户决策请求
     * @return 保存后的分析结果
     * @throws IllegalArgumentException 当分析结果不存在时抛出异常
     */
    @Override
    public DrgAnalysisResult saveUserDecision(UserDecisionRequest request) {
        // 验证请求参数
        validateRequest(request);
        
        // 查找现有的分析结果
        Long resultId = request.getResultId();
        Optional<DrgAnalysisResult> existingResultOpt = repository.findById(resultId);
        
        if (existingResultOpt.isEmpty()) {
            throw new IllegalArgumentException("分析结果不存在，ID: " + resultId);
        }
        
        DrgAnalysisResult existingResult = existingResultOpt.get();
        
        // 更新用户选择的MCC类型
        existingResult.setUserSelectedMccType(request.getSelectedMccType().name());
        
        // 生成最终的DRG编码
        String finalDrgCode = generateFinalDrgCode(existingResult.getFinalDrgCode(), request.getSelectedMccType());
        existingResult.setFinalDrgCode(finalDrgCode);
        
        // 保存更新后的分析结果
        DrgAnalysisResult savedResult = repository.save(existingResult);
        
        return savedResult;
    }

    /**
     * 根据分析结果ID查询决策结果
     * 
     * @param resultId 分析结果ID
     * @return 分析结果
     * @throws IllegalArgumentException 当分析结果不存在时抛出异常
     */
    @Override
    @Transactional(readOnly = true)
    public DrgAnalysisResult getDecisionResult(Long resultId) {
        Optional<DrgAnalysisResult> resultOpt = repository.findById(resultId);
        
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("分析结果不存在，ID: " + resultId);
        }
        
        return resultOpt.get();
    }

    /**
     * 根据患者ID查询决策结果
     * 
     * @param patientId 患者ID
     * @return 分析结果列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<DrgAnalysisResult> getDecisionResultsByPatientId(String patientId) {
        return repository.findByPatientIdAndNotDeleted(patientId);
    }

    /**
     * 验证用户决策请求
     * 
     * @param request 用户决策请求
     * @throws IllegalArgumentException 当请求参数无效时抛出异常
     */
    private void validateRequest(UserDecisionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("用户决策请求不能为空");
        }
        
        if (request.getResultId() == null) {
            throw new IllegalArgumentException("分析结果ID不能为空");
        }
        
        if (request.getSelectedMccType() == null) {
            throw new IllegalArgumentException("MCC类型不能为空");
        }
        
        if (request.getOperator() == null || request.getOperator().trim().isEmpty()) {
            throw new IllegalArgumentException("操作者不能为空");
        }
    }

    /**
     * 生成最终的DRG编码
     * 根据基础DRG编码和用户选择的MCC类型生成最终的DRG编码
     * 
     * @param baseDrgCode 基础DRG编码
     * @param mccType 用户选择的MCC类型
     * @return 最终的DRG编码
     */
    private String generateFinalDrgCode(String baseDrgCode, MccType mccType) {
        if (baseDrgCode == null || baseDrgCode.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        switch (mccType) {
            case MCC:
                return baseDrgCode + "-MCC";
            case CC:
                return baseDrgCode + "-CC";
            case NONE:
                return baseDrgCode;
            default:
                return baseDrgCode;
        }
    }
}
