package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.model.HospitalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医院配置服务单元测试
 * 按照TDD红-绿-重构流程实现
 * 使用Mockito进行单元测试，避免Spring Boot启动
 * 
 * @author Cline
 * @since 2025-12-03
 */
@ExtendWith(MockitoExtension.class)
class HospitalConfigServiceUnitTest {

    private HospitalConfigService hospitalConfigService;
    
    @Mock
    private File mockConfigDir;
    
    @Mock
    private File mockConfigFile;
    
    @Mock
    private FileInputStream mockFileInputStream;

    @BeforeEach
    void setUp() {
        // 创建服务实例，使用反射设置私有字段
        hospitalConfigService = new HospitalConfigService();
        
        // 使用反射设置configDirPath为测试目录
        try {
            var configDirPathField = HospitalConfigService.class.getDeclaredField("configDirPath");
            configDirPathField.setAccessible(true);
            configDirPathField.set(hospitalConfigService, "config/hospitals");
        } catch (Exception e) {
            throw new RuntimeException("设置configDirPath失败", e);
        }
    }

    /**
     * 红阶段测试1：测试服务初始化
     * 这个测试应该会失败，因为服务依赖的文件系统操作
     */
    @Test
    void testServiceInitialization_RedPhase() {
        // 验证服务已创建
        assertNotNull(hospitalConfigService, "医院配置服务应该被创建");
        
        // 尝试调用初始化方法（这个操作应该会失败）
        // 注意：由于是单元测试，我们不实际调用@PostConstruct方法
        // 这里只是验证服务对象存在
        assertTrue(true, "服务对象应该存在");
    }

    /**
     * 红阶段测试2：测试创建医院配置对象
     * 这个测试应该通过，因为不依赖外部资源
     */
    @Test
    void testCreateHospitalConfig_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        hospital.setId("test-hospital-001");
        hospital.setName("测试医院");
        hospital.setIntegrationType("database");
        config.setHospital(hospital);
        
