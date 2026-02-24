package com.example.medaiassistant.model;

import java.io.Serializable;
import java.util.Objects;

public class UserDepartmentId implements Serializable {
    private String userId;
    private int departmentId;

    // Default constructor
    public UserDepartmentId() {
    }

    // Constructor
    public UserDepartmentId(String userId, int departmentId) {
        this.userId = userId;
        this.departmentId = departmentId;
    }

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

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDepartmentId that = (UserDepartmentId) o;
        return departmentId == that.departmentId &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, departmentId);
    }
}
