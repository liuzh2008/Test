package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.ServerConfigDTO;
import com.example.medaiassistant.model.ServerConfiguration;
import com.example.medaiassistant.repository.ServerConfigurationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 服务器配置服务类
 * 
 * 该服务类负责处理服务器配置的业务逻辑，包括查询、保存和更新配置信息，
 * 以及存储和获取执行服务器的IP地址。
 */
@Service
public class ServerConfigService {

    /** 服务器配置数据访问对象 */
    @Autowired
    private ServerConfigurationRepository serverConfigurationRepository;

    /** 存储执行服务器IP的静态变量 */
    private static String decryptionServerIp;

    /**
     * 根据配置名称获取服务器配置
     * 
     * 该方法通过配置名称查询服务器配置信息，如果找到则返回配置对象，否则返回null。
     * 
     * @param configName 配置名称
     * @return 服务器配置对象，如果未找到则返回null
     * @see ServerConfigurationRepository#findByConfigName(String)
     */
    public ServerConfiguration getConfigByName(String configName) {
        return serverConfigurationRepository.findByConfigName(configName);
    }

    /**
     * 保存或更新服务器配置
     * 
     * 该方法根据配置名称检查是否存在现有配置，如果存在则更新，否则创建新配置。
     * 
     * @param configDTO 服务器配置数据传输对象
     * @return 保存后的服务器配置对象
     * @see ServerConfigDTO 配置数据传输对象
     * @see ServerConfiguration 服务器配置实体类
     */
    public ServerConfiguration saveOrUpdateConfig(ServerConfigDTO configDTO) {
        ServerConfiguration existingConfig = serverConfigurationRepository.findByConfigName(configDTO.getConfigName());

        if (existingConfig == null) {
            // 新建配置
            ServerConfiguration newConfig = new ServerConfiguration();
            newConfig.setConfigName(configDTO.getConfigName());
            newConfig.setConfigData(configDTO.getConfigData());
            newConfig.setEnvironment(configDTO.getEnvironment());
            newConfig.setCreatedAt(LocalDateTime.now());
            newConfig.setUpdatedAt(LocalDateTime.now());
            newConfig.setCreatedBy(configDTO.getCreatedBy());
            return serverConfigurationRepository.save(newConfig);
        } else {
            // 更新现有配置
            existingConfig.setConfigData(configDTO.getConfigData());
            existingConfig.setEnvironment(configDTO.getEnvironment());
            existingConfig.setUpdatedAt(LocalDateTime.now());
            return serverConfigurationRepository.save(existingConfig);
        }
    }

    /**
     * 获取执行服务器IP
     * 
     * 该方法返回存储在静态变量中的执行服务器IP地址。
     * 
     * @return 执行服务器IP地址，如果未设置则返回null
     */
    public String getDecryptionServerIp() {
        return decryptionServerIp;
    }

    /**
     * 设置执行服务器IP
     * 
     * 该方法将执行服务器IP地址存储在静态变量中，供应用程序其他部分使用。
     * 
     * @param ip 执行服务器IP地址
     */
    public void setDecryptionServerIp(String ip) {
        decryptionServerIp = ip;
    }

    /**
     * 获取AES加密密钥
     * 
     * @return AES加密密钥，如果未配置则返回null
     */
    public String getAesEncryptionKey() {
        ServerConfiguration config = getConfigByName("AES_ENCRYPTION_KEY");
        return config != null ? config.getConfigData() : null;
    }

    /**
     * 获取AES加密盐值
     * 
     * @return AES加密盐值，如果未配置则返回null
     */
    public String getAesEncryptionSalt() {
        ServerConfiguration config = getConfigByName("AES_ENCRYPTION_SALT");
        return config != null ? config.getConfigData() : null;
    }

    /**
     * 保存AES加密配置
     * 
     * @param encryptionKey 加密密钥
     * @param encryptionSalt 加密盐值
     * @param environment 环境
     * @param createdBy 创建者
     */
    public void saveAesEncryptionConfig(String encryptionKey, String encryptionSalt, 
                                       String environment, String createdBy) {
        // 保存加密密钥
        ServerConfigDTO keyConfig = new ServerConfigDTO();
        keyConfig.setConfigName("AES_ENCRYPTION_KEY");
        keyConfig.setConfigData(encryptionKey);
        keyConfig.setEnvironment(environment);
        keyConfig.setCreatedBy(createdBy);
        saveOrUpdateConfig(keyConfig);

        // 保存加密盐值
        ServerConfigDTO saltConfig = new ServerConfigDTO();
        saltConfig.setConfigName("AES_ENCRYPTION_SALT");
        saltConfig.setConfigData(encryptionSalt);
        saltConfig.setEnvironment(environment);
        saltConfig.setCreatedBy(createdBy);
        saveOrUpdateConfig(saltConfig);
    }
}
