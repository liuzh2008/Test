package com.example.medaiassistant.service;

import com.example.medaiassistant.config.MccScreeningProperties;
import com.example.medaiassistant.model.DrgMcc;
import com.example.medaiassistant.model.MccCandidate;
import com.example.medaiassistant.model.PatientDiagnosis;
import com.example.medaiassistant.repository.DrgMccRepository;
import com.example.medaiassistant.util.LevenshteinUtil;
import com.example.medaiassistant.util.TextNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * MCC预筛选服务类
 * 负责从患者诊断列表中筛选出与并发症字典相似的候选MCC
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-09
 */
@Service
public class MccScreeningService {
    
    @Autowired
    private LevenshteinUtil levenshteinUtil;
    
    @Autowired
    private TextNormalizer textNormalizer;
    
    @Autowired
    private MccScreeningProperties mccScreeningProperties;
    
    @Autowired
    private DrgMccRepository drgMccRepository;
    
    /**
     * MCC字典缓存 - 使用不可变对象确保线程安全
     */
    private AtomicReference<List<DrgMcc>> cachedMccDictionary = new AtomicReference<>();
    
    /**
     * 规范化MCC名称缓存 - 提高相似度计算性能
     */
    private AtomicReference<Map<String, String>> normalizedMccNames = new AtomicReference<>();
    
    /**
     * 初始化方法 - 启动时预加载MCC字典到内存
     */
    @PostConstruct
    public void init() {
        loadMccDictionary();
    }
    
    /**
     * 全局相似度阈值（从配置读取）
     */
    private double getGlobalThreshold() {
        return mccScreeningProperties.getSimilarityThreshold();
    }
    
    /**
     * 加载MCC字典到缓存
     * 使用不可变对象确保线程安全
     */
    private void loadMccDictionary() {
        List<DrgMcc> allMccs = drgMccRepository.findAll();
        
        // 预计算规范化名称，提高相似度计算性能
        Map<String, String> normalized = new HashMap<>();
        for (DrgMcc mcc : allMccs) {
            if (mcc.getMccCode() != null && mcc.getMccName() != null) {
                String normalizedName = textNormalizer.normalize(mcc.getMccName());
                if (normalizedName != null) {
                    normalized.put(mcc.getMccCode(), normalizedName);
                }
            }
        }
        
        // 使用不可变对象确保线程安全
        cachedMccDictionary.set(Collections.unmodifiableList(allMccs));
        normalizedMccNames.set(Collections.unmodifiableMap(normalized));
    }
    
    /**
     * 重新加载MCC字典
     * 支持字典热刷新而不影响运行中流程
     */
    public void reloadDictionary() {
        loadMccDictionary();
    }
    
    /**
     * 获取缓存的MCC字典
     * 如果缓存为空，则从数据库加载
     */
    private List<DrgMcc> getCachedMccDictionary() {
        List<DrgMcc> cached = cachedMccDictionary.get();
        if (cached == null || cached.isEmpty()) {
            loadMccDictionary();
            cached = cachedMccDictionary.get();
        }
        return cached;
    }
    
    /**
     * 计算两个诊断名称的相似度
     * 
     * @param diagnosis 患者诊断名称
     * @param mccName MCC名称
     * @return 相似度值（0.0-1.0）
     */
    public double calculateSimilarity(String diagnosis, String mccName) {
        return levenshteinUtil.calculateNormalizedSimilarity(
            diagnosis, mccName, textNormalizer
        );
    }
    
    /**
     * 尝试进行CODE精确匹配
     * 
     * @param diagnosis 患者诊断
     * @param mcc MCC字典记录
     * @return 匹配的候选结果，如果不匹配则返回空
     */
    public Optional<MccCandidate> tryCodeExactMatch(PatientDiagnosis diagnosis, DrgMcc mcc) {
        // 检查参数有效性
        if (diagnosis == null || mcc == null) {
            return Optional.empty();
        }
        
        String diagnosisIcdCode = diagnosis.getIcdCode();
        String mccCode = mcc.getMccCode();
        
        // 如果任一编码为空，返回空
        if (diagnosisIcdCode == null || mccCode == null) {
            return Optional.empty();
        }
        
        // 去除前后空格并忽略大小写进行比较
        String trimmedDiagnosisCode = diagnosisIcdCode.trim();
        String trimmedMccCode = mccCode.trim();
        
        if (trimmedDiagnosisCode.equalsIgnoreCase(trimmedMccCode)) {
            // 编码完全匹配，创建候选结果
            return Optional.of(MccCandidate.builder()
                .mccCode(mcc.getMccCode())
                .mccName(mcc.getMccName())
                .mccType(mcc.getMccType())
                .similarity(1.0)
                .matchType(MccCandidate.MATCH_TYPE_CODE_MATCH)
                .excluded(false)
                .sourceDiagnosis(diagnosis.getDiagnosisName())
                .sourceIcdCode(diagnosis.getIcdCode())
                .build());
        }
        
        return Optional.empty();
    }
    
