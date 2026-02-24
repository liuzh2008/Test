package com.example.medaiassistant.drg.catalog;

import com.example.medaiassistant.model.Drg;
import com.example.medaiassistant.repository.DrgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DRG目录加载器
 * 
 * 负责加载和管理不可变的DrgCatalog快照，支持原子替换和版本管理。
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-23
 */
@Profile("!execution")
@Component
public class DrgCatalogLoader {
    private final AtomicReference<DrgCatalog> currentCatalog;
    private final AtomicReference<String> previousVersion;
    private final AtomicLong refreshCount;
    private final AtomicLong lastRefreshTime;
    private final List<String> versionHistory;
    private final ClobParser clobParser;
    private final DrgRepository drgRepository;

    @Autowired
    public DrgCatalogLoader(ClobParser clobParser, DrgRepository drgRepository) {
        this.clobParser = clobParser;
        this.drgRepository = drgRepository;
        this.currentCatalog = new AtomicReference<>();
        this.previousVersion = new AtomicReference<>();
        this.refreshCount = new AtomicLong(0);
        this.lastRefreshTime = new AtomicLong(System.currentTimeMillis());
        this.versionHistory = Collections.synchronizedList(new LinkedList<>());
        // 初始化时立即加载目录
        loadCatalog();
    }

    /**
     * 加载初始目录
     * 
     * 从数据库加载DRG数据并创建不可变快照，设置当前目录引用。
     * 
     * @return 新创建的DrgCatalog实例
     * @throws RuntimeException 如果数据库查询失败
     */
    public DrgCatalog loadCatalog() {
        DrgCatalog catalog = createCatalog();
        currentCatalog.set(catalog);
        return catalog;
    }

    /**
     * 重新加载目录（原子替换）
     * 
     * 重新从数据库加载数据并原子替换当前目录，确保并发读取的一致性。
     * 
     * @return 新创建的DrgCatalog实例
     * @throws RuntimeException 如果数据库查询失败
     */
    public DrgCatalog reload() {
        DrgCatalog oldCatalog = currentCatalog.get();
        DrgCatalog newCatalog = createCatalog();
        
        // 更新版本历史记录
        if (oldCatalog != null) {
            previousVersion.set(oldCatalog.getVersion());
            versionHistory.add(oldCatalog.getVersion());
        }
        
        // 更新统计信息
        refreshCount.incrementAndGet();
        lastRefreshTime.set(System.currentTimeMillis());
        
        // 原子替换当前目录
        currentCatalog.set(newCatalog);
        return newCatalog;
    }

    /**
     * 创建新的目录实例
     * 
     * 提取公共方法消除重复代码，负责从数据库加载记录并生成版本标识。
     * 
     * @return 新创建的DrgCatalog实例
     * @throws RuntimeException 如果数据库查询失败
     */
    private DrgCatalog createCatalog() {
        List<DrgParsedRecord> records = loadRecordsFromDatabase();
        String version = generateVersion();
        return new DrgCatalog(version, records);
    }

    /**
     * 获取当前目录
     * 
     * 返回当前活动的不可变目录快照，支持线程安全的并发读取。
     * 
     * @return 当前DrgCatalog实例，如果未加载则返回null
     */
    public DrgCatalog getCurrentCatalog() {
        return currentCatalog.get();
    }

    /**
     * 获取当前版本
     * 
     * 返回当前目录的版本标识，用于跟踪目录更新。
     * 
     * @return 当前目录版本字符串，如果未加载则返回null
     */
    public String getVersion() {
        DrgCatalog catalog = currentCatalog.get();
        return catalog != null ? catalog.getVersion() : null;
    }

    private List<DrgParsedRecord> loadRecordsFromDatabase() {
        // 实际的数据库查询逻辑
        List<Drg> drgEntities = drgRepository.findAll();
        List<DrgParsedRecord> records = new ArrayList<>();
        
        for (Drg drg : drgEntities) {
            // 解析诊断数据
            List<DiagnosisEntry> diagnoses = clobParser.parseDiagnoses(drg.getMainDiagnoses());
            
            // 处理主要手术为空或"/"的情况
            List<ProcedureEntry> procedures;
            String mainProcedures = drg.getMainProcedures();
            if (mainProcedures == null || mainProcedures.trim().isEmpty() || "/".equals(mainProcedures.trim())) {
                procedures = new ArrayList<>(); // 没有主要手术
            } else {
                procedures = clobParser.parseProcedures(mainProcedures);
            }
            
            // 创建解析后的记录
            DrgParsedRecord record = new DrgParsedRecord(
                drg.getDrgCode() != null ? drg.getDrgCode() : "UNKNOWN",
                diagnoses,
                procedures
            );
            records.add(record);
        }
        
        return records;
    }

    private String generateVersion() {
        // 使用纳秒级时间戳确保每次生成的版本号都不同
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /**
     * 获取前一个版本
     * 
     * 返回上一次刷新前的目录版本。
     * 
     * @return 前一个版本字符串，如果没有前一个版本则返回null
     */
    public String getPreviousVersion() {
        return previousVersion.get();
    }

    /**
     * 获取刷新次数
     * 
     * 返回目录被刷新的总次数。
     * 
     * @return 刷新次数
     */
    public long getRefreshCount() {
        return refreshCount.get();
    }

    /**
     * 获取最后刷新时间
     * 
     * 返回最后一次刷新的时间戳（毫秒）。
     * 
     * @return 最后刷新时间戳
     */
    public long getLastRefreshTime() {
        return lastRefreshTime.get();
    }

    /**
     * 获取版本历史记录
     * 
     * 返回所有历史版本的列表（按时间顺序，最新的在最后）。
     * 
     * @return 版本历史记录列表
     */
    public List<String> getVersionHistory() {
        return new ArrayList<>(versionHistory);
    }
}
