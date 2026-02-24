package com.example.medaiassistant.hospital.service;

import com.example.medaiassistant.hospital.config.OrderSyncConfig;
import com.example.medaiassistant.hospital.dto.SqlQueryRequest;
import com.example.medaiassistant.hospital.dto.SqlQueryResult;
import com.example.medaiassistant.hospital.model.HospitalConfig;
import com.example.medaiassistant.hospital.model.SqlTemplate;
import com.example.medaiassistant.hospital.util.PatientIdParser;
import com.example.medaiassistant.model.LongTermOrder;
import com.example.medaiassistant.model.Patient;
import com.example.medaiassistant.repository.LongTermOrderRepository;
import com.example.medaiassistant.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 医嘱同步服务
 * 负责从医院HIS系统（Oracle）同步医嘱数据到主服务器数据库
 * 
 * <p><strong>核心功能</strong>：</p>
 * <ul>
 *   <li>从Oracle HIS系统查询医嘱数据（V_SS_ORDERS视图）</li>
 *   <li>字段映射与数据转换（DOCTOR→PHYSICIAN, ORDER_TEXT→ORDERNAME等）</li>
 *   <li>重复记录检测与更新策略（按ORDERID判断）</li>
 *   <li>主服务器独有字段默认值设置（ISANALYZED=0, ISTRIGGERED=0）</li>
 * </ul>
 * 
 * <p><strong>字段映射规范</strong>（SQL别名 → 目标字段）：</p>
 * <ul>
 *   <li>ORDER_ID → orderId（主键）</li>
 *   <li>PATIENT_ID_OLD → patientId</li>
 *   <li>PHYSICIAN → physician（医生）</li>
 *   <li>ORDER_NAME → orderName（医嘱名称）</li>
 *   <li>DOSAGE → dosage（剂量）</li>
 *   <li>UNIT → unit（剂量单位）</li>
 *   <li>FREQUENCY → frequency（频次）</li>
 *   <li>ROUTE → route（给药途径）</li>
 *   <li>ORDER_DATE → orderDate（开始时间）</li>
 *   <li>STOP_TIME → stopTime（停止时间）</li>
 *   <li>VISIT_ID → visitId（就诊ID）</li>
 *   <li>PATIENT_ID → patientIdNew（患者ID新格式）</li>
 * </ul>
 * 
 * @author TDD
 * @version 1.0
 * @since 2026-01-10
 * @see com.example.medaiassistant.model.LongTermOrder
 * @see com.example.medaiassistant.repository.LongTermOrderRepository
 */
@Service
@Slf4j
public class OrderSyncService {
    
    private final SqlExecutionService sqlExecutionService;
    private final LongTermOrderRepository longTermOrderRepository;
    private final PatientRepository patientRepository;
    private final HospitalConfigService hospitalConfigService;
    private final TemplateHotUpdateService templateHotUpdateService;
    private final OrderSyncConfig orderSyncConfig;
    
    /**
     * 构造函数 - 依赖注入
     *
     * @param sqlExecutionService SQL执行服务，用于连接Oracle数据库并执行SQL查询
     * @param longTermOrderRepository 长期医嘱数据访问层，用于保存和查询医嘱记录
     * @param patientRepository 患者数据访问层，用于验证患者ID有效性
     * @param hospitalConfigService 医院配置服务，用于获取当前活动医院ID
     * @param templateHotUpdateService 模板热更新服务，用于动态加载SQL模板
     * @param orderSyncConfig 医嘱同步配置，包含模板路径、查询名称等配置项
     */
    public OrderSyncService(
            SqlExecutionService sqlExecutionService,
            LongTermOrderRepository longTermOrderRepository,
            PatientRepository patientRepository,
            HospitalConfigService hospitalConfigService,
            TemplateHotUpdateService templateHotUpdateService,
            OrderSyncConfig orderSyncConfig) {
        this.sqlExecutionService = sqlExecutionService;
        this.longTermOrderRepository = longTermOrderRepository;
        this.patientRepository = patientRepository;
        this.hospitalConfigService = hospitalConfigService;
        this.templateHotUpdateService = templateHotUpdateService;
        this.orderSyncConfig = orderSyncConfig;
    }
    