    /**
     * 检查排除规则
     * 根据MCC_EXCEPT字段中的ICD编码列表排除不适用的候选
     * 
     * @param diagnosis 患者诊断
     * @param mcc MCC字典记录
     * @return 是否被排除
     */
    public boolean checkExclusionRules(PatientDiagnosis diagnosis, DrgMcc mcc) {
        // 检查参数有效性
        if (!isValidParameters(diagnosis, mcc)) {
            return false;
        }
        
        // 检查排除规则开关
        if (!isExclusionCheckEnabled()) {
            return false;
        }
        
        // 获取排除条件
        String except = mcc.getMccExcept();
        if (isBlankExclusionCondition(except)) {
            return false;
        }
        
        // 获取患者诊断编码
        String diagnosisIcdCode = diagnosis.getIcdCode();
        if (isBlankDiagnosisCode(diagnosisIcdCode)) {
            return false;
        }
        
        // 检查是否在排除列表中
        return isInExclusionList(diagnosisIcdCode, except);
    }
    
    /**
     * 检查参数有效性
     */
    private boolean isValidParameters(PatientDiagnosis diagnosis, DrgMcc mcc) {
        return diagnosis != null && mcc != null;
    }
    
    /**
     * 检查排除规则开关是否启用
     */
    private boolean isExclusionCheckEnabled() {
        return mccScreeningProperties.isExclusionCheckEnabled();
    }
    
    /**
     * 检查排除条件是否为空
     */
    private boolean isBlankExclusionCondition(String except) {
        return except == null || except.isBlank();
    }
    
    /**
     * 检查诊断编码是否为空
     */
    private boolean isBlankDiagnosisCode(String diagnosisIcdCode) {
        return diagnosisIcdCode == null || diagnosisIcdCode.isBlank();
    }
    
