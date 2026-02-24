package com.example.medaiassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dicinput")
public class DicInput {

    @Id
    @Column(name = "DicID")
    private Long dicID;

    @Column(name = "Content1")
    private String content1;

    @Column(name = "Content2")
    private String content2;

    @Column(name = "Content3")
    private String content3;

    @Column(name = "Sort")
    private Integer sort;

    // 构造函数
    public DicInput() {
    }

    // Getter和Setter方法
    public Long getDicID() {
        return dicID;
    }

    public void setDicID(Long dicID) {
        this.dicID = dicID;
    }

    public String getContent1() {
        return content1;
    }

    public void setContent1(String content1) {
        this.content1 = content1;
    }

    public String getContent2() {
        return content2;
    }

    public void setContent2(String content2) {
        this.content2 = content2;
    }

    public String getContent3() {
        return content3;
    }

    public void setContent3(String content3) {
        this.content3 = content3;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
