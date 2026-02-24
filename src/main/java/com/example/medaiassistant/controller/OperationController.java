package com.example.medaiassistant.controller;

import com.example.medaiassistant.service.EmrRecordService;
import com.example.medaiassistant.service.SurgeryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 手术记录控制器
 * 提供手术记录相关的API接口
 */
@RestController
@RequestMapping("/api/operations")
public class OperationController {

    @Autowired
    private EmrRecordService emrRecordService;

    @SuppressWarnings("unused")
    @Autowired
    private SurgeryService surgeryService;

    /**
     * 查找手术记录数据
     * 入口函数：FindOperations(patientPATI_ID)
     * 
     * @param patientId 病人ID，格式为字符串，如"99050801275226_1"
     * @return 合并后的手术记录内容，如果未找到记录则返回"未找到该患者的手术记录"
     * @apiNote 通过EmrRecordService从EMR_RECORD表获取手术记录，使用DOC_TYPE_NAME过滤手术相关文档类型，
     *          使用StringBuilder合并所有手术记录的"记录内容"
     * @example GET /api/operations/find?patientId=99050801275226_1
     * @since 1.0.0
     */
    @GetMapping("/find")
    public String findOperations(@RequestParam String patientId) {
        
        String operationContent = emrRecordService.findOperations(patientId, "");
        
        if (operationContent.isEmpty()) {
            return "未找到该患者的手术记录";
        }
        
        return operationContent;
    }
}
