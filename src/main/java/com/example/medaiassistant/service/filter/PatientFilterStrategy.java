package com.example.medaiassistant.service.filter;

import com.example.medaiassistant.config.SchedulingProperties;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.PatientRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 患者过滤策略接口
 * 定义了不同过滤策略的共同方法
 */
public interface PatientFilterStrategy {
    /**
     * 获取过滤策略名称
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 是否适用于当前配置
     * @param filterMode 过滤模式
     * @return 是否适用
     */
    boolean isApplicable(SchedulingProperties.TimerConfig.FilterMode filterMode);
    
    /**
     * 执行患者查询
     * @param repository 患者数据访问接口
     * @param config 定时任务配置
     * @param pageable 分页参数
     * @return 过滤后的患者列表
     */
    List<Patient> filterPatients(PatientRepository repository, 
                                SchedulingProperties.TimerConfig config,
                                Pageable pageable);
}
