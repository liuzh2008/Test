package com.example.medaiassistant.service;

import com.example.medaiassistant.config.AppDataSourceProperties;
import com.example.medaiassistant.config.ExecutionServerProperties;
import com.example.medaiassistant.config.OracleDatabaseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DatabaseConfigService {

    @Autowired
    private AppDataSourceProperties appDataSourceProperties;

    @Autowired
    private OracleDatabaseProperties oracleDatabaseProperties;

    @Autowired
    private ExecutionServerProperties executionServerProperties;

    /**
     * 获取当前数据库类型
     */
    public String getCurrentDatabaseType() {
        return appDataSourceProperties.getType();
    }

    /**
     * 切换数据库类型
     * 
     * @param dbType 数据库类型 (oracle)
     * @return 切换结果信息
     */
    public String switchDatabase(String dbType) {
        if ("oracle".equalsIgnoreCase(dbType)) {
            return String.format("数据库类型切换请求已接收: %s。注意：真正的数据库切换需要修改配置文件并重启应用。\n\n" +
                    "Oracle数据库连接注意事项：\n" +
                    "- 确保Oracle服务器可达: " + executionServerProperties.getIp() + ":1521\n" +
                    "- 确保网络连接正常\n" +
                    "- 如果连接失败，应用可能无法启动\n\n" +
                    "请执行以下操作之一：\n" +
                    "1. 修改 application.properties: app.datasource.type=%s\n" +
                    "2. 使用专用配置启动: java -jar app.jar --spring.config.location=classpath:application-oracle.properties\n"
                    +
                    "3. 使用命令行启动: java -jar app.jar --app.datasource.type=%s",
                    dbType, dbType, dbType);
        } else {
            throw new IllegalArgumentException("不支持的数据库类型: " + dbType + "。支持的类型: oracle");
        }
    }

    /**
     * 获取数据库配置信息
     */
    public Map<String, String> getDatabaseConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("currentType", getCurrentDatabaseType());
        config.put("oracleUrl", oracleDatabaseProperties.getUrl());
        config.put("oracleUsername", oracleDatabaseProperties.getUsername());
        config.put("oracleDriver", oracleDatabaseProperties.getDriverClassName());
        return config;
    }
}
