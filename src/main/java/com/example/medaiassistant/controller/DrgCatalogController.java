package com.example.medaiassistant.controller;

import com.example.medaiassistant.drg.catalog.DrgCatalog;
import com.example.medaiassistant.drg.catalog.DrgCatalogLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DRG目录管理控制器
 * 
 * 提供REST接口用于查看和管理DrgCatalog
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-23
 */
@Profile("!execution")
@RestController
@RequestMapping("/api/drg/catalog")
public class DrgCatalogController {

    private final DrgCatalogLoader drgCatalogLoader;

    @Autowired
    public DrgCatalogController(DrgCatalogLoader drgCatalogLoader) {
        this.drgCatalogLoader = drgCatalogLoader;
    }

    /**
     * 获取当前目录信息
     * 
     * @return 包含版本、记录数量和最后更新时间的目录信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCatalogInfo() {
        DrgCatalog currentCatalog = drgCatalogLoader.getCurrentCatalog();
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", drgCatalogLoader.getVersion());
        response.put("recordCount", currentCatalog != null ? currentCatalog.getRecordCount() : 0);
        response.put("lastUpdated", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 重新加载目录
     * 
     * @return 包含刷新结果和新版本信息的响应
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadCatalog() {
        DrgCatalog newCatalog = drgCatalogLoader.reload();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("newVersion", newCatalog.getVersion());
        response.put("message", "目录刷新成功");
        
        return ResponseEntity.ok(response);
    }
}
