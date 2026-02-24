package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.ConfigValidationResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 医院配置文件加载服务
 * 负责加载和管理医院配置文件
 */
@Service
public class HospitalConfigService {
    
    private static final Logger log = LoggerFactory.getLogger(HospitalConfigService.class);
    
    /**
     * 医院配置文件目录
     */
    @Value("${hospital.config.dir:config/hospitals}")
    private String configDirPath;
    
    /**
     * 医院配置缓存
     * key: 医院ID, value: 医院配置
     */
    private final Map<String, HospitalConfig> configCache = new ConcurrentHashMap<>();
    
    /**
     * 文件监听服务
     */
    private WatchService watchService;
    
    /**
     * 初始化方法：加载所有配置文件并启动文件监听
     */
    @PostConstruct
    public void init() {
        try {
            loadAllConfigs();
            startFileWatcher();
            log.info("医院配置服务初始化完成，已加载 {} 个医院配置", configCache.size());
        } catch (Exception e) {
            log.error("医院配置服务初始化失败", e);
        }
    }
    
    /**
     * 加载所有配置文件
     */
    private void loadAllConfigs() {
        File configDir = new File(configDirPath);
        log.info("医院配置目录路径: {}, 绝对路径: {}, 存在: {}, 是目录: {}", 
                 configDirPath, configDir.getAbsolutePath(), configDir.exists(), configDir.isDirectory());
        
        if (!configDir.exists() || !configDir.isDirectory()) {
            log.warn("医院配置文件目录不存在: {}", configDirPath);
            return;
        }
        
        File[] configFiles = configDir.listFiles((dir, name) -> 
            name.endsWith(".yaml") || name.endsWith(".yml"));
        
        log.info("在目录 {} 中找到 {} 个YAML配置文件", 
                 configDir.getAbsolutePath(), configFiles != null ? configFiles.length : 0);
        
        if (configFiles == null || configFiles.length == 0) {
            log.warn("医院配置文件目录中没有找到YAML文件: {}", configDirPath);
            return;
        }
        
        for (File configFile : configFiles) {
            log.info("正在加载配置文件: {}", configFile.getAbsolutePath());
            try {
                loadConfig(configFile);
            } catch (Exception e) {
                log.error("加载医院配置文件失败: {}", configFile.getAbsolutePath(), e);
            }
        }
    }
    
