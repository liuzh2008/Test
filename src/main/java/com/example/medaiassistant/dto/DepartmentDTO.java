package com.example.medaiassistant.dto;

/**
 * 用户科室DTO - 用于返回用户科室信息
 */
public class DepartmentDTO {
    private String departmentName;
    private int isPrimary;
    private int departmentId;

    public DepartmentDTO(String departmentName, int isPrimary, int departmentId) {
        this.departmentName = departmentName;
        this.isPrimary = isPrimary;
        this.departmentId = departmentId;
    }

    // Getters and Setters
    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(int isPrimary) {
        this.isPrimary = isPrimary;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }
}
