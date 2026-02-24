package com.example.medaiassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类 - 映射数据库users表
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;

    /**
     * 用户名 - 存储为TEXT类型
     */
    @Column(columnDefinition = "TEXT")
    private String username;

    /**
     * 密码哈希值 - 存储为TEXT类型
     */
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    /**
     * 账户激活状态 (1=激活, 0=禁用)
     */
    @Column(name = "is_active")
    private Integer isActive;

    /**
     * 账户创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 明文密码 (临时字段，不持久化)
     */
    private String password;

    /**
     * 用户真实姓名
     */
    private String name;

    // Getters and Setters

    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置用户ID
     * 
     * @param id 用户ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取用户名
     * 
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     * 
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码哈希值
     * 
     * @return 密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希值
     * 
     * @param passwordHash 密码哈希
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取账户激活状态
     * 
     * @return 1=激活, 0=禁用
     */
    public Integer getIsActive() {
        return isActive;
    }

    /**
     * 设置账户激活状态
     * 
     * @param isActive 1=激活, 0=禁用
     */
    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    /**
     * 获取账户创建时间
     * 
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置账户创建时间
     * 
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取明文密码 (临时)
     * 
     * @return 明文密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置明文密码 (临时)
     * 
     * @param password 明文密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取用户真实姓名
     * 
     * @return 姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置用户真实姓名
     * 
     * @param name 姓名
     */
    public void setName(String name) {
        this.name = name;
    }
}
