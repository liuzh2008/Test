package com.example.medaiassistant.model.executionserver;

import com.example.medaiassistant.model.DataStatus;
import jakarta.persistence.*;
import java.sql.Clob;
import java.sql.Timestamp;

/**
 * 执行服务器专用 - 加密数据临时表实体类
 * 对应执行服务器数据库表: ENCRYPTED_DATA_TEMP
 * 
 * 注意：此实体类专门用于执行服务器数据源，与主数据源的实体完全隔离
 * 
 * @author System
 * @version 1.0
 * @since 2025-12-22
 */
@Entity(name = "ExecutionEncryptedDataTemp")
@Table(name = "ENCRYPTED_DATA_TEMP")
public class EncryptedDataTemp {

    /**
     * 主键ID
     * VARCHAR2(50 BYTE) 类型，使用cdwyy加上随机数生成
     * 简化设计：与REQUEST_ID保持一致，减少复杂性
     */
    @Id
    @Column(name = "ID", length = 50)
    private String id;

    /**
     * 加密数据
     */
    @Lob
    @Column(name = "ENCRYPTED_DATA", nullable = false)
    private Clob encryptedData;

    /**
     * 解密数据
     */
    @Lob
    @Column(name = "DECRYPTED_DATA")
    private Clob decryptedData;

    /**
     * 执行结果
     */
    @Lob
    @Column(name = "EXECUTION_RESULT")
    private Clob executionResult;

    /**
     * 请求ID
     * 简化设计：与ID字段保持一致，只作为业务查询索引
     */
    @Column(name = "REQUEST_ID", length = 50, unique = true)
    private String requestId;

    /**
     * 数据来源
     */
    @Column(name = "SOURCE", length = 50)
    private String source;

    /**
     * 状态
     * 默认值: RECEIVED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private DataStatus status = DataStatus.RECEIVED;

    /**
     * 错误信息
     */
    @Lob
    @Column(name = "ERROR_MESSAGE")
    private Clob errorMessage;

    /**
     * 接收时间
     * 默认值: CURRENT_TIMESTAMP
     */
    @Column(name = "RECEIVED_TIME")
    private Timestamp receivedTime;

    /**
     * 处理时间
     */
    @Column(name = "PROCESSED_TIME")
    private Timestamp processedTime;

    /**
     * 创建时间
     * 默认值: CURRENT_TIMESTAMP
     */
    @Column(name = "CREATED_AT")
    private Timestamp createdAt;

    /**
     * 更新时间
     * 默认值: CURRENT_TIMESTAMP
     */
    @Column(name = "UPDATED_AT")
    private Timestamp updatedAt;

    /**
     * 乐观锁版本字段
     * 用于处理并发更新冲突
     */
    @Version
    @Column(name = "version")
    private Integer version;

    // 构造函数
    public EncryptedDataTemp() {
        // 默认构造函数
    }

    // 带参数的构造函数
    public EncryptedDataTemp(Clob encryptedData, Clob decryptedData, String requestId, String source) {
        this.encryptedData = encryptedData;
        this.decryptedData = decryptedData;
        this.requestId = requestId;
        this.source = source;
        this.receivedTime = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Clob getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(Clob encryptedData) {
        this.encryptedData = encryptedData;
    }

    public Clob getDecryptedData() {
        return decryptedData;
    }

    public void setDecryptedData(Clob decryptedData) {
        this.decryptedData = decryptedData;
    }

    public Clob getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(Clob executionResult) {
        this.executionResult = executionResult;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public DataStatus getStatus() {
        return status;
    }

    public void setStatus(DataStatus status) {
        this.status = status;
    }

    public Clob getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(Clob errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Timestamp receivedTime) {
        this.receivedTime = receivedTime;
    }

    public Timestamp getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(Timestamp processedTime) {
        this.processedTime = processedTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
        if (receivedTime == null) {
            receivedTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
