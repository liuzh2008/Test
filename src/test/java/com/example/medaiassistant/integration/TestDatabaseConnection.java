package com.example.medaiassistant.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestDatabaseConnection {
    // 数据库连接信息（从环境变量获取）
    private static final String DB_URL = System.getenv("ORACLE_DB_URL") != null ? 
        System.getenv("ORACLE_DB_URL") : "jdbc:oracle:thin:@100.66.1.2:1521/FREE";
    private static final String USERNAME = System.getenv("ORACLE_DB_USERNAME") != null ? 
        System.getenv("ORACLE_DB_USERNAME") : "system";
    private static final String PASSWORD = System.getenv("ORACLE_DB_PASSWORD") != null ? 
        System.getenv("ORACLE_DB_PASSWORD") : "Liuzh_123";

    public static void main(String[] args) {
        System.out.println("测试数据库连接和数据保存功能...");
        
        try {
            // 加载Oracle JDBC驱动
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("✅ Oracle JDBC驱动加载成功");
            
            // 建立数据库连接
            Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            System.out.println("✅ 数据库连接成功");
            
            // 插入测试数据，使用cdwyy加上随机数生成ID
            String insertSQL = "INSERT INTO ENCRYPTED_DATA_TEMP (ID, ENCRYPTED_DATA, DECRYPTED_DATA, SOURCE, STATUS) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertSQL);
            long randomId = System.currentTimeMillis() % 1000000; // 生成随机ID
            statement.setString(1, "cdwyy" + randomId);
            statement.setString(2, "测试加密数据");
            statement.setString(3, "测试解密数据");
            statement.setString(4, "TEST_SCRIPT");
            statement.setString(5, "RECEIVED");
            
            int rowsAffected = statement.executeUpdate();
            System.out.println("✅ 数据插入成功，影响行数: " + rowsAffected);
            
            // 关闭连接
            statement.close();
            connection.close();
            System.out.println("✅ 数据库连接已关闭");
            
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Oracle JDBC驱动加载失败: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("❌ 数据库操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
