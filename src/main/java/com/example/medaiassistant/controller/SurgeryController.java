package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.SurgeryTaskDTO;
import com.example.medaiassistant.model.Surgery;
import com.example.medaiassistant.model.SurgeryTask;
import com.example.medaiassistant.service.SurgeryService;
import com.example.medaiassistant.service.SurgeryTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手术控制器
 * 提供手术相关操作的API接口
 */
@RestController
@RequestMapping("/api/surgeries")
public class SurgeryController {

    @Autowired
    private SurgeryService surgeryService;

    @Autowired
    private SurgeryTaskService surgeryTaskService;

    @GetMapping
    public List<Surgery> getAllSurgeries() {
        return surgeryService.getAllSurgeries();
    }

    @GetMapping("/by-patient/{patientId}")
    public List<Surgery> getSurgeriesByPatientId(@PathVariable String patientId) {
        return surgeryService.getSurgeriesByPatientId(patientId);
    }

    /**
     * 根据患者ID获取手术任务列表
     * @param patientId 患者ID
     * @return 手术任务DTO列表
     */
    @GetMapping("/tasks/by-patient/{patientId}")
    public List<SurgeryTaskDTO> getSurgeryTasksByPatientId(@PathVariable String patientId) {
        return surgeryTaskService.getSurgeryTasksByPatientId(patientId);
    }

    /**
     * 根据患者ID和状态获取手术任务列表
     * @param patientId 患者ID
     * @param status 任务状态
     * @return 手术任务DTO列表
     */
    @GetMapping("/tasks/by-patient/{patientId}/status/{status}")
    public List<SurgeryTaskDTO> getSurgeryTasksByPatientIdAndStatus(
            @PathVariable String patientId,
            @PathVariable SurgeryTask.TaskStatus status) {
        return surgeryTaskService.getSurgeryTasksByPatientIdAndStatus(patientId, status);
    }

    /**
     * 获取所有手术任务
     * @return 手术任务DTO列表
     */
    @GetMapping("/tasks")
    public List<SurgeryTaskDTO> getAllSurgeryTasks() {
        return surgeryTaskService.getAllSurgeryTasks();
    }

    /**
     * 创建手术任务
     * @param surgeryTaskDTO 手术任务DTO对象
     * @return 创建后的手术任务DTO对象
     */
    @PostMapping("/tasks")
    public SurgeryTaskDTO createSurgeryTask(@RequestBody SurgeryTaskDTO surgeryTaskDTO) {
        return surgeryTaskService.createSurgeryTask(surgeryTaskDTO);
    }

    /**
     * 更新手术任务信息
     * @api {PUT} /api/surgeries/tasks/{taskId} 更新手术任务
     * @apiName UpdateSurgeryTask
     * @apiGroup SurgeryTasks
     * @apiVersion 1.0.0
     * 
     * @apiParam {Number} taskId 手术任务ID（路径参数）
     * @apiParam {Object} surgeryTaskDTO 手术任务数据对象（请求体）
     * @apiParam {String} surgeryTaskDTO.surgeryDate 手术日期（格式：yyyy-MM-dd）
     * @apiParam {String} surgeryTaskDTO.surgeryName 手术名称
     * @apiParam {String} surgeryTaskDTO.anesthesiaType 麻醉类型
     * @apiParam {String} surgeryTaskDTO.preopDiscussionHost 术前讨论主持人
     * @apiParam {String} surgeryTaskDTO.preopDiscussionParticipants 术前讨论参与者
     * @apiParam {String} surgeryTaskDTO.surgeryRiskAssessment 手术风险评估
     * @apiParam {String} surgeryTaskDTO.completedTasks 已完成任务（JSON数组字符串）
     * @apiParam {String} surgeryTaskDTO.patientId 患者ID
     * @apiParam {Number} surgeryTaskDTO.departmentId 科室ID
     * @apiParam {Number} surgeryTaskDTO.surgeonId 医生ID
     * @apiParam {String} surgeryTaskDTO.taskStatus 任务状态（计划中/进行中/已完成/已取消）
     * 
     * @apiSuccess {Object} SurgeryTaskDTO 更新后的手术任务对象
     * @apiSuccess {Number} SurgeryTaskDTO.taskId 任务ID
     * @apiSuccess {String} SurgeryTaskDTO.surgeryDate 手术日期
     * @apiSuccess {String} SurgeryTaskDTO.surgeryName 手术名称
     * @apiSuccess {String} SurgeryTaskDTO.anesthesiaType 麻醉类型
     * @apiSuccess {String} SurgeryTaskDTO.preopDiscussionHost 术前讨论主持人
     * @apiSuccess {String} SurgeryTaskDTO.preopDiscussionParticipants 术前讨论参与者
     * @apiSuccess {String} SurgeryTaskDTO.surgeryRiskAssessment 手术风险评估
     * @apiSuccess {String} SurgeryTaskDTO.completedTasks 已完成任务
     * @apiSuccess {String} SurgeryTaskDTO.patientId 患者ID
     * @apiSuccess {Number} SurgeryTaskDTO.departmentId 科室ID
     * @apiSuccess {Number} SurgeryTaskDTO.surgeonId 医生ID
     * @apiSuccess {String} SurgeryTaskDTO.taskStatus 任务状态
     * @apiSuccess {Date} SurgeryTaskDTO.createdAt 创建时间
     * @apiSuccess {Date} SurgeryTaskDTO.updatedAt 更新时间
     * 
     * @apiSuccessExample {json} 成功响应:
     * HTTP/1.1 200 OK
     * {
     *   "taskId": 1,
     *   "surgeryDate": "2025-08-22",
     *   "surgeryName": "阑尾切除术（更新）",
     *   "anesthesiaType": "局部麻醉",
     *   "preopDiscussionHost": "李医生",
     *   "preopDiscussionParticipants": "张医生,王医生,赵护士,钱护士",
     *   "surgeryRiskAssessment": "中等风险，需要注意患者过敏史",
     *   "completedTasks": "[\"术前检查\", \"麻醉评估\"]",
     *   "patientId": "P12345",
     *   "departmentId": 1,
     *   "surgeonId": 102,
     *   "taskStatus": "进行中",
     *   "createdAt": "2025-08-20T10:30:00.000+00:00",
     *   "updatedAt": "2025-08-20T15:30:00.000+00:00"
     * }
     * 
     * @apiErrorExample {json} 任务不存在:
     * HTTP/1.1 200 OK
     * null
     */
    @PutMapping("/tasks/{taskId}")
    public SurgeryTaskDTO updateSurgeryTask(
            @PathVariable Integer taskId,
            @RequestBody SurgeryTaskDTO surgeryTaskDTO) {
        return surgeryTaskService.updateSurgeryTask(taskId, surgeryTaskDTO);
    }