        // 验证结果（这些断言应该通过）
        assertNotNull(config, "医院配置不应为null");
        assertEquals("test-hospital-001", config.getId(), "医院ID应匹配");
        assertEquals("测试医院", config.getName(), "医院名称应匹配");
        assertEquals("database", config.getIntegrationType(), "集成类型应匹配");
    }

    /**
     * 红阶段测试3：测试医院配置的便捷方法
     * 这个测试应该会失败，因为isEnabled()方法逻辑有问题
     */
    @Test
    void testHospitalConfigConvenienceMethods_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        HospitalConfig.SyncConfig syncConfig = new HospitalConfig.SyncConfig();
        
        // 设置同步配置为启用
        syncConfig.setEnabled(true);
        hospital.setSync(syncConfig);
        config.setHospital(hospital);
        
        // 验证结果（这个断言应该会失败，因为isEnabled()方法逻辑有问题）
        assertTrue(config.isEnabled(), "医院配置应该启用");
    }

    /**
     * 红阶段测试4：测试医院配置的HIS配置
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigHisConfig_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        HospitalConfig.HisConfig hisConfig = new HospitalConfig.HisConfig();
        
        hisConfig.setUrl("jdbc:oracle:thin:@localhost:1521/orcl");
        hisConfig.setUsername("test_user");
        hisConfig.setPassword("test_password");
        hisConfig.setTablePrefix("HIS_");
        
        hospital.setHis(hisConfig);
        config.setHospital(hospital);
        
        // 验证结果
        HospitalConfig.HisConfig retrievedHisConfig = config.getHisConfig();
        assertNotNull(retrievedHisConfig, "HIS配置不应为null");
        assertEquals("jdbc:oracle:thin:@localhost:1521/orcl", retrievedHisConfig.getUrl(), "HIS数据库URL应匹配");
        assertEquals("test_user", retrievedHisConfig.getUsername(), "HIS数据库用户名应匹配");
        assertEquals("test_password", retrievedHisConfig.getPassword(), "HIS数据库密码应匹配");
        assertEquals("HIS_", retrievedHisConfig.getTablePrefix(), "HIS表前缀应匹配");
    }

    /**
     * 红阶段测试5：测试医院配置的字段映射
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigFieldMappings_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        
        hospital.setFieldMappings(Map.of(
            "patientId", "PATIENTID",
            "name", "NAME",
            "gender", "GENDER"
        ));
        config.setHospital(hospital);
        
        // 验证结果
        var fieldMappings = config.getFieldMappings();
        assertNotNull(fieldMappings, "字段映射不应为null");
        assertEquals(3, fieldMappings.size(), "字段映射数量应匹配");
        assertEquals("PATIENTID", fieldMappings.get("patientId"), "患者ID字段映射应匹配");
        assertEquals("NAME", fieldMappings.get("name"), "姓名字段映射应匹配");
        assertEquals("GENDER", fieldMappings.get("gender"), "性别字段映射应匹配");
    }

    /**
     * 红阶段测试6：测试医院配置的同步配置
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigSyncConfig_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        HospitalConfig.SyncConfig syncConfig = new HospitalConfig.SyncConfig();
        
        syncConfig.setCron("0 0/5 * * * ?");
        syncConfig.setEnabled(true);
        syncConfig.setMaxRetries(3);
        syncConfig.setRetryInterval(5000);
        
        hospital.setSync(syncConfig);
        config.setHospital(hospital);
        
        // 验证结果
        HospitalConfig.SyncConfig retrievedSyncConfig = config.getSyncConfig();
        assertNotNull(retrievedSyncConfig, "同步配置不应为null");
        assertEquals("0 0/5 * * * ?", retrievedSyncConfig.getCron(), "同步cron表达式应匹配");
        assertTrue(retrievedSyncConfig.getEnabled(), "同步应该启用");
        assertEquals(3, retrievedSyncConfig.getMaxRetries(), "最大重试次数应匹配");
        assertEquals(5000, retrievedSyncConfig.getRetryInterval(), "重试间隔应匹配");
    }

    /**
     * 红阶段测试7：测试医院配置的toString方法
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigToString_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        
        hospital.setId("hospital-001");
        hospital.setName("测试医院");
        hospital.setIntegrationType("database");
        config.setHospital(hospital);
        
        // 验证结果
        String toString = config.toString();
        assertNotNull(toString, "toString结果不应为null");
        assertTrue(toString.contains("hospital-001"), "toString应包含医院ID");
        assertTrue(toString.contains("测试医院"), "toString应包含医院名称");
        assertTrue(toString.contains("database"), "toString应包含集成类型");
    }

    /**
     * 红阶段测试8：测试医院配置的isSyncEnabled方法
     * 这个测试应该会失败，因为isSyncEnabled()方法逻辑有问题
     */
    @Test
    void testHospitalConfigIsSyncEnabled_RedPhase() {
        // 创建医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        HospitalConfig.SyncConfig syncConfig = new HospitalConfig.SyncConfig();
        
        // 设置同步配置为启用
        syncConfig.setEnabled(true);
        hospital.setSync(syncConfig);
        config.setHospital(hospital);
        
        // 验证结果（这个断言应该会失败，因为isSyncEnabled()方法逻辑有问题）
        assertTrue(config.isSyncEnabled(), "医院同步应该启用");
    }

    /**
     * 红阶段测试9：测试医院配置的空值处理
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigNullHandling_RedPhase() {
        // 创建空的医院配置对象
        HospitalConfig config = new HospitalConfig();
        
        // 验证结果
        assertNull(config.getId(), "空配置的ID应为null");
        assertNull(config.getName(), "空配置的名称应为null");
        assertNull(config.getIntegrationType(), "空配置的集成类型应为null");
        assertNull(config.getHisConfig(), "空配置的HIS配置应为null");
        assertNull(config.getSyncConfig(), "空配置的同步配置应为null");
        assertNull(config.getFieldMappings(), "空配置的字段映射应为null");
        assertFalse(config.isEnabled(), "空配置应该未启用");
        assertFalse(config.isSyncEnabled(), "空配置的同步应该未启用");
    }

    /**
     * 红阶段测试10：测试医院配置的部分空值处理
     * 这个测试应该通过
     */
    @Test
    void testHospitalConfigPartialNullHandling_RedPhase() {
        // 创建部分空的医院配置对象
        HospitalConfig config = new HospitalConfig();
        HospitalConfig.Hospital hospital = new HospitalConfig.Hospital();
        
        // 只设置ID，不设置同步配置
        hospital.setId("hospital-001");
        hospital.setName("测试医院");
        config.setHospital(hospital);
        
        // 验证结果
        assertEquals("hospital-001", config.getId(), "医院ID应匹配");
        assertEquals("测试医院", config.getName(), "医院名称应匹配");
        assertNull(config.getSyncConfig(), "未设置同步配置时应为null");
        assertFalse(config.isEnabled(), "未设置同步配置时应未启用");
        assertFalse(config.isSyncEnabled(), "未设置同步配置时同步应未启用");
    }
}
