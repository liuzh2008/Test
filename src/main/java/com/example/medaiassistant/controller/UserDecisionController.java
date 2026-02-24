package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.UserDecisionRequest;
import com.example.medaiassistant.model.DrgAnalysisResult;
import com.example.medaiassistant.service.UserDecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * UserDecision用户决策控制器
 * 提供用户选择MCC类别的REST API接口
 * 
 * @author MedAI Assistant Team
 * @since 2025-11-11
 */
@RestController
@RequestMapping("/api/user-decision")
public class UserDecisionController {

    private final UserDecisionService userDecisionService;

    /**
     * 构造函数注入
     * 
     * @param userDecisionService 用户决策服务
     */
    @Autowired
    public UserDecisionController(UserDecisionService userDecisionService) {
        this.userDecisionService = userDecisionService;
    }

    /**
     * 用户选择MCC类别并保存分析结果
     * 
     * @param request 用户决策请求
     * @return 保存后的分析结果
     * @apiNote POST /api/user-decision/save
     * @apiExample 请求示例：
     * {
     *   "resultId": 1,
     *   "patientId": "PAT001",
     *   "selectedMccType": "MCC",
     *   "operator": "test-user"
     * }
     */
    @PostMapping("/save")
    public Mono<ResponseEntity<DrgAnalysisResult>> saveUserDecision(@RequestBody UserDecisionRequest request) {
        // 验证请求参数
        if (request == null || request.getResultId() == null || request.getSelectedMccType() == null || 
            request.getOperator() == null || request.getOperator().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        try {
            DrgAnalysisResult result = userDecisionService.saveUserDecision(request);
            return Mono.just(ResponseEntity.ok(result));
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * 根据分析结果ID查询决策结果
     * 
     * @param resultId 分析结果ID
     * @return 分析结果
     * @apiNote GET /api/user-decision/{resultId}
     */
    @GetMapping("/{resultId}")
    public Mono<ResponseEntity<DrgAnalysisResult>> getDecisionResult(@PathVariable Long resultId) {
        try {
            DrgAnalysisResult result = userDecisionService.getDecisionResult(resultId);
            return Mono.just(ResponseEntity.ok(result));
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * 根据患者ID查询决策结果
     * 
     * @param patientId 患者ID
     * @return 分析结果列表
     * @apiNote GET /api/user-decision/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public Mono<ResponseEntity<List<DrgAnalysisResult>>> getDecisionResultsByPatientId(@PathVariable String patientId) {
        try {
            List<DrgAnalysisResult> results = userDecisionService.getDecisionResultsByPatientId(patientId);
            return Mono.just(ResponseEntity.ok(results));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * 健康检查端点
     * 
     * @return 健康状态
     * @apiNote GET /api/user-decision/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("UserDecision API is healthy"));
    }
}
