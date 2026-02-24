package com.example.medaiassistant.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_departments")
@IdClass(UserDepartmentId.class)
public class UserDepartment {
    @Id
    @Column(name = "user_Id")
    private String userId;

    @Id
    @Column(name = "department_Id")
    private int departmentId;

    @ManyToOne
    @JoinColumn(name = "departmentId", insertable = false, updatable = false)
    private Department department;

    private int isPrimary;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public int getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(int isPrimary) {
        this.isPrimary = isPrimary;
    }
}