    /**
     * 加载单个配置文件
     */
    private void loadConfig(File configFile) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        // 使用 BufferedInputStream 包装 FileInputStream 以支持 mark/reset 操作
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(configFile))) {
            log.info("开始解析配置文件: {}", configFile.getAbsolutePath());
            
            // 首先尝试直接加载为Map，查看原始结构
            inputStream.mark(Integer.MAX_VALUE);
            Map<String, Object> rawMap = yaml.load(inputStream);
            log.info("YAML原始解析结果 (Map): {}", rawMap);
            
            // 重置流并尝试加载为HospitalConfig
            inputStream.reset();
            HospitalConfig config = yaml.loadAs(inputStream, HospitalConfig.class);
            log.info("YAML解析为HospitalConfig对象: {}", config);
            
            if (config != null) {
                log.info("配置对象详情: hospital={}, id={}, name={}, integrationType={}", 
                         config.getHospital(), config.getId(), config.getName(), config.getIntegrationType());
                
                if (config.getId() != null) {
                    configCache.put(config.getId(), config);
                    log.info("加载医院配置: {} - {}", config.getId(), config.getName());
                    
                    // 验证配置
                    ConfigValidationResult validationResult = validateConfigDetailed(config);
                    if (!validationResult.isValid()) {
                        log.warn("医院配置验证失败: {} - 错误: {}", config.getId(), validationResult.getErrors());
                    } else {
                        log.debug("医院配置验证通过: {}", config.getId());
                    }
                } else {
                    log.warn("医院配置文件缺少ID: {}", configFile.getName());
                    log.warn("配置对象结构: hospital={}", config.getHospital());
                }
            } else {
                log.warn("YAML解析返回null对象: {}", configFile.getName());
                log.warn("原始YAML内容可能无法映射到HospitalConfig类");
            }
        } catch (Exception e) {
            log.error("解析医院配置文件失败: {}", configFile.getAbsolutePath(), e);
            log.error("失败原因: {}", e.getMessage());
            throw new RuntimeException("解析医院配置文件失败: " + configFile.getName(), e);
        }
    }
    
    /**
     * 启动文件监听服务
     */
    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path configDir = Paths.get(configDirPath);
            
            if (!Files.exists(configDir)) {
                log.warn("医院配置文件目录不存在，无法启动文件监听: {}", configDirPath);
                return;
            }
            
            configDir.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            
            Thread watcherThread = new Thread(this::watchConfigChanges);
            watcherThread.setDaemon(true);
            watcherThread.setName("hospital-config-watcher");
            watcherThread.start();
            
            log.info("医院配置文件监听服务已启动");
        } catch (Exception e) {
            log.error("启动医院配置文件监听服务失败", e);
        }
    }
    
    /**
     * 监听配置文件变化
     */
    private void watchConfigChanges() {
        try {
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    
                    if (fileName.toString().endsWith(".yaml") || 
                        fileName.toString().endsWith(".yml")) {
                        
                        log.info("检测到医院配置文件变化: {} - {}", kind.name(), fileName);
                        
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            // 文件删除：从缓存中移除对应的配置
                            removeConfigByFileName(fileName.toString());
                        } else {
                            // 文件创建或修改：重新加载配置
                            reloadConfig(fileName.toString());
                        }
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("医院配置文件监听服务被中断");
        } catch (Exception e) {
            log.error("医院配置文件监听服务异常", e);
        }
    }
    
    /**
     * 根据文件名移除配置
     */
    private void removeConfigByFileName(String fileName) {
        // 简单实现：重新扫描所有文件
        // 实际可以根据文件名映射到医院ID
        configCache.clear();
        loadAllConfigs();
        log.info("配置文件删除后重新加载，当前配置数量: {}", configCache.size());
    }
    
    /**
     * 重新加载配置文件
     */
    private void reloadConfig(String fileName) {
        File configFile = new File(configDirPath, fileName);
        if (configFile.exists()) {
            try {
                loadConfig(configFile);
                log.info("重新加载医院配置文件: {}", fileName);
            } catch (Exception e) {
                log.error("重新加载医院配置文件失败: {}", fileName, e);
            }
        }
    }
    
    /**
     * 获取所有医院配置
     */
    public List<HospitalConfig> getAllConfigs() {
        return new ArrayList<>(configCache.values());
    }
    
    /**
     * 根据医院ID获取配置
     */
    public HospitalConfig getConfig(String hospitalId) {
        return configCache.get(hospitalId);
    }
    
    /**
     * 根据医院ID获取配置，如果不存在则返回默认值
     */
    public HospitalConfig getConfigOrDefault(String hospitalId, HospitalConfig defaultValue) {
        return configCache.getOrDefault(hospitalId, defaultValue);
    }
    
    /**
     * 检查医院配置是否存在
     */
    public boolean hasConfig(String hospitalId) {
        return configCache.containsKey(hospitalId);
    }
    
    /**
     * 获取启用的医院配置列表
     */
    public List<HospitalConfig> getEnabledConfigs() {
        List<HospitalConfig> enabledConfigs = new ArrayList<>();
        for (HospitalConfig config : configCache.values()) {
            if (config.isEnabled()) {
                enabledConfigs.add(config);
            }
        }
        return enabledConfigs;
    }
    
    /**
     * 获取启用且同步启用的医院配置列表
     */
    public List<HospitalConfig> getEnabledAndSyncEnabledConfigs() {
        List<HospitalConfig> result = new ArrayList<>();
        for (HospitalConfig config : configCache.values()) {
            if (config.isEnabled() && config.isSyncEnabled()) {
                result.add(config);
            }
        }
        return result;
    }
    
    /**
     * 重新加载所有配置文件
     */
    public void reloadAllConfigs() {
        configCache.clear();
        loadAllConfigs();
        log.info("重新加载所有医院配置文件，当前配置数量: {}", configCache.size());
    }
    
    /**
     * 获取配置缓存大小
     */
    public int getConfigCount() {
        return configCache.size();
    }
    
    /**
     * 验证医院配置（简化版，返回布尔值）
     * 
     * @param config 医院配置
     * @return 验证结果，true表示有效，false表示无效
     */
    public boolean validateConfig(HospitalConfig config) {
        return validateConfigDetailed(config).isValid();
    }
    
    /**
     * 详细验证医院配置
     * 
     * @param config 医院配置
     * @return 详细的验证结果，包含错误信息
     */
    public ConfigValidationResult validateConfigDetailed(HospitalConfig config) {
        ConfigValidationResult result = new ConfigValidationResult();
        
        if (config == null) {
            result.addError("医院配置不能为null");
            return result;
        }
        
        if (config.getHospital() == null) {
            result.addError("医院配置缺少hospital节点");
            return result;
        }
        
        HospitalConfig.Hospital hospital = config.getHospital();
        
        // 验证必要字段
        if (hospital.getId() == null || hospital.getId().trim().isEmpty()) {
            result.addError("医院ID不能为空");
        }
        
        if (hospital.getName() == null || hospital.getName().trim().isEmpty()) {
            result.addError("医院名称不能为空");
        }
        
        if (hospital.getIntegrationType() == null || hospital.getIntegrationType().trim().isEmpty()) {
            result.addError("集成类型不能为空");
        } else {
            // 验证集成类型是否支持
            String integrationType = hospital.getIntegrationType();
            if (!"database".equals(integrationType) && !"api".equals(integrationType)) {
                result.addError("不支持的集成类型: " + integrationType + "，支持的集成类型: database, api");
            }
        }
        
        // 验证HIS配置（如果集成类型是database）
        if ("database".equals(hospital.getIntegrationType())) {
            if (hospital.getHis() == null) {
                result.addError("数据库集成类型需要配置HIS数据库连接信息");
            } else {
                HospitalConfig.HisConfig his = hospital.getHis();
                if (his.getUrl() == null || his.getUrl().trim().isEmpty()) {
                    result.addError("HIS数据库连接URL不能为空");
                }
                if (his.getUsername() == null || his.getUsername().trim().isEmpty()) {
                    result.addError("HIS数据库用户名不能为空");
                }
                if (his.getPassword() == null || his.getPassword().trim().isEmpty()) {
                    result.addError("HIS数据库密码不能为空");
                }
            }
        }
        
        // 验证API配置（如果集成类型是api）
        if ("api".equals(hospital.getIntegrationType())) {
            // 这里可以添加API配置的验证逻辑
            // 例如：验证API端点、认证信息等
        }
        
        // 验证同步配置
        if (hospital.getSync() != null) {
            HospitalConfig.SyncConfig sync = hospital.getSync();
            if (sync.getEnabled() != null && sync.getEnabled()) {
                if (sync.getCron() == null || sync.getCron().trim().isEmpty()) {
                    result.addError("启用同步时需要配置cron表达式");
                }
                if (sync.getMaxRetries() != null && sync.getMaxRetries() < 0) {
                    result.addError("最大重试次数不能为负数");
                }
                if (sync.getRetryInterval() != null && sync.getRetryInterval() < 0) {
                    result.addError("重试间隔不能为负数");
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取配置文件目录
     */
    public String getConfigDirPath() {
        return configDirPath;
    }
}
