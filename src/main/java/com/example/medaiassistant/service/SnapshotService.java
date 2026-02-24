package com.example.medaiassistant.service;

import com.example.medaiassistant.model.DrgAnalysisInputSnapshot;
import com.example.medaiassistant.repository.DrgAnalysisInputSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 快照服务类
 * 
 * 提供快照生成、查询和管理功能
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-19
 */
@Service
public class SnapshotService {
    
    private static final Logger logger = LoggerFactory.getLogger(SnapshotService.class);
    
    private final DrgAnalysisInputSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    
    public SnapshotService(DrgAnalysisInputSnapshotRepository snapshotRepository, ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 应用层快照生成逻辑 - 替代存储过程
     * 
     * @param patientId 患者ID
     * @param diagnosisIds 诊断ID集合
     * @param surgeryIds 手术ID集合
     * @param catalogVersion 目录版本
     * @param lastSourceDiagCount 源表诊断条目数
     * @param lastSourceProcCount 源表手术条目数
     * @param operationSource 操作来源
     * @param forceReanalyze 是否强制重算
     * @return 新创建的快照ID，如果未创建则返回null
     */
    public Long callGenDrgInputSnapshot(String patientId, List<String> diagnosisIds,
                                       List<String> surgeryIds, String catalogVersion,
                                       Integer lastSourceDiagCount, Integer lastSourceProcCount,
                                       String operationSource, boolean forceReanalyze) {
        try {
            // 将集合转换为JSON字符串
            String diagnosisIdsJson = objectMapper.writeValueAsString(diagnosisIds);
            String surgeryIdsJson = objectMapper.writeValueAsString(surgeryIds);
            
            // 处理可能为null的参数
            Integer diagCount = lastSourceDiagCount != null ? lastSourceDiagCount : 0;
            Integer procCount = lastSourceProcCount != null ? lastSourceProcCount : 0;
            
            logger.info("应用层快照生成 - patientId: {}, diagnosisCount: {}, surgeryCount: {}, catalogVersion: {}, operationSource: {}, forceReanalyze: {}, lastSourceDiagCount: {}, lastSourceProcCount: {}",
                    patientId, diagnosisIds.size(), surgeryIds.size(), catalogVersion, operationSource, forceReanalyze, diagCount, procCount);
            
            logger.info("诊断ID JSON: {}", diagnosisIdsJson);
            logger.info("手术ID JSON: {}", surgeryIdsJson);
            
            // 应用层快照判定逻辑
            Long snapshotId = generateSnapshotInApplicationLayer(
                    patientId, diagnosisIdsJson, surgeryIdsJson, catalogVersion,
                    diagCount, procCount, operationSource, forceReanalyze);
            
            logger.info("应用层快照生成完成 - snapshotId: {}", snapshotId);
            return snapshotId;
            
        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败", e);
            throw new RuntimeException("JSON序列化失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("应用层快照生成失败 - 详细错误信息:", e);
            throw new RuntimeException("应用层快照生成失败: " + e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * 应用层快照判定和生成逻辑
     */
    private Long generateSnapshotInApplicationLayer(String patientId, String diagnosisIdsJson,
                                                   String surgeryIdsJson, String catalogVersion,
                                                   Integer diagCount, Integer procCount,
                                                   String operationSource, boolean forceReanalyze) {
        // 强制重算直接创建新快照
        if (forceReanalyze) {
            return createNewSnapshot(patientId, diagnosisIdsJson, surgeryIdsJson, catalogVersion,
                                   diagCount, procCount, operationSource);
        }
        
        // 查找患者最新快照
        try {
            // 使用repository查询患者最新快照
            Optional<DrgAnalysisInputSnapshot> latestSnapshot = catalogVersion != null ?
                    snapshotRepository.findLatestByPatientIdAndCatalogVersion(patientId, catalogVersion) :
                    snapshotRepository.findLatestByPatientId(patientId);
            
            if (latestSnapshot.isPresent()) {
                DrgAnalysisInputSnapshot snapshot = latestSnapshot.get();
                // 检查是否需要创建新快照（诊断或手术集合发生变化）
                if (snapshot.getDiagCount().equals(diagCount) && snapshot.getProcCount().equals(procCount)) {
                    logger.info("使用现有快照 - snapshotId: {}, patientId: {}", snapshot.getSnapshotId(), patientId);
                    return snapshot.getSnapshotId();
                }
            }
            
            // 集合发生变化或没有现有快照，创建新快照
            return createNewSnapshot(patientId, diagnosisIdsJson, surgeryIdsJson, catalogVersion,
                                   diagCount, procCount, operationSource);
            
        } catch (Exception e) {
            logger.warn("查询最新快照失败，创建新快照", e);
            return createNewSnapshot(patientId, diagnosisIdsJson, surgeryIdsJson, catalogVersion,
                                   diagCount, procCount, operationSource);
        }
    }
    
    /**
     * 创建新快照记录
     */
    private Long createNewSnapshot(String patientId, String diagnosisIdsJson,
                                  String surgeryIdsJson, String catalogVersion,
                                  Integer diagCount, Integer procCount,
                                  String operationSource) {
        // 这里需要实现实际的数据库插入逻辑
        // 暂时返回模拟的快照ID
        Long snapshotId = System.currentTimeMillis();
        logger.info("创建新快照 - snapshotId: {}, patientId: {}", snapshotId, patientId);
        return snapshotId;
    }
    
    /**
     * 模拟存储过程调用 - 用于测试环境
     * 
     * @param patientId 患者ID
     * @param diagnosisIds 诊断ID集合
     * @param surgeryIds 手术ID集合
     * @param catalogVersion 目录版本
     * @param operationSource 操作来源
     * @param forceReanalyze 是否强制重算
     * @return 模拟的快照ID
     */
    public Long mockCallGenDrgInputSnapshot(String patientId, List<String> diagnosisIds,
                                           List<String> surgeryIds, String catalogVersion,
                                           String operationSource, boolean forceReanalyze) {
        logger.info("模拟存储过程调用 - patientId: {}, diagnosisCount: {}, surgeryCount: {}, catalogVersion: {}, operationSource: {}, forceReanalyze: {}",
                patientId, diagnosisIds.size(), surgeryIds.size(), catalogVersion, operationSource, forceReanalyze);
        
        // 模拟存储过程逻辑
        if (forceReanalyze) {
            // 强制重算，总是返回新ID
            return System.currentTimeMillis();
        }
        
        // 模拟集合变化检测
        // 这里可以添加更复杂的逻辑来模拟实际存储过程的行为
        if (diagnosisIds.size() > 0 || surgeryIds.size() > 0) {
            return System.currentTimeMillis();
        } else {
            return null; // 模拟不需要创建快照的情况
        }
    }
}
