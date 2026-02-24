package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 无过滤策略
 * 返回所有在院患者，不进行任何过滤
 */
@Component
public class NoFilterStrategy implements PatientFilterStrategy {
    @Override
    public String getStrategyName() { 
        return "无过滤"; 
    }
    
    @Override
    public boolean isApplicable(SchedulingProperties.TimerConfig.FilterMode filterMode) {
        return filterMode == SchedulingProperties.TimerConfig.FilterMode.NONE;
    }
    
    @Override
    public List<Patient> filterPatients(PatientRepository repository, 
                                      SchedulingProperties.TimerConfig config,
                                      Pageable pageable) {
        return repository.findByIsInHospital(true, pageable).getContent();
    }
}
