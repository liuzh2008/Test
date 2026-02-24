package com.example.medaiassistant.dto;

import java.util.List;

/**
 * 数据清洗请求模型
 *
 * 用于 /api/ai/clean-data 接口，承载待清洗的文本及关联的患者ID。
 */
public class CleanDataRequest {

    /** 患者ID，用于查询敏感信息 */
    private String patientId;

    /** 待清洗的文本列表 */
    private List<String> texts;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }
}
