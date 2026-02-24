package com.example.medaiassistant.converter;

import com.example.medaiassistant.model.AlertTask;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 字符串到AlertTask状态枚举转换器
 * 
 * 该转换器用于将HTTP请求参数中的中文字符串转换为AlertTask.TaskStatus枚举值。
 * 支持"待处理"、"进行中"、"已完成"三种状态的中文转换。
 * 
 * @author Cline
 * @since 2025-10-12
 */
@Component
public class StringToAlertTaskStatusConverter implements Converter<String, AlertTask.TaskStatus> {
    
    /**
     * 将字符串转换为AlertTask.TaskStatus枚举
     * 
     * @param source 输入的字符串，支持"待处理"、"进行中"、"已完成"
     * @return 对应的AlertTask.TaskStatus枚举值
     * @throws IllegalArgumentException 当输入字符串无法转换为有效枚举值时抛出
     */
    @Override
    public AlertTask.TaskStatus convert(@org.springframework.lang.NonNull String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("任务状态不能为空");
        }
        
        String trimmedSource = source.trim();
        
        switch (trimmedSource) {
            case "待处理":
                return AlertTask.TaskStatus.待处理;
            case "进行中":
                return AlertTask.TaskStatus.进行中;
            case "已完成":
                return AlertTask.TaskStatus.已完成;
            default:
                throw new IllegalArgumentException("无效的任务状态: " + source + 
                    "。有效值为: 待处理, 进行中, 已完成");
        }
    }
}
