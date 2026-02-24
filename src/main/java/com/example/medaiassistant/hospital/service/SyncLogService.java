package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.dto.PatientSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步日志服务
 * 负责记录病人数据同步的日志信息和提供统计查询
 * 
 * @author System
 * @version 2.0
 * @since 2025-12-10
 */
@Service
@Slf4j
public class SyncLogService {
    
    // 内存中的同步日志记录（简化实现，实际项目中应该使用数据库）
    private final List<Map<String, Object>> syncLogs = new ArrayList<>();
    
    /**
     * 记录病人数据同步日志
     * 
     * @param result 同步结果
     */
    public void logPatientSync(PatientSyncResult result) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("hospitalId", result.getHospitalId());
        logEntry.put("department", result.getDepartment());
        logEntry.put("success", result.isSuccess());
        logEntry.put("executionTime", result.getExecutionTime());
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("oraclePatientCount", result.getOraclePatientCount());
        logEntry.put("mainServerPatientCount", result.getMainServerPatientCount());
        logEntry.put("addedCount", result.getAddedCount());
        logEntry.put("updatedCount", result.getUpdatedCount());
        logEntry.put("dischargedCount", result.getDischargedCount());
        logEntry.put("errorMessage", result.getErrorMessage());
        
        syncLogs.add(logEntry);
        