    /**
     * 检查诊断编码是否在排除列表中
     */
    private boolean isInExclusionList(String diagnosisIcdCode, String except) {
        // 解析排除条件（支持逗号、分号、空格分隔）
        String[] exclusionCodes = except.split("[,;\\s]+");
        
        // 检查患者诊断编码是否在排除列表中（忽略大小写）
        for (String exclusionCode : exclusionCodes) {
            String trimmedExclusionCode = exclusionCode.trim();
            if (!trimmedExclusionCode.isBlank() && 
                diagnosisIcdCode.trim().equalsIgnoreCase(trimmedExclusionCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 根据相似度阈值过滤候选
     * 
     * @param similarity 相似度值
     * @param threshold 阈值（如果为null则使用全局阈值）
     * @return 是否通过阈值过滤
     */
    public boolean isSimilarityAboveThreshold(double similarity, Double threshold) {
        double actualThreshold = threshold != null ? threshold : getGlobalThreshold();
        return similarity >= actualThreshold;
    }
    
    /**
     * 使用全局阈值检查相似度是否通过
     * 
     * @param similarity 相似度值
     * @return 是否通过阈值过滤
     */
    public boolean isSimilarityAboveThreshold(double similarity) {
        return isSimilarityAboveThreshold(similarity, null);
    }
    
    /**
     * 使用指定阈值检查相似度是否通过
     * 
     * @param similarity 相似度值
     * @param threshold 指定阈值
     * @return 是否通过阈值过滤
     */
    public boolean isSimilarityAboveThreshold(double similarity, double threshold) {
        return similarity >= threshold;
    }
    
    /**
     * 获取当前全局阈值
     * 
     * @return 全局相似度阈值
     */
    public double getCurrentThreshold() {
        return getGlobalThreshold();
    }
    
    /**
     * 获取配置属性
     * 
     * @return MCC预筛选配置属性
     */
    public MccScreeningProperties getMccScreeningProperties() {
        return mccScreeningProperties;
    }
    
    /**
     * 对候选列表进行排序
     * 按相似度降序排序，相同相似度时MCC优先于CC
     * 
     * @param candidates 候选列表
     * @return 排序后的候选列表
     */
    public List<MccCandidate> sortCandidates(List<MccCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        return candidates.stream()
            .sorted(Comparator
                .comparing(MccCandidate::getSimilarity).reversed()
                .thenComparing(candidate -> "MCC".equals(candidate.getMccType()) ? 0 : 1)
            )
            .collect(Collectors.toList());
    }
    
    /**
     * 按来源诊断分组候选
     * 将候选按来源诊断分组，每组内按相似度降序排序，并应用Top-K控制
     * 
     * @param diagnoses 患者诊断列表
     * @return 按诊断分组的候选结果
     */
    public Map<String, List<MccCandidate>> screenMccCandidatesGrouped(List<PatientDiagnosis> diagnoses) {
        // 获取所有候选（平铺列表）
        List<MccCandidate> allCandidates = screenMccCandidates(diagnoses);
        
        // 按来源诊断分组，并对每组内的候选进行排序
        Map<String, List<MccCandidate>> grouped = allCandidates.stream()
            .collect(Collectors.groupingBy(
                MccCandidate::getSourceDiagnosis,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::sortCandidates
                )
            ));
        
        // 应用Top-K控制
        return applyTopKControl(grouped);
    }
    
    /**
     * 应用Top-K控制到分组后的候选结果
     * 如果Top-K开关开启，则对每个诊断的候选列表进行截断
     * 
     * @param grouped 分组后的候选结果
     * @return 应用Top-K控制后的候选结果
     */
    private Map<String, List<MccCandidate>> applyTopKControl(Map<String, List<MccCandidate>> grouped) {
        // 检查Top-K开关是否开启
        if (!mccScreeningProperties.isTopKEnabled()) {
            return grouped;
        }
        
        // 获取Top-K值
        int topK = mccScreeningProperties.getTopKDiag();
        
        // 对每个诊断的候选列表应用Top-K控制
        Map<String, List<MccCandidate>> result = new HashMap<>();
        for (Map.Entry<String, List<MccCandidate>> entry : grouped.entrySet()) {
            String diagnosis = entry.getKey();
            List<MccCandidate> candidates = entry.getValue();
            
            // 如果候选数量超过Top-K值，则截断列表
            if (candidates.size() > topK) {
                result.put(diagnosis, candidates.subList(0, topK));
            } else {
                result.put(diagnosis, candidates);
            }
        }
        
        return result;
    }
    
    /**
     * 筛选MCC候选（平铺列表）
     * 对每个患者诊断，遍历MCC字典，生成候选列表
     * 使用缓存提高性能，支持并发访问
     * 
     * @param diagnoses 患者诊断列表
     * @return 所有候选结果列表
     */
    public List<MccCandidate> screenMccCandidates(List<PatientDiagnosis> diagnoses) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用缓存获取MCC字典记录，避免重复数据库查询
        List<DrgMcc> allMccs = getCachedMccDictionary();
        if (allMccs.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<MccCandidate> allCandidates = new ArrayList<>();
        
        // 对每个患者诊断进行筛选
        for (PatientDiagnosis diagnosis : diagnoses) {
            // 跳过无效的诊断
            if (diagnosis == null || !diagnosis.hasDiagnosisName()) {
                continue;
            }
            
            // 对每个MCC记录进行匹配
            for (DrgMcc mcc : allMccs) {
                // 1. 尝试CODE精确匹配
                Optional<MccCandidate> codeMatch = tryCodeExactMatch(diagnosis, mcc);
                if (codeMatch.isPresent()) {
                    allCandidates.add(codeMatch.get());
                    continue; // CODE匹配成功，跳过名称相似度匹配
                }
                
                // 2. 名称相似度匹配
                if (diagnosis.hasDiagnosisName() && mcc.getMccName() != null) {
                    double similarity = calculateSimilarity(diagnosis.getDiagnosisName(), mcc.getMccName());
                    
                    // 检查是否通过相似度阈值
                    if (isSimilarityAboveThreshold(similarity)) {
                        // 检查排除规则
                        boolean excluded = checkExclusionRules(diagnosis, mcc);
                        
                        // 创建候选结果
                        MccCandidate candidate = MccCandidate.builder()
                            .mccCode(mcc.getMccCode())
                            .mccName(mcc.getMccName())
                            .mccType(mcc.getMccType())
                            .similarity(similarity)
                            .matchType(MccCandidate.MATCH_TYPE_NAME_MATCH)
                            .excluded(excluded)
                            .sourceDiagnosis(diagnosis.getDiagnosisName())
                            .sourceIcdCode(diagnosis.getIcdCode())
                            .build();
                        
                        allCandidates.add(candidate);
                    }
                }
            }
        }
        
        return allCandidates;
    }
}
