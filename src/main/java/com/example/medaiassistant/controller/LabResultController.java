package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.LabResult;
import com.example.medaiassistant.repository.LabResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api/lab-results")
public class LabResultController {

    @Autowired
    private LabResultRepository labResultRepository;

    /**
     * 按患者ID查询化验结果
     * <p>返回指定患者的所有化验结果，先按报告时间降序排列（最新的在前），再按异常状态排序（H、L、N）</p>
     * 
     * @param patientId 患者ID
     * @return 化验结果列表，按labReportTime降序、异常状态排序
     */
    @GetMapping("/by-patient/{patientId}")
    @Transactional(readOnly = true)
    public List<LabResult> getLabResultsByPatientId(@PathVariable String patientId) {
        List<LabResult> results = labResultRepository.findByPatientId(patientId);
        // 强制在事务内加载CLOB字段，避免延迟加载导致序列化失败
        results.forEach(r -> {
            if (r.getLabResult() != null) {
                r.getLabResult().length();
            }
        });
        // 先按labReportTime降序排序（最新的在前），再按异常标志排序（H、L、N）
        results.sort((a, b) -> {
            if (a.getLabReportTime() == null) return 1;
            if (b.getLabReportTime() == null) return -1;
            int timeCompare = b.getLabReportTime().compareTo(a.getLabReportTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            int abnormalOrderA = getAbnormalOrder(a.getAbnormalIndicator());
            int abnormalOrderB = getAbnormalOrder(b.getAbnormalIndicator());
            return Integer.compare(abnormalOrderA, abnormalOrderB);
        });
        return results;
    }

    /**
     * 按患者ID和分析状态查询化验结果
     * <p>返回指定患者按分析状态筛选的化验结果，先按报告时间降序排列，再按异常状态排序（H、L、N）</p>
     * 
     * @param patientId 患者ID
     * @param isAnalyzed 分析状态（1=已分析，0=未分析）
     * @return 化验结果列表，按labReportTime降序、异常状态排序
     */
    @GetMapping("/by-patient-and-analyzed/{patientId}/{isAnalyzed}")
    @Transactional(readOnly = true)
    public List<LabResult> getLabResultsByPatientIdAndAnalyzed(
            @PathVariable String patientId,
            @PathVariable Integer isAnalyzed) {
        List<LabResult> results = labResultRepository.findByPatientIdAndIsAnalyzed(patientId, isAnalyzed);
        // 强制在事务内加载CLOB字段
        results.forEach(r -> {
            if (r.getLabResult() != null) {
                r.getLabResult().length();
            }
        });
        // 先按labReportTime降序排序（最新的在前），再按异常标志排序（H、L、N）
        results.sort((a, b) -> {
            if (a.getLabReportTime() == null) return 1;
            if (b.getLabReportTime() == null) return -1;
            int timeCompare = b.getLabReportTime().compareTo(a.getLabReportTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            int abnormalOrderA = getAbnormalOrder(a.getAbnormalIndicator());
            int abnormalOrderB = getAbnormalOrder(b.getAbnormalIndicator());
            return Integer.compare(abnormalOrderA, abnormalOrderB);
        });
        return results;
    }

    /**
     * 调试端点：查看数据库中PatientID的实际格式
     */
    @GetMapping("/debug/patient-ids")
    @Transactional(readOnly = true)
    public Map<String, Object> debugPatientIds() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<LabResult> allResults = labResultRepository.findAll();
        result.put("总记录数", allResults.size());
        
        // 获取前20个不同的PatientID样本
        List<String> patientIdSamples = allResults.stream()
            .map(LabResult::getPatientId)
            .filter(id -> id != null)
            .distinct()
            .limit(20)
            .collect(java.util.stream.Collectors.toList());
        result.put("PatientID样本(前20个)", patientIdSamples);
        
        return result;
    }

    @GetMapping("/formatted-by-patient/{patientId}")
    @Transactional(readOnly = true)
    public String getFormattedLabResultsByPatient(@PathVariable String patientId) {
        List<LabResult> labResults = labResultRepository.findByPatientId(patientId);
        System.out.println("查询到化验结果数量: " + labResults.size());
        System.out.println("查询使用的PatientID: [" + patientId + "]");
        
        // 按时间排序 - labReportTime现在是Timestamp类型，直接比较
        labResults.sort((a, b) -> {
            try {
                if (a.getLabReportTime() == null) return 1;
                if (b.getLabReportTime() == null) return -1;
                return a.getLabReportTime().compareTo(b.getLabReportTime());
            } catch (Exception e) {
                return 0;
            }
        });
        
        // 按时间和类型分组
        Map<String, Map<String, List<LabResult>>> groupedResults = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        
        for (LabResult labResult : labResults) {
            String issueTime = "未知时间";
            try {
                if (labResult.getLabReportTime() != null) {
                    // labReportTime现在是Timestamp类型，直接格式化
                    issueTime = sdf.format(labResult.getLabReportTime());
                }
            } catch (Exception e) {
                System.out.println("日期格式化失败: " + labResult.getLabReportTime());
            }
            
            String labType = labResult.getLabType() != null ? labResult.getLabType() : "未知类型";
            
            groupedResults
                .computeIfAbsent(issueTime, k -> new LinkedHashMap<>())
                .computeIfAbsent(labType, k -> new ArrayList<>())
                .add(labResult);
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, List<LabResult>>> timeEntry : groupedResults.entrySet()) {
            String issueTime = timeEntry.getKey();
            
            for (Map.Entry<String, List<LabResult>> typeEntry : timeEntry.getValue().entrySet()) {
                String labType = typeEntry.getKey();
                sb.append(issueTime).append(" ").append(labType).append("\n");
                
                for (LabResult labResult : typeEntry.getValue()) {
                    String labName = labResult.getLabName() != null ? labResult.getLabName() : "未知项目";
                    String resultValue = labResult.getLabResult() != null ? labResult.getLabResult() : "";
                    String unit = labResult.getUnit() != null ? labResult.getUnit() : "";
                    String abnormalIndicator = labResult.getAbnormalIndicator() != null ? labResult.getAbnormalIndicator() : "";
                    
                    sb.append(labName).append(" ")
                      .append(resultValue).append(" ")
                      .append(unit);
                      
                    if ("h".equalsIgnoreCase(abnormalIndicator)) {
                        sb.append(" (升高)");
                    } else if ("l".equalsIgnoreCase(abnormalIndicator)) {
                        sb.append(" (降低)");
                    }
                    
                    sb.append("\n");
                }
                
                sb.append("\n");
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * 异常标志排序权重：H(0) < L(1) < N(2) < 其它/空(3)
     * 
     * @param abnormalIndicator 异常标志（H=高于正常值，L=低于正常值，N=正常）
     * @return 排序权重值
     */
    private int getAbnormalOrder(String abnormalIndicator) {
        if (abnormalIndicator == null) {
            return 3;
        }
        String flag = abnormalIndicator.trim();
        if ("H".equalsIgnoreCase(flag)) {
            return 0;
        }
        if ("L".equalsIgnoreCase(flag)) {
            return 1;
        }
        if ("N".equalsIgnoreCase(flag)) {
            return 2;
        }
        return 3;
    }
}