        if (result.isSuccess()) {
            log.info("病人数据同步成功 - 医院: {}, 科室: {}, {}", 
                result.getHospitalId(), result.getDepartment(), result.getSummary());
        } else {
            log.error("病人数据同步失败 - 医院: {}, 科室: {}, 错误: {}", 
                result.getHospitalId(), result.getDepartment(), result.getErrorMessage());
        }
    }
    
    /**
     * 获取最近的同步记录
     * 
     * @param hospitalId 医院ID（可选）
     * @param department 科室名称（可选）
     * @param limit 返回记录数限制
     * @return 最近的同步记录列表
     */
    public List<Map<String, Object>> getRecentPatientSyncLogs(String hospitalId, String department, int limit) {
        List<Map<String, Object>> filteredLogs = new ArrayList<>();
        
        for (Map<String, Object> logEntry : syncLogs) {
            boolean matches = true;
            
            if (hospitalId != null && !hospitalId.isEmpty()) {
                matches = matches && hospitalId.equals(logEntry.get("hospitalId"));
            }
            
            if (department != null && !department.isEmpty()) {
                matches = matches && department.equals(logEntry.get("department"));
            }
            
            if (matches) {
                filteredLogs.add(logEntry);
            }
        }
        
        // 按时间戳倒序排序（最新的在前）
        filteredLogs.sort((a, b) -> {
            long timeA = (Long) a.get("timestamp");
            long timeB = (Long) b.get("timestamp");
            return Long.compare(timeB, timeA);
        });
        
        // 限制返回数量
        return filteredLogs.subList(0, Math.min(limit, filteredLogs.size()));
    }
    
    /**
     * 获取同步记录数量
     * 
     * @param hospitalId 医院ID（可选）
     * @param department 科室名称（可选）
     * @return 同步记录数量
     */
    public long getPatientSyncCount(String hospitalId, String department) {
        return syncLogs.stream()
            .filter(logEntry -> {
                boolean matches = true;
                
                if (hospitalId != null && !hospitalId.isEmpty()) {
                    matches = matches && hospitalId.equals(logEntry.get("hospitalId"));
                }
                
                if (department != null && !department.isEmpty()) {
                    matches = matches && department.equals(logEntry.get("department"));
                }
                
                return matches;
            })
            .count();
    }
    
    /**
     * 获取成功的同步记录数量
     * 
     * @param hospitalId 医院ID（可选）
     * @param department 科室名称（可选）
     * @return 成功的同步记录数量
     */
    public long getPatientSyncSuccessCount(String hospitalId, String department) {
        return syncLogs.stream()
            .filter(logEntry -> {
                boolean matches = (Boolean) logEntry.get("success");
                
                if (hospitalId != null && !hospitalId.isEmpty()) {
                    matches = matches && hospitalId.equals(logEntry.get("hospitalId"));
                }
                
                if (department != null && !department.isEmpty()) {
                    matches = matches && department.equals(logEntry.get("department"));
                }
                
                return matches;
            })
            .count();
    }
    
    /**
     * 获取平均同步执行时间
     * 
     * @param hospitalId 医院ID（可选）
     * @param department 科室名称（可选）
     * @return 平均执行时间（毫秒）
     */
    public double getAveragePatientSyncTime(String hospitalId, String department) {
        List<Map<String, Object>> filteredLogs = syncLogs.stream()
            .filter(logEntry -> {
                boolean matches = true;
                
                if (hospitalId != null && !hospitalId.isEmpty()) {
                    matches = matches && hospitalId.equals(logEntry.get("hospitalId"));
                }
                
                if (department != null && !department.isEmpty()) {
                    matches = matches && department.equals(logEntry.get("department"));
                }
                
                return matches;
            })
            .toList();
        
        if (filteredLogs.isEmpty()) {
            return 0.0;
        }
        
        double totalTime = filteredLogs.stream()
            .mapToLong(logEntry -> (Long) logEntry.get("executionTime"))
            .sum();
        
        return totalTime / filteredLogs.size();
    }
    
    /**
     * 获取同步统计信息
     * 
     * @param hospitalId 医院ID（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @return 同步统计信息
     */
    public Map<String, Object> getPatientSyncStatistics(String hospitalId, String startDate, String endDate) {
        // 简化实现：忽略日期过滤
        long totalCount = getPatientSyncCount(hospitalId, null);
        long successCount = getPatientSyncSuccessCount(hospitalId, null);
        long failureCount = totalCount - successCount;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        
        return stats;
    }
    
    /**
     * 获取科室分布统计
     * 
     * @param hospitalId 医院ID（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @return 科室分布统计
     */
    public List<Map<String, Object>> getPatientSyncDepartmentDistribution(String hospitalId, String startDate, String endDate) {
        // 简化实现：统计各科室的同步次数
        Map<String, Long> departmentCounts = new HashMap<>();
        
        for (Map<String, Object> logEntry : syncLogs) {
            if (hospitalId != null && !hospitalId.isEmpty()) {
                if (!hospitalId.equals(logEntry.get("hospitalId"))) {
                    continue;
                }
            }
            
            String department = (String) logEntry.get("department");
            departmentCounts.put(department, departmentCounts.getOrDefault(department, 0L) + 1);
        }
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map.Entry<String, Long> entry : departmentCounts.entrySet()) {
            Map<String, Object> deptStat = new HashMap<>();
            deptStat.put("department", entry.getKey());
            deptStat.put("syncCount", entry.getValue());
            distribution.add(deptStat);
        }
        
        return distribution;
    }
    
    /**
     * 获取时间趋势统计
     * 
     * @param hospitalId 医院ID（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @return 时间趋势统计
     */
    public List<Map<String, Object>> getPatientSyncTimeTrend(String hospitalId, String startDate, String endDate) {
        // 简化实现：按小时分组统计
        Map<String, Long> hourlyCounts = new HashMap<>();
        
        for (Map<String, Object> logEntry : syncLogs) {
            if (hospitalId != null && !hospitalId.isEmpty()) {
                if (!hospitalId.equals(logEntry.get("hospitalId"))) {
                    continue;
                }
            }
            
            long timestamp = (Long) logEntry.get("timestamp");
            // 简化为按小时分组
            String hourKey = String.format("%02d:00", (timestamp / (60 * 60 * 1000)) % 24);
            hourlyCounts.put(hourKey, hourlyCounts.getOrDefault(hourKey, 0L) + 1);
        }
        
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, Long> entry : hourlyCounts.entrySet()) {
            Map<String, Object> hourStat = new HashMap<>();
            hourStat.put("time", entry.getKey());
            hourStat.put("syncCount", entry.getValue());
            trend.add(hourStat);
        }
        
        return trend;
    }
    
    /**
     * 获取所有同步日志（用于调试）
     * 
     * @return 所有同步日志
     */
    public List<Map<String, Object>> getAllSyncLogs() {
        return new ArrayList<>(syncLogs);
    }
    
    /**
     * 清空同步日志（用于测试）
     */
    public void clearSyncLogs() {
        syncLogs.clear();
    }
}
