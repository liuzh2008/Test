package com.example.medaiassistant.dto;

/**
 * EMR病历记录内容响应DTO
 * 用于接口2：获取病历记录内容的响应数据
 * 
 * @since 1.0.0
 */
public class EmrRecordContentDTO {
    
    /**
     * 病历记录内容
     */
    private String CONTENT;
    
    // 默认构造函数
    public EmrRecordContentDTO() {
    }
    
    // 带参构造函数
    public EmrRecordContentDTO(String CONTENT) {
        this.CONTENT = CONTENT;
    }
    
    // Getters and Setters
    
    public String getCONTENT() {
        return CONTENT;
    }
    
    public void setCONTENT(String CONTENT) {
        this.CONTENT = CONTENT;
    }
    
    @Override
    public String toString() {
        return "EmrRecordContentDTO{" +
                "CONTENT='" + (CONTENT != null ? CONTENT.substring(0, Math.min(CONTENT.length(), 50)) + "..." : "null") + '\'' +
                '}';
    }
}
