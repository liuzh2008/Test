package com.example.medaiassistant.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestOracleConnection {
    public static void main(String[] args) {
        // 数据库连接信息（从环境变量获取）
        String url = System.getenv("ORACLE_DB_URL") != null ? 
            System.getenv("ORACLE_DB_URL") : "jdbc:oracle:thin:@100.66.1.2:1521/FREE";
        String username = System.getenv("ORACLE_DB_USERNAME") != null ? 
            System.getenv("ORACLE_DB_USERNAME") : "system";
        String password = System.getenv("ORACLE_DB_PASSWORD") != null ? 
            System.getenv("ORACLE_DB_PASSWORD") : "Liuzh_123";
        
        System.out.println("尝试连接到Oracle数据库...");
        System.out.println("URL: " + url);
        System.out.println("用户名: " + username);
        
        try {
            // 加载Oracle JDBC驱动
            Class.forName("oracle.jdbc.OracleDriver");
            
            // 建立连接
            Connection connection = DriverManager.getConnection(url, username, password);
            
            // 检查连接是否有效
            if (connection.isValid(5)) {
                System.out.println("成功连接到Oracle数据库！");
                System.out.println("数据库产品名称: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("数据库版本: " + connection.getMetaData().getDatabaseProductVersion());
                connection.close();
            } else {
                System.out.println("连接无效。");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("找不到Oracle JDBC驱动: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("连接数据库时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