    /**
     * 获取患者医嘱的统计信息
     *
     * <p><strong>返回结果包含</strong>：</p>
     * <ul>
     *   <li>sourceOrderCount - Oracle源表中的医嘱记录数</li>
     *   <li>oraclePatientId - Oracle患者ID</li>
     *   <li>orderPatientId - 目标患者ID</li>
     *   <li>statsError - 查询失败时的错误信息</li>
     * </ul>
     *
     * @param mainServerPatientId 主服务器病人ID（格式：990500000178405-1）
     * @return 包含本地记录数和源表记录数的Map，查询失败时sourceOrderCount为-1
     */
    public Map<String, Object> getOrderStats(String mainServerPatientId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            String oraclePatientId = PatientIdParser.parsePatiId(mainServerPatientId);
            String orderPatientId = parseOrderPatientId(mainServerPatientId);
            String visitId = PatientIdParser.parseVisitIdAsString(mainServerPatientId);
            
            // 获取源表V_SS_ORDERS记录数
            long sourceCount = fetchSourceOrderCount(oraclePatientId, visitId);
            stats.put("sourceOrderCount", sourceCount);
            
            stats.put("oraclePatientId", oraclePatientId);
            stats.put("orderPatientId", orderPatientId);
            stats.put("visitId", visitId);
            
        } catch (Exception e) {
            log.error("获取医嘱统计信息失败 - patientId: {}", mainServerPatientId, e);
            stats.put("sourceOrderCount", -1L);
            stats.put("statsError", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 从源表V_SS_ORDERS获取记录数
     *
     * @param oraclePatientId Oracle格式的患者ID
     * @param visitId 就诊标识符
     * @return 记录数量，查询失败时返回-1
     */
    private long fetchSourceOrderCount(String oraclePatientId, String visitId) {
        try {
            List<Map<String, Object>> results = fetchOracleOrders(oraclePatientId, visitId);
            return results.size();
        } catch (Exception e) {
            log.warn("获取源表记录数失败 - oraclePatientId: {}, visitId: {}", oraclePatientId, visitId, e);
            return -1L;
        }
    }
    
    /**
     * 导入医嘱
     * 根据主服务器病人ID从Oracle HIS系统同步医嘱数据
     * 
     * <p><strong>处理流程</strong>：</p>
     * <ol>
     *   <li>参数验证（空值检查）</li>
     *   <li>从主服务器查询病人信息</li>
     *   <li>解析患者ID（Oracle格式和目标格式）</li>
     *   <li>从Oracle HIS系统获取医嘱数据</li>
     *   <li>插入/更新到主服务器数据库</li>
     * </ol>
     * 
     * <p><strong>执行命令</strong>：OrderSyncService.importOrders</p>
     * 
     * @param mainServerPatientId 主服务器病人ID（格式：990500000178405-1）
     * @return 导入的记录数量，-1表示失败，0表示无医嘱记录
     */
    public int importOrders(String mainServerPatientId) {
        long startTime = System.currentTimeMillis();
        log.info("[执行命令] OrderSyncService.importOrders - 开始执行, patientId: {}", mainServerPatientId);
        
        try {
            // 1. 参数验证
            if (mainServerPatientId == null || mainServerPatientId.trim().isEmpty()) {
                log.error("主服务器病人ID不能为空");
                return -1;
            }
            
            log.info("开始导入医嘱 - 主服务器病人ID: {}", mainServerPatientId);
            
            // 2. 从主服务器查询病人信息
            Patient patient = getPatientFromMainServer(mainServerPatientId);
            if (patient == null) {
                log.error("病人未找到 - patientId: {}", mainServerPatientId);
                return -1;
            }
            
            // 3. 解析ID
            String oraclePatientId = PatientIdParser.parsePatiId(mainServerPatientId);
            String orderPatientId = parseOrderPatientId(mainServerPatientId);
            String visitId = PatientIdParser.parseVisitIdAsString(mainServerPatientId);
            
            log.info("解析完成 - Oracle患者ID: {}, Order患者ID: {}, visitId: {}", 
                oraclePatientId, orderPatientId, visitId);
            
            // 4. 从Oracle HIS系统获取医嘱数据
            List<Map<String, Object>> oracleOrders = fetchOracleOrders(oraclePatientId, visitId);
            log.info("从Oracle获取到 {} 条医嘱记录", oracleOrders.size());
            
            if (oracleOrders.isEmpty()) {
                log.info("未找到医嘱数据，导入完成");
                return 0;
            }
            
            // 5. 插入到主服务器数据库
            int importedCount = insertOrdersToMainServer(oracleOrders, orderPatientId);
            
            log.info("[执行命令] OrderSyncService.importOrders - 执行完成, 成功导入 {} 条记录，总耗时: {}ms", 
                importedCount, System.currentTimeMillis() - startTime);
            
            return importedCount;
            
        } catch (Exception e) {
            log.error("[执行命令] OrderSyncService.importOrders - 执行失败, patientId: {}", mainServerPatientId, e);
            return -1;
        }
    }
    
    /**
     * 从Oracle HIS系统获取医嘱数据
     *
     * <p><strong>处理流程</strong>：</p>
     * <ol>
     *   <li>获取活动医院ID</li>
     *   <li>加载SQL模板文件</li>
     *   <li>执行SQL查询</li>
     *   <li>过滤无效记录</li>
     * </ol>
     *
     * <p><strong>查询条件</strong>: patient_id = :patientId AND visit_id = :visitId</p>
     *
     * @param patientId Oracle格式的患者ID
     * @param visitId 就诊标识符
     * @return 医嘱数据列表，每条记录为Map结构
     * @throws RuntimeException 模板加载失败、SQL为空或查询失败时抛出
     */
    private List<Map<String, Object>> fetchOracleOrders(String patientId, String visitId) {
        log.info("从Oracle HIS系统获取医嘱 - 患者ID: {}, 就诊ID: {}", patientId, visitId);
        
        try {
            // 获取活动医院ID
            String hospitalId = getActiveHospitalId();
            
            // 获取模板路径
            String templateFilePath = orderSyncConfig.getTemplateFilePath(hospitalId);
            log.info("使用的SQL模板路径: {}, 医院ID: {}", templateFilePath, hospitalId);
            
            // 加载模板
            SqlTemplate template = templateHotUpdateService.loadTemplate(templateFilePath);
            if (template == null) {
                log.error("模板加载失败 - 医院: {}, 文件路径: {}", hospitalId, templateFilePath);
                throw new RuntimeException("模板加载失败 - 文件路径: " + templateFilePath);
            }
            
            // 获取SQL语句
            String sql = template.getEffectiveSql();
            if (sql == null || sql.trim().isEmpty()) {
                log.error("SQL语句为空 - 医院: {}", hospitalId);
                throw new RuntimeException("SQL语句为空 - 医院: " + hospitalId);
            }
            
            // 创建SQL查询请求
            SqlQueryRequest request = new SqlQueryRequest();
            request.setSql(sql);
            request.setDatabaseType("his");
            request.setParameters(Map.of(
                "patientId", patientId,
                "visitId", visitId
            ));
            request.setMaxRows(2000);
            request.setTimeoutSeconds(60);
            
            // 执行查询
            SqlQueryResult queryResult = sqlExecutionService.executeQuery(hospitalId, request);
            
            if (queryResult.isSuccess()) {
                List<Map<String, Object>> data = queryResult.getData();
                log.info("查询到的医嘱数据量: {} 条记录", data.size());
                
                // 数据过滤：移除无效记录
                List<Map<String, Object>> validData = new ArrayList<>();
                for (Map<String, Object> record : data) {
                    if (isValidOrderRecord(record)) {
                        validData.add(record);
                    }
                }
                
                if (validData.size() < data.size()) {
                    log.warn("数据过滤: 共{}条记录，跳过{}条无效记录", 
                        data.size(), data.size() - validData.size());
                }
                
                return validData;
            } else {
                log.error("获取Oracle医嘱失败 - 错误: {}", queryResult.getErrorMessage());
                throw new RuntimeException("获取Oracle医嘱失败: " + queryResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("获取Oracle医嘱异常 - 患者ID: {}, 就诊ID: {}", patientId, visitId, e);
            throw new RuntimeException("获取Oracle医嘱异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将医嘱数据插入到主服务器数据库
     *
     * <p><strong>处理逻辑</strong>：检查重复，重复则跳过，不重复则插入新记录</p>
     * <p><strong>判重条件</strong>：PatientID + OrderName + OrderDate + RepeatIndicator + StopTime is Null</p>
     *
     * @param oracleResults 从Oracle查询的医嘱数据列表
     * @param targetPatientId 目标患者ID（将写入数据库的patientId字段）
     * @return 处理的记录数量（仅新增，不含跳过的重复记录）
     * @throws RuntimeException 数据库操作失败时抛出
     */
    @Transactional(transactionManager = "transactionManager")
    public int insertOrdersToMainServer(List<Map<String, Object>> oracleResults, String targetPatientId) {
        int processedCount = 0;
        int insertedCount = 0;
        int skippedCount = 0;
        
        try {
            for (Map<String, Object> record : oracleResults) {
                String orderName = getStringValue(record, "ORDER_NAME");
                java.sql.Timestamp orderDate = convertToTimestamp(record.get("ORDER_DATE"));
                Integer repeatIndicator = getIntegerValue(record, "REPEAT_INDICATOR");
                
                if (orderName.isEmpty()) {
                    log.warn("跳过无效记录：医嘱名称为空");
                    skippedCount++;
                    continue;
                }
                
                // 重复判断：PatientID + OrderName + OrderDate + RepeatIndicator + StopTime is Null
                // 已停用的医嘱不视为重复，允许同一患者在不同时间有相同医嘱
                // 临时医嘱(repeatIndicator=0)和长期医嘱(repeatIndicator=1)分别判重
                // 重复则跳过，不重复则插入
                boolean exists = longTermOrderRepository.existsActiveOrder(
                    targetPatientId, orderName, orderDate, repeatIndicator);
                
                if (exists) {
                    String orderType = (repeatIndicator != null && repeatIndicator == 0) ? "临时医嘱" : "长期医嘱";
                    log.debug("跳过重复{} - PatientID: {}, OrderName: {}, OrderDate: {}", 
                        orderType, targetPatientId, orderName, orderDate);
                    skippedCount++;
                    continue;
                }
                
                // 不存在则插入新医嘱
                LongTermOrder order = convertToLongTermOrder(record, targetPatientId);
                longTermOrderRepository.save(order);
                insertedCount++;
                
                String orderType = (repeatIndicator != null && repeatIndicator == 0) ? "临时医嘱" : "长期医嘱";
                log.debug("插入新{}记录 - PatientID: {}, OrderName: {}", 
                    orderType, targetPatientId, orderName);
                
                processedCount++;
            }
            
            log.info("主服务器数据库操作完成 - 新增: {} 条, 跳过重复: {} 条", 
                insertedCount, skippedCount);
            
            return processedCount;
            
        } catch (Exception e) {
            log.error("插入主服务器数据库失败", e);
            throw e;
        }
    }
    
    /**
     * 将Oracle数据转换为LongTermOrder实体
     * 
     * <p>根据字段映射规范，将Oracle查询结果转换为实体。</p>
     * 
     * @param oracleData Oracle查询结果的Map数据，字段名为SQL别名
     * @param targetPatientId 目标患者ID
     * @return 转换后的LongTermOrder实体
     */
    public LongTermOrder convertToLongTermOrder(Map<String, Object> oracleData, String targetPatientId) {
        LongTermOrder order = new LongTermOrder();
        
        // ORDERID由数据库触发器自动生成，不需要设置
        
        // 设置患者ID
        order.setPatientId(targetPatientId);
        
        // 字段映射
        order.setRepeatIndicator(getIntegerValue(oracleData, "REPEAT_INDICATOR"));
        order.setPhysician(getStringValue(oracleData, "PHYSICIAN"));
        order.setOrderName(getStringValue(oracleData, "ORDER_NAME"));
        order.setDosage(getStringValue(oracleData, "DOSAGE"));
        order.setUnit(getStringValue(oracleData, "UNIT"));
        order.setFrequency(getStringValue(oracleData, "FREQUENCY"));
        order.setRoute(getStringValue(oracleData, "ROUTE"));
        
        // 时间字段转换
        order.setOrderDate(convertToTimestamp(oracleData.get("ORDER_DATE")));
        order.setStopTime(convertToTimestamp(oracleData.get("STOP_TIME")));
        
        // 就诊ID
        order.setVisitId(getLongValue(oracleData, "VISIT_ID"));
        
        // 默认值设置 - 新记录ISANALYZED=0, ISTRIGGERED=0
        order.setIsAnalyzed(0);
        order.setIsTriggered(0);
        
        log.debug("转换LongTermOrder完成 - PatientID: {}, OrderName: {}", 
            targetPatientId, order.getOrderName());
        
        return order;
    }
    
    
    /**
     * 验证医嘱记录是否有效
     *
     * <p><strong>验证规则</strong>：</p>
     * <ul>
     *   <li>必须包含非空的PATIENT_ID</li>
     * </ul>
     *
     * @param record 待验证的医嘱记录Map
     * @return true表示记录有效，false表示无效需跳过
     */
    private boolean isValidOrderRecord(Map<String, Object> record) {
        // 必须有患者ID
        Object patientId = record.get("PATIENT_ID");
        if (patientId == null || patientId.toString().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 从主服务器查询病人信息
     *
     * @param patientId 主服务器病人ID
     * @return 病人实体，未找到或查询失败时返回null
     */
    private Patient getPatientFromMainServer(String patientId) {
        try {
            log.info("查询病人信息 - patientId: {}", patientId);
            Patient patient = patientRepository.findByPatientId(patientId);
            if (patient == null) {
                log.warn("病人未找到 - patientId: {}", patientId);
            }
            return patient;
        } catch (Exception e) {
            log.error("查询病人信息失败 - patientId: {}", patientId, e);
            return null;
        }
    }
    
    /**
     * 获取当前活动的医院ID
     *
     * <p>优先从HospitalConfigService获取，若未加载则使用默认值</p>
     *
     * @return 活动医院ID，如"hospital-Local"
     */
    private String getActiveHospitalId() {
        List<HospitalConfig> configs = hospitalConfigService.getAllConfigs();
        if (configs != null && !configs.isEmpty()) {
            String activeId = configs.get(0).getId();
            log.info("从HospitalConfigService获取活动医院ID: {}", activeId);
            return activeId;
        }
        
        String defaultId = orderSyncConfig.getDefaultHospitalId();
        log.warn("HospitalConfigService未加载任何配置，使用默认医院ID: {}", defaultId);
        return defaultId;
    }
    
    // parseOraclePatientId 已替换为 PatientIdParser.parsePatiId() 工具方法
    
    /**
     * 解析目标患者ID
     *
     * <p>数据库中病人ID和医嘱表中病人ID格式一致，无需转换</p>
     *
     * @param mainServerPatientId 主服务器病人ID
     * @return 目标格式的患者ID
     */
    private String parseOrderPatientId(String mainServerPatientId) {
        return mainServerPatientId;
    }
    
    // parseVisitId 已替换为 PatientIdParser.parseVisitIdAsString() 工具方法
    
    /**
     * 从数据记录Map中获取字符串值
     *
     * @param map 数据记录Map
     * @param key 字段名称
     * @return 字符串值，null或空字符串时返回空字符串
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            return "";
        }
        return value.toString().trim();
    }
    
    /**
     * 从数据记录Map中获取Long值
     *
     * @param map 数据记录Map
     * @param key 字段名称
     * @return Long值，无法解析时返回null
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 从数据记录Map中获取Integer值
     *
     * @param map 数据记录Map
     * @param key 字段名称
     * @return Integer值，无法解析时返回null
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 转换为Timestamp类型
     *
     * <p><strong>支持的格式</strong>：</p>
     * <ul>
     *   <li>java.sql.Timestamp</li>
     *   <li>java.util.Date</li>
     *   <li>字符串格式：yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss, yyyy-MM-dd, yyyy/MM/dd</li>
     * </ul>
     *
     * @param value 待转换的值
     * @return Timestamp对象，无法转换时返回null
     */
    private Timestamp convertToTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Timestamp) {
                return (Timestamp) value;
            } else if (value instanceof Date) {
                return new Timestamp(((Date) value).getTime());
            } else if (value instanceof String) {
                String strValue = value.toString().trim();
                SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd"),
                    new SimpleDateFormat("yyyy/MM/dd")
                };
                
                for (SimpleDateFormat format : formats) {
                    try {
                        Date date = format.parse(strValue);
                        return new Timestamp(date.getTime());
                    } catch (Exception e) {
                        continue;
                    }
                }
                
                log.warn("日期时间格式无法解析: {}", strValue);
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("日期时间转换失败: {}", value, e);
            return null;
        }
    }
}
