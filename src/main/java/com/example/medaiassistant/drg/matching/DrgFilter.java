package com.example.medaiassistant.drg.matching;

import com.example.medaiassistant.dto.drg.DrgParsedRecord;
import com.example.medaiassistant.dto.drg.PatientData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DRG分流过滤器
 * 
 * 根据患者是否有手术来过滤DRG记录，实现分流规则
 * 
 * @author MedAI Assistant Team
 * @since 2025-10-22
 */
public class DrgFilter {

    private DrgFilter() {
        // 工具类，防止实例化
    }

    /**
     * 根据患者是否有手术来过滤DRG记录
     * 
     * 分流规则：
     * - 如果患者有手术，只保留有手术的DRG记录
     * - 如果患者无手术，只保留无手术的DRG记录
     * 
     * @param patientData 患者数据
     * @param allDrgs 所有DRG记录
     * @return 过滤后的DRG记录列表
     */
    public static List<DrgParsedRecord> filterByProcedurePresence(PatientData patientData, List<DrgParsedRecord> allDrgs) {
        if (allDrgs == null || allDrgs.isEmpty()) {
            return List.of();
        }

        boolean patientHasProcedures = patientData.hasProcedures();

        return allDrgs.stream()
                .filter(drg -> {
                    boolean drgHasProcedures = drg.hasProcedures();
                    // 分流规则：患者有手术时匹配有手术的DRG，患者无手术时匹配无手术的DRG
                    return patientHasProcedures == drgHasProcedures;
                })
                .collect(Collectors.toList());
    }
}
