package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 仅科室过滤策略
 * 返回指定科室的所有在院患者
 */
@Component
public class DepartmentOnlyFilterStrategy implements PatientFilterStrategy {
    @Override
    public String getStrategyName() { 
        return "仅科室过滤"; 
    }
    
    @Override
    public boolean isApplicable(SchedulingProperties.TimerConfig.FilterMode filterMode) {
        return filterMode == SchedulingProperties.TimerConfig.FilterMode.DEPARTMENT_ONLY;
    }
    
    @Override
    public List<Patient> filterPatients(PatientRepository repository, 
                                      SchedulingProperties.TimerConfig config,
                                      Pageable pageable) {
        return repository.findByDepartmentsAndIsInHospitalSafe(
            config.getTargetDepartments(), true, pageable).getContent();
    }
}
