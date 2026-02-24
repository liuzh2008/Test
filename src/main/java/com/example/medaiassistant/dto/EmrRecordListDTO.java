package com.example.medaiassistant.dto;

import java.util.Date;

/**
 * EMR病历记录列表响应DTO
 * 用于接口1：获取病人病历记录列表的响应数据
 * 
 * @since 1.0.0
 */
public class EmrRecordListDTO {
    
    /**
     * 记录唯一标识
     */
    private String ID;
    
    /**
     * 文档类型名称
     */
    private String DOC_TYPE_NAME;
    
    /**
     * 文档标题时间
     */
    private Date DOC_TITLE_TIME;
    
    // 默认构造函数
    public EmrRecordListDTO() {
    }
    
    // 带参构造函数
    public EmrRecordListDTO(String ID, String DOC_TYPE_NAME, Date DOC_TITLE_TIME) {
        this.ID = ID;
        this.DOC_TYPE_NAME = DOC_TYPE_NAME;
        this.DOC_TITLE_TIME = DOC_TITLE_TIME;
    }
    
    // Getters and Setters
    
    public String getID() {
        return ID;
    }
    
    public void setID(String ID) {
        this.ID = ID;
    }
    
    public String getDOC_TYPE_NAME() {
        return DOC_TYPE_NAME;
    }
    
    public void setDOC_TYPE_NAME(String DOC_TYPE_NAME) {
        this.DOC_TYPE_NAME = DOC_TYPE_NAME;
    }
    
    public Date getDOC_TITLE_TIME() {
        return DOC_TITLE_TIME;
    }
    
    public void setDOC_TITLE_TIME(Date DOC_TITLE_TIME) {
        this.DOC_TITLE_TIME = DOC_TITLE_TIME;
    }
    
    @Override
    public String toString() {
        return "EmrRecordListDTO{" +
                "ID='" + ID + '\'' +
                ", DOC_TYPE_NAME='" + DOC_TYPE_NAME + '\'' +
                ", DOC_TITLE_TIME=" + DOC_TITLE_TIME +
                '}';
    }
}