    /**
     * 根据任务ID获取手术任务详情
     * @api {GET} /api/surgeries/tasks/{taskId} 获取手术任务详情
     * @apiName GetSurgeryTaskById
     * @apiGroup SurgeryTasks
     * @apiVersion 1.0.0
     * 
     * @apiParam {Number} taskId 手术任务ID（路径参数）
     * 
     * @apiSuccess {Object} SurgeryTaskDTO 手术任务对象
     * @apiSuccess {Number} SurgeryTaskDTO.taskId 任务ID
     * @apiSuccess {String} SurgeryTaskDTO.surgeryDate 手术日期
     * @apiSuccess {String} SurgeryTaskDTO.surgeryName 手术名称
     * @apiSuccess {String} SurgeryTaskDTO.anesthesiaType 麻醉类型
     * @apiSuccess {String} SurgeryTaskDTO.preopDiscussionHost 术前讨论主持人
     * @apiSuccess {String} SurgeryTaskDTO.preopDiscussionParticipants 术前讨论参与者
     * @apiSuccess {String} SurgeryTaskDTO.surgeryRiskAssessment 手术风险评估
     * @apiSuccess {String} SurgeryTaskDTO.completedTasks 已完成任务
     * @apiSuccess {String} SurgeryTaskDTO.patientId 患者ID
     * @apiSuccess {Number} SurgeryTaskDTO.departmentId 科室ID
     * @apiSuccess {Number} SurgeryTaskDTO.surgeonId 医生ID
     * @apiSuccess {String} SurgeryTaskDTO.taskStatus 任务状态
     * @apiSuccess {Date} SurgeryTaskDTO.createdAt 创建时间
     * @apiSuccess {Date} SurgeryTaskDTO.updatedAt 更新时间
     * 
     * @apiSuccessExample {json} 成功响应:
     * HTTP/1.1 200 OK
     * {
     *   "taskId": 1,
     *   "surgeryDate": "2025-08-21",
     *   "surgeryName": "阑尾切除术",
     *   "anesthesiaType": "全身麻醉",
     *   "preopDiscussionHost": "张医生",
     *   "preopDiscussionParticipants": "李医生,王医生,赵护士",
     *   "surgeryRiskAssessment": "低风险，患者身体状况良好",
     *   "completedTasks": "[]",
     *   "patientId": "P12345",
     *   "departmentId": 1,
     *   "surgeonId": 101,
     *   "taskStatus": "计划中",
     *   "createdAt": "2025-08-20T10:30:00.000+00:00",
     *   "updatedAt": "2025-08-20T10:30:00.000+00:00"
     * }
     * 
     * @apiErrorExample {json} 任务不存在:
     * HTTP/1.1 200 OK
     * null
     */
    @GetMapping("/tasks/{taskId}")
    public SurgeryTaskDTO getSurgeryTaskById(@PathVariable Integer taskId) {
        return surgeryTaskService.getSurgeryTaskById(taskId);
    }

    /**
     * 删除手术任务
     * @api {DELETE} /api/surgeries/tasks/{taskId} 删除手术任务
     * @apiName DeleteSurgeryTask
     * @apiGroup SurgeryTasks
     * @apiVersion 1.0.0
     * 
     * @apiParam {Number} taskId 手术任务ID（路径参数）
     * 
     * @apiSuccess {Boolean} success 删除结果
     * 
     * @apiSuccessExample {json} 删除成功:
     * HTTP/1.1 200 OK
     * true
     * 
     * @apiSuccessExample {json} 任务不存在:
     * HTTP/1.1 200 OK
     * false
     */
    @DeleteMapping("/tasks/{taskId}")
    public boolean deleteSurgeryTask(@PathVariable Integer taskId) {
        return surgeryTaskService.deleteSurgeryTask(taskId);
    }
}
