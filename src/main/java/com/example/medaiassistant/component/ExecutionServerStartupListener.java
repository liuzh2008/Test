package com.example.medaiassistant.component;

import com.example.medaiassistant.model.ServerConfiguration;
import com.example.medaiassistant.service.ServerConfigService;
import com.example.medaiassistant.repository.ServerConfigurationRepository;
import com.example.medaiassistant.util.NetworkUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 执行服务器启动监听器
 * 在服务器启动时读取执行服务器的IP配置
 * 
 * 该组件负责在Spring Boot应用程序启动完成后，从数据库中读取执行服务器的配置信息，
 * 解析其中的IP地址，并将其存储在ServerConfigService中供应用程序其他部分使用。
 */
@Component
public class ExecutionServerStartupListener {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ExecutionServerStartupListener.class);

    /** JSON对象映射器，用于解析配置数据 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 服务器配置服务，用于存储解析后的IP地址 */
    @Autowired
    private ServerConfigService serverConfigService;

    /** 服务器配置数据访问对象，用于从数据库查询配置信息 */
    @Autowired
    private ServerConfigurationRepository serverConfigurationRepository;

    /**
     * 在应用程序准备就绪时执行
     * 从SERVER_CONFIGURATIONS表中读取执行服务器的IP配置
     * 
     * 该方法在Spring Boot应用程序完全启动后执行，会查询数据库中的执行服务器配置，
     * 解析其中的IP地址，并将其存储在ServerConfigService中。
     * 
     * @see ApplicationReadyEvent Spring Boot应用程序准备就绪事件
     * @see ServerConfigurationRepository#findByConfigName(String) 查询配置信息的方法
     * @see ServerConfigService#setDecryptionServerIp(String) 存储IP地址的方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("执行服务器启动完成，开始读取执行服务器IP配置...");

        try {
            // 获取本机IP地址
            String localIpAddress = NetworkUtil.getLocalIpAddress();
            String hostName = NetworkUtil.getLocalHostName();

            // 从SERVER_CONFIGURATIONS表中直接读取执行服务器配置
            // 根据实际数据库记录，配置名称为"execution_server"
            ServerConfiguration executionServerConfig = serverConfigurationRepository
                    .findByConfigName("execution_server");

            if (executionServerConfig != null) {
                String configData = executionServerConfig.getConfigData();
                logger.info("成功读取执行服务器配置: {}", configData);

                // 解析JSON配置数据并提取IP地址
                String executionServerIp = parseIpAddressFromConfig(configData);

                if (executionServerIp != null) {
                    logger.info("成功解析执行服务器IP: {}", executionServerIp);

                    // 存储执行服务器IP到ServerConfigService中，供应用程序其他部分使用
                    serverConfigService.setDecryptionServerIp(executionServerIp);

                    // 输出服务器IP地址和运行状态
                    logger.info("===============================================");
                    logger.info("执行服务器启动信息:");
                    logger.info("服务器IP地址: {}", executionServerIp);
                    logger.info("服务器主机名: {}", hostName != null ? hostName : "未知");
                    logger.info("本地IP地址: {}", localIpAddress != null ? localIpAddress : "未知");
                    logger.info("服务器运行状态: 正常运行");
                    logger.info("服务器端口: 8082");
                    logger.info("服务用途: 处理加密的Prompt请求");
                    logger.info("===============================================");
                } else {
                    logger.warn("无法从配置数据中解析IP地址: {}", configData);

                    // 输出服务器信息（即使IP解析失败）
                    logger.info("===============================================");
                    logger.info("执行服务器启动信息:");
                    logger.info("服务器IP地址: 无法从配置中解析");
                    logger.info("服务器主机名: {}", hostName != null ? hostName : "未知");
                    logger.info("本地IP地址: {}", localIpAddress != null ? localIpAddress : "未知");
                    logger.info("服务器运行状态: 正常运行");
                    logger.info("服务器端口: 8082");
                    logger.info("服务用途: 处理加密的Prompt请求");
                    logger.info("===============================================");
                }
            } else {
                logger.warn("未找到执行服务器配置，请检查SERVER_CONFIGURATIONS表中是否存在配置名为'execution_server'的记录");

                // 输出服务器信息（即使没有配置）
                logger.info("===============================================");
                logger.info("执行服务器启动信息:");
                logger.info("服务器IP地址: 未配置");
                logger.info("服务器主机名: {}", hostName != null ? hostName : "未知");
                logger.info("本地IP地址: {}", localIpAddress != null ? localIpAddress : "未知");
                logger.info("服务器运行状态: 正常运行");
                logger.info("服务器端口: 8082");
                logger.info("服务用途: 处理加密的Prompt请求");
                logger.info("===============================================");
            }
        } catch (Exception e) {
            logger.error("读取执行服务器IP配置时发生错误", e);
        }
    }

    /**
     * 从JSON配置数据中解析IP地址
     * 
     * 该方法接收JSON格式的配置数据，解析其中的"ip"字段并返回IP地址字符串。
     * 如果解析失败或未找到"ip"字段，则返回null。
     * 
     * @param configData JSON格式的配置数据，例如: {"ip": "10.0.19.251"}
     * @return 解析出的IP地址字符串，如果解析失败则返回null
     * @see ObjectMapper#readTree(String) JSON解析方法
     * @see JsonNode#get(String) 获取JSON节点的方法
     * @see JsonNode#asText() 获取节点文本值的方法
     */
    private String parseIpAddressFromConfig(String configData) {
        try {
            JsonNode jsonNode = objectMapper.readTree(configData);
            JsonNode ipNode = jsonNode.get("ip");
            return ipNode != null ? ipNode.asText() : null;
        } catch (Exception e) {
            logger.error("解析IP地址时发生错误: {}", e.getMessage());
            return null;
        }
    }
}
